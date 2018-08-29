package org.typelevel.brickstore.module

import cats.effect.Sync
import cats.implicits._
import cats.temp.par.Par
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import org.typelevel.brickstore.InMemoryOrderRepository.OrdersRef
import org.typelevel.brickstore._
import org.typelevel.brickstore.auth.RequestAuthenticator
import org.typelevel.brickstore.cart.InMemoryCartService.CartRef
import org.typelevel.brickstore.cart.{CartController, CartService, InMemoryCartService}

class MainModule[F[_]: Sync: Par] private (transactor: Transactor[F], cartRef: CartRef[F], ordersRef: OrdersRef[F])
    extends Module[F] {
  import com.softwaremill.macwire._
  private type CIO[A] = ConnectionIO[A]

  private val requestAuthenticator: RequestAuthenticator[F] = wire[RequestAuthenticator[F]]

  //bricks
  private val repository: BricksRepository[F, CIO]   = wire[DoobieBricksRepository[F]]
  private val service: BricksService[F]              = wire[BricksServiceImpl[F, CIO]]
  override val bricksController: BricksController[F] = wire[BricksController[F]]

  //cart
  private val cartService: CartService[F]        = wire[InMemoryCartService[F, CIO]]
  override val cartController: CartController[F] = wire[CartController[F]]

  //order
  private val orderRepository: OrderRepository[F]  = wire[InMemoryOrderRepository[F]]
  private val orderService: OrderService[F]        = wire[OrderServiceImpl[F]]
  override val orderController: OrderController[F] = wire[OrderController[F]]
}

object MainModule {

  def make[F[_]: Sync: Par](transactor: Transactor[F]): F[Module[F]] =
    for {
      cartRef   <- InMemoryCartService.makeRef[F]
      ordersRef <- InMemoryOrderRepository.makeRef[F]
    } yield new MainModule[F](transactor, cartRef, ordersRef)
}
