package muffin

import cats.Monad
import io.circe.{Codec, Encoder, Json, JsonObject}
import muffin.predef.*
import muffin.{Body, HttpClient, Method}
import cats.syntax.all.given
import muffin.channels.*
import muffin.dialogs.*
import muffin.emoji.*
import muffin.posts.*
import muffin.reactions.*
import muffin.users.*

case class ClientConfig(baseUrl: String, auth: String, botName: String)

case class CreateDirectPostRequest(
  message: Option[String] = None,
  props: Option[Props] = None,
  root_id: Option[MessageId] = None,
  file_ids: List[String] = Nil // TODO make Id
) derives Encoder.AsObject

class ApiClient[F[_]: HttpClient: Monad](cfg: ClientConfig)
    extends Posts[F]
    with Dialogs[F]
    with Channels[F]
    with Emoji[F]
    with Reactions[F]
    with Users[F] {

  def botId: F[UserId] = userByUsername(cfg.botName).map(_.id)

  def createPost(req: CreatePostRequest): F[CreatePostResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/posts",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def createPost(
    userId: UserId,
    req: CreateDirectPostRequest
  ): F[CreatePostResponse] =
    for {
      id <- botId
      info <- direct(userId :: id :: Nil)
      res <- createPost(
        CreatePostRequest(
          info.id,
          req.message,
          req.props,
          req.root_id,
          req.file_ids
        )
      )
    } yield res

  def createPost(
    userIds: List[UserId],
    req: CreateDirectPostRequest
  ): F[CreatePostResponse] =
    for {
      id <- botId
      info <- direct(id :: userIds)
      res <- createPost(
        CreatePostRequest(
          info.id,
          req.message,
          req.props,
          req.root_id,
          req.file_ids
        )
      )
    } yield res

  def createPostEphemeral(req: CreatePostEphemeral): F[CreatePostResponse] = ???

  def getPost(req: GetPostRequest): F[GetPostResponse] = ???

  def deletePost(req: DeletePostRequest): F[DeletePostResponse] =
    summon[HttpClient[F]]
      .request[DeletePostRequest, Json](
        cfg.baseUrl + s"posts/$req",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )
      .map(a => println(a))

  def updatePost(req: PatchPostRequest): F[Unit] =
    summon[HttpClient[F]]
      .request(
        cfg.baseUrl + s"/posts/${req.post_id}",
        Method.Put,
        Body.Json(req),
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def patchPost(req: PatchPostRequest): F[CreatePostResponse] =
    summon[HttpClient[F]]
      .request(
        cfg.baseUrl + s"/posts/${req.post_id}/patch",
        Method.Put,
        Body.Json(req),
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def performAction(req: PerformActionRequest): F[PerformActionResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/posts/${req.post_id}/actions/${req.action_id}",
      Method.Post,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  ////////////////////////////////////////////////

  def openDialog(req: OpenDialogRequest): F[Unit] =
    summon[HttpClient[F]]
      .request[OpenDialogRequest, Json](
        cfg.baseUrl + "/actions/dialogs/open",
        Method.Post,
        Body.Json(req),
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )
      .map(a => println(a))

  def members(req: MembersRequest): F[List[ChannelMember]] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/channels/${req.channelId}/members", // TODO pagination
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def direct(req: CreateDirectChannelRequest): F[ChannelInfo] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/channels/direct",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def createEmoji(req: CreateEmojiRequest): F[CreateEmojiResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji",
      Method.Post,
      Body.Multipart(
        MultipartElement.FileElement("image", req.image) ::
          MultipartElement.StringElement(
            "emoji",
            Json
              .fromJsonObject(
                JsonObject(
                  "creator_id" -> Json.fromString(req.creatorId.toString),
                  "name" -> Json.fromString(req.emojiName)
                )
              )
              .noSpaces
          ) :: Nil
      ),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getEmojiList(req: GetEmojiListRequest): F[GetEmojiListResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji?page=${req.page}&per_page=${req.per_page}&sort=${req.sort}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getEmoji(req: GetEmojiRequest): F[GetEmojiResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji/${req.emoji_id}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def deleteEmoji(req: DeleteEmojiRequest): F[DeleteEmojiResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji/name/${req.emoji_id}",
      Method.Delete,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getEmojiByName(req: GetEmojiNameRequest): F[GetEmojiNameResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji/name/${req.name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def searchEmoji(req: SearchEmojiRequest): F[SearchEmojiResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji/search",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def autocompleteEmoji(
    req: AutocompleteEmojiRequest
  ): F[AutocompleteEmojiResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/emoji/autocomplete?name=${req.name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def createReaction(req: CreateReactionRequest): F[ReactionInfo] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/reactions",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getReactions(req: GetListReactionsRequest): F[GetListReactionsResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/posts/${req.post_id}/reactions",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def removeReaction(req: RemoveReactionRequest): F[RemoveReactionResponse] =
    summon[HttpClient[F]]
      .request[RemoveReactionRequest, StatusResponse](
        cfg.baseUrl + s"/users/${req.user_id}/posts/${req.post_id}/reactions/${req.emoji_name}",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )
      .map(_.status == "ok")

  def bulkReactions(
    req: BulkReactionsRequest
  ): F[Map[String, List[ReactionInfo]]] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/posts/ids/reactions",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def userByUsername(
    req: GetUserByUsernameRequest
  ): F[GetUserByUsernameResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/users/usernames/$req",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

}

private case class StatusResponse(status: String) derives Codec.AsObject
