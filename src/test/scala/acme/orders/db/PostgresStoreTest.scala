package acme.orders.db

import cats.effect.*
import munit.CatsEffectSuite
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import doobie.*
import doobie.implicits.*
import acme.orders.models.*
import java.time.Instant
import java.util.UUID

class PostgresStoreTest extends CatsEffectSuite with TestContainerForAll:

  override val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:13"),
    databaseName = "test",
    username = "test",
    password = "test"
  )

  test("createOrder and findOrder should work") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        store <- PostgresStore.resource[IO](config).use(IO.pure)
        order = createTestOrder()
        _ <- store.commit(store.createOrder(order))
        foundOrder <- store.commit(store.findOrder(order.id))
      yield {
        assert(foundOrder.isDefined)
        assertEquals(foundOrder.get.id, order.id)
        assertEquals(foundOrder.get.userId, order.userId)
      }
    }
  }

  test("findOrdersByUser should return user's orders") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        store <- PostgresStore.resource[IO](config).use(IO.pure)
        userId = UserId("user1")
        order1 = createTestOrder(userId = userId)
        order2 = createTestOrder(userId = userId)
        order3 = createTestOrder(userId = UserId("user2"))
        _ <- store.commit(store.createOrder(order1))
        _ <- store.commit(store.createOrder(order2))
        _ <- store.commit(store.createOrder(order3))
        userOrders <- store.commit(store.findOrdersByUser(userId))
      yield {
        assertEquals(userOrders.length, 2)
        assert(userOrders.forall(_.userId == userId))
      }
    }
  }

  test("createSubscription and findSubscriptionsByUser should work") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        store <- PostgresStore.resource[IO](config).use(IO.pure)
        order = createTestOrder()
        subscription = createTestSubscription(order.id, order.userId)
        _ <- store.commit(store.createOrder(order))
        _ <- store.commit(store.createSubscription(subscription))
        subscriptions <- store.commit(store.findSubscriptionsByUser(order.userId))
      yield {
        assertEquals(subscriptions.length, 1)
        assertEquals(subscriptions.head.id, subscription.id)
        assertEquals(subscriptions.head.orderId, order.id)
      }
    }
  }

  private def createDatabaseConfig(postgres: PostgreSQLContainer): IO[PostgresStore.DatabaseConfig] =
    IO.pure(PostgresStore.DatabaseConfig(
      url = postgres.jdbcUrl,
      user = postgres.username,
      password = postgres.password
    ))

  private def migrateDatabase(config: PostgresStore.DatabaseConfig): IO[Unit] =
    IO.fromEither(DatabaseMigration.migrate(config).left.map(msg => new RuntimeException(msg)))

  private def createTestOrder(
    id: OrderId = OrderId(UUID.randomUUID()),
    userId: UserId = UserId("user1"),
    productId: ProductId = ProductId("monthly")
  ): Order =
    Order(
      id = id,
      userId = userId,
      productId = productId,
      status = OrderStatus.Active,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  private def createTestSubscription(
    orderId: OrderId,
    userId: UserId
  ): Subscription =
    Subscription(
      id = SubscriptionId(UUID.randomUUID()),
      orderId = orderId,
      userId = userId,
      productId = ProductId("monthly"),
      startDate = Instant.now(),
      endDate = Instant.now().plusSeconds(2592000),
      status = SubscriptionStatus.Active,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )