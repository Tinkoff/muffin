package muffin.insights

import fs2.Stream

import muffin.predef.*

trait Insights[F[_]] {
  def getTopReactions(teamId: TeamId, timeRange: TimeRange): Stream[F, ReactionInsight]

  def getTopReactions(userId: UserId, timeRange: TimeRange, teamId: Option[TeamId] = None): Stream[F, ReactionInsight]

  def getTopChannels(teamId: TeamId, timeRange: TimeRange): Stream[F, ChannelInsight]

  def getTopChannels(userId: UserId, timeRange: TimeRange, teamId: Option[TeamId] = None): Stream[F, ChannelInsight]
}
