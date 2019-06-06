package org.typelevel.brickstore.orders

import cats.data.NonEmptyList
import cats.effect.Concurrent
import cats.implicits._
import cats.temp.par._
import io.scalaland.chimney.dsl._
import fs2._
import fs2.concurrent.Queue
import org.typelevel.brickstore.app.util.Now
import org.typelevel.brickstore.bricks.BricksRepository
import org.typelevel.brickstore.cart.{CartLine, CartService}
import org.typelevel.brickstore.orders.dto.OrderSummary
import org.typelevel.brickstore.users.UserId

trait OrderService[F[_]] {
  def streamAll: Stream[F, OrderSummary]
  def placeOrder(auth: UserId): F[Option[OrderId]]
}

class OrderServiceImpl[F[_]: Concurrent: Now: Par, CIO[_]](cartService: CartService[F],
                                                           orderRepository: OrderRepository[F],
                                                           bricksRepository: BricksRepository[F, CIO],
                                                           publishOrder: OrderSummary => F[Unit],
                                                           newOrderStream: Stream[F, OrderSummary])
    extends OrderService[F] {

  override val streamAll: Stream[F, OrderSummary] = {
    Stream.eval(Now[F].instant).flatMap { now =>
      Stream.eval(Queue.bounded[F, OrderSummary](100)).flatMap { q =>
        (orderRepository.streamExisting(now).evalMap(toOrderSummary) concurrently newOrderStream
          .through(q.enqueue)) ++ q.dequeue.filter(_.placedAt.isAfter(now))
      }
    }
  }

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
