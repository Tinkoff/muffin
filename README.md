


# Muffin

----

Mattermost API v4 client for Scala 3.





----
Status 

| Endpoints            | Status |
|----------------------|:------:|
| Users                |   ❎    |        
| Bots                 |   ❎    |
| Teams                |   ❎    |     
| Channels             |   ❎    | 
| Posts                |   ❎    |
| Threads              |   ❎    |
| Files                |   ❎    |
| Uploads              |   ❎    |
| Preferences          |   ❎    |
| Status               |   ❎    |
| Emoji                |   ☑    |
| Reactions            |   ☑    |
| Webhooks             |   ❎    |
| Commands             |   ❎    |
| System               |   ❎    |
| Brand                |   ❎    | 
| integration_actions  |   ❎    |
| ....                 |   ❎    |    




# Getting started

## sttp

Zio or other sttp backend
```scala
"com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.5.1" 
```

```scala
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.{Task, ZIOAppDefault}
import zio.interop.catz.given

object App extends ZIOAppDefault {
  val run =
    for {
      backend                <- HttpClientZioBackend()

      given HttpClient[Task] <- SttpClient[Task, Task](backend)
      client                  = ApiClient[Task](ClientConfig("API_V4_URL", "TOKEN"))
      
      _ <- client.createPost(CreatePostRequest(ChannelId("CHANNEL_ID"), "Muffin + Mattermost = <3"))
    } yield ()
}
```

## Custom http backend

You just need implement [HttpClient](https://github.com/little-inferno/muffin/blob/master/modules/http/src/main/scala/muffin/HttpClient.scala) trait
