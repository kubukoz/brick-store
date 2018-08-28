package org.typelevel.brickstore.entity
import enumeratum._
import io.circe.Decoder

import scala.collection.immutable

case class BrickId(value: Long) extends AnyVal

case class Brick(id: BrickId, name: String, price: Long, color: BrickColor)

sealed trait BrickColor extends EnumEntry

object BrickColor extends Enum[BrickColor] {
  override def values: immutable.IndexedSeq[BrickColor] = findValues

  implicit val decoder: Decoder[BrickColor] =
    Decoder[String].emap(withNameLowercaseOnlyOption(_).toRight("Didn't find a BrickColor"))
}
