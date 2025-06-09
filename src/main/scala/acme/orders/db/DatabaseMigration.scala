package acme.orders.db

import cats.effect.Sync
import cats.syntax.all._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object DatabaseMigration:

  protected[db] def flywayFromConfig(dbConfig: PostgresStore.DatabaseConfig): Flyway =
    Flyway.configure().dataSource(dbConfig.url, dbConfig.user, dbConfig.password).connectRetries(10).locations("classpath:db/migration").load()

  def migrate[F[_]: Sync: LoggerFactory](dbConfig: PostgresStore.DatabaseConfig): F[MigrateResult] =
    val logger: SelfAwareStructuredLogger[F] = LoggerFactory[F].getLogger
    Sync[F]
      .blocking(flywayFromConfig(dbConfig).migrate())
      .handleErrorWith { t =>
        logger.error(Map("operation" -> "databaseMigration"), t)(s"Failed to migrate database. ${t.getMessage}") >> t.raiseError[F, MigrateResult]
      }
      .flatTap { result =>
        logger.info(Map("operation" -> "databaseMigration", "warningCount" -> result.warnings.size.toString))(
          s"Database migrated. Warnings: ${result.warnings.size}"
        )
      }
