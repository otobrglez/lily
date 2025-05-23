package dev.lily

sealed trait Event

// Event sent from Server --> Client
final case class ServerEvent(name: String) extends Event

// Event sent from Client --> Server
final case class ClientEvent(
  clientEventName: String,
  serverEventName: String,
  liID: Option[String] = None
) extends Event
