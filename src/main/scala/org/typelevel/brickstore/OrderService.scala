package org.typelevel.brickstore
import cats.Monad
import cats.data.NonEmptySet
import cats.implicits._
import cats.temp.par._
import io.scalaland.chimney.dsl._
import org.typelevel.brickstore.cart.{CartLine, CartService}
import org.typelevel.brickstore.entity.{OrderId, OrderLine, UserId}

import scala.collection.immutable.SortedSet

trait OrderService[F[_]] {
  def placeOrder(auth: UserId): F[Option[OrderId]]
}

class OrderServiceImpl[F[_]: Monad: Par](cartService: CartService[F], orderRepository: OrderRepository[F])
    extends OrderService[F] {
  override def placeOrder(auth: UserId): F[Option[OrderId]] = {
    val cartBricks = cartService.findLines(auth)

    cartBricks.map(_.to[SortedSet]).flatMap {
      _.toNes.traverse(saveOrder(_)(auth))
    }
  }

  private def saveOrder(cartLines: NonEmptySet[CartLine])(auth: UserId): F[OrderId] = {
    for {
      orderId <- orderRepository.createOrder(auth)
      _       <- cartLines.toNonEmptyList.parTraverse(saveLine(orderId, _))
      //todo wrap above in transaction
      _ <- cartService.clear(auth)
    } yield orderId
  }

  private def saveLine(orderId: OrderId, line: CartLine): F[Unit] = {
    val orderLine = createOrderLine(orderId)(line)
    orderRepository.addOrderLine(orderId, orderLine)
  }

  private def createOrderLine(orderId: OrderId): CartLine => OrderLine =
    _.into[OrderLine].withFieldConst(_.orderId, orderId).transform
}
