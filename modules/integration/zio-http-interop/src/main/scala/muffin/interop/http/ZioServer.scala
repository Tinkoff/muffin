package muffin.interop.http


import cats.effect.Sync
import muffin.codec.CodecSupport
import zhttp.http.*
import zhttp.service.*
import zhttp.service.server.ServerChannelFactory
import zio.*
import muffin.codec.{CodecSupport, Decode, Encode}
import muffin.input.{CommandContext, Router, AppResponse, DialogContext, RawAction}
import java.nio.charset.Charset

class ZioServer {
  def routes[R, To[_], From[_]](router: Router[Task, R], codec: CodecSupport[R, To, From]): Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "commands" / command =>
      import codec.given
      handleEvent[CommandContext](req)(router.handleCommand(command, _))

    case req@Method.POST -> !! / "actions" / actions =>
      import codec.given
      handleEvent[RawAction[R]](req)(router.handleAction(actions, _))

    case req@Method.POST -> !! / "dialogs" / dialogs =>
      import codec.given
      handleEvent[DialogContext](req)(router.handleDialog(dialogs, _))
  }

  def handleEvent[In](request: Request)(fun: In => Task[AppResponse])(using decoder: Decode[In], encoder: Encode[AppResponse]): Task[Response] = {
    for {
      buf <- request.data.toByteBuf
      response <- decoder.apply(buf.toString(Charset.forName("UTF-8"))) match
        case Left(error) => ZIO.fail(error)
        case Right(value) => fun(value)
    } yield Response.json(encoder.apply(response))
  }
}
