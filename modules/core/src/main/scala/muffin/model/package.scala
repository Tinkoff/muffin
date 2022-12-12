package muffin.model

import cats.Show

import muffin.internal.NewType

type Login = Login.Type
object Login extends NewType[String]

type UserId = UserId.Type
object UserId extends NewType[String]

type GroupId = GroupId.Type
object GroupId extends NewType[String]

type TeamId = TeamId.Type
object TeamId extends NewType[String]

type ChannelId = ChannelId.Type
object ChannelId extends NewType[String]

type MessageId = MessageId.Type
object MessageId extends NewType[String]
