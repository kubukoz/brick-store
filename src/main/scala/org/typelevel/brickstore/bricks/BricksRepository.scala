package org.typelevel.brickstore.bricks

trait BricksRepository[F[_], CIO[_]] {
  def insert(brick: Brick): F[BrickId]
  def findById(id: BrickId): F[Option[Brick]]
  val findBrickIds: fs2.Stream[F, BrickId]
}
