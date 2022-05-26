package muffin.app

import cats.Applicative
import muffin.ApiClient
import cats.syntax.all.given

case class AppContext[F[_]: Applicative](client: ApiClient[F]) {
  def ok: AppResponse = AppResponse.Ok()
  
}
