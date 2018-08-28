package org.typelevel.brickstore.module
import org.typelevel.brickstore.{BricksController, CartController}

trait Module[F[_]] {
  val bricksController: BricksController[F]
  val cartController: CartController[F]
}
