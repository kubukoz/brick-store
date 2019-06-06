package org.typelevel.brickstore

import java.time.Instant

import cats.data.Chain
import cats.effect.concurrent.{Deferred, Ref}
import cats.effect.implicits._
import cats.effect.{Concurrent, ContextShift, IO, Timer}
import cats.implicits._
import fs2.Stream
import org.scalatest.compatible.Assertion
import org.scalatest.{Matchers, WordSpec}
import org.typelevel.brickstore.orders.{OrderService, OrderServiceImpl, OrderWithLines}

import scala.concurrent.ExecutionContext.global

class OrderServiceStreamingTests extends WordSpec with Matchers {

  import org.typelevel.brickstore.app.util.Now.implicits._

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]               = IO.timer(global)

  def collectToListUntilFinish[F[_]: Concurrent, A](stream: Stream[F, A], finish: Deferred[F, Unit]): F[Chain[A]] =
    Ref.of[F, Chain[A]](Chain.empty).flatMap { storage =>
      (stream.evalMap(elem => storage.update(_.append(elem))).compile.drain race finish.get) *> storage.get
    }

  "streamAll" should {
    "be empty if both components are empty" in {

      val test: IO[Assertion] = Deferred[IO, Unit].flatMap { finishLatch =>
        val orderService: OrderService[IO] = new OrderServiceImpl[IO, IO](
          new CartServiceStub[IO],
          new OrderRepositoryStub[IO] {
            override def streamExisting(before: Instant): Stream[IO, OrderWithLines] = Stream.empty
          },
          new BricksRepositoryStub[IO],
          _ => IO.unit,
          Stream.eval(finishLatch.complete(())).drain
        )

        collectToListUntilFinish(orderService.streamAll, finishLatch).map {
          _ shouldBe Chain.empty
        }
      }

      test.unsafeRunSync()
    }
  }
}
