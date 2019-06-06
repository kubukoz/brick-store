package org.typelevel.brickstore.orders

import java.time.Instant

import cats.MonadError
import cats.data.NonEmptyList
import cats.implicits._
import cats.temp.par._
import io.scalaland.chimney.dsl._
import fs2._
import org.typelevel.brickstore.bricks.BricksRepository
import org.typelevel.brickstore.cart.{CartLine, CartService}
import org.typelevel.brickstore.orders.dto.OrderSummary
import org.typelevel.brickstore.users.UserId

trait OrderService[F[_]] {
  def streamExisting(before: Instant): Stream[F, OrderSummary]
  def placeOrder(auth: UserId): F[Option[OrderId]]
}

class OrderServiceImpl[F[_]: MonadError[?[_], Throwable]: Par, CIO[_]](cartService: CartService[F],
                                                                       orderRepository: OrderRepository[F],
                                                                       bricksRepository: BricksRepository[F, CIO],
                                                                       publishOrder: OrderSummary => F[Unit])
    extends OrderService[F] {

  override def streamExisting(before: Instant): Stream[F, OrderSummary] = orderRepository.streamExisting(before).evalMap(toOrderSummary)

  override def placeOrder(auth: UserId): F[Option[OrderId]] = {
    cartService
      .findLines(auth)
      .flatMap(_.traverse(saveOrder(_)(auth)))
  }

  private def saveOrder(cartLines: NonEmptyList[CartLine])(auth: UserId): F[OrderId] = {
    def publishSummary(orderId: OrderId): F[Unit] = {
      orderRepository
        .getSummary(orderId)
        .flatMap(_.liftTo[F](new Exception("Order not found after saving!")))
        .flatMap(toOrderSummary)
        .flatMap(publishOrder)
    }

    val clearCart = cartService.clear(auth)

    val createOrder: F[OrderId] =
      orderRepository
        .createOrder(auth)

    createOrder
      .flatTap(orderId => cartLines.traverse(saveLine(orderId, _)))
      .flatTap(publishSummary) <* clearCart
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
