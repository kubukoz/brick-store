package org.typelevel.brickstore.bricks.dto

import enumeratum.Circe._
import enumeratum._
import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import org.typelevel.brickstore.bricks.BrickColor

import scala.collection.immutable

case class BrickToCreate(name: String, price: Long, color: BrickColor)

object BrickToCreate {
  implicit val decoder: Decoder[BrickToCreate] = deriveDecoder
}

sealed trait BrickValidationError extends EnumEntry

object BrickValidationError extends Enum[BrickValidationError] {
  case object NameTooLong      extends BrickValidationError
  case object PriceNotPositive extends BrickValidationError

  override val values: immutable.IndexedSeq[BrickValidationError] = findValues
  implicit val encoderBVE: Encoder[BrickValidationError]          = encoder(this)
}
