package org.typelevel.brickstore

import sourcecode.Enclosing

object Stub {
  def apply[T](implicit enc: Enclosing): T = throw new UnsupportedOperationException(s"Incomplete mock: ${enc.value}")
}
