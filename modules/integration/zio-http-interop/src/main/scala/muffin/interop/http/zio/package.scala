package muffin.interop.http.zio

import zio.*

type RHttp[-R] = [A] =>> RIO[R, A]
