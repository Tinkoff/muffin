package muffin.teams

import muffin.predef.*
import fs2.Stream

trait Teams[F[_]] {
  def createTeam(name: String, displayName: String, teamType: TeamType): F[Team]

  def getTeams: Stream[F, Team]

  def getTeam(teamId: TeamId): F[Team]

  def getTeamByName(name: String): F[Team]

  def restoreTeam(teamId: TeamId): F[Team]

  def getUserTeams(userId: UserId): F[List[Team]]

  def getTeamMembers(teamId: TeamId): Stream[F, TeamMember]

  def getTeamMembers(teamId: TeamId, userIds: List[UserId]): F[List[TeamMember]]

  def addUserToTeam(userId: UserId, teamId: TeamId): F[TeamMember]

  def removeUserFromTeam(userId: UserId, teamId: TeamId): F[Unit]

  def getTeamStats(teamId: TeamId): F[TeamStats]

  def unreadsForTeam(userId: UserId, teamId: TeamId): F[TeamUnreads]
}
