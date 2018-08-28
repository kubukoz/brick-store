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
  def add(id: BrickId)(userId: UserId): F[Option[Unit]]
  def findBricks(userId: UserId): F[Set[Brick]]
}

class InMemoryCartService[F[_]: Monad, CIO[_]](ref: MapRef[F, UserId, Set[Brick]], repository: BricksRepository[F, CIO])
    extends CartService[F] {

  override def add(id: BrickId)(userId: UserId): F[Option[Unit]] = {
    def addBrick(brick: Brick): F[Unit] = {
      val newElem = Map(userId -> ListSet(brick))
      ref.modify(_ |+| newElem).void
    }

    repository.findById(id).flatMap(_.traverse(addBrick))
  }

  override def findBricks(userId: UserId): F[Set[Brick]] = ref.get.map(_.getOrElse(userId, Set.empty))
}

object InMemoryCartService {
  type MapRef[F[_], K, V] = Ref[F, immutable.Map[K, V]]
  def makeRef[F[_]: Sync]: F[MapRef[F, UserId, Set[Brick]]] = Ref(Map.empty)
}
