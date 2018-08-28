package org.typelevel.brickstore

import cats.effect.Sync
import cats.implicits._
import fs2.{Pipe, Stream}
import io.circe.Decoder
import io.circe.fs2._
import io.circe.syntax._
import io.scalaland.chimney.dsl._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.typelevel.brickstore.dto.BrickToCreate
import org.typelevel.brickstore.entity.Brick

class BricksController[F[_]: Sync](bricksService: BricksService[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case req @ POST -> Root / "import" =>
        val bodyBricks: Stream[F, BrickToCreate] = req.body.through(decodeByteStream[BrickToCreate])

        val insertAll = bodyBricks.map(_.transformInto[Brick]).evalMap(bricksService.insert).compile.foldMonoid

        for {
          insertedCount <- insertAll
          response      <- Ok(Map("imported" -> insertedCount).asJson)
        } yield response
    }
  }

  private def decodeByteStream[A: Decoder]: Pipe[F, Byte, A] = {
    _.through(byteStreamParser).through(decoder[F, A])
  }
}
