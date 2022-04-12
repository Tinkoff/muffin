package muffin.app

import muffin.predef.*

case class CommandContext(
  channelId: ChannelId,
  channelName: String,
  responseUrl: String, // TODO URL
  teamDomain: String,
  teamId: String, // TODO ID,
  text: Option[String],
  triggerId: String,
  userId: UserId,
  userName: String
)

sealed trait AppResponse

//channel_id=jf4165gyybybtjd1yxtb1asf9a
//channel_name=off-topic
//command=%2Fkek
//response_url=http%3A%2F%2Flocalhost%3A8065%2Fhooks%2Fcommands%2Fnnmhhozqafd9ifk17eozwjqkqw
//team_domain=test
//team_id=ww39dn9jejy8jjkuooysc18crh
//text=
//token=njkhjqngpfgnpeozd4eikexmxe
//trigger_id=NTE1cmZodWt3cDhiOGozMW1haW1tM3RyYWg6NGlzYXllcmNydGJqM2VhODVjMzdhYjc2OHc6MTY0OTQyMDEzNjM3MDpNRVlDSVFDM09oRFlxcldDeWhFeWw4YlliZGJMMEIzYVQzQTg2bGlFdG9mY3JjdEw3Z0loQUpKbHRTc1k5VG9mamZpNEVRSmpFdXhvSlBVcCsydTNTS1NVL2J3VXVHYkc%3D
//user_id=4isayercrtbj3ea85c37ab768w
//user_name=danil

class App[F[_]] {

  def command(commandName: String)(
    action: CommandContext => F[AppResponse]
  ): App[F] =
    ???

}
