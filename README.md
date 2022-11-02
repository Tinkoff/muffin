<img align="right" src="images/logo.png" height="100px" style="padding-left: 20px"/>

# Muffin

----

Mattermost v4 API client for Scala 3.

# Getting started


1. Add muffin to your project dependencies:

```sbt
libraryDependencies += "ru.tinkoff" %% "muffin-core" % "latest version in badge"
```

2. Choose your integrations and include them, 
for example circe, http4s, and sttp with AsyncHttpClientCatsBackend:

```sbt
libraryDependencies += "ru.tinkoff" %% "muffin-circe-json-interop" % "latest version in badge"
libraryDependencies += "ru.tinkoff" %% "muffin-sttp-http-interop" % "latest version in badge"
libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.7.6"
```

3. Program example

```scala
import java.time.{LocalDateTime, ZoneId}

import cats.effect.*
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend

import io.circe.*

import muffin.api.*
import muffin.model.*
import muffin.dsl.*
import muffin.interop.circe.codec
import muffin.interop.http.Http4sRoute

type Api = ApiClient[IO, Encoder, Decoder]

class SimpleCommandHandler(api: Api){
  def time(command: CommandAction): IO[AppResponse[Nothing]] = {
    api.postToChannel(command.channelId, LocalDateTime.now().toString.some).as(ok)
  }
}

object Application extends IOApp.Simple {
    for {
        backend      <- AsyncHttpClientCatsBackend[IO]()
        httpClient   <- SttpClient[Task, Task, Encoder, Decoder](backend)
        given ZoneId <- ZIO.succeed(ZoneId.systemDefault())
        cfg          = ClientConfig("base mattermost api url", "auth token", "bot name", "your service base url")
        apiClient    = ApiClient[IO, Encoder, Decoder](httpClient, cfg)(codec)
      
        handler = SimpleCommandHandler(apiClient)
      
        router <- handle(handler).command(_.time).in[Task, Task]
      
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(Router("/" -> Http4sRoute.routes(router, codec)).orNotFound)
          .build
          .allocated
          .never
    } yield ()
}
```

# Supported integrations

## Json integrations

### [circe](https://github.com/circe/circe)

```sbt
libraryDependencies += "space.littleinferno" %% "muffin-circe-json-interop" % "latest version in badge"
```

```scala
import muffin.interop.circe.codec.given
```

### [zio-json](https://github.com/zio/zio-json)

```sbt
libraryDependencies += "space.littleinferno" %% "muffin-zio-json-interop" % "latest version in badge"
```

```scala
import muffin.interop.zio.codec.given
```

## Http integrations

### [http4s](https://github.com/http4s/http4s)

```sbt
libraryDependencies += "space.littleinferno" %% "muffin-http4s-http-interop" % "latest version in badge"
```

```scala
import cats.effect.IO

import muffin.interop.http.Http4sRoute

val router: Router[IO] = ???
val codec = ??? // Choose you codec in json integrations section

val server = EmberServerBuilder
  .default[IO]
  .withHost(ipv4"0.0.0.0")
  .withPort(port"8080")
  .withHttpApp(Router("/" -> Http4sRoute.routes(app, codec)).orNotFound)
  .build
```

### [zio-http](https://github.com/dream11/zio-http)

```sbt
libraryDependencies += "space.littleinferno" %% "muffin-zio-http-interop" % "latest version in badge"
```

```scala
import zio.*

import muffin.interop.http.ZioServer
import muffin.interop.circe.codec

val router: Router[Task] = ???
val codec = ??? // Choose you codec in json integrations section

val client = ZioClient[Task, /* Supported json encoder */, /* Supported json decoder */](codec)


val server = Server.start(8080, ZioServer.routes(router, codec))
```

### [sttp](https://github.com/softwaremill/sttp)

```sbt
libraryDependencies += "space.littleinferno" %% "muffin-sttp-http-interop" % "latest version in badge"
```

```scala
import java.time.ZoneId

import cats.effect.IO

import muffin.model.*

val client = SttpClient[IO, IO, /* Supported json encoder */, /* Supported json decoder */](backend, codec)
```

## Copyright

Copyright the maintainers

Logos made by [Midjourney](https://discord.com/channels/662267976984297473/976997500349186119/1016053747639656498)
