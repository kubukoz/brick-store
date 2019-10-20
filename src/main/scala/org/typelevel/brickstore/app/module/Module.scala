package org.typelevel.brickstore.app.module
import org.typelevel.brickstore.bricks.BricksController
import org.typelevel.brickstore.cart.CartController
import org.typelevel.brickstore.orders.OrderController

trait Module[F[_]] {
  def bricksController: BricksController[F]
  def cartController: CartController[F]
  def orderController: OrderController[F]
}
