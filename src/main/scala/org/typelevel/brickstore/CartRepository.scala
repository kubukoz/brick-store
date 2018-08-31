package org.typelevel.brickstore

import cats.Functor
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits._
import fs2.async.Ref
import org.typelevel.brickstore.InMemoryCartRepository.CartRef
import org.typelevel.brickstore.entity.{CartLine, UserId}

import scala.collection.immutable

trait CartRepository[F[_]] {
  def saveToCart(cartLine: CartLine)(auth: UserId): F[Unit]
  def findLines(auth: UserId): F[Option[NonEmptyList[CartLine]]]
  def clear(auth: UserId): F[Unit]
}

class InMemoryCartRepository[F[_]: Functor](ref: CartRef[F]) extends CartRepository[F] {
  override def saveToCart(cartLine: CartLine)(auth: UserId): F[Unit] = {
    val newElem = Map(auth -> NonEmptyList.one(cartLine))
    ref.modify(old => (old |+| newElem).mapValues(_.distinct)).void
  }

  override def findLines(auth: UserId): F[Option[NonEmptyList[CartLine]]] = ref.get.map(_.get(auth))
  override def clear(auth: UserId): F[Unit]                               = ref.modify(_ - auth).void
}

object InMemoryCartRepository {
  type MapRef[F[_], K, V] = Ref[F, immutable.Map[K, V]]
  type CartRef[F[_]]      = MapRef[F, UserId, NonEmptyList[CartLine]]

  def makeRef[F[_]: Sync]: F[CartRef[F]] = Ref(Map.empty)
}
