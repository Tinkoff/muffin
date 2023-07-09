package muffin.api

import java.time.ZoneId

import cats.effect.IO
import cats.syntax.all.*

import org.scalatest.{Assertion, Succeeded, Tag}
import org.scalatest.featurespec.AsyncFeatureSpec

import muffin.codec.*
import muffin.dsl.*
import muffin.http.*
import muffin.model.*

trait ApiTest[To[_], From[_]](integration: String, codecSupport: CodecSupport[To, From]) extends ApiTestSupport {
  protected def httpClient: HttpClient[IO, To, From]

  import codecSupport.{*, given}

  case class AppContext(int: Int, str: String)

  protected def toContext: To[AppContext]
  protected def fromContext: From[AppContext]

  object AppContext {
    given Encode[AppContext] = EncodeTo(toContext)
    given Decode[AppContext] = DecodeFrom(fromContext)
  }

  private given ZoneId = ZoneId.of("UTC")

  private val config = ClientConfig(
    baseUrl = "http/test",
    auth = "auth",
    botName = "testbot",
    serviceUrl = "service",
    perPage = 1
  )

  private val apiClient = new ApiClient[IO, To, From](httpClient, config)(codecSupport)

  Feature("reactions") {
    Scenario(s"create in $integration", Tag(integration)) {
      val user      = UserId("w5qwc1xdc3rgxde8dufr1nfpzr")
      val messageId = MessageId("34b52dfdd69f485bb0e70d1879")
      val name      = "grinning"

      apiClient.createReaction(
        user,
        messageId,
        name
      ).map { reaction =>
        assert(reaction.emojiName == name)
        assert(reaction.postId == messageId)
        assert(reaction.userId == user)
      }
    }

    Scenario(s"get reactions in $integration") {
      val messageId = MessageId("34b52dfdd69f485bb0e70d1879")

      apiClient.getReactions(messageId).map(reactions =>
        assert(reactions.size == 3)
      )
    }

    Scenario(s"remove reaction in $integration") {
      apiClient.removeReaction(
        UserId("65wezbrt3ibw8pect7bc4zepqh"),
        MessageId("wonhq16y7jrmifrt8r8zby6g5w"),
        "grinning"
      ).as(succeed)
    }
  }

  Feature("channel") {
    Scenario(s"get by name in $integration") {
      apiClient.getChannelByName(TeamId("5b62e70ce90f4ee68c9b6b6b95"), "superkek").map {
        channel =>
          assert(channel.id == ChannelId("5b62e70ce90f4ee68c9b6b6b95"))
      }
    }

    Scenario(s"get channel members in $integration") {
      apiClient.members(ChannelId("w5qwc1xdc3rgxde8dufr1nfpzr"))
        .compile
        .toList
        .map { members =>
          assert(members.size == 3)
        }
    }
  }

  Feature("users") {
    Scenario(s"get user by id in $integration") {
      apiClient.user(UserId("w5qwc1xdc3rgxde8dufr1nfpzr")).map {
        user =>
          assert(user.username == "danil")
      }
    }

    Scenario(s"get users by id in $integration") {
      apiClient.usersById(List(
        UserId("97qfjny8pbgrbrjhnre8sxfe1y")
      )).map {
        users =>
          assert(users.head.username == "danil")
      }
    }

    Scenario(s"get user by username in $integration") {
      apiClient.userByUsername("danil").map {
        user =>
          assert(user.username == "danil")
      }
    }

    Scenario(s"get users by usernames in $integration") {
      apiClient.usersByUsername(List(
        "danil"
      )).map {
        users =>
          assert(users.head.username == "danil")
      }
    }

    Scenario(s"get user list in $integration") {
      apiClient.users(active = true.some).compile.toList.map(users =>
        assert(users.size == 2)
      )
    }

  }

  Feature("posts") {
    Scenario(s"perform action in $integration") {
      apiClient.performAction(MessageId("34b52dfdd69f485bb0e70d1879"), "test").as(Succeeded)
    }

    Scenario(s"send post to channel in $integration") {
      apiClient.postToChannel(
        ChannelId("w9s6bs5hs7yj5dcwbrdbw137dh"),
        "message".some,
        Props(
          attachment
            .text(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod",
              "Excepteur sint occaecat cupidatat non proident".some,
              "Duis aute irure dolor in reprehenderit".some
            )
            .color("#00FF00")
            .action(selectOptions(
              "select",
              _.Context("url2", AppContext(123, "str")),
              SelectOption("Зеленый", "green") :: SelectOption("Синий", "blue") :: Nil
            ))
            .action(button("button", _.Url("url"), Style.Danger, "customId".some))
            .make :: Nil
        )
      ).map(post => assert(post.id == MessageId("34b52dfdd69f485bb0e70d1879")))
    }

    Scenario(s"send post to chat in $integration") {
      apiClient.postToChat(
        UserId("w5qwc1xdc3rgxde8dufr1nfpzr") ::
          UserId("y6hu6uafnbrgjce68sos88xorr") ::
          UserId("6bic3idfo387xg55o4dgzzp9xe") :: Nil,
        "message".some,
        Props(
          attachment
            .text(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod",
              "Excepteur sint occaecat cupidatat non proident".some,
              "Duis aute irure dolor in reprehenderit".some
            )
            .color("#00FF00")
            .action(selectOptions(
              "select",
              _.Context("url2", AppContext(123, "str")),
              SelectOption("Зеленый", "green") :: SelectOption("Синий", "blue") :: Nil
            ))
            .action(button("button", _.Url("url"), Style.Danger, "customId".some))
            .make :: Nil
        )
      ).map(post => assert(post.id == MessageId("34b52dfdd69f485bb0e70d1879")))
    }

    Scenario(s"send post to direct in $integration") {
      val user = UserId("w5qwc1xdc3rgxde8dufr1nfpzr")

      apiClient.postToDirect(
        user,
        "message".some,
        Props(
          attachment
            .text(
              "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod",
              "Excepteur sint occaecat cupidatat non proident".some,
              "Duis aute irure dolor in reprehenderit".some
            )
            .color("#00FF00")
            .action(selectOptions(
              "select",
              _.Context("url2", AppContext(123, "str")),
              SelectOption("Зеленый", "green") :: SelectOption("Синий", "blue") :: Nil
            ))
            .action(button("button", _.Url("url"), Style.Danger, "customId".some))
            .make :: Nil
        )
      ).map(post => assert(post.id == MessageId("34b52dfdd69f485bb0e70d1879")))
    }

    Scenario(s"get post by id in $integration") {
      apiClient.getPost(MessageId("34b52dfdd69f485bb0e70d1879")).map(post =>
        assert(post.id == MessageId("34b52dfdd69f485bb0e70d1879"))
      )
    }

    Scenario(s"delete post in $integration") {
      apiClient.deletePost(MessageId("34b52dfdd69f485bb0e70d1879")).as(succeed)
    }

    Scenario(s"update post in $integration") {
      apiClient.updatePost(MessageId("34b52dfdd69f485bb0e70d1879"), "text".some)
        .map(post => assert(post.id == MessageId("34b52dfdd69f485bb0e70d1879")))
    }

    Scenario(s"patch post in $integration") {
      apiClient.patchPost(MessageId("34b52dfdd69f485bb0e70d1879"), "text".some)
        .map(post => assert(post.id == MessageId("34b52dfdd69f485bb0e70d1879")))
    }
  }

  Feature("emoji") {
    Scenario(s"get emojis in $integration") {
      apiClient.getEmojis(EmojiSorting.Name).compile.toList
        .map(emojis => assert(emojis.size == 2))
    }

    Scenario(s"get emoji by id in $integration") {
      apiClient.getEmojiById(EmojiId("xx8d1br9f7n9imj5zi6enhxd3o"))
        .map(emoji =>
          assert(emoji.id == EmojiId("xx8d1br9f7n9imj5zi6enhxd3o"))
        )
    }

    Scenario(s"delete emoji in $integration") {
      apiClient.deleteEmoji(EmojiId("xx8d1br9f7n9imj5zi6enhxd3o")).as(succeed)
    }

    Scenario(s"get emoji by name $integration") {
      apiClient.getEmojiByName("test").map {
        emoji => assert(emoji.id == EmojiId("xx8d1br9f7n9imj5zi6enhxd3o"))
      }
    }

    Scenario(s"autocomplete emoji in $integration") {
      apiClient.autocompleteEmoji("te").map {
        emojis =>
          assert(emojis.size == 1)
      }
    }
  }

}
