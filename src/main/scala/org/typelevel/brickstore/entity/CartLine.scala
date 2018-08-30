package org.typelevel.brickstore.entity

import cats.Order

case class CartLine(brickId: BrickId, quantity: Int)

object CartLine {
  implicit val order: Order[CartLine] = Order.by(_.brickId)
}
