package org.typelevel.brickstore

import java.time.Instant

import cats.data.Chain
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.implicits._
import cats.effect.{Concurrent, ContextShift, IO, Timer}
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Queue
import org.scalatest.compatible.Assertion
import org.scalatest.{Matchers, WordSpec}
import org.typelevel.brickstore.app.util.Now
import org.typelevel.brickstore.orders.dto.OrderSummary
import org.typelevel.brickstore.orders.{BrickOrder, OrderId, OrderService, OrderServiceImpl, OrderWithLines}
import org.typelevel.brickstore.users.UserId

import scala.concurrent.ExecutionContext.global

class OrderServiceStreamingTests extends WordSpec with Matchers {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]               = IO.timer(global)

  def collectToListUntilFinish[F[_]: Concurrent, A](stream: Stream[F, A], finish: Deferred[F, Unit]): F[Chain[A]] =
    Ref.of[F, Chain[A]](Chain.empty).flatMap { storage =>
      (stream.evalMap(elem => storage.update(_.append(elem))).compile.drain race finish.get) *> storage.get
    }

  "streamAll" should {
    "be empty if both components are empty" in {
      import org.typelevel.brickstore.app.util.Now.implicits._

      val test: IO[Assertion] = Deferred[IO, Unit].flatMap { finishLatch =>
        val orderService: OrderService[IO] = new OrderServiceImpl[IO, IO](
          new CartServiceStub[IO],
          new OrderRepositoryStub[IO] {
            override def streamExisting(before: Instant): Stream[IO, OrderWithLines] = Stream.empty
          },
          new BricksRepositoryStub[IO],
          _ => IO.unit,
          Stream.eval_(finishLatch.complete(()))
        )

        collectToListUntilFinish(orderService.streamAll, finishLatch).map {
          _ shouldBe Chain.empty
        }
      }

      test.unsafeRunSync()
    }

    "find elements that are produced to a queue after consuming the first streams begins" in {
      implicit val now: Now[IO] = Now.liftF(IO.pure(Instant.EPOCH))

      val sampleOrder  = OrderSummary(OrderId(1L), UserId(1L), 0, Instant.EPOCH)
      val sampleOrderL = OrderWithLines(BrickOrder(OrderId(1L), UserId(1L), Instant.EPOCH), List())

      val test: IO[Assertion] = Deferred[IO, Unit].flatMap { finishLatch =>
        Deferred[IO, Unit].flatMap { startedConsumingExisting =>
          Queue.bounded[IO, OrderSummary](100).flatMap { q =>
            val existing = Stream.eval_(startedConsumingExisting.complete(())) ++ Stream
              .emit(sampleOrderL)
              .combineN(10)

            val orderService: OrderService[IO] = new OrderServiceImpl[IO, IO](
              new CartServiceStub[IO],
              new OrderRepositoryStub[IO] {
                override def streamExisting(before: Instant): Stream[IO, OrderWithLines] = existing
              },
              new BricksRepositoryStub[IO],
              q.enqueue1,
              Stream.eval_(startedConsumingExisting.get) ++ q.dequeue ++ Stream.eval_(finishLatch.complete(()))
            )

            collectToListUntilFinish(orderService.streamAll, finishLatch).map {
              _ shouldBe Chain(sampleOrder).combineN(10)
            }
          }
        }
      }

      test.unsafeRunSync()
    }
  }
}
