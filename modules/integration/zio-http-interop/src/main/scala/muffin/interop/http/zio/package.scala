package muffin.interop.http.zio

import zio.*
import zio.http.*

import muffin.http.HttpClient

type RHttp[-R] = [A] =>> RIO[R, A]
