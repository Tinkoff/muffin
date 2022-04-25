package muffin

import io.circe.{Json, JsonObject}
import muffin.app.{App, AppResponse}
import muffin.http.SttpClient
import muffin.emoji.*
import muffin.posts.*
import muffin.predef.*
import muffin.reactions.*
import muffin.DefaultApp
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.{Task, ZIOAppDefault}
import zio.interop.catz.given

import java.io.File
import zio.interop.catz.implicits.given

object Application extends ZIOAppDefault {

  val token = "yxu1ybk3zjydpp5km9zy3mt4ka"

  val neoId = UserId("96qoqoeifjy33qqeriphy6n8pr")

  val postId = MessageId("1oe489dopinhxx9wf9tj7iiynw")

  val run =
    for {
      app: App[Task] <- DefaultApp(
        ClientConfig("http://localhost:8065/api/v4", token)
      )

      CreatePostResponse(message_id) <- app.ctx.client.createPost(
        CreatePostRequest(
          ChannelId("8otforyp3igapnoodtga8ho5iy"),
          "qwe",
          Some(
            Props(
              Attachment(
                "attachment",
                text = Some("Вложение"),
                actions = List(
                  Button(
                    "update",
                    "update",
                    Integration(
                      "http://host.docker.internal:8080/commands",
                      JsonObject(
                        "action" -> Json.fromString("super"),
                        "qwe" -> Json.fromJsonObject(
                          JsonObject("abs" -> Json.fromBoolean(true))
                        )
                      )
                    )
                  )
                )
              ) :: Nil
            )
          )
        )
      )

      _ = app.command("kek") { (ctx, action) =>

        println(action)
        Task(AppResponse.Ok())
      }

      _ <- DefaultServer(app).useForever
//      application = app
//        .command("/start") { (ctx, command) =>
//          ???
//        }
//        .actions("action") { (ctx, action) =>
//          ???
//        }
//        .actions("second") { (ctx, action) =>
//          ???
//        }

//

//      _ <- client.performAction(
//        PerformActionRequest(message_id, "update_duper")
//      )

//      fileContent = {
//        import java.nio.file.Files
//
//        import javax.imageio.ImageIO
//        import java.awt.image.BufferedImage
//        import java.awt.image.DataBufferByte
//        import java.awt.image.WritableRaster
//        import java.io.File
//        import java.io.IOException
//
//        val imgPath = new File("C:\\Users\\danil\\Desktop\\neo.jpg")
//        val bufferedImage = ImageIO.read(imgPath)
//        val raster = bufferedImage.getRaster
//        val data = raster.getDataBuffer.asInstanceOf[DataBufferByte]
//        data.getData
//      }

//      fileContent = {
//        import java.nio.file.Files
//
//        File
//        Files.readAllBytes(.toPath)
//      }

//      res <- client.createEmoji(CreateEmojiRequest(new File("C:\\Users\\danil\\Desktop\\neo.jpg"), "keanu", neoId))
//      _ = println(res)

//      _ <- emoji(client)
//      _ <- reactions(client, message_id)

    } yield ()

  def emoji(client: Emoji[Task]): Task[Unit] =
    for {
      _ <- client.getEmoji(EmojiRequest(EmojiId("5mgk9wk76ffrdggnqkrytbpgch")))
      _ <- client.getEmojiByName(GetEmojiNameRequest("keanu")).map(println(_))
      _ <- client.getEmojiList(GetEmojiListRequest()).map(println(_))
      _ <- client
        .autocompleteEmoji(AutocompleteEmojiRequest("ne"))
        .map(println(_))
    } yield ()

  def reactions(client: Reactions[Task], message: MessageId): Task[Unit] =
    for {
//      _ <- client.createReaction(CreateReactionRequest(neoId, postId, "smile"))
      _ <- client.createReaction(ReactionRequest(neoId, message, "grin"))

//      reactions <- client.getReactions(GetListReactionsRequest(postId))
//      _ = println(reactions)

//      bulk <- client.bulkReactions(postId::Nil)
//      _ = println(bulk)

      _ <- client.removeReaction(ReactionRequest(neoId, message, "grin"))

    } yield ()
}