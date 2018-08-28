package org.typelevel.brickstore.entity
import io.circe.Decoder

object BrickId {
  implicit val decoder: Decoder[BrickId] = Decoder[Long].map(apply)
}
case class BrickId(value: Long) extends AnyVal
