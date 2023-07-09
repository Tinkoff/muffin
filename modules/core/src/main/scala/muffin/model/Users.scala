package muffin.model

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
