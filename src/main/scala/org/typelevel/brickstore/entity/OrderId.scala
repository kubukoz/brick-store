package org.typelevel.brickstore.entity

import cats.implicits._
import cats.kernel.Monoid
import cats.{Order, Show}
import io.circe.{Decoder, Encoder}

case class OrderId(value: Long) extends AnyVal {
  def inc: OrderId = this |+| OrderId(1)
}

object OrderId {
  implicit val decoder: Decoder[OrderId] = Decoder[Long].map(apply)
  implicit val encoder: Encoder[OrderId] = Encoder[Long].contramap(_.value)

  implicit val order: Order[OrderId] = Order.by(_.value)
  implicit val show: Show[OrderId]   = _.value.show

  implicit val monoid: Monoid[OrderId] = new Monoid[OrderId] {
    override def empty: OrderId                           = OrderId(0)
    override def combine(x: OrderId, y: OrderId): OrderId = OrderId(x.value |+| y.value)
  }
}
