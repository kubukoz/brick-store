package org.typelevel.brickstore.orders

case class OrderWithLines(order: BrickOrder, lines: List[OrderLine])
