package muffin.model

import java.time.LocalDateTime

import muffin.api.*

case class ReactionInfo(userId: UserId, postId: MessageId, emojiName: String, createAt: LocalDateTime)
