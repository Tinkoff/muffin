package muffin.api

import cats.Monad
import cats.effect.Concurrent
import cats.syntax.all.given
import fs2.*
import io.circe.{Codec, Encoder, Json, JsonObject}
import muffin.api.ApiClient.params
import muffin.api.channels.*
import muffin.api.dialogs.{Dialog, Dialogs}
import muffin.api.emoji.*
import muffin.api.insights.*
import muffin.api.posts
import muffin.api.posts.*
import muffin.api.preferences.*
import muffin.api.reactions.*
import muffin.api.roles.*
import muffin.api.status.*
import muffin.api.users.*
import muffin.codec.*
import muffin.predef.*
import muffin.http.*

import java.time.*

case class CreateDirectPostRequest(
                                    message: Option[String] = None,
                                    props: Option[Props] = None,
                                    root_id: Option[MessageId] = None,
                                    file_ids: List[String] = Nil // TODO make Id
                                  ) derives Encoder.AsObject

class ApiClient[
  F[_] : Concurrent,
  R,
  To[_],
  From[_]
](http: HttpClient[F, To, From], cfg: ClientConfig)(codec: CodecSupport[R, To, From])(using ZoneId)
  extends Posts[F]
    with Dialogs[F]
    with Channels[F]
    with Emoji[F]
    with Reactions[F]
    with Users[F]
    with Preferences[F]
    with Status[F]
    with Insights[F]
    with Roles[F] {

  import codec.{given, *}
  import ApiClient.*

  private given To[NonJson] = assert(false).asInstanceOf[Nothing] // should never call

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
        posts.CreatePostRequest(
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
        posts.CreatePostRequest(
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
  def openDialog(triggerId: String, url: String, dialog: Dialog): F[Unit] =
    http
      .request(
        cfg.baseUrl + "/actions/dialogs/open",
        Method.Post,
        json
          .field("trigger_id", triggerId)
          .field("url", url)
          .field("dialog", dialog)
          .build,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def members(channelId: ChannelId): Stream[F, ChannelMember] = {
    def single(page: Int): F[List[ChannelMember]] =
      http.request[NonJson, List[ChannelMember]](
        cfg.baseUrl + s"/channels/$channelId/members?page=$page",
        Method.Get,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

    Stream
      .unfoldEval(0) { page =>
        single(page)
          .map(list => if (list.isEmpty) None else Some((list, page + 1)))
      }
      .flatMap(Stream.emits)
  }

  def direct(userIds: List[UserId]): F[ChannelInfo] =
    http.request(
      cfg.baseUrl + "/channels/direct",
      Method.Post,
      Body.Json(userIds),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getChannelByName(teamId: TeamId, name: String): F[ChannelInfo] = {
    http.request[NonJson, ChannelInfo](
      cfg.baseUrl + s"/teams/$teamId/channels/name/$name",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def createEmoji(req: CreateEmojiRequest): F[EmojiInfo] =
    http.request[NonJson, EmojiInfo](
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

  def getEmojis(sorting: EmojiSorting = EmojiSorting.None): Stream[F, EmojiInfo] = ???
  //    http.request[NonJson, GetEmojiListResponse](
  //      cfg.baseUrl + s"/emoji?page=${req.page}&per_page=${req.per_page}&sort=${req.sort}",
  //      Method.Get,
  //      Body.Empty,
  //      Map("Authorization" -> s"Bearer ${cfg.auth}")
  //    )

  def getEmoji(emojiId: EmojiId): F[EmojiInfo] =
    http.request[NonJson, EmojiInfo](
      cfg.baseUrl + s"/emoji/$emojiId",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def deleteEmoji(emojiId: EmojiId): F[EmojiInfo] =
    http.request[NonJson, EmojiInfo](
      cfg.baseUrl + s"/emoji/name/$emojiId",
      Method.Delete,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getEmojiByName(name: String): F[EmojiInfo] =
    http.request[NonJson, EmojiInfo](
      cfg.baseUrl + s"/emoji/name/$name",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def searchEmoji(term: String, prefixOnly: Option[String]): F[List[EmojiInfo]] =
    http.request(
      cfg.baseUrl + s"/emoji/search",
      Method.Post,
      json
        .field("term", term)
        .field("prefix_only", prefixOnly)
        .build,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def autocompleteEmoji(
                         name: String
                       ): F[List[EmojiInfo]] =
    http.request[NonJson, List[EmojiInfo]](
      cfg.baseUrl + s"/emoji/autocomplete?name=$name",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def createReaction(userId: UserId,
                     postId: MessageId,
                     emojiName: String): F[ReactionInfo] =
    http.request(
      cfg.baseUrl + "/reactions",
      Method.Post,
      json
        .field("user_id", userId)
        .field("post_id", postId)
        .field("emoji_name", emojiName)
        .build,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getReactions(postId: MessageId): F[List[ReactionInfo]] =
    http.request[NonJson, List[ReactionInfo]](
      cfg.baseUrl + s"/posts/$postId/reactions",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def removeReaction(userId: UserId,
                     postId: MessageId,
                     emojiName: String): F[Unit] =
    http
      .request[NonJson, Unit](
        cfg.baseUrl + s"/users/$userId/posts/$postId/reactions/$emojiName",
        Method.Delete,
        Body.Empty,
        Map("Authorization" -> s"Bearer ${cfg.auth}")
      )

  def bulkReactions(messages: List[MessageId]): F[Map[String, List[ReactionInfo]]] =
    http.request(
      cfg.baseUrl + s"/posts/ids/reactions",
      Method.Post,
      Body.Json(messages),
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
  def getTopReactions(teamId: TeamId, timeRange: TimeRange): Stream[F, ReactionInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ReactionInsight]](
        cfg.baseUrl + s"/teams/$teamId/top/reactions?time_range=$timeRange&page=$page",
        Method.Get,
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

  def getTopReactions(userId: UserId, timeRange: TimeRange, teamId: Option[TeamId]): Stream[F, ReactionInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ReactionInsight]](
        cfg.baseUrl + s"/users/$userId/top/reactions?time_range=$timeRange&page=$page${teamId.map(id => s"&team_id=$id")}",
        Method.Get,
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

  def getTopChannels(teamId: TeamId, timeRange: TimeRange): Stream[F, ChannelInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ChannelInsight]](
        cfg.baseUrl + s"/teams/$teamId/top/channels?time_range=$timeRange&page=$page",
        Method.Get,
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

  def getTopChannels(userId: UserId, timeRange: TimeRange, teamId: Option[TeamId]): Stream[F, ChannelInsight] = {
    def single(page: Int) =
      http.request[NonJson, ListWrapper[ChannelInsight]](
        cfg.baseUrl + s"/users/$userId/top/channels?time_range=$timeRange&page=$page${teamId.map(id => s"&team_id=$id")}",
        Method.Get,
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

  //  Roles
  def getAllRoles: F[List[RoleInfo]] =
    http.request[NonJson, List[RoleInfo]](
      cfg.baseUrl + s"/roles",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getRoleById(id: String): F[RoleInfo] =
    http.request[NonJson, RoleInfo](
      cfg.baseUrl + s"/roles/$id",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getRoleByName(name: String): F[RoleInfo] =
    http.request[NonJson, RoleInfo](
      cfg.baseUrl + s"/roles/name/$name",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def updateRole(id: String, permissions: List[String]): F[RoleInfo] =
    http.request(
      cfg.baseUrl + s"/roles/$id/patch",
      Method.Patch,
      Body.Json(permissions),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )

  def getRoles(names: List[String]): F[List[RoleInfo]] =
    http.request(
      cfg.baseUrl + s"/roles/names",
      Method.Post,
      Body.Json(names),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  //  Roles
}

object ApiClient {
  private case class NonJson()

  private def params(params: (String, String)*): String = {
    params.toMap.map(p => s"${p._1}=${p._2}").mkString("?", "&", "")
  }
}


private case class StatusResponse(status: String) derives Codec.AsObject
