package org.typelevel.brickstore.entity
import org.typelevel.brickstore.BrickOrder

case class OrderWithLines(order: BrickOrder, lines: List[OrderLine])
