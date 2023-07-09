package muffin.model

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
  case class Custom(name: String, displayName: String)
}

case class GroupStats(groupId: GroupId, totalMemberCount: Int)
