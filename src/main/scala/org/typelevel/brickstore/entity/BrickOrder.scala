package org.typelevel.brickstore.entity

import cats.Order
case class BrickOrder(id: OrderId, userId: UserId)

object BrickOrder {
  implicit val order: Order[BrickOrder] = Order.by(_.id)
}
