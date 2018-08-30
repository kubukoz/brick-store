package org.typelevel.brickstore.cart
import cats.Functor
import cats.effect.Sync
import cats.implicits._
import fs2.async.Ref
import org.typelevel.brickstore.cart.InMemoryCartRepository.CartRef
import org.typelevel.brickstore.entity.{CartLine, UserId}

import scala.collection.immutable
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

object InMemoryCartRepository {
  type MapRef[F[_], K, V] = Ref[F, immutable.Map[K, V]]
  type CartRef[F[_]]      = MapRef[F, UserId, Set[CartLine]]

  def makeRef[F[_]: Sync]: F[CartRef[F]] = Ref(Map.empty)
}
