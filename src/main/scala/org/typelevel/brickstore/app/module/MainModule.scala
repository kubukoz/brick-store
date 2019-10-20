package org.typelevel.brickstore.app.module

import cats.effect.Concurrent
import cats.Parallel
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import fs2.Stream
import fs2.concurrent.Topic
import org.typelevel.brickstore.app.auth.RequestAuthenticator
import org.typelevel.brickstore.bricks._
import org.typelevel.brickstore.cart.InMemoryCartRepository.CartRef
import org.typelevel.brickstore.cart._
import org.typelevel.brickstore.orders.InMemoryOrderRepository.OrdersRef
import org.typelevel.brickstore.orders.dto.OrderSummary
import org.typelevel.brickstore.orders._
import org.typelevel.brickstore.users.UserId

final class MainModule[F[_]: Concurrent: Parallel] private (
  cartRef: CartRef[F],
  ordersRef: OrdersRef[F],
  newOrderTopic: Topic[F, OrderSummary]
)(
  implicit transactor: Transactor[F]
) extends Module[F] {
  private type CIO[A] = ConnectionIO[A]

  //skip the initial value in the topic
  implicit private val orderStream: Stream[F, OrderSummary] = newOrderTopic.subscribe(100).tail

  implicit private val requestAuthenticator: RequestAuthenticator[F] = new RequestAuthenticator[F]

  //bricks
  implicit private val repository: BricksRepository[F, CIO] = new DoobieBricksRepository[F]
  implicit private val service: BricksService[F]            = new BricksServiceImpl[F, CIO]
  override val bricksController: BricksController[F]        = new BricksController[F]

  //cart
  implicit private val cartRepository: CartRepository[F] = new InMemoryCartRepository[F](cartRef)
  implicit private val cartService: CartService[F]       = new CartServiceImpl[F, CIO]
  override val cartController: CartController[F]         = new CartController[F]

  //order
  implicit private val orderRepository: OrderRepository[F] = new InMemoryOrderRepository[F](ordersRef)

  implicit private val publishOrder: OrderSummary => F[Unit] = newOrderTopic.publish1
  implicit private val orderService: OrderService[F]         = new OrderServiceImpl[F, CIO](publishOrder)
  override val orderController: OrderController[F]           = new OrderController(orderStream)
}

object MainModule {

  def make[F[_]: Concurrent: Parallel](implicit transactor: Transactor[F]): F[Module[F]] = {
    (
      InMemoryCartRepository.makeRef[F],
      InMemoryOrderRepository.makeRef[F],
      Topic[F, OrderSummary](OrderSummary(OrderId(0), UserId(0), 0))
    ).mapN(new MainModule[F](_, _, _))
  }
}
