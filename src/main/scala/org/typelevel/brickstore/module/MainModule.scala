package org.typelevel.brickstore.module

import cats.effect.Sync
import cats.implicits._
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import org.typelevel.brickstore.InMemoryCartService.MapRef
import org.typelevel.brickstore._
import org.typelevel.brickstore.entity.{BrickId, UserId}

class MainModule[F[_]: Sync] private (transactor: Transactor[F], cartRef: MapRef[F, UserId, Set[BrickId]])
    extends Module[F] {
  import com.softwaremill.macwire._
  private type CIO[A] = ConnectionIO[A]

  private val repository: BricksRepository[F, CIO]          = wire[DoobieBricksRepository[F]]
  private val cartService: CartService[F]                   = wire[InMemoryCartService[F, CIO]]
  private val service: BricksService[F]                     = wire[BricksServiceImpl[F, CIO]]
  private val requestAuthenticator: RequestAuthenticator[F] = wire[RequestAuthenticator[F]]
  override val bricksController: BricksController[F]        = wire[BricksController[F]]
  override val cartController: CartController[F]            = wire[CartController[F]]
}

object MainModule {

  def make[F[_]: Sync](transactor: Transactor[F]): F[Module[F]] =
    for {
      ref <- InMemoryCartService.makeRef[F]
    } yield new MainModule[F](transactor, ref)
}
