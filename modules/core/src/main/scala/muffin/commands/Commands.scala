package muffin.commands

import io.circe.{Codec, Encoder}
import muffin.predef.*

trait Commands[F[_]] {

  def listCommands(req: GetCommandsRequest): F[GetCommandsResponse]

  def listAutocompleteCommands(
    req: GetAutocompleteCommandsRequest
  ): F[GetAutocompleteCommandsResponse]

}
