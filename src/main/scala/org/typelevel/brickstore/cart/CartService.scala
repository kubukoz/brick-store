package org.typelevel.brickstore.cart

import cats.data._
import cats.implicits._
import cats.mtl.FunctorLayer
import cats.mtl.implicits._
import cats.temp.par._
import cats.{Monad, MonadError}
import io.scalaland.chimney.dsl._
import org.typelevel.brickstore.BricksRepository
import org.typelevel.brickstore.dto.CartBrick
import org.typelevel.brickstore.entity.{Brick, UserId}
import org.typelevel.brickstore.util.either._

trait CartService[F[_]] {
  val add: CartAddRequest => UserId => F[EitherNel[CartAddError, Unit]]
  def findBricks(auth: UserId): F[Set[CartBrick]]
  def findLines(auth: UserId): F[Set[CartLine]]

  def clear(auth: UserId): F[Unit]
}

class CartServiceImpl[F[_]: Par: Monad, CIO[_]](cartRepository: CartRepository[F],
                                                brickRepository: BricksRepository[F, CIO])
    extends CartService[F] {

  override val add: CartAddRequest => UserId => F[EitherNel[CartAddError, Unit]] = req => {
    auth => doAdd[EitherT[F, NonEmptyList[CartAddError], ?]](req)(auth).value
  }

  private def doAdd[G[_]](request: CartAddRequest)(auth: UserId)(implicit P: Par[G],
                                                                 ME: MonadError[G, NonEmptyList[CartAddError]],
                                                                 FL: FunctorLayer[G, F]): G[Unit] = {
    //validations
    val brickExists: G[Unit] =
      FL.layer(brickRepository.findById(request.brickId))
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

    val saveToCart: G[Unit] = FL.layer(cartRepository.saveToCart(request.transformInto[CartLine])(auth))

    (
      brickExists,
      quantityPositive
    ).parTupled.void <* saveToCart
  }

  override def findBricks(auth: UserId): F[Set[CartBrick]] =
    findLines(auth).flatMap {
      _.toList
        .parTraverse(findBrick)
        .map(_.flatten.toSet)
    }

  private def findBrick(line: CartLine): F[Option[CartBrick]] = {
    val convert: Brick => CartBrick = _.into[CartBrick].withFieldConst(_.quantity, line.quantity).transform

    brickRepository
      .findById(line.brickId)
      .map(_.map(convert))
  }

  override def findLines(auth: UserId): F[Set[CartLine]] = cartRepository.findLines(auth)

  override def clear(auth: UserId): F[Unit] = cartRepository.clear(auth)
}
