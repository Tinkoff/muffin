package muffin.insights

import fs2.Stream

import muffin.predef.*

trait Insights[F[_]] {
  def getTopReactions(teamId: String, timeRange: TimeRange): Stream[F, ReactionInsight]

  def getTopReactions(userId: UserId, timeRange: TimeRange, teamId: Option[String] = None): Stream[F, ReactionInsight]

  def getTopChannels(teamId: String, timeRange: TimeRange): Stream[F, ChannelInsight]

  def getTopChannels(userId: UserId, timeRange: TimeRange, teamId: Option[String] = None): Stream[F, ChannelInsight]
}
