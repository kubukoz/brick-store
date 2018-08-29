package org.typelevel.brickstore
import cats.Monad
import cats.effect.Sync
import cats.implicits._
import fs2.async.Ref
import org.typelevel.brickstore.InMemoryCartService.MapRef
import org.typelevel.brickstore.entity.{Brick, BrickId, UserId}

import scala.collection.immutable
import scala.collection.immutable.ListSet

trait CartService[F[_]] {
  def add(id: BrickId)(userId: UserId): F[Boolean]
  def findBricks(userId: UserId): F[Set[Brick]]
}

class InMemoryCartService[F[_], CIO[_]](ref: MapRef[F, UserId, Set[BrickId]],
                                               repository: BricksRepository[F, CIO])(implicit F: Monad[F])
    extends CartService[F] {

  override def add(id: BrickId)(userId: UserId): F[Boolean] = {
    val newElem = Map(userId -> ListSet(id))

    repository.findById(id).map(_.isDefined).flatTap {
      case true => ref.modify(_ |+| newElem).void
      case false => F.unit
    }
  }

  override def findBricks(userId: UserId): F[Set[Brick]] =
    ref.get
      .map(_.getOrElse(userId, Set.empty))
      .flatMap(_.toList.traverse(repository.findById))
      .map(_.flatten.toSet)
}

object InMemoryCartService {
  type MapRef[F[_], K, V] = Ref[F, immutable.Map[K, V]]
  def makeRef[F[_]: Sync]: F[MapRef[F, UserId, Set[BrickId]]] = Ref(Map.empty)
}
