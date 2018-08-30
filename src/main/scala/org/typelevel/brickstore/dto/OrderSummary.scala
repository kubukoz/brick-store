package org.typelevel.brickstore.dto
import io.circe.Encoder
import io.circe.derivation._
import org.typelevel.brickstore.entity.{OrderId, UserId}

case class OrderSummary(id: OrderId, userId: UserId, total: Long)

object OrderSummary {
  implicit val encoder: Encoder[OrderSummary] = deriveEncoder
}
