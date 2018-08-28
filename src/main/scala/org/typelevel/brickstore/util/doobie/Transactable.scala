package org.typelevel.brickstore.util.doobie
import cats.arrow.FunctionK
import cats.{~>, Monad}
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor

/**
  * An abstraction over Transactor that supports translating connection algebras and streams thereof.
  **/
trait Transactable[F[_], CIO[_]] {
  val trans: CIO ~> F
  val streamTrans: fs2.Stream[CIO, ?] ~> fs2.Stream[F, ?]
}

object Transactable {

  /**
    * The identity Transactable.
    * */
  def id[F[_]]: Transactable[F, F] = new Transactable[F, F] {
    override val trans: F ~> F                                     = FunctionK.id[F]
    override val streamTrans: fs2.Stream[F, ?] ~> fs2.Stream[F, ?] = FunctionK.id[fs2.Stream[F, ?]]
  }
}

class TransactorTransactable[F[_]: Monad](xa: Transactor[F]) extends Transactable[F, ConnectionIO] {
  override val trans: ConnectionIO ~> F                                     = xa.trans
  override val streamTrans: fs2.Stream[ConnectionIO, ?] ~> fs2.Stream[F, ?] = xa.transP
}
