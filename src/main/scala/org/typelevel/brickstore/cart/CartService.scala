package org.typelevel.brickstore.cart
import cats.data._
import cats.effect.Sync
import cats.implicits._
import cats.mtl.FunctorLayer
import cats.mtl.instances.all._
import cats.temp.par.{parToParallel, Par}
import cats.{Monad, MonadError}
import fs2.async.Ref
import io.scalaland.chimney.dsl._
import org.typelevel.brickstore.BricksRepository
import org.typelevel.brickstore.cart.InMemoryCartService.CartRef
import org.typelevel.brickstore.dto.CartBrick
import org.typelevel.brickstore.entity.UserId
import org.typelevel.brickstore.util.either._

import scala.collection.immutable
import scala.collection.immutable.ListSet

trait CartService[F[_]] {
  val add: CartAddRequest => UserId => F[EitherNel[CartAddError, Unit]]
  def findBricks(auth: UserId): F[Set[CartBrick]]
  def findLines(auth: UserId): F[Set[CartLine]]

  def clear(auth: UserId): F[Unit]
}

class InMemoryCartService[F[_], CIO[_]](ref: CartRef[F], repository: BricksRepository[F, CIO])(implicit F: Monad[F],
                                                                                               P: Par[F])
    extends CartService[F] {

  private def addToCart[G[_]](request: CartAddRequest)(auth: UserId)(implicit P: Par[G],
                                                                     ME: MonadError[G, NonEmptyList[CartAddError]],
                                                                     FL: FunctorLayer[G, F]): G[Unit] = {

    val cartLine = request.transformInto[CartLine]
    val newElem  = Map(auth -> ListSet(cartLine))

    //validations
    val brickExists: G[Unit] =
      FL.layer(repository.findById(request.brickId))
        .map(_.toRight[CartAddError](CartAddError.BrickNotFound).toEitherNel)
        .rethrow
        .void

    val quantityPositive: G[Unit] =
      request.quantity
        .asRight[CartAddError]
        .ensure(CartAddError.QuantityNotPositive)(_ > 0)
        .toEitherNel
        .liftTo[G]
        .void

    val addToRef = FL.layer(ref.modify(_ |+| newElem))

    (
      brickExists,
      quantityPositive
    ).parTupled.void <* addToRef
  }

  override val add: CartAddRequest => UserId => F[EitherNel[CartAddError, Unit]] = req =>
    auth => addToCart[EitherT[F, NonEmptyList[CartAddError], ?]](req)(auth).value

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
