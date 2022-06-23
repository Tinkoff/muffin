package muffin.api.commands

import muffin.predef.*

enum CommandMethod:
  case Post, Get

case class Command(
  id: String,
  token: String,
  createAt: Long,
  updateAt: Long,
  deleteAt: Long,
  creatorId: UserId,
  teamId: String, // TODO ID
  trigger: String,
  method: CommandMethod,
  username: String,
  iconUrl: String, // TODO URL
  autoComplete: Boolean,
  autoCompleteDesc: String,
  autoCompleteHint: String,
  displayName: String,
  description: String,
  url: String // TODO URL
)
