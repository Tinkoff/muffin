package muffin.users

import muffin.predef.*

trait Users[F[_]] {

  def users(req: GetUsersRequest): F[GetUsersResponse] = ???

  def usersById(req: GetUsersByIdRequest): F[GetUsersByIdResponse] = ???
  
  def usersByUsername(
    req: GetUsersByUsernameRequest
  ): F[GetUsersByUsernameResponse]

  def user(req: GetUserRequest): F[GetUserResponse] = ???
  
  def userByUsername(req: GetUserByUsernameRequest): F[GetUserByUsernameResponse]
}
