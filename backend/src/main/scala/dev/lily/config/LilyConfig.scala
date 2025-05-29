package dev.lily.config

import zio.{ULayer, ZIO, ZLayer}

import scala.concurrent.duration.FiniteDuration

final case class LilyConfig(
  reloadOnDisconnect: Boolean = false,
  reloadTimeout: FiniteDuration = FiniteDuration(10, "seconds")
)

object LilyConfig:
  val default: ULayer[LilyConfig]                      = ZLayer.succeed(LilyConfig())
  def from(lilyConfig: LilyConfig): ULayer[LilyConfig] = ZLayer.succeed(lilyConfig)
