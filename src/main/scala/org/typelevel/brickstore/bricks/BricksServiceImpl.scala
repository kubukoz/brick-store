package org.typelevel.brickstore.bricks

import cats.Applicative
import cats.data.EitherNel
import cats.implicits._
import fs2.Pipe
import org.typelevel.brickstore.app.data.ImportResult
import org.typelevel.brickstore.app.data.ImportResult.ImportResultNel
import org.typelevel.brickstore.bricks
import org.typelevel.brickstore.bricks.dto.{BrickToCreate, BrickValidationError}

final class BricksServiceImpl[F[_]: Applicative, CIO[_]](implicit repository: BricksRepository[F, CIO])
    extends BricksService[F] {

  private def validate(brickToCreate: BrickToCreate): EitherNel[BrickValidationError, Brick] = {
    val validateName =
      brickToCreate.name
        .asRight[BrickValidationError]
        .ensure(BrickValidationError.NameTooLong)(_.length <= 20)
        .toEitherNel

    val validatePrice =
      brickToCreate.price.asRight[BrickValidationError].ensure(BrickValidationError.PriceNotPositive)(_ > 0).toEitherNel

    (validateName, validatePrice)
      .parMapN(bricks.Brick(_, _, brickToCreate.color))
  }

  override val findBrickIds: fs2.Stream[F, BrickId] = repository.findBrickIds

  override val createEach: Pipe[F, BrickToCreate, ImportResultNel[BrickValidationError, BrickId]] =
    _.map(validate).zipWithIndex.map {
      case (validated, index) =>
        val lineNumber: LineNumber = index + 1

        validated.leftMap(lineNumber -> _)
    }.evalMap(_.traverse(repository.insert)).map(ImportResult.fromEither)
}
