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
                username: String,
                first_name: Option[String],
                last_name: Option[String],
                nickname: Option[String],
                email: Option[String],
                roles: Option[String],
                locale: Option[String],
               )
