package muffin.emoji
import io.circe.{Codec, Decoder, Encoder}
import muffin.predef.*

import java.io.File

opaque type EmojiId = String
object EmojiId {
  def apply(id: String): EmojiId = id

  given encoder: Encoder[EmojiId] = Encoder.encodeString.contramap(identity)
  given decoder: Decoder[EmojiId] = Decoder.decodeString.map(identity)
}

case class EmojiInfo(
  id: EmojiId,
  creator_id: UserId,
  name: String,
  create_at: Long,
  update_at: Long,
  delete_at: Long
) derives Codec.AsObject

case class EmojiRequest(emoji_id: EmojiId)

case class CreateEmojiRequest(image: File, emojiName: String, creatorId: UserId)
type CreateEmojiResponse = EmojiInfo

case class GetEmojiListRequest(page: Int = 0, per_page: Int = 60, sort: String = "")
type GetEmojiListResponse = List[EmojiInfo]

type GetEmojiRequest = EmojiRequest
type GetEmojiResponse = EmojiInfo

type DeleteEmojiRequest = EmojiRequest
type DeleteEmojiResponse = EmojiInfo

case class GetEmojiNameRequest(name: String)
type GetEmojiNameResponse = EmojiInfo

case class SearchEmojiRequest(term: String, prefix_only: Option[String])
    derives Encoder.AsObject
type SearchEmojiResponse = List[EmojiInfo]

type GetEmojiImageRequest = EmojiRequest

case class AutocompleteEmojiRequest(name: String)
type AutocompleteEmojiResponse = List[EmojiInfo]
