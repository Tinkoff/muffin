package muffin.insights

import muffin.predef.ChannelId


case class ReactionInsight(emojiName: String, count: Long)


case class ChannelInsight(
                           id: ChannelId,
                           channelType: String,
                           name: String,
                           teamId: String,
                           messageCount: Long
                         )

enum TimeRange:
  case Today
  case Day7
  case Day28


case class ListWrapper[T](hasNext: Boolean, items: List[T])