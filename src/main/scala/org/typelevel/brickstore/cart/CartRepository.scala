package org.typelevel.brickstore.cart
import cats.Functor
import cats.implicits._
import org.typelevel.brickstore.cart.CartServiceImpl.CartRef
import org.typelevel.brickstore.entity.UserId

import scala.collection.immutable.ListSet

trait CartRepository[F[_]] {
  def saveToCart(cartLine: CartLine)(auth: UserId): F[Unit]
  def findLines(auth: UserId): F[Set[CartLine]]
  def clear(auth: UserId): F[Unit]
}

class InMemoryCartRepository[F[_]: Functor](ref: CartRef[F]) extends CartRepository[F] {
  override def saveToCart(cartLine: CartLine)(auth: UserId): F[Unit] = {
    val newElem = Map(auth -> ListSet(cartLine))
    ref.modify(_ |+| newElem).void
  }

  override def findLines(auth: UserId): F[Set[CartLine]] = ref.get.map(_.getOrElse(auth, Set.empty))
  override def clear(auth: UserId): F[Unit]              = ref.modify(_ - auth).void
}
