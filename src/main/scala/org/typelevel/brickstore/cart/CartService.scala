package org.typelevel.brickstore.cart
import cats.Monad
import cats.effect.Sync
import cats.implicits._
import cats.temp.par._
import fs2.async.Ref
import io.scalaland.chimney.dsl._
import org.typelevel.brickstore.cart.InMemoryCartService.CartRef
import org.typelevel.brickstore.dto.CartBrick
import org.typelevel.brickstore.entity.UserId
import org.typelevel.brickstore.BricksRepository

import scala.collection.immutable
import scala.collection.immutable.ListSet

trait CartService[F[_]] {
  def add(request: CartAddRequest)(auth: UserId): F[Boolean]
  def findBricks(auth: UserId): F[Set[CartBrick]]
  def findLines(auth: UserId): F[Set[CartLine]]

  def clear(auth: UserId): F[Unit]
}

class InMemoryCartService[F[_]: Par, CIO[_]](ref: CartRef[F], repository: BricksRepository[F, CIO])(
  implicit F: Monad[F])
    extends CartService[F] {

  override def add(request: CartAddRequest)(auth: UserId): F[Boolean] = {
    val cartLine = request.transformInto[CartLine]

    val newElem = Map(auth -> ListSet(cartLine))

    repository.findById(request.brickId).map(_.isDefined).flatTap {
      case true  => ref.modify(_ |+| newElem).void
      case false => F.unit
    }
  }

  override def findBricks(auth: UserId): F[Set[CartBrick]] =
    findLines(auth).flatMap {
      _.toList.parTraverse(findBrickAndConvert)
    }.map(_.flatten.toSet)

  private def findBrickAndConvert(line: CartLine): F[Option[CartBrick]] = {
    repository.findById(line.brickId).map(_.map(_.into[CartBrick].withFieldConst(_.quantity, line.quantity).transform))
  }

  override def findLines(auth: UserId): F[Set[CartLine]] = ref.get.map(_.getOrElse(auth, Set.empty))

  override def clear(auth: UserId): F[Unit] = ref.modify(_ - auth).void
}

object InMemoryCartService {
  type MapRef[F[_], K, V] = Ref[F, immutable.Map[K, V]]
  type CartRef[F[_]]      = MapRef[F, UserId, Set[CartLine]]

  def makeRef[F[_]: Sync]: F[CartRef[F]] = Ref(Map.empty)
}
