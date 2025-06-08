package acme.orders.db

import cats.effect.*
import cats.effect.kernel.MonadCancelThrow
import cats.effect.std.Console
import cats.syntax.all.*
import cats.{~>, MonadThrow}
import com.zaxxer.hikari.HikariConfig
import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import acme.orders.models.*
import java.util.UUID

class PostgresStore[F[_]: MonadCancelThrow](xa: Transactor[F]) extends Store[F, ConnectionIO]:

  def createOrder(order: Order): ConnectionIO[OrderId] =
    Statements.insertOrder(order).run.as(order.id)

  def findOrder(orderId: OrderId): ConnectionIO[Option[Order]] =
    Statements.selectOrder(orderId).option

  def findOrdersByUser(userId: UserId): ConnectionIO[List[Order]] =
    Statements.selectOrdersByUser(userId).to[List]

  def updateOrder(order: Order): ConnectionIO[Unit] =
    Statements.updateOrderStatus(order.id, order.status, order.updatedAt).run.void

  def createSubscription(subscription: Subscription): ConnectionIO[SubscriptionId] =
    Statements.insertSubscription(subscription).run.as(subscription.id)

  def findSubscriptionsByUser(userId: UserId): ConnectionIO[List[Subscription]] =
    Statements.selectSubscriptionsByUser(userId).to[List]

  def commit[A](ca: ConnectionIO[A]): F[A] =
    ca.transact(xa)

object PostgresStore:
  
  case class DatabaseConfig(
    url: String,
    user: String,
    password: String,
    driver: String = "org.postgresql.Driver",
    maxPoolSize: Int = 10
  )

  def transactor[F[_]: Async](config: DatabaseConfig): Resource[F, HikariTransactor[F]] =
    for 
      hikariConfig <- Resource.pure {
        val hc = new HikariConfig()
        hc.setJdbcUrl(config.url)
        hc.setUsername(config.user)
        hc.setPassword(config.password)
        hc.setDriverClassName(config.driver)
        hc.setMaximumPoolSize(config.maxPoolSize)
        hc
      }
      xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
    yield xa

  def resource[F[_]: Async](config: DatabaseConfig): Resource[F, PostgresStore[F]] =
    transactor(config).map(new PostgresStore(_))