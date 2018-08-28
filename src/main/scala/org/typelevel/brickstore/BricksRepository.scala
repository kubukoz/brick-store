package org.typelevel.brickstore
import cats.Monad
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.brickstore.entity.Brick

trait BricksRepository[F[_], CIO[_]] {
  def insert(brick: Brick): F[Int]
}

class DoobieBricksRepository[F[_]: Monad](xa: Transactor[F]) extends BricksRepository[F, ConnectionIO] {
  override def insert(brick: Brick): F[Int] = {
    val query =
      sql"""insert into bricks(name, price, color)
            values(${brick.name}, ${brick.price}, ${brick.color})"""

    query.update.run.transact(xa)
  }
}
