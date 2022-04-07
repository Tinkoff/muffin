package muffin.posts

import io.circe.*
import io.circe.syntax.given
import muffin.*
import muffin.predef.*

import scala.collection.immutable.List

trait Posts[F[_]] {

  def createPost(req: CreatePostRequest): F[CreatePostResponse]

  def createPostEphemeral(req: CreatePostEphemeral): F[CreatePostResponse]

  def getPost(req: GetPostRequest): F[GetPostResponse]

  def deletePost(req: DeletePostRequest): F[DeletePostResponse]

}
