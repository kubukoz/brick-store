package org.typelevel.brickstore
import cats.effect.Bracket
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.typelevel.brickstore.entity.{Brick, BrickId}

import scala.language.reflectiveCalls

trait BricksRepository[F[_], CIO[_]] {
  def insert(brick: Brick): F[BrickId]
  def findById(id: BrickId): F[Option[Brick]]
  val findBrickIds: fs2.Stream[F, BrickId]
}

class DoobieBricksRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])
    extends BricksRepository[F, ConnectionIO] {

  override val findBrickIds: fs2.Stream[F, BrickId] = sql"""select id from bricks""".query[BrickId].stream.transact(xa)

  override def insert(brick: Brick): F[BrickId] = {
    val query =
      sql"""insert into bricks(name, price, color)
            values(${brick.name}, ${brick.price}, ${brick.color})"""

    query.update.withUniqueGeneratedKeys[BrickId]("id").transact(xa)
  }
  override def findById(id: BrickId): F[Option[Brick]] = {
    sql"""select name, price, color from bricks where id = $id""".query[Brick].option.transact(xa)
  }
}
