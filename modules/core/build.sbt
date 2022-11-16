import Dependencies._

libraryDependencies ++=
  fs2 ::
    cats.core ::
    cats.effect ::
    scalatest.core ::
    scalatest.featureSpec ::
    circe.core   % Test ::
    circe.parser % Test ::
    Nil
