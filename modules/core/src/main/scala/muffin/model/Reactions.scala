package muffin.model

import java.time.LocalDateTime

case class ReactionInfo(userId: UserId, postId: MessageId, emojiName: String, createAt: LocalDateTime)
