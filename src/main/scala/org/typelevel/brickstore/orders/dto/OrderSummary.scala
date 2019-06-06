package org.typelevel.brickstore.orders.dto

import java.time.Instant

import io.circe.Encoder
import io.circe.derivation._
import org.typelevel.brickstore.orders.OrderId
import org.typelevel.brickstore.users.UserId

case class OrderSummary(id: OrderId, userId: UserId, total: Long, placedAt: Instant)

object OrderSummary {
  implicit val encoder: Encoder[OrderSummary] = deriveEncoder
}
