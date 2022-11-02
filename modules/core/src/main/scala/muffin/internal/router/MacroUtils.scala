package muffin.internal.router

import scala.quoted.*

trait MacroUtils {

  protected def getSymbolFromType[T: Type](name: String)(using q: Quotes): q.reflect.Symbol = {
    import q.reflect.*

    TypeRepr
      .of[T]
      .typeSymbol
      .caseFields
      .find(_.name == name)
      .getOrElse(report.errorAndAbort(s"Can't find $name in ${Type.show[T]}"))
  }

  protected def summonNames[T <: Tuple: Type](using q: Quotes): List[String] =
    Type
      .valueOfTuple[T]
      .map(summonNames(_))
      .getOrElse(q.reflect.report.errorAndAbort(s"Can't get value from ${Type.show[T]}"))

  protected def summonNames[T <: Tuple](method: T)(using q: Quotes): List[String] = {
    import q.reflect.*

    method match {
      case head *: tail if head.isInstanceOf[String] => head.asInstanceOf[String] :: summonNames(tail)
      case head *: _                                 => report.errorAndAbort(s"Can't get value from: $head isn't String")
      case EmptyTuple                                => Nil
    }
  }

  protected def getMethodSymbol[H: Type](name: String)(using q: Quotes): q.reflect.Symbol = {
    import q.reflect.*

    TypeTree.of[H].tpe.classSymbol.map(_.declaredMethod(name)).toList.flatten match {
      case head :: Nil => head
      case head :: _   => report.errorAndAbort(s"Router builder doesn't support overloading $head")
      case _           => report.errorAndAbort(s"Can't find $name in ${Type.show[H]}")
    }
  }

  protected def summonGiven[T: Type](using q: Quotes) = {
    import q.reflect.*

    Expr.summon[T].getOrElse(report.errorAndAbort(s"Could not summon ${Type.show[T]}"))
  }

}
