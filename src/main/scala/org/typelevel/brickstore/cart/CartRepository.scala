package org.typelevel.brickstore.cart

import cats.Functor
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import org.typelevel.brickstore.cart.InMemoryCartRepository.CartRef
import org.typelevel.brickstore.users.UserId

import scala.collection.immutable

trait CartRepository[F[_]] {
  def saveToCart(cartLine: CartLine)(auth: UserId): F[Unit]
  def findLines(auth: UserId): F[Option[NonEmptyList[CartLine]]]
  def clear(auth: UserId): F[Unit]
}

class InMemoryCartRepository[F[_]: Functor](ref: CartRef[F]) extends CartRepository[F] {
  override def saveToCart(cartLine: CartLine)(auth: UserId): F[Unit] = {
    val newElem = Map(auth -> NonEmptyList.one(cartLine))
    ref.update(old => (old |+| newElem).mapValues(_.distinct))
  }

  override def findLines(auth: UserId): F[Option[NonEmptyList[CartLine]]] = ref.get.map(_.get(auth))
  override def clear(auth: UserId): F[Unit]                               = ref.update(_ - auth)
}

object InMemoryCartRepository {
  type MapRef[F[_], K, V] = Ref[F, immutable.Map[K, V]]
  type CartRef[F[_]]      = MapRef[F, UserId, NonEmptyList[CartLine]]

  def makeRef[F[_]: Sync]: F[CartRef[F]] = Ref.of(Map.empty)
}
