package muffin.api.groups

import muffin.predef.*
import fs2.Stream

case class Group(
                  id: GroupId,
                  name: String,
                  displayName: String,
                  description: String,
                  source: String,
                  remoteId: Option[GroupId],
                  createAt: Long,
                  updateAt: Long,
                  deleteAt: Long,
                  hasSyncables: Boolean
                )


sealed trait CreateGroup


object CreateGroup {
  case class Custom(
                     name: String,
                     displayName: String,
                   )
}


case class GroupStats(
                       groupId: GroupId,
                       totalMemberCount: Int
                     )


trait Groups[F[_]] {
  def getGroupsStats(groupId: GroupId): F[GroupStats]

  def removeMembers(groupId: GroupId, userIds: List[UserId]): F[Unit]

  def addMembers(groupId: GroupId, userIds: List[UserId]): F[Unit]

  def getChannelGroups(filterAllowReference: Boolean = false): Stream[F, Group]

  def getTeamGroup(teamId: TeamId, filterAllowReference: Boolean = false): Stream[F, Group]

  def getUserGroup(userId: UserId): Stream[F, Group]
}
