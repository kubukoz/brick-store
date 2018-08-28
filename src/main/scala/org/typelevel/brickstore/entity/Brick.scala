package org.typelevel.brickstore.entity

import io.circe.Decoder
import io.circe.derivation._

case class Brick(id: BrickId, name: String, price: Long, color: BrickColor)

object Brick {
  implicit val decoder: Decoder[Brick] = deriveDecoder
}
