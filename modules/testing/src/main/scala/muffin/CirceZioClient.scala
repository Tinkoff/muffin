//package muffin
//
//import muffin.api.{ApiClient, ClientConfig}
//import muffin.codec.Encode
//import muffin.http.HttpClient
//import muffin.interop.circe.codec
//import muffin.interop.circe.codec.given
//import muffin.interop.http.SttpClient
//import zio.*
//import zio.interop.catz.given
//import zio.interop.catz.implicits.given
//import muffin.interop.http.ZioClient
//import java.time.ZoneId
//
//import io.circe.{Json, Encoder, Decoder}
//
//
//object CirceZioClient {
//  type Api[F[_]] = ApiClient[F, Json, Encoder, Decoder]
//
//  def apply(cfg: ClientConfig): Task[Api[ZioClient.ZHttp]] =
//    for {
//      client <- ZioClient[Task, Encoder, Decoder]
//      given ZoneId <- ZIO.succeed(ZoneId.systemDefault())
//    } yield new ApiClient[ZioClient.ZHttp, Json, Encoder, Decoder](client, cfg)(codec)
//}
