import sbt._

object Dependencies extends process.Deps with test.Deps {}

object process {
  object version {
    val sttp = "3.5.1"
    val circe = "0.14.1"
    val tapir = "1.0.0-M7"
    val http4s ="0.23.11"
  }

  trait Deps {
    object sttp {
      val core = "com.softwaremill.sttp.client3" %% "core" % version.sttp
      val zio =
        "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % version.sttp
    }

    object tapir {
      val core = "com.softwaremill.sttp.tapir" %% "tapir-core" % version.tapir
      val http4s =
        "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % version.tapir
      val circe =
        "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % version.tapir
    }

    object http4s {
      val ember  = "org.http4s" %% "http4s-ember-server" % version.http4s
    }

    object circe {
      val core = "io.circe" %% "circe-core" % version.circe
      val generic = "io.circe" %% "circe-generic" % version.circe
      val parser = "io.circe" %% "circe-parser" % version.circe
    }

    val zioCats = "dev.zio" %% "zio-interop-cats" % "3.3.0-RC2"
    //    lazy val enumeratum  = "com.beachape"               %% "enumeratum"        % version.enumeratum
//    lazy val ficus       = "com.iheart"                 %% "ficus"             % version.ficus excludeAll ficusExclude
//    lazy val quicklens   = "com.softwaremill.quicklens" %% "quicklens"         % version.quicklens
//    lazy val ldap        = "com.unboundid"               % "unboundid-ldapsdk" % version.ldap
//    lazy val logback     = "ch.qos.logback"              % "logback-classic"   % version.logback excludeModule slf4j
//    lazy val swaggerUi   = "org.webjars.npm"             % "swagger-ui-dist"   % version.swaggerUi
//    lazy val refined     = "eu.timepit"                 %% "refined"           % version.refined

  }
}

object test {
  object version {
//    val scalaTest      = "3.1.0"
//    val testcontainers = "0.39.5"
//    val postgresql     = "1.15.3"
//    val flyway         = "6.1.4"
//    val scalamock      = "4.4.0"
  }
//
  trait Deps {
//    lazy val scalaTest      = "org.scalatest"     %% "scalatest"            % version.scalaTest      % "it, test"
//    lazy val scalastic      = "org.scalactic"     %% "scalactic"            % version.scalaTest      % "it, test"
//    lazy val testcontainers = "com.dimafeng"      %% "testcontainers-scala" % version.testcontainers % "it"
//    lazy val postgresql     = "org.testcontainers" % "postgresql"           % version.postgresql     % "it"
//    lazy val flyway         = "org.flywaydb"       % "flyway-core"          % version.flyway         % "it"
//    lazy val scalamock      = "org.scalamock"     %% "scalamock"            % version.scalamock      % "it, test"
  }
}

object ModulesSyntax {
  final class ModuleOps(private val id: ModuleID) extends AnyVal {
    def excludeModule(module: ModuleID*): ModuleID =
      excludeAll(
        module.map(m =>
          ExclusionRule(
            m.organization,
            if (m.crossVersion == Binary()) s"${m.name}_2.13" else m.name
          )
        )
      )

    def excludeAll(rules: Seq[ExclusionRule])(implicit
      d: DummyImplicit
    ): ModuleID = id.excludeAll(rules: _*)
  }
}
