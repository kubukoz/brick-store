package org.typelevel.brickstore

import cats.Applicative
import cats.data.EitherNel
import cats.implicits._
import fs2.Pipe
import org.typelevel.brickstore.dto.ImportResult.ImportResultNel
import org.typelevel.brickstore.dto.{BrickToCreate, BrickValidationError, ImportResult}
import org.typelevel.brickstore.entity.{Brick, BrickId}
import org.typelevel.brickstore.util.either._

trait BricksService[F[_]] {
  type LineNumber = Long
  val createEach: Pipe[F, BrickToCreate, ImportResultNel[BrickValidationError, BrickId]]
  val findBrickIds: fs2.Stream[F, BrickId]
}

class BricksServiceImpl[F[_]: Applicative, CIO[_]](repository: BricksRepository[F, CIO]) extends BricksService[F] {

  private def validate(brickToCreate: BrickToCreate): EitherNel[BrickValidationError, Brick] = {
    val validateName =
      brickToCreate.name
        .asRight[BrickValidationError]
        .ensure(BrickValidationError.NameTooLong)(_.length <= 20)
        .toEitherNel

    val validatePrice =
      brickToCreate.price.asRight[BrickValidationError].ensure(BrickValidationError.PriceNotPositive)(_ > 0).toEitherNel

    (validateName, validatePrice)
      .parMapN(Brick(_, _, brickToCreate.color))
  }



  override  val findBrickIds: fs2.Stream[F, BrickId] = repository.findBrickIds

  override val createEach: Pipe[F, BrickToCreate, ImportResultNel[BrickValidationError, BrickId]] =
    _.map(validate).zipWithIndex.map {
      case (validated, index) =>
        val lineNumber: LineNumber = index + 1

        validated.leftMap(lineNumber -> _)
    }.evalMap(_.traverse(repository.insert)).map(ImportResult.fromEither)
}
