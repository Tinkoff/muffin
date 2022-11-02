package muffin.interop.http.zio

import zhttp.service.*
import zio.*

import muffin.http.HttpClient

type HttpR[R] = R & EventLoopGroup & ChannelFactory

type ZRHttp[R, A] = ZIO[HttpR[R], Throwable, A]

type RHttp[R] = [A] =>> ZRHttp[R, A]
