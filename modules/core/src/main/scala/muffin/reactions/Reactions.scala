package muffin.reactions

import io.circe.{Codec, Encoder}

trait Reactions[F[_]] {
  def createReaction(req: CreateReactionRequest): F[CreateReactionResponse]

  def getReactions(req: GetListReactionsRequest): F[GetListReactionsResponse]

  def removeReaction(req: RemoveReactionRequest): F[RemoveReactionResponse]

  def bulkReactions(req: BulkReactionsRequest): F[BulkReactionsResponse]
}
