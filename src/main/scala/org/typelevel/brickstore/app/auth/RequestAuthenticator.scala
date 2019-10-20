package org.typelevel.brickstore.app.auth
import cats.Monad
import cats.data.{Kleisli, OptionT}
import org.http4s.server.AuthMiddleware
import org.http4s.{HttpRoutes, Request}
import org.typelevel.brickstore.users.UserId
import org.http4s.AuthedRoutes

class RequestAuthenticator[F[_]: Monad] {

  //todo replace with real authentication ;)
  //Kleisli[OptionT[F, ?], Request[F], UserId] is equivalent to
  //Request[F] => F[Option[UserId]]
  private val authUser: Kleisli[OptionT[F, ?], Request[F], UserId] =
    Kleisli.pure[OptionT[F, ?], Request[F], UserId](UserId(1))

  private val middleware = AuthMiddleware.withFallThrough(authUser)

  def apply(authedService: AuthedRoutes[UserId, F]): HttpRoutes[F] = middleware(authedService)

  //just an alias, in the future it might take the shape of `apply`
  //with different requirements (such as having an admin role)
  val admin: RequestAuthenticator[F] = this
}
