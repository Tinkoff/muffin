package muffin.internal

object syntax {

  extension [A](a: A) {
    inline def tap[B](f: A => B): B = f(a)
  }

}
