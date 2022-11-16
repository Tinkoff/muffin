package muffin.model

import cats.Show
import fs2.Stream

import muffin.api.*

case class ReactionInsight(emojiName: String, count: Long)

case class ChannelInsight(id: ChannelId, channelType: String, name: String, teamId: TeamId, messageCount: Long)

enum TimeRange {
  case Today
  case Day7
  case Day28
}

object TimeRange {

  given Show[TimeRange] = {
    case TimeRange.Today => "today"
    case TimeRange.Day7  => "7_day"
    case TimeRange.Day28 => "28_day"
  }

}

case class ListWrapper[T](hasNext: Boolean, items: List[T])
