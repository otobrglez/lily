package dev.lily

import zio.{Chunk, Task, ZIO}
import zio.http.ChannelEvent.Read
import zio.http.{WebSocketChannel, WebSocketFrame}

object WebSocketOps:

  extension (ws: WebSocketChannel)
    def sendString(msg: String): ZIO[Any, Throwable, Unit] =
      ws.send(Read(WebSocketFrame.Text(msg)))

    def sendBinary(blob: Array[Byte]): Task[Unit] =
      ws.send(Read(WebSocketFrame.Binary(Chunk.fromArray(blob))))
