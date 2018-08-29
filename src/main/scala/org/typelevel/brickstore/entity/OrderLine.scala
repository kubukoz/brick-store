package org.typelevel.brickstore.entity

case class OrderLine(brickId: BrickId, quantity: Int, orderId: OrderId)
