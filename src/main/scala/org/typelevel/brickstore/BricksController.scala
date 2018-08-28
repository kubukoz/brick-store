package org.typelevel.brickstore

import cats.Monad
import cats.implicits._
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

class BricksController[F[_]: Monad](repository: BricksRepository[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "hello" =>
        for {
          result   <- repository.findOne
          response <- Ok(Json.obj("message" -> Json.fromString(show"found: $result")))
        } yield response
    }
  }
}
