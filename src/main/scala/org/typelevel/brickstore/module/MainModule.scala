package org.typelevel.brickstore.module

import cats.effect.Sync
import doobie.util.transactor.Transactor
import org.typelevel.brickstore.{BricksController, BricksRepository}

class MainModule[F[_]: Sync](value: Transactor[F]) extends Module[F] {
  import com.softwaremill.macwire._

  private val repository: BricksRepository[F]        = wire[BricksRepository[F]]
  override val bricksController: BricksController[F] = wire[BricksController[F]]
}
