package muffin.api.reactions

import muffin.predef.*
import java.time.LocalDateTime

case class ReactionInfo(
  userId: UserId,
  postId: MessageId,
  emojiName: String,
  createAt: LocalDateTime
)

