package org.typelevel.brickstore

import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.effect.Concurrent.ops._
import cats.implicits._
import cats.temp.par._
import io.scalaland.chimney.dsl._
import fs2._
import org.typelevel.brickstore.data.OrderWithLines
import org.typelevel.brickstore.dto.OrderSummary
import org.typelevel.brickstore.entity.{UserId, _}

trait OrderService[F[_]] {
  val streamExisting: Stream[F, OrderSummary]
  def placeOrder(auth: UserId): F[Option[OrderId]]
}

class OrderServiceImpl[F[_]: Concurrent: Par, CIO[_]](cartService: CartService[F],
                                                      orderRepository: OrderRepository[F],
                                                      bricksRepository: BricksRepository[F, CIO],
                                                      publishOrder: OrderSummary => F[Unit])
    extends OrderService[F] {

  override val streamExisting: Stream[F, OrderSummary] = orderRepository.streamExisting.evalMap(toOrderSummary)

  override def placeOrder(auth: UserId): F[Option[OrderId]] = {
    cartService
      .findLines(auth)
      .flatMap(_.traverse(saveOrder(_)(auth)))
  }

  private def saveOrder(cartLines: NonEmptyList[CartLine])(auth: UserId): F[OrderId] = {
    def publishSummary(orderId: OrderId): F[Unit] = {
      orderRepository
        .getSummary(orderId)
        .map(_.toRight[Throwable](new Exception("Order not found after saving!")))
        .rethrow
        .flatMap(toOrderSummary)
        .flatMap(publishOrder)
    }

    val clearCart = cartService.clear(auth)

    val createOrder: F[OrderId] =
      orderRepository
        .createOrder(auth)

    createOrder
      .flatTap(orderId => cartLines.traverse(saveLine(orderId, _)))
      .flatTap(publishSummary(_).start) <* clearCart
  }

  private def saveLine(orderId: OrderId, line: CartLine): F[Unit] = {
    val orderLine = createOrderLine(orderId)(line)
    orderRepository.addOrderLine(orderId, orderLine)
  }

  private def createOrderLine(orderId: OrderId): CartLine => OrderLine =
    _.into[OrderLine].withFieldConst(_.orderId, orderId).transform

  private def toOrderSummary(orderWithLines: OrderWithLines): F[OrderSummary] = {
    for {
      prices <- orderWithLines.lines.parTraverse(lineTotal)
      orderTotal = prices.combineAll
    } yield orderWithLines.order.into[OrderSummary].withFieldConst(_.total, orderTotal).transform
  }

  private def lineTotal(line: OrderLine): F[Long] =
    bricksRepository.findById(line.brickId).map(_.foldMap(_.price * line.quantity))
}
