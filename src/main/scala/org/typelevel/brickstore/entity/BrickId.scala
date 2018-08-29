package org.typelevel.brickstore.entity
import cats.Order
import io.circe.{Decoder, Encoder}
import cats.implicits._

case class BrickId(value: Long) extends AnyVal

object BrickId {
  implicit val decoder: Decoder[BrickId] = Decoder[Long].map(apply)
  implicit val encoder: Encoder[BrickId] = Encoder[Long].contramap(_.value)

  implicit val order: Order[BrickId] = Order.by(_.value)
}
