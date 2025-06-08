package acme.orders

import cats.effect.*
import cats.syntax.all.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{Logger, CORS}
import org.http4s.HttpRoutes
import com.comcast.ip4s.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import acme.orders.config.Config
import acme.orders.db.{PostgresStore, DatabaseMigration}
import acme.orders.routes.OrderRoutes

object Main extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    (for
      logger <- Resource.eval(Slf4jLogger.create[IO])
      config <- Resource.eval(Config.load[IO])
      _ <- Resource.eval(runMigrations(config))
      store <- PostgresStore.resource[IO](config.database)
      orderService = OrderService[IO, doobie.ConnectionIO](store)
      routes = OrderRoutes.routes[IO](orderService)
      httpApp = Logger.httpApp(true, true)(CORS.policy.withAllowOriginAll(routes).orNotFound)
      server <- EmberServerBuilder.default[IO]
        .withHost(Host.fromString(config.server.host).getOrElse(host"0.0.0.0"))
        .withPort(Port.fromInt(config.server.port).getOrElse(port"8080"))
        .withHttpApp(httpApp)
        .build
    yield server).useForever.as(ExitCode.Success)

  private def runMigrations(config: Config): IO[Unit] =
    IO.fromEither(DatabaseMigration.migrate(config.database).left.map(msg => new RuntimeException(msg)))
      .adaptError { case ex => new RuntimeException(s"Migration failed: ${ex.getMessage}", ex) }