package muffin.posts

import io.circe.*
import io.circe.syntax.given
import muffin.*
import muffin.predef.*

import scala.collection.immutable.List

import predef.*
import fs2.Stream

trait Posts[F[_]] {

  def createPost(req: CreatePostRequest): F[CreatePostResponse]

  def createPostEphemeral(req: CreatePostEphemeral): F[CreatePostResponse]

  def getPost(req: GetPostRequest): F[GetPostResponse]

  def deletePost(req: DeletePostRequest): F[DeletePostResponse]

  def updatePost(req: PatchPostRequest): F[Unit]

  def markUnreadFrom(): F[Unit] = ???

  def patchPost(req: PatchPostRequest): F[CreatePostResponse]

  def getThread(): F[Unit] = ???

  def getFlaggedPosts(): F[Unit] = ???

//  def getPostsForChannel(channelId: ChannelId, modifiers: Option[SearchPostModifiers]): Stream[F, Post]

  def searchTeamPosts(): F[Unit] = ???

  def pinPost(req: PinPostRequest): F[PinPostResponse] = ???

  def unpinChannel(req: UnpinPostRequest): F[UnpinPostResponse] = ???

  def performAction(req: PerformActionRequest): F[PerformActionResponse]

  def getPostsByIds(req: GetPostsByIdsRequest): F[GetPostsByIdsResponse] = ???

}
