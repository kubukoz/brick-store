package org.typelevel.brickstore.bricks

import cats.effect.Sync
import fs2.{Pipe, Stream}
import io.circe.Decoder
import io.circe.fs2._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.brickstore.app.util.http4s.jsonUtils
import org.typelevel.brickstore.bricks.dto.BrickToCreate

final class BricksController[F[_]: Sync](implicit bricksService: BricksService[F]) extends Http4sDsl[F] {

  private val results = bricksService.findBrickIds

  val routes: HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root =>
        jsonUtils.toJsonArray(results)(Ok.apply(_))

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
