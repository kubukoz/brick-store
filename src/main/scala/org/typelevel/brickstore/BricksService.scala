package org.typelevel.brickstore
import org.typelevel.brickstore.entity.Brick

trait BricksService[F[_]] {
  def insert(brick: Brick): F[Int]
}

class BricksServiceImpl[F[_], CIO[_]](repository: BricksRepository[F, CIO]) extends BricksService[F] {
  override def insert(brick: Brick): F[Int] = repository.insert(brick)
}
