package muffin.api.emoji

import muffin.predef.*

import java.io.File
import java.time.LocalDateTime

opaque type EmojiId = String

object EmojiId {
  def apply(id: String): EmojiId = id
}

case class EmojiInfo(
  id: EmojiId,
  creatorId: UserId,
  name: String,
  createAt: LocalDateTime,
  updateAt: LocalDateTime,
  deleteAt: Option[LocalDateTime]
)


enum EmojiSorting:
  case None
  case Name(text: String)

case class CreateEmojiRequest(image: File, emojiName: String, creatorId: UserId)
