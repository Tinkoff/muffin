package muffin

import cats.Monad
import cats.effect.Concurrent
import io.circe.{Codec, Encoder, Json, JsonObject}
import muffin.predef.*
import muffin.http.*
import muffin.http.given
import cats.syntax.all.given
import muffin.channels.*
import muffin.dialogs.*
import muffin.emoji.*
import muffin.posts.*
import muffin.reactions.*
import muffin.users.*
import fs2.*
import muffin.ApiClient.params
import muffin.codec.*
import muffin.insights.*
import muffin.preferences.*
import muffin.status.*

import java.time.*

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

class ApiClient[F[_] : Concurrent, To[_], From[_]](http: HttpClient[F, To, From], cfg: ClientConfig)(codec: MuffinCodec[To, From])(using ZoneId)
  extends Posts[F]
    with Dialogs[F]
    with Channels[F]
    with Emoji[F]
    with Reactions[F]
    with Users[F]
    with Preferences[F]
    with Status[F]
    with Insights[F] {

  import codec.{given, *}
  import ApiClient.*

  private given To[NonJson] = assert(true).asInstanceOf[Nothing] // should never call

  def command(name: String): String =
    cfg.serviceUrl + s"/commands/$name"

  def dialog(name: String): String =
    cfg.serviceUrl + s"/dialogs/$name"

  def actions(name: String): String =
    cfg.serviceUrl + s"/actions/$name"

  def botId: F[UserId] = userByUsername(cfg.botName).map(_.id)

  def createPost(req: CreatePostRequest): F[CreatePostResponse] =
    http.request(
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

  def deletePost(req: DeletePostRequest): F[Unit] =
    http
      .request[DeletePostRequest, Unit](
        cfg.baseUrl + s"posts/$req",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def updatePost(req: PatchPostRequest): F[Unit] =
    http
      .request(
        cfg.baseUrl + s"/posts/${req.post_id}",
        Method.Put,
        Body.Json(req),
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def patchPost(req: PatchPostRequest): F[CreatePostResponse] =
    http
      .request(
        cfg.baseUrl + s"/posts/${req.post_id}/patch",
        Method.Put,
        Body.Json(req),
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def performAction(req: PerformActionRequest): F[PerformActionResponse] =
    http.request[NonJson, PerformActionResponse](
      cfg.baseUrl + s"/posts/${req.post_id}/actions/${req.action_id}",
      Method.Post,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  ////////////////////////////////////////////////

  def openDialog(req: OpenDialogRequest): F[Unit] =
    http
      .request[OpenDialogRequest, Json](
        cfg.baseUrl + "/actions/dialogs/open",
        Method.Post,
        Body.Json(req),
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )
      .map(a => println(a))

  def members(req: MembersRequest): F[List[ChannelMember]] =
    http.request[NonJson, List[ChannelMember]](
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
    http.request(
      cfg.baseUrl + "/channels/direct",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getChannelByName(team: String, name: String): F[ChannelInfo] = {
    http.request[NonJson, ChannelInfo](
      cfg.baseUrl + s"/teams/$team/channels/name/${name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def createEmoji(req: CreateEmojiRequest): F[CreateEmojiResponse] =
    http.request[NonJson, CreateEmojiResponse](
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
    http.request[NonJson, GetEmojiListResponse](
      cfg.baseUrl + s"/emoji?page=${req.page}&per_page=${req.per_page}&sort=${req.sort}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getEmoji(req: GetEmojiRequest): F[GetEmojiResponse] =
    http.request[NonJson, GetEmojiResponse](
      cfg.baseUrl + s"/emoji/${req.emoji_id}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def deleteEmoji(req: DeleteEmojiRequest): F[DeleteEmojiResponse] =
    http.request[NonJson, DeleteEmojiResponse](
      cfg.baseUrl + s"/emoji/name/${req.emoji_id}",
      Method.Delete,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getEmojiByName(req: GetEmojiNameRequest): F[GetEmojiNameResponse] =
    http.request[NonJson, GetEmojiResponse](
      cfg.baseUrl + s"/emoji/name/${req.name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def searchEmoji(req: SearchEmojiRequest): F[SearchEmojiResponse] =
    http.request(
      cfg.baseUrl + s"/emoji/search",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def autocompleteEmoji(
                         req: AutocompleteEmojiRequest
                       ): F[AutocompleteEmojiResponse] =
    http.request[NonJson, AutocompleteEmojiResponse](
      cfg.baseUrl + s"/emoji/autocomplete?name=${req.name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def createReaction(req: CreateReactionRequest): F[ReactionInfo] =
    http.request(
      cfg.baseUrl + "/reactions",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getReactions(req: GetListReactionsRequest): F[GetListReactionsResponse] =
    http.request[NonJson, GetListReactionsResponse](
      cfg.baseUrl + s"/posts/${req.post_id}/reactions",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def removeReaction(req: RemoveReactionRequest): F[RemoveReactionResponse] =
    http
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
    http.request(
      cfg.baseUrl + s"/posts/ids/reactions",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def users(req: GetUsersRequest): F[GetUsersResponse] =
    http.request[NonJson, GetUsersResponse](
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

  def usersStream(req: GetUsersRequest): Stream[F, User] = {
    Stream
      .unfoldEval(0) { page =>
        users(req.copy(page = page.some))
          .map(list => if (list.isEmpty) None else Some((list, page + 1)))
      }
      .flatMap(Stream.emits)
  }

  def userByUsername(
                      req: GetUserByUsernameRequest
                    ): F[GetUserByUsernameResponse] =
    http.request[NonJson, GetUserByUsernameResponse](
      cfg.baseUrl + s"/users/username/$req",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  //  Preferences
  def getUserPreferences(userId: UserId): F[List[Preference]] =
    http.request[NonJson, List[Preference]](
      cfg.baseUrl + s"/users/$userId/preferences",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getUserPreferences(userId: UserId, category: String): F[List[Preference]] =
    http.request[NonJson, List[Preference]](
      cfg.baseUrl + s"/users/$userId/preferences/${category}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getUserPreference(userId: UserId, category: String, name: String): F[Preference] =
    http.request[NonJson, Preference](
      cfg.baseUrl + s"/users/$userId/preferences/${category}/name/${name}",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def saveUserPreference(userId: UserId, preferences: List[Preference]): F[Unit] =
    http.request(
      cfg.baseUrl + s"/users/$userId/preferences",
      Method.Put,
      Body.Json(preferences),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def deleteUserPreference(userId: UserId, preferences: List[Preference]): F[Unit] = {
    http.request(
      cfg.baseUrl + s"/users/$userId/preferences/delete",
      Method.Post,
      Body.Json(preferences),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  //  Preferences


  // Status
  def getUserStatus(userId: UserId): F[UserStatus] = {
    http.request[NonJson, UserStatus](
      cfg.baseUrl + s"/users/$userId/status",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def getUserStatuses(users: List[UserId]): F[List[UserStatus]] = {
    http.request(
      cfg.baseUrl + s"/users/status/ids",
      Method.Post,
      Body.Json(users),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def updateUserStatus(userId: UserId, statusUser: StatusUser): F[Unit] =
    http.request(
      cfg.baseUrl + s"/users/$userId/status/custom",
      Method.Put,
      Body.Json(UpdateUserStatusRequest(userId, statusUser)),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def updateCustomStatus(userId: UserId, customStatus: CustomStatus): F[Unit] = {
    http.request(
      cfg.baseUrl + s"/users/$userId/status/custom",
      Method.Put,
      Body.Json(customStatus),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def unsetCustomStatus(userId: UserId): F[Unit] = {
    http.request[NonJson, Unit](
      cfg.baseUrl + s"/users/$userId/status/custom",
      Method.Delete,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }
  // Status


  // Insights
  def getTopReactions(teamId: String, timeRange: TimeRange): Stream[F, ReactionInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ReactionInsight]](
        cfg.baseUrl + s"/teams/$teamId/top/reactions?time_range=$timeRange&page=$page",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

    Stream
      .unfoldEval(0) { page =>
        single(page).map(lw =>
          if (lw.hasNext) Some(lw.items -> (page + 1)) else None
        )
      }
      .flatMap(Stream.emits)
  }

  def getTopReactions(userId: UserId, timeRange: TimeRange, teamId: Option[String]): Stream[F, ReactionInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ReactionInsight]](
        cfg.baseUrl + s"/users/$userId/top/reactions?time_range=$timeRange&page=$page${teamId.map(id => s"&team_id=$id")}",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

    Stream
      .unfoldEval(0) { page =>
        single(page).map(lw =>
          if (lw.hasNext) Some(lw.items -> (page + 1)) else None
        )
      }
      .flatMap(Stream.emits)
  }

  def getTopChannels(teamId: String, timeRange: TimeRange): Stream[F, ChannelInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ChannelInsight]](
        cfg.baseUrl + s"/teams/$teamId/top/channels?time_range=$timeRange&page=$page",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

    Stream
      .unfoldEval(0) { page =>
        single(page).map(lw =>
          if (lw.hasNext) Some(lw.items -> (page + 1)) else None
        )
      }
      .flatMap(Stream.emits)
  }

  def getTopChannels(userId: UserId, timeRange: TimeRange, teamId: Option[String]): Stream[F, ChannelInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ChannelInsight]](
        cfg.baseUrl + s"/users/$userId/top/channels?time_range=$timeRange&page=$page${teamId.map(id => s"&team_id=$id")}",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

    Stream
      .unfoldEval(0) { page =>
        single(page).map(lw =>
          if (lw.hasNext) Some(lw.items -> (page + 1)) else None
        )
      }
      .flatMap(Stream.emits)
  }
  // Insights

}

object ApiClient {
  private case class NonJson()

  private def params(params: (String, String)*): String = {
    params.toMap.map(p => s"${p._1}=${p._2}").mkString("?", "&", "")
  }
}


private case class StatusResponse(status: String) derives Codec.AsObject
