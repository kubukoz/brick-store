package org.typelevel.brickstore.module
import org.typelevel.brickstore.cart.CartController
import org.typelevel.brickstore.{BricksController, OrderController}

trait Module[F[_]] {
  val bricksController: BricksController[F]
  val cartController: CartController[F]
  val orderController: OrderController[F]
}
