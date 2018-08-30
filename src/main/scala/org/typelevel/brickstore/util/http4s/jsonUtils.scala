package org.typelevel.brickstore.util.http4s

import cats.effect.Sync
import cats.implicits._
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Response}

object jsonUtils {

  def toJsonArray[F[_]: Sync, T: Encoder](stream: fs2.Stream[F, T], mode: StreamingMode = StreamingMode.JsonLines)(
    buildResponse: fs2.Stream[F, String] => F[Response[F]]): F[Response[F]] = {

    val coreStream = stream.map(_.asJson.noSpaces)

    val textStream = mode match {
      case StreamingMode.JsonLines => coreStream.map(_ + "\n")
      case StreamingMode.JsonArray => fs2.Stream.emit("[") ++ coreStream.intersperse(",") ++ fs2.Stream.emit("]")
    }

    buildResponse(textStream).map(_.withContentType(`Content-Type`(MediaType.`application/json`)))
  }

}

sealed trait StreamingMode extends Product with Serializable

object StreamingMode {
  case object JsonLines extends StreamingMode
  case object JsonArray extends StreamingMode
}
