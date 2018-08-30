package org.typelevel.brickstore.data

import org.typelevel.brickstore.entity.{BrickOrder, OrderLine}

case class OrderWithLines(order: BrickOrder, lines: List[OrderLine])
