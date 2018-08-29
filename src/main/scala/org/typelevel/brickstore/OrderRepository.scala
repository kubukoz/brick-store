package org.typelevel.brickstore
import cats.effect.Sync
import cats.implicits._
import cats.{Monad, Semigroup}
import fs2.async.Ref
import org.typelevel.brickstore.InMemoryOrderRepository.OrdersRef
import org.typelevel.brickstore.entity.{OrderId, OrderLine, UserId}

import scala.collection.immutable.SortedMap

trait OrderRepository[F[_]] {
  def createOrder(auth: UserId): F[OrderId]
  def addOrderLine(orderId: OrderId, line: OrderLine): F[Unit]
}

class InMemoryOrderRepository[F[_]: Monad](ref: OrdersRef[F]) extends OrderRepository[F] {
  private val getNewOrderId: F[OrderId] = ref.get.map(_.value.keySet.map(_.id).max.inc)

  override def createOrder(auth: UserId): F[OrderId] = getNewOrderId.flatTap { newId =>
    ref.modify(_ |+| OrderState.emptyOrder(BrickOrder(newId, auth)))
  }

  override def addOrderLine(orderId: OrderId, line: OrderLine): F[Unit] =
    ref.modify(_.addToOrder(orderId, line)).void
}

object InMemoryOrderRepository {
  type OrdersRef[F[_]] = Ref[F, OrderState]

  def makeRef[F[_]: Sync]: F[OrdersRef[F]] = Ref(OrderState(SortedMap.empty[BrickOrder, List[OrderLine]]))
}

final case class OrderState private (value: SortedMap[BrickOrder, List[OrderLine]]) {
  def addToOrder(orderId: OrderId, line: OrderLine): OrderState = modifyById(orderId)(_ |+| List(line))

  private def modifyById(id: OrderId)(mod: List[OrderLine] => List[OrderLine]): OrderState = {
    val atKey = value.find { case (order, _) => order.id === id }

    atKey match {
      case None => this

      case Some((key, oldValue)) =>
        val newMap = value + (key -> mod(oldValue))

        copy(value = newMap)
    }
  }
}

object OrderState {
  implicit val semigroup: Semigroup[OrderState] = (a, b) => OrderState(a.value |+| b.value)

  def emptyOrder(order: BrickOrder): OrderState = OrderState(SortedMap(order -> Nil))
}
