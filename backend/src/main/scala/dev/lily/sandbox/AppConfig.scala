package dev.lily.sandbox

import zio.*
import zio.Config.*
import zio.config.*
import zio.config.magnolia.*

import java.net.URI

type Port = Int

final case class AppConfig(
  @name("port")
  port: Port,
  @name("redis_uri")
  redisUri: URI,
  @name("duckdb_uri")
  duckdbUri: URI
)

object AppConfig:
  private def configDef: Config[AppConfig] = deriveConfig[AppConfig]
  def config: IO[Error, AppConfig]         = ZIO.config(configDef)
  def port: IO[Error, Port]                = config.map(_.port)
  def redisUri: IO[Error, URI]             = config.map(_.redisUri)
  def duckdbUri: IO[Error, URI]            = config.map(_.duckdbUri)
