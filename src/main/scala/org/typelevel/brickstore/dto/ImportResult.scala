package org.typelevel.brickstore.dto
import cats.data.NonEmptyList
import io.circe.Encoder
import io.circe.derivation._
import io.circe.syntax._

sealed trait ImportResult[+E, +A] extends Product with Serializable

object ImportResult {
  type ImportResultNel[+E, +A] = ImportResult[NonEmptyList[E], A]

  case class Failed[+E](lineNumber: Long, error: E) extends ImportResult[E, Nothing]
  case class Successful[+A](result: A)              extends ImportResult[Nothing, A]

  def fromEither[E, A](either: Either[(Long, E), A]): ImportResult[E, A] = either match {
    case Left((line, value)) => Failed(line, value)
    case Right(value)        => Successful(value)
  }

  private implicit def failedEncoder[E: Encoder]: Encoder[Failed[E]]         = deriveEncoder
  private implicit def successfulEncoder[A: Encoder]: Encoder[Successful[A]] = deriveEncoder

  implicit def encoder[E: Encoder, A: Encoder]: Encoder[ImportResult[E, A]] = {
    case f @ Failed(_, _)  => f.asJson
    case s @ Successful(_) => s.asJson
  }
}
