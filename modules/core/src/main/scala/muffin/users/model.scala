package muffin.users

import muffin.predef.*

case class GetUsersRequest(
  page: Option[Int],
  per_page: Option[Int],
  in_team: Option[String],
  not_in_team: Option[String],
  in_channel: Option[String],
  not_in_channel: Option[String],
  in_group: Option[String],
  group_constrained: Option[Boolean],
  without_team: Option[Boolean],
  active: Option[Boolean],
  inactive: Option[Boolean],
  role: Option[String],
  //    sort
  roles: Option[List[String]],
  channel_roles: Option[List[String]],
  team_roles: Option[List[String]]
)

type GetUsersResponse = List[User]

type GetUsersByIdRequest = List[UserId]
type GetUsersByIdResponse = List[User]

type GetUsersByUsernameRequest = List[String]
type GetUsersByUsernameResponse = List[User]

type GetUserRequest = UserId
type GetUserResponse = User

case class User(
  id: UserId,
  create_at: Long,
  update_at: Long,
  delete_at: Long,
  username: String,
  first_name: String,
  last_name: String,
  nickname: String,
  email: String,
  email_verified: Boolean,
  auth_service: String,
  roles: String,
  locale: String,
  //    notify_props
  last_password_update: Long,
  last_picture_update: Long,
  failed_attempts: Long,
  mfa_active: Boolean,
  //    timezone
  terms_of_service_id: String,
  terms_of_service_create_at: Long
)
