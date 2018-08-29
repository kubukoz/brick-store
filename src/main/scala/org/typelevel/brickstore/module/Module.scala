package org.typelevel.brickstore.module
import org.typelevel.brickstore.{BricksController, OrderController}
import org.typelevel.brickstore.cart.CartController

trait Module[F[_]] {
  val bricksController: BricksController[F]
  val cartController: CartController[F]
  val orderController: OrderController[F]
}
