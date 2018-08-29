package org.typelevel.brickstore.cart

import cats.effect.Sync
import cats.implicits._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedService, HttpService}
import org.typelevel.brickstore.auth.RequestAuthenticator

class CartController[F[_]: Sync](cart: CartService[F], authenticated: RequestAuthenticator[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = authenticated {
    AuthedService {
      case (req @ POST -> Root / "add") as auth =>
        for {
          body  <- req.decodeJson[CartAddRequest]
          added <- cart.add(body)(auth)
          response <- {
            if (added) Ok()
            else UnprocessableEntity("Brick not found")
          }
        } yield response

      case GET -> Root as auth =>
        for {
          result   <- cart.findBricks(auth)
          response <- Ok.apply(result.asJson)
        } yield response
    }
  }
}
