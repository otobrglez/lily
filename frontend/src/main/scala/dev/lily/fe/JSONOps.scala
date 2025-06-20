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

object JSONSerde:
  def singleQuotesToDoubleQuotes(singleQuotedJson: String): String =
    var insideString = false
    var isEscaping   = false
    val result       = new StringBuilder()

    for i <- 0 until singleQuotedJson.length do
      val char = singleQuotedJson.charAt(i)

      if isEscaping then
        result.append(char)
        isEscaping = false
      else if char == '\\' then
        result.append(char)
        isEscaping = true
      else if char == '\'' && !insideString then
        result.append('"')
        insideString = true
      else if char == '\'' && insideString then
        result.append('"')
        insideString = false
      else if char == '"' && insideString then result.append("\\\"")
      else result.append(char)

    result.toString()
