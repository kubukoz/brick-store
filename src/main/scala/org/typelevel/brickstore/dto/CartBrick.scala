package org.typelevel.brickstore.dto

import io.circe.Encoder
import io.circe.derivation.deriveEncoder
import org.typelevel.brickstore.entity.BrickColor

case class CartBrick(name: String, price: Long, quantity: Int, color: BrickColor)

object CartBrick {
  implicit val encoder: Encoder[CartBrick] = deriveEncoder
}
