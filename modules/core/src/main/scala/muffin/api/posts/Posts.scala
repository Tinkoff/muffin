package muffin.api.posts

import muffin.*
import muffin.predef.*

import scala.collection.immutable.List

import predef.*
import fs2.Stream

trait Posts[F[_]] {
  def postToDirect(
                    userId: UserId,
                    message: Option[String] = None,
                    props: Option[Props] = None
                  ): F[Post]

  def postToChat(
                  userIds: List[UserId],
                  message: Option[String] = None,
                  props: Option[Props] = None
                ): F[Post]
  def postToChannel(channelId: ChannelId,
                    message: Option[String] = None,
                    props: Option[Props] = None
                   ): F[Post]

  def createEphemeralPost(userId: UserId, channelId: ChannelId, message: String): F[Post]

  def getPost(postId: MessageId): F[Post]

  def deletePost(postId: MessageId): F[Unit]

  def updatePost(postId: MessageId,
                 message: Option[String] = None,
                 props: Option[Props] = None): F[Post]

  def patchPost(postId: MessageId,
                message: Option[String] = None,
                props: Option[Props] = None
               ): F[Post]

  def getPostsByIds(messageId: List[MessageId]): F[List[Post]]
}
