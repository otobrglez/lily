package dev.lily.fe

import dev.lily.ClientEvent
import scala.scalajs.js
import scala.scalajs.js.JSON

object JSONOps:
  extension (e: ClientEvent)

    private def toJsObject(event: ClientEvent): js.Object =
      val obj = js.Object()
      obj.asInstanceOf[js.Dynamic].clientEventName = event.clientEventName
      obj.asInstanceOf[js.Dynamic].serverEventName = event.serverEventName
      event.liID.foreach(id => obj.asInstanceOf[js.Dynamic].liID = id)
      obj

    def toJSON: String = JSON.stringify(toJsObject(e))
