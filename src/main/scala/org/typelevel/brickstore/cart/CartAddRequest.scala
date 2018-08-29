package org.typelevel.brickstore.cart

import io.circe.Decoder
import io.circe.derivation._
import org.typelevel.brickstore.entity.BrickId

case class CartAddRequest(brickId: BrickId, quantity: Int)

object CartAddRequest {
  implicit val decoder: Decoder[CartAddRequest] = deriveDecoder
}
