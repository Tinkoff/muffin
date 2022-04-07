package muffin

import io.circe.{Json, JsonObject}
import muffin.http.ZHttpClient
import muffin.ApiClient
import muffin.predef.*
import muffin.reactions.{ReactionRequest, Reactions}
import zio.{Task, ZIOAppDefault}
import zio.interop.catz.`given`

object Application extends ZIOAppDefault {


  val token = "j8x1dkqokf8cupj6x8n6q79use"

  val neoId = UserId("jbrahfps57f458qsdth6urbsnw")

  val postId = MessageId("1oe489dopinhxx9wf9tj7iiynw")

  val run =
    for {
      httpClient:HttpClient[Task] <- ZHttpClient()
      client = {
        given a: HttpClient[Task] = httpClient
        ApiClient[Task](ClientConfig("http://localhost:8065/api/v4", token))
      }
//      CreatePostResponse(message_id) <- client.createPost(
//        CreatePostRequest(
//          ChannelId("jjxiug518ifc8g4zd5r1n8afjc"),
//          "qwe",
//          Some(Props(
//          Attachment(
//            "attachment",
//            text = Some("Вложение"),
//            actions = List(
//              Button(
//                "update",
//                "update",
//                Integration("https://neo-mm.requestcatcher.com", JsonObject(
//                  "action"-> Json.fromString("update_duper"),
////                  "qwe" -> Json.fromJsonObject(JsonObject(
////                    "abs"-> Json.fromBoolean(true)
////                  ))
//                ))
//              )
//            )
//          ) ::Nil))
//        )
//      )




      _ <- reactions(client)



    } yield ()


  def reactions(client: Reactions[Task]):Task[Unit] =
    for {
//      _ <- client.createReaction(CreateReactionRequest(neoId, postId, "smile"))
      _ <- client.createReaction(ReactionRequest(neoId, postId, "grin"))

//      reactions <- client.getReactions(GetListReactionsRequest(postId))
//      _ = println(reactions)


//      bulk <- client.bulkReactions(postId::Nil)
//      _ = println(bulk)

      _ <- client.removeReaction(ReactionRequest(neoId, postId, "grin"))

    } yield ()
}



