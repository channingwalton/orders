package acme.orders.db

import acme.orders.utils.LoggingContext
import acme.orders.utils.LoggingContext._
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
    val baseContext = LoggingContext.withOperation("databaseMigration")
    Sync[F]
      .blocking(flywayFromConfig(dbConfig).migrate())
      .handleErrorWith { t =>
        logger.errorWithContext(baseContext, t)(s"Failed to migrate database. ${t.getMessage}") >> t.raiseError[F, MigrateResult]
      }
      .flatTap { result =>
        val successContext = baseContext.withCustom("warningCount", result.warnings.size.toString)
        logger.infoWithContext(successContext)(s"Database migrated. Warnings: ${result.warnings.size}")
      }
