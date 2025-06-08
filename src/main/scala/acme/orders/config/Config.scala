package acme.orders.config

import cats.effect.*
import cats.syntax.all.*
import ciris.*
import acme.orders.db.PostgresStore

case class Config(
  server: ServerConfig,
  database: PostgresStore.DatabaseConfig
)

case class ServerConfig(
  host: String,
  port: Int
)

object Config:

  def load[F[_]: Async]: F[Config] = (
    serverConfig,
    databaseConfig
  ).parMapN(Config.apply).load[F]

  private val serverConfig: ConfigValue[Effect, ServerConfig] = (
    env("HTTP_HOST").as[String].default("0.0.0.0"),
    env("HTTP_PORT").as[Int].default(8080)
  ).parMapN(ServerConfig.apply)

  private val databaseConfig: ConfigValue[Effect, PostgresStore.DatabaseConfig] = (
    env("DATABASE_URL").as[String].default("jdbc:postgresql://localhost:5432/orders"),
    env("DATABASE_USER").as[String].default("orders"),
    env("DATABASE_PASSWORD").as[String].default("orders"),
    env("DATABASE_DRIVER").as[String].default("org.postgresql.Driver"),
    env("DATABASE_MAX_POOL_SIZE").as[Int].default(10)
  ).parMapN(PostgresStore.DatabaseConfig.apply)
