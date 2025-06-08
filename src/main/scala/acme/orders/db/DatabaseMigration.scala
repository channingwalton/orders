package acme.orders.db

import org.flywaydb.core.Flyway
import scala.util.{Try, Success, Failure}

object DatabaseMigration:

  def migrate(config: PostgresStore.DatabaseConfig): Either[String, Unit] = Try {
    val flyway = Flyway.configure().dataSource(config.url, config.user, config.password).locations("classpath:db/migration").load()

    flyway.migrate()
  } match {
    case Success(_)  => Right(())
    case Failure(ex) => Left(s"Database migration failed: ${ex.getMessage}")
  }
