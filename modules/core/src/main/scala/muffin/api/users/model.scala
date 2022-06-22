package muffin.api.users

import io.circe.Codec
import muffin.predef.*

case class GetUsersRequest(
                            page: Option[Int] = None,
                            per_page: Option[Int] = None,
                            in_team: Option[String] = None,
                            not_in_team: Option[String] = None,
                            in_channel: Option[String] = None,
                            not_in_channel: Option[String] = None,
                            in_group: Option[String] = None,
                            group_constrained: Option[Boolean] = None,
                            without_team: Option[Boolean] = None,
                            active: Option[Boolean] = None,
                            inactive: Option[Boolean] = None,
                            role: Option[String] = None,
                            //    sort
                            roles: Option[List[String]] = None,
                            channel_roles: Option[List[String]] = None,
                            team_roles: Option[List[String]] = None
                          )

type GetUsersResponse = List[User]

type GetUsersByIdRequest = List[UserId]
type GetUsersByIdResponse = List[User]

type GetUsersByUsernameRequest = List[String]
type GetUsersByUsernameResponse = List[User]

type GetUserRequest = UserId
type GetUserResponse = User

type GetUserByUsernameRequest = String
type GetUserByUsernameResponse = User

case class User(id: UserId,
                create_at: Option[Long],
                update_at: Option[Long],
                delete_at: Option[Long],
                username: String,
                first_name: Option[String],
                last_name: Option[String],
                nickname: Option[String],
                email: Option[String],
                email_verified: Option[Boolean],
                auth_service: Option[String],
                roles: Option[String],
                locale: Option[String],
                //    notify_props
                last_password_update: Option[Long],
                last_picture_update: Option[Long],
                failed_attempts: Option[Long],
                mfa_active: Option[Boolean],
                //    timezone
                terms_of_service_id: Option[String],
                terms_of_service_create_at: Option[Long]
               ) derives Codec.AsObject
