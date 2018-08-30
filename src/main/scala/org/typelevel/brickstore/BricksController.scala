package org.typelevel.brickstore

import cats.effect.Sync
import fs2.{Pipe, Stream}
import io.circe.Decoder
import io.circe.fs2._
import org.http4s.HttpService
import org.http4s.dsl.Http4sDsl
import org.typelevel.brickstore.dto.BrickToCreate
import org.typelevel.brickstore.util.http4s.jsonUtils

class BricksController[F[_]: Sync](bricksService: BricksService[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case req @ POST -> Root / "import" =>
        val bodyBricks: Stream[F, BrickToCreate] = req.body.through(decodeByteStream[BrickToCreate])

        val results = bodyBricks.through(bricksService.createEach)

        jsonUtils.toJsonArray(results)(Accepted.apply(_))
    }
  }

  private def decodeByteStream[A: Decoder]: Pipe[F, Byte, A] = {
    _.through(byteStreamParser).through(decoder[F, A])
  }
}
