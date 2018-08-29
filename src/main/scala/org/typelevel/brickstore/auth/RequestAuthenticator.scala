package org.typelevel.brickstore.auth
import cats.Monad
import cats.data.{Kleisli, OptionT}
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedService, HttpService, Request}
import org.typelevel.brickstore.entity.UserId

class RequestAuthenticator[F[_]: Monad] {
  private val authUser: Kleisli[OptionT[F, ?], Request[F], UserId] =
    Kleisli.pure[OptionT[F, ?], Request[F], UserId](UserId(1))

  private val middleware = AuthMiddleware(authUser)

  def apply(authedService: AuthedService[UserId, F]): HttpService[F] = middleware(authedService)
}
