package muffin.api.users

import muffin.predef.*

import fs2.Stream

trait Users[F[_]] {
  def users(options: GetUserOptions): Stream[F, User]

  def usersById(userIds: List[UserId]): F[List[User]]

  def usersByUsername(users: List[String]): F[List[User]]

  def user(userId: UserId): F[User]

  def userByUsername(user: String): F[User]
}
