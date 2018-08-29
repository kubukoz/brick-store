package org.typelevel.brickstore
import cats.Order
import org.typelevel.brickstore.entity.{OrderId, UserId}

case class BrickOrder(id: OrderId, userid: UserId)

object BrickOrder {
  implicit val order: Order[BrickOrder] = Order.by(_.id)
}
