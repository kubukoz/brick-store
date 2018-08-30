package org.typelevel.brickstore.module

import cats.effect.{Concurrent, ConcurrentEffect}
import cats.implicits._
import cats.temp.par.Par
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import fs2.Stream
import fs2.async.mutable.Topic
import org.typelevel.brickstore.InMemoryOrderRepository.OrdersRef
import org.typelevel.brickstore._
import org.typelevel.brickstore.auth.RequestAuthenticator
import org.typelevel.brickstore.cart.InMemoryCartService.CartRef
import org.typelevel.brickstore.cart.{CartController, CartService, InMemoryCartService}
import org.typelevel.brickstore.dto.OrderSummary
import org.typelevel.brickstore.entity.{OrderId, UserId}

import scala.concurrent.ExecutionContext

class MainModule[F[_]: Concurrent: Par] private (transactor: Transactor[F],
                                                 cartRef: CartRef[F],
                                                 ordersRef: OrdersRef[F],
                                                 newOrderTopic: Topic[F, OrderSummary])
    extends Module[F] {
  import com.softwaremill.macwire._
  private type CIO[A] = ConnectionIO[A]

  private val orderStream: Stream[F, OrderSummary] = newOrderTopic.subscribe(100).tail

  private val requestAuthenticator: RequestAuthenticator[F] = wire[RequestAuthenticator[F]]

  //bricks
  private val repository: BricksRepository[F, CIO]   = wire[DoobieBricksRepository[F]]
  private val service: BricksService[F]              = wire[BricksServiceImpl[F, CIO]]
  override val bricksController: BricksController[F] = wire[BricksController[F]]

  //cart
  private val cartService: CartService[F]        = wire[InMemoryCartService[F, CIO]]
  override val cartController: CartController[F] = wire[CartController[F]]

  //order
  private val orderRepository: OrderRepository[F] = wire[InMemoryOrderRepository[F]]

  private val publishOrder: OrderSummary => F[Unit] = newOrderTopic.publish1
  private val orderService: OrderService[F]         = wire[OrderServiceImpl[F, CIO]]
  override val orderController: OrderController[F]  = wire[OrderController[F]]
}

object MainModule {

  def make[F[_]: ConcurrentEffect: Par](transactor: Transactor[F])(implicit ec: ExecutionContext): F[Module[F]] =
    for {
      cartRef       <- InMemoryCartService.makeRef[F]
      ordersRef     <- InMemoryOrderRepository.makeRef[F]
      newOrderTopic <- Topic[F, OrderSummary](OrderSummary(OrderId(0), UserId(0), 0))
    } yield new MainModule[F](transactor, cartRef, ordersRef, newOrderTopic)
}
