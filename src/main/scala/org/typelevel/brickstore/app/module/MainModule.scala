package org.typelevel.brickstore.app.module

import java.time.Instant

import cats.effect.{Clock, Concurrent, Timer}
import cats.implicits._
import cats.temp.par._
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import fs2.Stream
import fs2.concurrent.Topic
import org.typelevel.brickstore.app.auth.RequestAuthenticator
import org.typelevel.brickstore.app.util.Now
import org.typelevel.brickstore.bricks._
import org.typelevel.brickstore.cart.InMemoryCartRepository.CartRef
import org.typelevel.brickstore.cart._
import org.typelevel.brickstore.orders.InMemoryOrderRepository.OrdersRef
import org.typelevel.brickstore.orders.dto.OrderSummary
import org.typelevel.brickstore.orders._
import org.typelevel.brickstore.users.UserId

class MainModule[F[_]: Concurrent: Par: Clock] private (transactor: Transactor[F],
                                                        cartRef: CartRef[F],
                                                        ordersRef: OrdersRef[F],
                                                        newOrderTopic: Topic[F, OrderSummary])
    extends Module[F] {
  import com.softwaremill.macwire._
  private type CIO[A] = ConnectionIO[A]

  implicit val now: Now[F] = Now.fromClock
  //skip the initial value in the topic
  private val orderStream: Stream[F, OrderSummary] = newOrderTopic.subscribe(100).tail

  private val requestAuthenticator: RequestAuthenticator[F] = wire[RequestAuthenticator[F]]

  //bricks
  private val repository: BricksRepository[F, CIO]   = wire[DoobieBricksRepository[F]]
  private val service: BricksService[F]              = wire[BricksServiceImpl[F, CIO]]
  override val bricksController: BricksController[F] = wire[BricksController[F]]

  //cart
  private val cartRepository: CartRepository[F]  = wire[InMemoryCartRepository[F]]
  private val cartService: CartService[F]        = wire[CartServiceImpl[F, CIO]]
  override val cartController: CartController[F] = wire[CartController[F]]

  //order
  private val orderRepository: OrderRepository[F] = wire[InMemoryOrderRepository[F]]

  private val publishOrder: OrderSummary => F[Unit] = newOrderTopic.publish1
  private val orderService: OrderService[F]         = wire[OrderServiceImpl[F, CIO]]
  override val orderController: OrderController[F]  = wire[OrderController[F]]
}

object MainModule {

  def make[F[_]: Concurrent: Par: Timer](transactor: Transactor[F]): F[Module[F]] = {
    //for topic only
    for {
      cartRef       <- InMemoryCartRepository.makeRef[F]
      ordersRef     <- InMemoryOrderRepository.makeRef[F]
      newOrderTopic <- Topic[F, OrderSummary](OrderSummary(OrderId(0), UserId(0), 0, Instant.EPOCH))
    } yield new MainModule[F](transactor, cartRef, ordersRef, newOrderTopic)
  }
}
