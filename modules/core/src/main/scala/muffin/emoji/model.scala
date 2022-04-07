package muffin.emoji
import muffin.predef.*

case class CreateEmojiRequest(image: Array[Byte], emoji: String)

type CreateEmojiResponse = EmojiInfo

case class EmojiInfo(
  id: String, // TODO id,
  creator_id: UserId,
  name: String,
  create_at: Long,
  update_at: Long,
  delete_at: Long
)

case class GetEmojiListRequest(page: Int, per_page: Int, sort: String = "")

type GetEmojiListResponse = List[EmojiInfo]

type GetEmojiRequest = String
type GetEmojiResponse = EmojiInfo

type DeleteEmojiRequest = String
type DeleteEmojiResponse = EmojiInfo

type GetEmojiNameRequest = String
type GetEmojiNameResponse = EmojiInfo

case class SearchEmojiRequest(term: String, prefix_only: Option[String])
type SearchEmojiResponse = List[EmojiInfo]

type AutocompleteEmojiRequest = String
type AutocompleteEmojiResponse = List[EmojiInfo]
