package org.typelevel.brickstore.util

import cats.data.{EitherNel, NonEmptyList}
import cats.implicits._

object either {
  implicit class EitherExt[E, T](val either: Either[E, T]) extends AnyVal {
    //curried version of EitherNel
    type EitherNelC[EE] = { type λ[A] = EitherNel[EE, A] }

    //lambda trick to make the result type conform to F[_] for IDEA
    //the type of this is equivalent to EitherNel[E, T]
    //but returning it as a curried type gives IDEA the idea (pun unintended) that it's of kind * -> *
    def toEitherNel[EE >: E]: EitherNelC[E]#λ[T] = either.leftMap(NonEmptyList.one)
  }
}
