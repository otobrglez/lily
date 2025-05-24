package dev.lily

import dev.lily.lhtml.HtmlDiff
import dev.lily.lhtml.HtmlJsonCodecs.given
import io.circe.*
import io.circe.generic.semiauto.*

// Events from Server --> Client
final case class DomChanged(diff: HtmlDiff)
object DomChanged:
  given domChangedEncoder: Encoder[DomChanged] = deriveEncoder[DomChanged]
  given domChangedDecoder: Decoder[DomChanged] = deriveDecoder[DomChanged]

// Events from Client --> Server
final case class ClientEvent(
  clientEventName: String, // TODO: Make it type safe
  serverEventName: String,
  liID: Option[String],
  value: Option[String]
):
  def toProtocol: String = ClientEvent.eventEncoder(this).noSpaces

object ClientEvent:
  given eventEncoder: Encoder[ClientEvent] = deriveEncoder[ClientEvent]
  given eventDecoder: Decoder[ClientEvent] = deriveDecoder[ClientEvent]

  def fromProtocol(raw: String): Either[String, ClientEvent] =
    eventDecoder.decodeJson(io.circe.parser.parse(raw).getOrElse(Json.Null)).left.map(_.getMessage)
