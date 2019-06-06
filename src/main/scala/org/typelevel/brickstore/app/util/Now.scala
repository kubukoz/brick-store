package org.typelevel.brickstore.app.util

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.Functor
import cats.effect.Clock
import cats.implicits._

trait Now[F[_]] {
  def instant: F[Instant]
}

object Now {
  def apply[F[_]](implicit F: Now[F]): Now[F] = F

  def liftF[F[_]](action: F[Instant]): Now[F] = new Now[F] {
    override val instant: F[Instant] = action
  }

  def fromClock[F[_]: Clock: Functor]: Now[F] =
    liftF(Clock[F].realTime(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli))
}
