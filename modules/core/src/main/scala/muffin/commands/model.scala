package muffin.commands

import io.circe.{Codec, Encoder}
import muffin.predef.*

case class CreateCommandRequest(
  team_id: String, // TODO ID
  method: CommandMethod,
  trigger: String,
  url: String // TODO URL
)

enum CommandMethod derives Encoder.AsObject:
  case P, G

case class Command(
  id: String,
  token: String,
  create_at: Long,
  update_at: Long,
  delete_at: Long,
  creator_id: UserId,
  team_id: String, // TODO ID
  trigger: String,
  method: CommandMethod,
  username: String,
  icon_url: String, // TODO URL
  auto_complete: Boolean,
  auto_complete_desc: String,
  auto_complete_hint: String,
  display_name: String,
  description: String,
  url: String // TODO URL
) derives Codec.AsObject

case class GetCommandsRequest(team_id: String, custom_only: Boolean = false)
type GetCommandsResponse = List[Command]

case class GetAutocompleteCommandsRequest(team_id: String)
type GetAutocompleteCommandsResponse = List[Command]
