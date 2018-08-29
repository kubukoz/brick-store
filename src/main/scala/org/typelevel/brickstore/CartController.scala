package org.typelevel.brickstore

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.dsl._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedService, HttpService, Request}
import org.typelevel.brickstore.entity.{BrickColor, BrickId, UserId}

class RequestAuthenticator[F[_]: Monad] {
  private val authUser: Kleisli[OptionT[F, ?], Request[F], UserId] =
    Kleisli.pure[OptionT[F, ?], Request[F], UserId](UserId(1))

  private val middleware = AuthMiddleware(authUser)

  def apply(authedService: AuthedService[UserId, F]): HttpService[F] = middleware(authedService)
}

class CartController[F[_]: Sync](cart: CartService[F], authenticated: RequestAuthenticator[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = authenticated {
    AuthedService {
      case (req @ POST -> Root / "add") as auth =>
        for {
          body  <- req.decodeJson[CartAddRequest]
          added <- cart.add(body.brickId)(auth)
          response <- {
            if (added) Ok()
            else UnprocessableEntity("Brick not found")
          }
        } yield response

      case GET -> Root as auth =>
        for {
          result   <- cart.findBricks(auth)
          response <- Ok.apply(result.map(_.transformInto[CartBrick]).asJson)
        } yield response
    }
  }
}

import io.circe.derivation._
case class CartBrick(name: String, price: Long, color: BrickColor)

object CartBrick {
  implicit val encoder: Encoder[CartBrick] = deriveEncoder
}

case class CartAddRequest(brickId: BrickId)

object CartAddRequest {
  implicit val decoder: Decoder[CartAddRequest] = deriveDecoder
}
