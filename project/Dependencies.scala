import sbt._

object Dependencies {

  object Version {
    val sttp = "3.8.3"

    val circe = "0.15.0-M1"

    val http4s = "1.0.0-M37"

    object zio {
      val zhttp = "2.0.0-RC11"
      val json  = "0.3.0"
      val core  = "2.0.2"
    }

    val fs2 = "3.3.0"

    object cats {
      val core   = "2.8.0"
      val effect = "3.3.14"
    }

    val scalatest = "3.2.14"
  }

  val sttp = "com.softwaremill.sttp.client3" %% "core" % Version.sttp

  object http4s {
    val core = "org.http4s" %% "http4s-core" % Version.http4s
    val dsl  = "org.http4s" %% "http4s-dsl"  % Version.http4s
  }

  object circe {
    val core   = "io.circe" %% "circe-core"   % Version.circe
    val parser = "io.circe" %% "circe-parser" % Version.circe
  }

  object zio {
    val json = "dev.zio" %% "zio-json" % Version.zio.json
    val http = "io.d11"  %% "zhttp"    % Version.zio.zhttp
    val core = "dev.zio" %% "zio"      % Version.zio.core
  }

  val fs2 = "co.fs2" %% "fs2-core" % Version.fs2

  object cats {
    val core   = "org.typelevel" %% "cats-core"   % Version.cats.core
    val effect = "org.typelevel" %% "cats-effect" % Version.cats.effect
  }

  object scalatest {
    val core        = "org.scalatest" %% "scalatest-core"        % Version.scalatest % Test
    val featureSpec = "org.scalatest" %% "scalatest-featurespec" % Version.scalatest % Test
  }

}
