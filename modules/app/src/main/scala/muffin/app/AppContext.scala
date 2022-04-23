package muffin.app

import muffin.ApiClient

case class AppContext[F[_]](client: ApiClient[F])
