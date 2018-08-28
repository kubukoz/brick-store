package org.typelevel.brickstore

import cats.effect.Effect
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds
import cats.implicits._

class BricksController[F[_]: Effect](repository: BricksRepository[F]) extends Http4sDsl[F] {

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
