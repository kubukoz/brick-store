package org.typelevel.brickstore.entity

import cats.implicits._
import cats.{Order, Show}
import io.circe.{Decoder, Encoder}

case class OrderId(value: Long) extends AnyVal {
  def inc: OrderId = OrderId(value + 1)
}

object OrderId {
  implicit val decoder: Decoder[OrderId] = Decoder[Long].map(apply)
  implicit val encoder: Encoder[OrderId] = Encoder[Long].contramap(_.value)

  implicit val order: Order[OrderId] = Order.by(_.value)
  implicit val show: Show[OrderId]   = _.value.show
}
