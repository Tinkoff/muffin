package muffin.model

import java.io.File
import java.time.LocalDateTime

import cats.Show
import fs2.Stream

import muffin.api.*

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

enum EmojiSorting {
  case None
  case Name
}

object EmojiSorting {

  given Show[EmojiSorting] = {
    case EmojiSorting.None => ""
    case EmojiSorting.Name => "name"
  }

}

case class CreateEmojiRequest(image: File, emojiName: String, creatorId: UserId)
