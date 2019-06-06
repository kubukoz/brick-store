package org.typelevel.brickstore
import java.time.Instant

import org.typelevel.brickstore.orders.{OrderId, OrderLine, OrderRepository, OrderWithLines}
import org.typelevel.brickstore.users.UserId

class OrderRepositoryStub[F[_]] extends OrderRepository[F] {
  def streamExisting(before: Instant): fs2.Stream[F, OrderWithLines] = Stub.apply
  def getSummary(orderId: OrderId): F[Option[OrderWithLines]]        = Stub.apply
  def createOrder(auth: UserId): F[OrderId]                          = Stub.apply
  def addOrderLine(orderId: OrderId, line: OrderLine): F[Unit]       = Stub.apply
}
