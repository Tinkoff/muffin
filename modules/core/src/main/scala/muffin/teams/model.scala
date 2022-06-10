package muffin.teams

import muffin.predef.*

import java.time.LocalDateTime

case class Team(
  id: TeamId,
  createAt: LocalDateTime,
  updateAt: LocalDateTime,
  deleteAt: Option[LocalDateTime],
  displayName: String,
  name: String,
  description: Option[String],
  email: Option[String],
  teamType: TeamType,
  companyName: Option[String],
  allowedDomains: Option[String],
  inviteId: Option[String],
  allowOpenInvite: Boolean,
  schemeId: Option[String],
  groupConstrained: Boolean,
  policyId: Option[String]
)

enum TeamType:
  case Open
  case Invite


case class TeamMember(
                       teamId: TeamId,
                       userId: UserId,
                       roles: String,
                       deleteAt: Option[LocalDateTime],
                       schemeUser: Boolean,
                       schemeAdmin: Boolean,
                       explicitRoles: String
                     )


case class TeamStats(
                      teamId: TeamId,
                      totalMemberCount: Long,
                      activeMemberCount: Long
                    )


case class TeamUnreads(
                        teamId: TeamId,
                        messageCount: Long,
                        mentionCount: Long
                      )