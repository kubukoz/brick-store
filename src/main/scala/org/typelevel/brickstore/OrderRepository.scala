package org.typelevel.brickstore
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.temp.par._
import cats.{Monad, Semigroup}
import fs2.Stream
import org.typelevel.brickstore.InMemoryOrderRepository.OrdersRef
import org.typelevel.brickstore.entity.{UserId, _}
import org.typelevel.brickstore.data.OrderWithLines

import scala.collection.immutable.SortedMap

trait OrderRepository[F[_]] {
  val streamExisting: fs2.Stream[F, OrderWithLines]
  def getSummary(orderId: OrderId): F[Option[OrderWithLines]]
  def createOrder(auth: UserId): F[OrderId]
  def addOrderLine(orderId: OrderId, line: OrderLine): F[Unit]
}

class InMemoryOrderRepository[F[_]: Monad: Par](ref: OrdersRef[F]) extends OrderRepository[F] {

  private val getNewOrderId: OrderState => OrderId = _.value.keySet.map(_.id).maximumOption.getOrElse(OrderId(0)).inc

  override def getSummary(orderId: OrderId): F[Option[OrderWithLines]] =
    ref.get
      .map(_.value.find(_._1.id === orderId))
      .map(_.map(OrderWithLines.tupled))

  override val streamExisting: fs2.Stream[F, OrderWithLines] =
    Stream
      .eval(ref.get)
      .map(_.value.toList)
      .flatMap(Stream.emits(_))
      .map(OrderWithLines.tupled)

  override def createOrder(auth: UserId): F[OrderId] = ref.modify { oldState =>
    val newId = getNewOrderId(oldState)

    val newState = oldState |+| OrderState.emptyOrder(BrickOrder(newId, auth))

    (newState, newId)
  }

  override def addOrderLine(orderId: OrderId, line: OrderLine): F[Unit] =
    ref.update(_.addToOrder(orderId, line))
}

object InMemoryOrderRepository {
  type OrdersRef[F[_]] = Ref[F, OrderState]

  def makeRef[F[_]: Sync]: F[OrdersRef[F]] = Ref.of(OrderState(SortedMap.empty[BrickOrder, List[OrderLine]]))
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
