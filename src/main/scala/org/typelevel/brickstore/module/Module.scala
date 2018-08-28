package org.typelevel.brickstore.module
import org.typelevel.brickstore.BricksController

trait Module[F[_]] {
  val bricksController: BricksController[F]
}
