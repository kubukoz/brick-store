package org.typelevel.brickstore.module

import cats.effect.Sync
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import org.typelevel.brickstore._

class MainModule[F[_]: Sync](value: Transactor[F]) extends Module[F] {
  import com.softwaremill.macwire._
  private type CIO[A] = ConnectionIO[A]

  private val repository: BricksRepository[F, CIO]   = wire[DoobieBricksRepository[F]]
  private val service: BricksService[F]              = wire[BricksServiceImpl[F, CIO]]
  override val bricksController: BricksController[F] = wire[BricksController[F]]
}
