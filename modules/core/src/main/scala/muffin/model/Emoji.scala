package muffin.model

import java.io.File
import java.time.LocalDateTime

import cats.Show
import fs2.Stream

import muffin.api.*
import muffin.internal.NewType

type EmojiId = EmojiId.Type
object EmojiId extends NewType[String]

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

case class CreateEmojiRequest(image: Array[Byte], emojiName: String, creatorId: UserId)
