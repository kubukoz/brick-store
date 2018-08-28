package org.typelevel.brickstore.entity
import io.circe.{Decoder, Encoder}

case class BrickId(value: Long) extends AnyVal

object BrickId {
  implicit val decoder: Decoder[BrickId] = Decoder[Long].map(apply)
  implicit val encoder: Encoder[BrickId] = Encoder[Long].contramap(_.value)
}
