package org.typelevel.brickstore.entity
import enumeratum.Circe._
import enumeratum.EnumEntry.LowerCamelcase
import enumeratum._
import io.circe.{Decoder, Encoder}

import scala.collection.immutable

sealed trait BrickColor extends EnumEntry with LowerCamelcase

object BrickColor extends Enum[BrickColor] {
  case object Red    extends BrickColor
  case object Blue   extends BrickColor
  case object Green  extends BrickColor
  case object Yellow extends BrickColor
  case object Black  extends BrickColor
  case object Gray   extends BrickColor
  case object Pink   extends BrickColor

  override def values: immutable.IndexedSeq[BrickColor] = findValues

  implicit val bcCecoder: Decoder[BrickColor] = decoder(this)
  implicit val bcEncoder: Encoder[BrickColor] = encoder(this)
}
