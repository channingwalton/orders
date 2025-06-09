package acme.orders

import acme.orders.config.Config
import acme.orders.db.{DatabaseMigration, PostgresStore}
import acme.orders.routes.OrderRoutes
import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Main extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

    (for
      config <- Resource.eval(Config.load[IO])
      _ <- Resource.eval(runMigrations(config))
      store <- PostgresStore.resource[IO](config.database)
      orderService = OrderService[IO, doobie.ConnectionIO](store)
      routes = OrderRoutes.routes[IO](orderService)
      httpApp = Logger.httpApp(true, true)(CORS.policy.withAllowOriginAll(routes).orNotFound)
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.server.host).getOrElse(host"0.0.0.0"))
        .withPort(Port.fromInt(config.server.port).getOrElse(port"8080"))
        .withHttpApp(httpApp)
        .build
    yield server).useForever.as(ExitCode.Success)

  private def runMigrations(config: Config)(implicit lf: LoggerFactory[IO]): IO[Unit] = DatabaseMigration.migrate[IO](config.database).void
