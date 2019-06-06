package org.typelevel.brickstore.users

import io.circe.Encoder

case class UserId(value: Long) extends AnyVal

object UserId {
  implicit val encoder: Encoder[UserId] = Encoder[Long].contramap(_.value)
}
