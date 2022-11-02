package muffin.model

import fs2.Stream

import muffin.api.*

case class GetUserOptions(
    inTeam: Option[TeamId] = None,
    notInTeam: Option[TeamId] = None,
    inChannel: Option[ChannelId] = None,
    notInChannel: Option[ChannelId] = None,
    active: Option[Boolean] = None
)

case class User(
    id: UserId,
    username: String,
    firstName: Option[String],
    lastName: Option[String],
    nickname: Option[String],
    email: Option[String],
    roles: Option[String],
    locale: Option[String]
)
