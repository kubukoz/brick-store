package org.typelevel.brickstore

import java.time.{Duration, Instant, LocalDateTime, Month, ZoneOffset}

import cats.data.NonEmptyList
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, IO, Sync, Timer}
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}
import org.typelevel.brickstore.app.util.Now
import org.typelevel.brickstore.bricks.{Brick, BrickColor, BrickId, BricksRepository}
import org.typelevel.brickstore.cart.{CartLine, CartService}
import org.typelevel.brickstore.orders.dto.OrderSummary
import org.typelevel.brickstore.orders.{InMemoryOrderRepository, OrderId, OrderService, OrderServiceImpl}
import org.typelevel.brickstore.users.UserId
import fs2.Stream
import scala.concurrent.ExecutionContext.global

class OrderServiceImplTests extends WordSpec with Matchers {
  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]               = IO.timer(global)

  "placeOrder" when {
    "the cart is empty" should {
      "not create an order" in {
        implicit val now: Now[IO] = Now.fromClock

        val mockCartService: CartService[IO] = new CartServiceStub[IO] {
          override def findLines(auth: UserId): IO[Option[NonEmptyList[CartLine]]] = IO.pure(None)
        }

        val userId = UserId(1)

        val program = for {
          publishedRef <- Ref[IO].of(0)

          service: OrderService[IO] = new OrderServiceImpl[IO, IO](mockCartService,
                                                                   new OrderRepositoryStub[IO],
                                                                   null,
                                                                   _ => publishedRef.update(_ + 1),
                                                                   Stream.empty)

          result <- service.placeOrder(userId)

          publishedSummaryCount <- publishedRef.get
        } yield {
          result shouldBe empty
          publishedSummaryCount shouldBe 0
        }

        program.unsafeRunSync()
      }
    }

    "the cart isn't empty" should {
      "create a new order with the right totals, publish the summary and clear the cart" in {
        val userId1 = UserId(1)
        val userId2 = UserId(14)

        def mockCartService(cartCleared: Ref[IO, List[UserId]]): CartService[IO] = new CartServiceStub[IO] {
          override def findLines(auth: UserId): IO[Option[NonEmptyList[CartLine]]] =
            auth match {
              case `userId1` =>
                NonEmptyList
                  .of(
                    CartLine(BrickId(1), 10),
                    CartLine(BrickId(3), 1)
                  )
                  .some
                  .pure[IO]
              case `userId2` =>
                NonEmptyList
                  .of(
                    CartLine(BrickId(2), 5),
                    CartLine(BrickId(4), 3)
                  )
                  .some
                  .pure[IO]

              case _ => Stub.apply
            }

          override def clear(auth: UserId): IO[Unit] = cartCleared.update(auth :: _)
        }

        val mockBrickRepository: BricksRepository[IO, IO] = new BricksRepositoryStub[IO] {
          override def findById(id: BrickId): IO[Option[Brick]] =
            Map(
              BrickId(1) -> bricks.Brick("Test brick 1", 100, BrickColor.Blue),
              BrickId(2) -> bricks.Brick("Test brick 2", 150, BrickColor.Black),
              BrickId(3) -> bricks.Brick("Test brick 3", 200, BrickColor.Red),
              BrickId(4) -> bricks.Brick("Test brick 4", 50, BrickColor.Green)
            ).get(id).pure[IO]
        }

        val noon   = LocalDateTime.of(2019, Month.JUNE, 5, 12, 0, 0).toInstant(ZoneOffset.UTC)
        val noon01 = noon.plus(Duration.ofMinutes(1))

        val program = for {
          implicit0(now: Now[IO]) <- NowInstances.addMinute[IO](noon)

          publishedRef <- Ref.of[IO, List[OrderSummary]](Nil)
          cartCleared  <- Ref.of[IO, List[UserId]](Nil)
          ordersRef    <- InMemoryOrderRepository.makeRef[IO]
          publishOrder = (msg: OrderSummary) => publishedRef.update(msg :: _)

          service: OrderService[IO] = new OrderServiceImpl[IO, IO](mockCartService(cartCleared),
                                                                   new InMemoryOrderRepository[IO](ordersRef),
                                                                   mockBrickRepository,
                                                                   publishOrder,
                                                                   Stream.empty)

          (order1Id, order2Id) <- (service.placeOrder(userId1), service.placeOrder(userId2)).parTupled

          publishedSummaries <- publishedRef.get
          clearedCarts       <- cartCleared.get
        } yield {
          //uncertain ordering because orders are made in parallel
          List(order1Id.get, order2Id.get) should contain theSameElementsAs List(OrderId(1), OrderId(2))

          publishedSummaries should contain theSameElementsAs List(
            OrderSummary(order1Id.get, userId1, 1200, noon),
            OrderSummary(order2Id.get, userId2, 900, noon01)
          )

          clearedCarts should contain theSameElementsAs List(userId1, userId2)
        }

        program.unsafeRunSync()
      }
    }
  }
}

object NowInstances {

  def addMinute[F[_]: Sync](startTime: Instant): F[Now[F]] = Ref[F].of(startTime).map { ref =>
    Now.liftF {
      ref.modify { previous =>
        (previous.plus(Duration.ofMinutes(1L)), previous)
      }
    }
  }
}
