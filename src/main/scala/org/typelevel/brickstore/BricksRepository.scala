package org.typelevel.brickstore
import cats.Monad
import doobie.implicits._
import doobie.util.transactor.Transactor

class BricksRepository[F[_]: Monad](xa: Transactor[F]) {
  val findOne: F[Option[Int]] = sql"""select 1""".query[Int].option.transact(xa)
}
