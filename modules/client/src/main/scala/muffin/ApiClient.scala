package muffin

import cats.Applicative
import io.circe.Codec
import muffin.predef.*
import muffin.{Body, HttpClient, Method}
import cats.syntax.all.given
import muffin.channels.*
import muffin.dialogs.*
import muffin.emoji.*
import muffin.posts.*
import muffin.reactions.*


case class ClientConfig(baseUrl: String, auth: String)

class ApiClient[F[_]: HttpClient: Applicative](cfg: ClientConfig)
    extends Posts[F]
    with Dialogs[F]
    with Channels[F]
    with Emoji[F]
    with Reactions[F] {
  def createPost(req: CreatePostRequest): F[CreatePostResponse] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/posts",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def createPostEphemeral(req: CreatePostEphemeral): F[CreatePostResponse] = ???

  def getPost(req: GetPostRequest): F[GetPostResponse] = ???

  def deletePost(req: DeletePostRequest): F[DeletePostResponse] = ???

  def openDialog(req: OpenDialogRequest): F[Unit] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/actions/dialogs/open",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def members(req: MembersRequest): F[List[ChannelMember]] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl,
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def create(req: CreateEmojiRequest): F[CreateEmojiResponse] = ???

  def getEmojiList(req: GetEmojiListRequest): F[GetEmojiListResponse] = ???

  def getEmoji(req: GetEmojiRequest): F[GetEmojiResponse] = ???

  def deleteEmoji(req: DeleteEmojiRequest): F[DeleteEmojiResponse] = ???

  def getEmojiByName(req: GetEmojiNameRequest): F[GetEmojiNameResponse] = ???

  def searchEmoji(req: SearchEmojiRequest): F[SearchEmojiResponse] = ???

  def autocompleteEmoji(
      req: AutocompleteEmojiRequest
  ): F[AutocompleteEmojiResponse] = ???

  def createReaction(req: CreateReactionRequest): F[ReactionInfo] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl + "/reactions",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def getReactions(
      req: GetListReactionsRequest
  ): F[GetListReactionsResponse] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/posts/${req.post_id}/reactions",
      Method.Get,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }

  def removeReaction(req: RemoveReactionRequest): F[RemoveReactionResponse] = {
    summon[HttpClient[F]].request[RemoveReactionRequest, StatusResponse](
      cfg.baseUrl + s"/users/${req.user_id}/posts/${req.post_id}/reactions/${req.emoji_name}",
      Method.Delete,
      Body.Empty,
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    ).map(_.status == "ok")
  }

  def bulkReactions(req: BulkReactionsRequest): F[Map[String, List[ReactionInfo]]] = {
    summon[HttpClient[F]].request(
      cfg.baseUrl + s"/posts/ids/reactions",
      Method.Post,
      Body.Json(req),
      Map("Authorization" -> s"Bearer ${cfg.auth}")
    )
  }
}


private case class StatusResponse(status:String)  derives Codec.AsObject
