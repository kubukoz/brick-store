package org.typelevel.brickstore
import cats.data.EitherNel
import org.typelevel.brickstore.dto.{CartAddError, CartAddRequest, CartBrick}
import org.typelevel.brickstore.entity.{CartLine, UserId}

class CartServiceStub[F[_]] extends CartService[F] {
  val add: CartAddRequest => UserId => F[EitherNel[CartAddError, Unit]] = _ => _ => Stub.apply
  def findBricks(auth: UserId): F[Set[CartBrick]]                       = Stub.apply
  def findLines(auth: UserId): F[Set[CartLine]]                         = Stub.apply
  def clear(auth: UserId): F[Unit]                                      = Stub.apply
}
