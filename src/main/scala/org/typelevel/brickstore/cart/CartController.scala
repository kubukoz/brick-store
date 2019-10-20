package org.typelevel.brickstore.cart

import cats.effect.Sync
import cats.implicits._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.typelevel.brickstore.app.auth.RequestAuthenticator
import org.typelevel.brickstore.cart.dto.CartAddRequest
import org.http4s.AuthedRoutes

class CartController[F[_]: Sync](implicit cart: CartService[F], authenticated: RequestAuthenticator[F])
    extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = authenticated {
    AuthedRoutes.of {
      case (req @ POST -> Root / "add") as auth =>
        for {
          body      <- req.decodeJson[CartAddRequest]
          addResult <- cart.add(body)(auth)
          response <- addResult match {
            case Left(errors) => UnprocessableEntity(errors.asJson)
            case _            => Ok()
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
