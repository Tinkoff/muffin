package muffin.api.emoji

import io.circe.{Codec, Decoder, Encoder}
import muffin.predef.*

import java.io.File

opaque type EmojiId = String

object EmojiId {
  def apply(id: String): EmojiId = id
}

case class EmojiInfo(
                      id: EmojiId,
                      creatorId: UserId,
                      name: String,
                      createAt: Long,
                      updateAt: Long,
                      deleteAt: Long
                    )


enum EmojiSorting:
  case None
  case Name(text: String)

case class CreateEmojiRequest(image: File, emojiName: String, creatorId: UserId)
