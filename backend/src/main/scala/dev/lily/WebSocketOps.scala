package dev.lily

import zio.ZIO
import zio.http.ChannelEvent.Read
import zio.http.{WebSocketChannel, WebSocketFrame}

object WebSocketOps:

  extension (ws: WebSocketChannel)
    def sendString(msg: String): ZIO[Any, Throwable, Unit] =
      ws.send(
        Read(WebSocketFrame.Text(msg))
      )
