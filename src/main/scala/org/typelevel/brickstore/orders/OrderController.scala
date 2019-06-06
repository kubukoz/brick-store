package org.typelevel.brickstore.orders

import cats.effect.Concurrent
import cats.implicits._
import fs2._
import fs2.concurrent.Queue
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedService, HttpRoutes}
import org.typelevel.brickstore.app.auth.RequestAuthenticator
import org.typelevel.brickstore.app.util.Now
import org.typelevel.brickstore.app.util.http4s.jsonUtils
import org.typelevel.brickstore.orders.dto.OrderSummary

class OrderController[F[_]: Concurrent: Now](orderService: OrderService[F],
                                             authenticated: RequestAuthenticator[F],
                                             newOrderStream: Stream[F, OrderSummary])
    extends Http4sDsl[F] {

  private val adminRoutes: HttpRoutes[F] = authenticated.admin {
    AuthedService {
      case GET -> Root / "stream" as _ =>
        val allStream =
          //todo belongs in service
          Stream.eval(Now[F].instant).flatMap { now =>
            Stream.eval(Queue.bounded[F, OrderSummary](100)).flatMap { q =>
              (orderService.streamExisting(now) concurrently newOrderStream.through(q.enqueue)) ++ q.dequeue.filter(
                _.placedAt.isAfter(now))
            }
          }

        jsonUtils.toJsonArray(allStream)(Ok.apply(_))
    }
  }

  private val userRoutes: HttpRoutes[F] = authenticated {
    AuthedService {
      case POST -> Root as auth =>
        for {
          orderResult <- orderService.placeOrder(auth)
          response <- orderResult.fold(UnprocessableEntity("Didn't find any items in the cart for current user")) {
            id =>
              Created(show"Created order with id: $id")
          }
        } yield response
    }
  }

  val routes: HttpRoutes[F] = adminRoutes <+> userRoutes
}
