package org.typelevel.brickstore
import cats.Applicative
import cats.data.{EitherNel, NonEmptyList}
import cats.implicits._
import fs2.Pipe
import io.scalaland.chimney.dsl._
import org.typelevel.brickstore.dto.ImportResult.ImportResultNel
import org.typelevel.brickstore.dto.{BrickToCreate, BrickValidationError, ImportResult}
import org.typelevel.brickstore.entity.{Brick, BrickId}
import org.typelevel.brickstore.utilz._

trait BricksService[F[_]] {
  type LineNumber = Long
  val createEach: Pipe[F, BrickToCreate, ImportResultNel[BrickValidationError, BrickId]]
}

class BricksServiceImpl[F[_]: Applicative, CIO[_]](repository: BricksRepository[F, CIO]) extends BricksService[F] {

  private def validate(brickToCreate: BrickToCreate): EitherNel[BrickValidationError, Brick] =
    Either
      .cond(brickToCreate.name.length <= 20, brickToCreate.transformInto[Brick], BrickValidationError.NameTooLong)
      .toEitherNel

  override val createEach: Pipe[F, BrickToCreate, ImportResultNel[BrickValidationError, BrickId]] =
    _.map(validate).zipWithIndex.map {
      case (validated, index) =>
        val lineNumber: LineNumber = index + 1

        validated.leftMap(lineNumber -> _)
    }.evalMap(_.traverse(repository.insert)).map(ImportResult.fromEither)
}

object utilz {
  implicit class EitherExt[E, T](val either: Either[E, T]) extends AnyVal {
    def toEitherNel[EE >: E]: EitherNel[EE, T] = either.leftMap(NonEmptyList.one)
  }
}
