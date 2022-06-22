package muffin.api.reactions

import io.circe.{Codec, Encoder}

import muffin.predef.*

case class ReactionInfo(
  userId: UserId,
  postId: MessageId,
  emojiName: String,
  createAt: Long
) derives Codec.AsObject

