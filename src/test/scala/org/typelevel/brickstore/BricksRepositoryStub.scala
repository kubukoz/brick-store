package org.typelevel.brickstore
import org.typelevel.brickstore.entity.{Brick, BrickId}

class BricksRepositoryStub[F[_]] extends BricksRepository[F, F] {
  override def insert(brick: Brick): F[BrickId]        = Stub.apply
  override def findById(id: BrickId): F[Option[Brick]] = Stub.apply
  override val findBrickIds: fs2.Stream[F, BrickId]    = fs2.Stream.empty
}
