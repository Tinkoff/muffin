package muffin

import cats.Monad
import cats.effect.Concurrent
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
import fs2.*
import muffin.ApiClient.params

case class ClientConfig(
                         baseUrl: String,
                         auth: String,
                         botName: String,
                         serviceUrl: String
                       )

case class CreateDirectPostRequest(
                                    message: Option[String] = None,
                                    props: Option[Props] = None,
                                    root_id: Option[MessageId] = None,
                                    file_ids: List[String] = Nil // TODO make Id
                                  ) derives Encoder.AsObject

class ApiClient[F[_] : HttpClient : Concurrent](cfg: ClientConfig)
  extends Posts[F]
    with Dialogs[F]
    with Channels[F]
    with Emoji[F]
    with Reactions[F]
    with Users[F] {

  def command(name: String): String =
    cfg.serviceUrl + s"/commands/$name"

  def dialog(name: String): String =
    cfg.serviceUrl + s"/dialogs/$name"

  def actions(name: String): String =
    cfg.serviceUrl + s"/actions/$name"

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
      cfg.baseUrl + s"/channels/${req.channelId}/members?page=${req.page}&per_page=${req.per_page}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def members(channelId: ChannelId, perPage: Int): Stream[F, ChannelMember] = {
    Stream
      .unfoldEval(0) { page =>
        members(MembersRequest(channelId, page, perPage))
          .map(list => if (list.isEmpty) None else Some((list, page + 1)))
      }
      .flatMap(Stream.emits)
  }

  def direct(req: CreateDirectChannelRequest): F[ChannelInfo] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/channels/direct",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getChannelByName(team: String, name: String): F[ChannelInfo] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/teams/${team}/channels/name/${name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

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

  def users(req: GetUsersRequest): F[GetUsersResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/posts/ids/reactions${
        params(
          "page" -> req.page.toString,
          "per_page" -> req.per_page.toString,
          "in_team" -> req.in_team.toString,
          "not_in_team" -> req.not_in_team.toString,
          "in_channel" -> req.in_channel.toString,
          "not_in_channel" -> req.not_in_channel.toString,
          "in_group" -> req.in_group.toString,
          "group_constrained" -> req.group_constrained.toString,
          "without_team" -> req.without_team.toString,
          "active" -> req.active.toString,
          "inactive" -> req.inactive.toString,
          "role" -> req.role.toString
        )
      }",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def usersStream(req: GetUsersRequest): Stream[F, GetUsersResponse] = {
    Stream
      .unfoldEval(0) { page =>
        users(req.copy(page=page.some))
          .map(list => if (list.isEmpty) None else Some((list, page + 1)))
      }
      .flatMap(Stream.emits)
  }

  def userByUsername(
                      req: GetUserByUsernameRequest
                    ): F[GetUserByUsernameResponse] =
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/users/username/$req",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

}

object ApiClient {
  private def params(params: (String, String)*): String = {
    params.toMap.map(p => s"${p._1}=${p._2}").mkString("?", "&", "")
  }
}


private case class StatusResponse(status: String) derives Codec.AsObject
