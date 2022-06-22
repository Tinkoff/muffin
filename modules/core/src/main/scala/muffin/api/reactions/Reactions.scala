package muffin.api.reactions

import muffin.predef.*

trait Reactions[F[_]] {
  def createReaction(userId: UserId,
                     postId: MessageId,
                     emojiName: String): F[ReactionInfo]

  def getReactions(postId: MessageId): F[List[ReactionInfo]]

  def removeReaction(userId: UserId,
                     postId: MessageId,
                     emojiName: String): F[Unit]

  def bulkReactions(messages: List[MessageId]): F[Map[String, List[ReactionInfo]]]
}
