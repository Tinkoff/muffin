package muffin.users

import muffin.predef.*

import fs2.Stream

trait Users[F[_]] {

  def users(req: GetUsersRequest): F[GetUsersResponse]

  def usersStream(req: GetUsersRequest): Stream[F, GetUsersResponse]

  def usersById(req: GetUsersByIdRequest): F[GetUsersByIdResponse] = ???

  def usersByUsername(
    req: GetUsersByUsernameRequest
  ): F[GetUsersByUsernameResponse] = ???

  def user(req: GetUserRequest): F[GetUserResponse] = ???

  def userByUsername(req: GetUserByUsernameRequest): F[GetUserByUsernameResponse]
}
