package muffin.internal

trait Coercible[A, B] {
  inline final def apply(a: A): B = a.asInstanceOf[B]
}

object Coercible {
  def apply[A, B](using ev: Coercible[A, B]): Coercible[A, B] = ev

  def instance[A, B]: Coercible[A, B] = anyToAny.asInstanceOf[Coercible[A, B]]

  private val anyToAny = new Coercible[Any, Any] {}

  given [M1[_], M2[_], A, B](using ev: Coercible[M2[A], M2[B]]): Coercible[M1[M2[A]], M1[M2[B]]] = Coercible.instance
}