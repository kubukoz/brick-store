package org.typelevel.brickstore.dto

import enumeratum.Circe._
import enumeratum._
import io.circe.derivation._
import io.circe.{Decoder, Encoder}
import org.typelevel.brickstore.entity.BrickColor

import scala.collection.immutable

case class BrickToCreate(name: String, price: Long, color: BrickColor)

object BrickToCreate {
  implicit val decoder: Decoder[BrickToCreate] = deriveDecoder
}

sealed trait BrickValidationError extends EnumEntry

object BrickValidationError extends Enum[BrickValidationError] {
  case object NameTooLong   extends BrickValidationError
  case object NegativePrice extends BrickValidationError

  override val values: immutable.IndexedSeq[BrickValidationError] = findValues
  implicit val encoderBVE: Encoder[BrickValidationError]          = encoder(this)
}
