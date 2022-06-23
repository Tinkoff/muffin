package muffin.api.users

import muffin.predef.*

case class GetUserOptions(
                           inTeam: Option[TeamId] = None,
                           notInTeam: Option[TeamId] = None,
                           inChannel: Option[ChannelId] = None,
                           notInChannel: Option[ChannelId] = None,
                           active: Option[Boolean] = None,
                         )

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
               )
