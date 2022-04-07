package muffin.emoji

import muffin.predef.*

trait Emoji[F[_]] {
  def create(req: CreateEmojiRequest): F[CreateEmojiResponse]

  def getEmojiList(req: GetEmojiListRequest): F[GetEmojiListResponse]

  def getEmoji(req: GetEmojiRequest): F[GetEmojiResponse]

  def deleteEmoji(req: DeleteEmojiRequest): F[DeleteEmojiResponse]

  def getEmojiByName(req: GetEmojiNameRequest): F[GetEmojiNameResponse]

  def searchEmoji(req: SearchEmojiRequest): F[SearchEmojiResponse]

  def autocompleteEmoji(
    req: AutocompleteEmojiRequest
  ): F[AutocompleteEmojiResponse]
}
