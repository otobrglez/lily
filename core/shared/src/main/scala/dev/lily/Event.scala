package dev.lily

import cats.implicits.catsSyntaxEitherId
import dev.lily.lhtml.HtmlDiff
import dev.lily.lhtml.HtmlJsonCodecs.given
import io.circe.*
import io.circe.generic.semiauto.*
import io.bullet.borer.Cbor
import io.bullet.borer.compat.circe.*

// Events from Server --> Client
final case class DomChanged(diff: HtmlDiff):
  private def serializeToCbor[T: Encoder](value: T): Array[Byte] = Cbor.encode(value).toByteArray

  def toProtocol: Array[Byte] = serializeToCbor(this)

object DomChanged:
  given domChangedEncoder: Encoder[DomChanged]                       = deriveEncoder[DomChanged]
  given domChangedDecoder: Decoder[DomChanged]                       = deriveDecoder[DomChanged]
  private def deserializeFromCbor[T: Decoder](bytes: Array[Byte]): T = Cbor.decode(bytes).to[T].value
  def fromBinary(blob: Array[Byte]): Either[String, DomChanged]      = deserializeFromCbor(blob).asRight

// Events from Client --> Server
type ServerEventName = String
type ClientEventName = String
type LIID            = String
type Value           = String
type Data            = List[String]
final case class ClientEvent(
  serverEventName: ServerEventName,
  clientEventName: ClientEventName,
  liID: Option[LIID],
  value: Option[Value],
  data: Data = Nil
):
  private def serializeToCbor[T: Encoder](value: T): Array[Byte] = Cbor.encode(value).toByteArray
  def toProtocol: Array[Byte]                                    = serializeToCbor(this)

object ClientEvent:
  given eventEncoder: Encoder[ClientEvent] = deriveEncoder[ClientEvent]
  given eventDecoder: Decoder[ClientEvent] = deriveDecoder[ClientEvent]

  private def deserializeFromCbor[T: Decoder](bytes: Array[Byte]): T = Cbor.decode(bytes).to[T].value

  def fromBinary(blob: Array[Byte]): Either[String, ClientEvent] =
    deserializeFromCbor(blob).asRight

  def unapply(event: ClientEvent): Option[ServerEventName] = Some(event.serverEventName)

  object onData:
    def unapply(event: ClientEvent): Option[(ServerEventName, Option[Value], Data)] =
      Some((event.serverEventName, event.value, event.data))

  object on:
    def unapply(event: ClientEvent): Option[(ServerEventName, Option[Value])] =
      Some(event.serverEventName -> event.value)
