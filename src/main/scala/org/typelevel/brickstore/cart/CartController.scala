package org.typelevel.brickstore.cart

import cats.data.{EitherT, NonEmptyList}
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
