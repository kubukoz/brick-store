package org.typelevel.brickstore.app.util.http4s

import cats.Functor
import cats.implicits._
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Response}

object jsonUtils {

  def toJsonArray[F[_]: Functor, T: Encoder](stream: fs2.Stream[F, T], mode: StreamingMode = StreamingMode.JsonLines)(
    buildResponse: fs2.Stream[F, String] => F[Response[F]]): F[Response[F]] = {

    val coreStream = stream.map(_.asJson.noSpaces)

    def makeResponse(stream: fs2.Stream[F, String], mediaType: MediaType): F[Response[F]] = {
      buildResponse(stream).map(_.withContentType(`Content-Type`(mediaType)))
    }

    mode match {
      case StreamingMode.JsonLines =>
        val stream = coreStream.map(_ + "\n")
        makeResponse(stream, MediaType.application.`ld+json`)

      case StreamingMode.JsonArray =>
        val stream = fs2.Stream.emit("[") ++ coreStream.intersperse(",") ++ fs2.Stream.emit("]")
        makeResponse(stream, MediaType.application.json)
    }
  }

}

sealed trait StreamingMode extends Product with Serializable

object StreamingMode {
  case object JsonLines extends StreamingMode
  case object JsonArray extends StreamingMode
}
