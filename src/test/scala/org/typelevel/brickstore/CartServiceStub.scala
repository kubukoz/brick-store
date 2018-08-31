package org.typelevel.brickstore
import cats.data.{EitherNel, NonEmptyList}
import org.typelevel.brickstore.dto.{CartAddError, CartAddRequest, CartBrick}
import org.typelevel.brickstore.entity.{CartLine, UserId}

class CartServiceStub[F[_]] extends CartService[F] {
  val add: CartAddRequest => UserId => F[EitherNel[CartAddError, Unit]] = _ => _ => Stub.apply
  def findBricks(auth: UserId): F[Option[NonEmptyList[CartBrick]]]      = Stub.apply
  def findLines(auth: UserId): F[Option[NonEmptyList[CartLine]]]        = Stub.apply
  def clear(auth: UserId): F[Unit]                                      = Stub.apply
}
