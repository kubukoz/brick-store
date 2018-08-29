package org.typelevel.brickstore.cart
import cats.Order
import org.typelevel.brickstore.entity.BrickId

case class CartLine(brickId: BrickId, quantity: Int)

object CartLine {
  implicit val order: Order[CartLine] = Order.by(_.brickId)
}
