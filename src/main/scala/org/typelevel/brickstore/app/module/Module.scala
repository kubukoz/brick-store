package org.typelevel.brickstore.app.module
import org.typelevel.brickstore.bricks.BricksController
import org.typelevel.brickstore.cart.CartController
import org.typelevel.brickstore.orders.OrderController

trait Module[F[_]] {
  val bricksController: BricksController[F]
  val cartController: CartController[F]
  val orderController: OrderController[F]
}
