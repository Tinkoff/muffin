package muffin.posts

import io.circe.*
import io.circe.syntax.given
import muffin.*
import muffin.predef.*

import scala.collection.immutable.List

import predef.*

trait Posts[F[_]] {

  def createPost(req: CreatePostRequest): F[CreatePostResponse]

  def createPostEphemeral(req: CreatePostEphemeral): F[CreatePostResponse]

  def getPost(req: GetPostRequest): F[GetPostResponse]

  def deletePost(req: DeletePostRequest): F[DeletePostResponse]

  def updatePost(): F[Unit] = ???

  def markUnreadFrom(): F[Unit] = ???

  def patchPost(): F[Unit] = ???

  def getThread(): F[Unit] = ???

  def getFlaggedPosts(): F[Unit] = ???

  def getPostsForChannel(): F[Unit] = ???

  def searchTeamPosts(): F[Unit] = ???

  def pinPost(): F[Unit] = ???

  def unpinChannel(): F[Unit] = ???

  def performAction(req: PerformActionRequest): F[PerformActionResponse]

  def getPostsByIds(): F[Unit] = ???

}

case class PerformActionRequest(post_id: MessageId, action_id: String)
type PerformActionResponse = Boolean
