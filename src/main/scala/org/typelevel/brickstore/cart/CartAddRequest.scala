package org.typelevel.brickstore.cart

import io.circe.{Decoder, Encoder}
import io.circe.derivation._
import org.typelevel.brickstore.entity.BrickId
import enumeratum._
import enumeratum.Circe._

import scala.collection.immutable

case class CartAddRequest(brickId: BrickId, quantity: Int)

object CartAddRequest {
  implicit val decoder: Decoder[CartAddRequest] = deriveDecoder
}

sealed trait CartAddError extends EnumEntry

object CartAddError extends Enum[CartAddError] {
  case object QuantityNotPositive extends CartAddError
  case object BrickNotFound       extends CartAddError

  implicit val encoderCAE: Encoder[CartAddError] = encoder(this)

  override val values: immutable.IndexedSeq[CartAddError] = findValues
}
