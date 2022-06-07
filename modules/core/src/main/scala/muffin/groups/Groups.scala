package muffin.groups

import muffin.predef.*
import fs2.Stream

case class Group(
                  id: GroupId,
                  name: String,
                  display_name: String,
                  description: String,
                  source: String,
                  remote_id: Option[GroupId],
                  create_at: Long,
                  update_at: Long,
                  delete_at: Long,
                  has_syncables: Boolean
                )


sealed trait CreateGroup


object CreateGroup {
  case class Custom(
                     name: String,
                     display_name: String,
                   )
}


case class GroupStats(
                       group_id: GroupId,
                       total_member_count: Int
                     )


trait Groups[F[_]] {


  def deleteLinkLDAPGroup(remote_id: GroupId): F[Unit] = ???

  def createCustomGroup(group: Group, users: List[UserId]): F[Unit] = ???


  def getGroupsStats(groupId: GroupId): F[GroupStats]


//  def getGroupUsers(groupId: GroupId, per_page: Int = 60)

  def removeMembers(groupId: GroupId, userIds: List[UserId]): F[Unit]

  def addMembers(groupId: GroupId, userIds: List[UserId]): F[Unit]

  def getChannelGroups(per_page: Int = 60, filter_allow_reference: Boolean = false): Stream[F, Group]

  def getTeamGroup(teamId: TeamId, per_page: Int = 60, filter_allow_reference: Boolean = false): Stream[F, Group]

  def getUserGroup(userId: UserId): Stream[F, Group]
}
