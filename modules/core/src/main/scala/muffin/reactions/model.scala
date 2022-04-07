package muffin.reactions

import io.circe.{Codec, Encoder}

import muffin.predef.*

case class ReactionRequest(
  user_id: UserId,
  post_id: MessageId,
  emoji_name: String
) derives Encoder.AsObject

case class ReactionInfo(
  user_id: UserId,
  post_id: MessageId,
  emoji_name: String,
  create_at: Long
) derives Codec.AsObject

type CreateReactionRequest = ReactionRequest
type CreateReactionResponse = ReactionInfo

case class GetListReactionsRequest(post_id: MessageId)
type GetListReactionsResponse = List[ReactionInfo]

type RemoveReactionRequest = ReactionRequest
type RemoveReactionResponse = Boolean

type BulkReactionsRequest = List[MessageId]
type BulkReactionsResponse = Map[String, List[ReactionInfo]]
