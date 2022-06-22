package muffin.api.emoji

import muffin.predef.*
import fs2.Stream

trait Emoji[F[_]] {
  def createEmoji(req: CreateEmojiRequest): F[EmojiInfo]

  def getEmojis(sorting: EmojiSorting = EmojiSorting.None): Stream[F, EmojiInfo]

  def getEmoji(emojiId: EmojiId): F[EmojiInfo]

  def deleteEmoji(emojiId: EmojiId): F[EmojiInfo]

  def getEmojiByName(name: String): F[EmojiInfo]

  def searchEmoji(term: String, prefixOnly: Option[String] = None): F[List[EmojiInfo]]

  def autocompleteEmoji(
                         name: String
                       ): F[List[EmojiInfo]]
}
