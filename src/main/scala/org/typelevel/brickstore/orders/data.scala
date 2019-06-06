package org.typelevel.brickstore.orders
import cats.implicits._
import cats.{Order, Show}
import io.circe.{Decoder, Encoder}
import org.typelevel.brickstore.bricks.BrickId
import org.typelevel.brickstore.users.UserId

case class BrickOrder(id: OrderId, userId: UserId)

object BrickOrder {
  implicit val order: Order[BrickOrder] = Order.by(_.id)
}

case class OrderId(value: Long) extends AnyVal {
  def inc: OrderId = OrderId(value + 1)
}

object OrderId {
  implicit val decoder: Decoder[OrderId] = Decoder[Long].map(apply)
  implicit val encoder: Encoder[OrderId] = Encoder[Long].contramap(_.value)

  implicit val order: Order[OrderId] = Order.by(_.value)
  implicit val show: Show[OrderId]   = _.value.show
}

case class OrderLine(brickId: BrickId, quantity: Int, orderId: OrderId)
