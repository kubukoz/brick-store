package org.typelevel.brickstore.bricks

import fs2.Pipe
import org.typelevel.brickstore.app.data.ImportResult.ImportResultNel
import org.typelevel.brickstore.bricks.dto.{BrickToCreate, BrickValidationError}

trait BricksService[F[_]] {
  type LineNumber = Long
  def createEach: Pipe[F, BrickToCreate, ImportResultNel[BrickValidationError, BrickId]]
  def findBrickIds: fs2.Stream[F, BrickId]
}
