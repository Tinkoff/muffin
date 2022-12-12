package muffin.internal

trait NewType[Real] {
  opaque type Type = Real
  inline def apply(value: Real): Type            = value
  extension (value: Type) inline def value: Real = value

  given Coercible[Real, Type] = Coercible.instance
  given [F[_]]: Coercible[F[Real], F[Type]] = Coercible.instance

  given Coercible[Type, Real] = Coercible.instance
}
