package muffin.api.commands

import muffin.predef.*

trait Commands[F[_]] {
  def listCommands(teamId: TeamId, customOnly: Boolean = false): F[List[Command]]

  def listAutocompleteCommands(teamId: TeamId): F[List[Command]]
}
