package org.typelevel.brickstore.bricks

import cats.Order
import doobie.util.Meta
import enumeratum.EnumEntry.LowerCamelcase
import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}
import cats.implicits._
import scala.collection.immutable

case class Brick(name: String, price: Long, color: BrickColor)

sealed trait BrickColor extends EnumEntry with LowerCamelcase

object BrickColor extends Enum[BrickColor] {
  case object Red    extends BrickColor
  case object Blue   extends BrickColor
  case object Green  extends BrickColor
  case object Yellow extends BrickColor
  case object Black  extends BrickColor
  case object Gray   extends BrickColor
  case object Pink   extends BrickColor

  override val values: immutable.IndexedSeq[BrickColor] = findValues

  implicit val bcDecoder: Decoder[BrickColor] = enumeratum.Circe.decoder(this)
  implicit val bcEncoder: Encoder[BrickColor] = enumeratum.Circe.encoder(this)

  import doobie.postgres.implicits._
  implicit val meta: Meta[BrickColor] = pgEnumStringOpt("brick_color", withNameOption, _.entryName)
}

case class BrickId(value: Long) extends AnyVal

object BrickId {
  implicit val decoder: Decoder[BrickId] = Decoder[Long].map(apply)
  implicit val encoder: Encoder[BrickId] = Encoder[Long].contramap(_.value)

  implicit val order: Order[BrickId] = Order.by(_.value)
}
