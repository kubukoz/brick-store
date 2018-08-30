package org.typelevel.brickstore
import org.typelevel.brickstore.data.OrderWithLines
import org.typelevel.brickstore.entity.{OrderId, OrderLine, UserId}

class OrderRepositoryStub[F[_]] extends OrderRepository[F] {
  val streamExisting: fs2.Stream[F, OrderWithLines]            = fs2.Stream.empty
  def getSummary(orderId: OrderId): F[Option[OrderWithLines]]  = Stub.apply
  def createOrder(auth: UserId): F[OrderId]                    = Stub.apply
  def addOrderLine(orderId: OrderId, line: OrderLine): F[Unit] = Stub.apply
}
