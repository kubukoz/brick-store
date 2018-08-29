package org.typelevel.brickstore
import cats.effect.Sync
import org.http4s.{AuthedService, HttpService}
import org.http4s.dsl.Http4sDsl
import cats.implicits._
import org.typelevel.brickstore.auth.RequestAuthenticator

class OrderController[F[_]: Sync](orderService: OrderService[F], authenticated: RequestAuthenticator[F])
    extends Http4sDsl[F] {

  val service: HttpService[F] = authenticated {
    AuthedService {
      case POST -> Root as auth =>
        for {
          orderId <- orderService.placeOrder(auth)
          response <- orderId.fold(NotFound("Didn't find any items in the cart for current user")) { id =>
            Created(show"Created order with id: $id")
          }
        } yield response
    }
  }
}
