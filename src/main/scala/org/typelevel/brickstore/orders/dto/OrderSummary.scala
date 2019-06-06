package org.typelevel.brickstore.orders.dto

import io.circe.Encoder
import io.circe.derivation._
import org.typelevel.brickstore.orders.OrderId
import org.typelevel.brickstore.users.UserId

case class OrderSummary(id: OrderId, userId: UserId, total: Long)

object OrderSummary {
  implicit val encoder: Encoder[OrderSummary] = deriveEncoder
}
