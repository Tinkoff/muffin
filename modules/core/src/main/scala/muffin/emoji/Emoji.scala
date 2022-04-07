package muffin.emoji

import muffin.predef.*

trait Emoji[F[_]] {
  def createEmoji(req: CreateEmojiRequest): F[CreateEmojiResponse]

  def getEmojiList(req: GetEmojiListRequest): F[GetEmojiListResponse]

  def getEmoji(req: GetEmojiRequest): F[GetEmojiResponse]

  def deleteEmoji(req: DeleteEmojiRequest): F[DeleteEmojiResponse]

  def getEmojiByName(req: GetEmojiNameRequest): F[GetEmojiNameResponse]

  def getEmojiImage(req: GetEmojiImageRequest): F[Unit] = ??? // TODO

  def searchEmoji(req: SearchEmojiRequest): F[SearchEmojiResponse]

  def autocompleteEmoji(
    req: AutocompleteEmojiRequest
  ): F[AutocompleteEmojiResponse]
}
