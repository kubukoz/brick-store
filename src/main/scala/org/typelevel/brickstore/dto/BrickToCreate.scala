package org.typelevel.brickstore.dto
import io.circe.Decoder
import io.circe.derivation._
import org.typelevel.brickstore.entity.BrickColor

case class BrickToCreate(name: String, price: Long, color: BrickColor)

object BrickToCreate {
  implicit val decoder: Decoder[BrickToCreate] = deriveDecoder
}
