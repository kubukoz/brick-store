package org.typelevel.brickstore

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import io.circe.Json
import io.circe.fs2._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.typelevel.brickstore.dto.BrickToCreate

class BricksController[F[_]: Sync](repository: BricksRepository[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case req @ POST -> Root / "import" =>
        val bodyBricks: Stream[F, BrickToCreate] = req.body.through(byteStreamParser).through(decoder[F, BrickToCreate])

        for {
          _        <- bodyBricks.evalMap(brick => Sync[F].delay(println(s"got brick $brick"))).compile.drain
          result   <- repository.findOne
          response <- Ok(Json.obj("message" -> Json.fromString(show"found: $result")))
        } yield response
    }
  }
}
