package org.typelevel.brickstore

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._
import fs2.async.Ref
import org.scalatest.{Matchers, WordSpec}
import org.typelevel.brickstore.dto.OrderSummary
import org.typelevel.brickstore.entity._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class OrderServiceImplTests extends WordSpec with Matchers {
  "placeOrder" when {
    "the cart is empty" should {
      "not create an order" in {

        val mockCartService: CartService[IO] = new CartServiceStub[IO] {
          override def findLines(auth: UserId): IO[Option[NonEmptyList[CartLine]]] = IO.pure(None)
        }

        val userId = UserId(1)

        val program = for {
          publishedRef <- Ref[IO, Int](0)

          service: OrderService[IO] = new OrderServiceImpl[IO, IO](mockCartService,
                                                                   new OrderRepositoryStub[IO],
                                                                   null,
                                                                   _ => publishedRef.modify(_ + 1).void)

          result <- service.placeOrder(userId)

          //todo when cats-effect 1.x is released, replace with test clock tick
          _                     <- IO.sleep(200.millis)
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

        val sleepRandom: IO[Unit] = IO(scala.util.Random.nextInt(2)).map(_.millis * 200) >>= IO.sleep

        def mockCartService(cartCleared: Ref[IO, List[UserId]]): CartService[IO] = new CartServiceStub[IO] {
          override def findLines(auth: UserId): IO[Option[NonEmptyList[CartLine]]] = {
            val lines = auth match {
              case `userId1` =>
                NonEmptyList
                  .of(
                    CartLine(BrickId(1), 10),
                    CartLine(BrickId(3), 1)
                  )
                  .some
              case `userId2` =>
                NonEmptyList
                  .of(
                    CartLine(BrickId(2), 5),
                    CartLine(BrickId(4), 3)
                  )
                  .some

              case _ => Stub.apply
            }

            lines.pure[IO]
          }

          override def clear(auth: UserId): IO[Unit] = cartCleared.modify(auth :: _).void
        }

        val mockBrickRepository: BricksRepository[IO, IO] = new BricksRepositoryStub[IO] {
          override def findById(id: BrickId): IO[Option[Brick]] =
            Map(
              BrickId(1) -> Brick("Test brick 1", 100, BrickColor.Blue),
              BrickId(2) -> Brick("Test brick 2", 150, BrickColor.Black),
              BrickId(3) -> Brick("Test brick 3", 200, BrickColor.Red),
              BrickId(4) -> Brick("Test brick 4", 50, BrickColor.Green)
            ).get(id).pure[IO]
        }

        val program = for {
          publishedRef <- Ref[IO, List[OrderSummary]](Nil)
          cartCleared  <- Ref[IO, List[UserId]](Nil)
          ordersRef    <- InMemoryOrderRepository.makeRef[IO]
          publishOrder = (msg: OrderSummary) => sleepRandom *> publishedRef.modify(msg :: _).void

          service: OrderService[IO] = new OrderServiceImpl[IO, IO](mockCartService(cartCleared),
                                                                   new InMemoryOrderRepository[IO](ordersRef),
                                                                   mockBrickRepository,
                                                                   publishOrder)

          (order1Id, order2Id) <- (service.placeOrder(userId1), service.placeOrder(userId2)).parTupled

          _                  <- IO.sleep(500.millis)
          publishedSummaries <- publishedRef.get
          clearedCarts       <- cartCleared.get
        } yield {
          //uncertain ordering because orders are made in parallel
          List(order1Id.get, order2Id.get) should contain theSameElementsAs List(OrderId(1), OrderId(2))

          publishedSummaries should contain theSameElementsAs List(
            OrderSummary(order1Id.get, userId1, 1200),
            OrderSummary(order2Id.get, userId2, 900)
          )

          clearedCarts should contain theSameElementsAs List(userId1, userId2)
        }

        program.unsafeRunSync()
      }
    }
  }
}
