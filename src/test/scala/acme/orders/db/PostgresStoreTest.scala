package acme.orders.db

import java.time.Instant
import java.util.UUID

import acme.orders.models._
import acme.orders.utils.TimeUtils
import cats.effect._
import cats.syntax.all._
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.TestContainerForEach
import munit.CatsEffectSuite
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class PostgresStoreTest extends CatsEffectSuite with TestContainerForEach:

  override val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15"),
    databaseName = "test",
    username = "test",
    password = "test"
  )

  test("createOrder and findOrder should work") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val order = createTestOrder()
          for
            _ <- store.commit(store.createOrder(order))
            foundOrder <- store.commit(store.findOrder(order.id))
          yield {
            assert(foundOrder.isDefined)
            assertEquals(foundOrder.get.id, order.id)
            assertEquals(foundOrder.get.userId, order.userId)
          }
        }
      yield result
    }
  }

  test("findOrdersByUser should return user's orders") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("user1")
          val order1 = createTestOrder(userId = userId)
          val order2 = createTestOrder(userId = userId)
          val order3 = createTestOrder(userId = UserId("user2"))
          for
            // Create all test orders in single transaction
            _ <- store.commit(
              store.createOrder(order1) *>
                store.createOrder(order2) *>
                store.createOrder(order3)
            )

            userOrders <- store.commit(store.findOrdersByUser(userId))
          yield {
            assertEquals(userOrders.length, 2)
            assert(userOrders.forall(_.userId == userId))
          }
        }
      yield result
    }
  }

  test("createSubscription and findSubscriptionsByUser should work") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val order = createTestOrder()
          val subscription = createTestSubscription(order.id, order.userId)
          for
            // Create order and subscription in single transaction
            _ <- store.commit(
              store.createOrder(order) *>
                store.createSubscription(subscription)
            )

            subscriptions <- store.commit(store.findSubscriptionsByUser(order.userId))
          yield {
            assertEquals(subscriptions.length, 1)
            assertEquals(subscriptions.head.id, subscription.id)
            assertEquals(subscriptions.head.orderId, order.id)
          }
        }
      yield result
    }
  }

  test("updateOrder should update order status and timestamp") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val order = createTestOrder()
          val updatedTime = TimeUtils.truncateToSeconds(order.createdAt.plusSeconds(60))
          val updatedOrder = order.copy(status = OrderStatus.Cancelled, updatedAt = updatedTime)
          for
            _ <- store.commit(store.createOrder(order))
            _ <- store.commit(store.updateOrder(updatedOrder))
            foundOrder <- store.commit(store.findOrder(order.id))
          yield {
            assert(foundOrder.isDefined)
            assertEquals(foundOrder.get.status, OrderStatus.Cancelled)
            // Check that updatedAt was changed and is later than createdAt
            assert(foundOrder.get.updatedAt.isAfter(order.createdAt))
            // Since we're using second precision, ensure we don't have microsecond differences
            assertEquals(foundOrder.get.updatedAt.getNano(), 0)
            assertEquals(foundOrder.get.id, order.id)
            assertEquals(foundOrder.get.userId, order.userId)
            // Verify timestamps have second precision (no nanoseconds)
            assertEquals(foundOrder.get.createdAt.getNano(), 0)
          }
        }
      yield result
    }
  }

  test("findActiveSubscriptionsByUser should filter by date and status") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("user1")
          val order1 = createTestOrder(userId = userId)
          val order2 = createTestOrder(userId = userId)
          val order3 = createTestOrder(userId = userId)

          val now = Instant.now()
          val activeSubscription = createTestSubscriptionWithDates(
            order1.id,
            userId,
            startDate = now.minusSeconds(3600),
            endDate = now.plusSeconds(3600),
            status = SubscriptionStatus.Active
          )
          val expiredSubscription = createTestSubscriptionWithDates(
            order2.id,
            userId,
            startDate = now.minusSeconds(7200),
            endDate = now.minusSeconds(3600),
            status = SubscriptionStatus.Active
          )
          val cancelledSubscription = createTestSubscriptionWithDates(
            order3.id,
            userId,
            startDate = now.minusSeconds(3600),
            endDate = now.plusSeconds(3600),
            status = SubscriptionStatus.Cancelled
          )

          for
            // Setup all test data in a single transaction
            _ <- store.commit(
              store.createOrder(order1) *>
                store.createOrder(order2) *>
                store.createOrder(order3) *>
                store.createSubscription(activeSubscription) *>
                store.createSubscription(expiredSubscription) *>
                store.createSubscription(cancelledSubscription)
            )

            // Query for active subscriptions
            activeSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(userId))
          yield {
            assertEquals(activeSubscriptions.length, 1)
            assertEquals(activeSubscriptions.head.id, activeSubscription.id)
            assertEquals(activeSubscriptions.head.status, SubscriptionStatus.Active)
          }
        }
      yield result
    }
  }

  test("findActiveSubscriptionsByUser should return empty list for user with no active subscriptions") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("user1")
          val order = createTestOrder(userId = userId)

          val now = Instant.now()
          val expiredSubscription = createTestSubscriptionWithDates(
            order.id,
            userId,
            startDate = now.minusSeconds(7200),
            endDate = now.minusSeconds(3600),
            status = SubscriptionStatus.Active
          )

          for
            // Create test data in single transaction
            _ <- store.commit(
              store.createOrder(order) *>
                store.createSubscription(expiredSubscription)
            )

            activeSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(userId))
          yield {
            assertEquals(activeSubscriptions.length, 0)
          }
        }
      yield result
    }
  }

  test("findOrder should return None for non-existent order") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val nonExistentOrderId = OrderId(UUID.randomUUID())
          for foundOrder <- store.commit(store.findOrder(nonExistentOrderId))
          yield {
            assertEquals(foundOrder, None)
          }
        }
      yield result
    }
  }

  test("findOrdersByUser should return empty list for user with no orders") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("nonexistent-user")
          for orders <- store.commit(store.findOrdersByUser(userId))
          yield {
            assertEquals(orders, List.empty[Order])
          }
        }
      yield result
    }
  }

  test("findSubscriptionsByUser should return empty list for user with no subscriptions") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("nonexistent-user")
          for subscriptions <- store.commit(store.findSubscriptionsByUser(userId))
          yield {
            assertEquals(subscriptions, List.empty[Subscription])
          }
        }
      yield result
    }
  }

  test("findActiveSubscriptionsByUser should return empty list for user with no subscriptions") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("nonexistent-user")
          for activeSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(userId))
          yield {
            assertEquals(activeSubscriptions, List.empty[Subscription])
          }
        }
      yield result
    }
  }

  test("complete order lifecycle should work") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("user1")
          val order = createTestOrder(userId = userId)
          val subscription = createTestSubscription(order.id, userId)
          val updatedTime = Instant.now().plusSeconds(60)
          val cancelledOrder = order.copy(status = OrderStatus.Cancelled, updatedAt = updatedTime)

          for
            // Initial setup in single transaction
            _ <- store.commit(
              store.createOrder(order) *>
                store.createSubscription(subscription)
            )

            // Query initial state
            initialOrders <- store.commit(store.findOrdersByUser(userId))
            initialSubscriptions <- store.commit(store.findSubscriptionsByUser(userId))
            initialActiveSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(userId))

            // Update order status
            _ <- store.commit(store.updateOrder(cancelledOrder))

            // Query final state
            finalOrder <- store.commit(store.findOrder(order.id))
            finalOrders <- store.commit(store.findOrdersByUser(userId))
          yield {
            assertEquals(initialOrders.length, 1)
            assertEquals(initialSubscriptions.length, 1)
            assertEquals(initialActiveSubscriptions.length, 1)

            assert(finalOrder.isDefined)
            assertEquals(finalOrder.get.status, OrderStatus.Cancelled)
            assertEquals(finalOrders.head.status, OrderStatus.Cancelled)
          }
        }
      yield result
    }
  }

  test("multiple users and orders should be isolated correctly") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val user1 = UserId("user1")
          val user2 = UserId("user2")

          val order1 = createTestOrder(userId = user1)
          val order2 = createTestOrder(userId = user1)
          val order3 = createTestOrder(userId = user2)

          val subscription1 = createTestSubscription(order1.id, user1)
          val subscription2 = createTestSubscription(order2.id, user1)
          val subscription3 = createTestSubscription(order3.id, user2)

          for
            // Setup all data in a single transaction
            _ <- store.commit(
              store.createOrder(order1) *>
                store.createOrder(order2) *>
                store.createOrder(order3) *>
                store.createSubscription(subscription1) *>
                store.createSubscription(subscription2) *>
                store.createSubscription(subscription3)
            )

            // Query data - separate commits for clarity
            user1Orders <- store.commit(store.findOrdersByUser(user1))
            user2Orders <- store.commit(store.findOrdersByUser(user2))
            user1Subscriptions <- store.commit(store.findSubscriptionsByUser(user1))
            user2Subscriptions <- store.commit(store.findSubscriptionsByUser(user2))
            user1ActiveSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(user1))
            user2ActiveSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(user2))
          yield {
            assertEquals(user1Orders.length, 2)
            assertEquals(user2Orders.length, 1)
            assertEquals(user1Subscriptions.length, 2)
            assertEquals(user2Subscriptions.length, 1)
            assertEquals(user1ActiveSubscriptions.length, 2)
            assertEquals(user2ActiveSubscriptions.length, 1)

            assert(user1Orders.forall(_.userId == user1))
            assert(user2Orders.forall(_.userId == user2))
            assert(user1Subscriptions.forall(_.userId == user1))
            assert(user2Subscriptions.forall(_.userId == user2))
          }
        }
      yield result
    }
  }

  test("createOrderCancellation and findOrderCancellation should work") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val order = createTestOrder()
          val cancellation = createTestOrderCancellation(order.id)

          for
            _ <- store.commit(
              store.createOrder(order) *>
                store.createOrderCancellation(cancellation)
            )
            foundCancellation <- store.commit(store.findOrderCancellation(order.id))
          yield {
            assert(foundCancellation.isDefined)
            assertEquals(foundCancellation.get.id, cancellation.id)
            assertEquals(foundCancellation.get.orderId, order.id)
            assertEquals(foundCancellation.get.reason, CancellationReason.UserRequest)
            assertEquals(foundCancellation.get.cancellationType, CancellationType.Immediate)
            assertEquals(foundCancellation.get.notes, Some("User requested cancellation"))
          }
        }
      yield result
    }
  }

  test("findOrderCancellation should return None for non-cancelled order") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val order = createTestOrder()

          for
            _ <- store.commit(store.createOrder(order))
            foundCancellation <- store.commit(store.findOrderCancellation(order.id))
          yield {
            assertEquals(foundCancellation, None)
          }
        }
      yield result
    }
  }

  test("updateSubscription should update cancellation fields") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val order = createTestOrder()
          val subscription = createTestSubscription(order.id, order.userId)
          val now = TimeUtils.now()
          val cancelledSubscription = subscription.copy(
            status = SubscriptionStatus.Cancelled,
            cancelledAt = Some(now),
            effectiveEndDate = Some(now.plusSeconds(3600)),
            updatedAt = now
          )

          for
            _ <- store.commit(
              store.createOrder(order) *>
                store.createSubscription(subscription)
            )
            _ <- store.commit(store.updateSubscription(cancelledSubscription))
            foundSubscriptions <- store.commit(store.findSubscriptionsByUser(order.userId))
          yield {
            assertEquals(foundSubscriptions.length, 1)
            val found = foundSubscriptions.head
            assertEquals(found.status, SubscriptionStatus.Cancelled)
            assert(found.cancelledAt.isDefined)
            assert(found.effectiveEndDate.isDefined)
            assert(found.cancelledAt.get.isAfter(subscription.createdAt.minusSeconds(1)))
          }
        }
      yield result
    }
  }

  test("findActiveSubscriptionsByUser should exclude cancelled subscriptions") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("user1")
          val order1 = createTestOrder(userId = userId)
          val order2 = createTestOrder(userId = userId)

          val activeSubscription = createTestSubscription(order1.id, userId)
          val cancelledSubscription = createTestSubscription(order2.id, userId).copy(
            status = SubscriptionStatus.Cancelled,
            cancelledAt = Some(TimeUtils.now())
          )

          for
            _ <- store.commit(
              store.createOrder(order1) *>
                store.createOrder(order2) *>
                store.createSubscription(activeSubscription) *>
                store.createSubscription(cancelledSubscription)
            )
            activeSubscriptions <- store.commit(store.findActiveSubscriptionsByUser(userId))
            allSubscriptions <- store.commit(store.findSubscriptionsByUser(userId))
          yield {
            assertEquals(allSubscriptions.length, 2)
            assertEquals(activeSubscriptions.length, 1)
            assertEquals(activeSubscriptions.head.status, SubscriptionStatus.Active)
          }
        }
      yield result
    }
  }

  test("findSubscriptionsByOrder should return subscriptions for specific order") {
    withContainers { case postgres: PostgreSQLContainer =>
      for
        config <- createDatabaseConfig(postgres)
        _ <- migrateDatabase(config)
        result <- PostgresStore.resource[IO](config).use { store =>
          val userId = UserId("user1")
          val order = createTestOrder(userId = userId)
          val subscription = createTestSubscription(order.id, userId)

          for
            _ <- store.commit(
              store.createOrder(order) *>
                store.createSubscription(subscription)
            )
            subscriptions <- store.commit(store.findSubscriptionsByOrder(order.id))
          yield {
            assertEquals(subscriptions.length, 1)
            assertEquals(subscriptions.head.orderId, order.id)
          }
        }
      yield result
    }
  }

  private def createDatabaseConfig(postgres: PostgreSQLContainer): IO[PostgresStore.DatabaseConfig] = IO.pure(
    PostgresStore.DatabaseConfig(
      url = postgres.jdbcUrl,
      user = postgres.username,
      password = postgres.password
    )
  )

  private def migrateDatabase(config: PostgresStore.DatabaseConfig): IO[Unit] =
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    DatabaseMigration.migrate[IO](config).void

  private def createTestOrder(
    id: OrderId = OrderId(UUID.randomUUID()),
    userId: UserId = UserId("user1"),
    productId: ProductId = ProductId("monthly")
  ): Order = Order(
    id = id,
    userId = userId,
    productId = productId,
    status = OrderStatus.Active,
    createdAt = TimeUtils.now(),
    updatedAt = TimeUtils.now()
  )

  private def createTestSubscription(
    orderId: OrderId,
    userId: UserId
  ): Subscription = Subscription(
    id = SubscriptionId(UUID.randomUUID()),
    orderId = orderId,
    userId = userId,
    productId = ProductId("monthly"),
    startDate = TimeUtils.now(),
    endDate = TimeUtils.now().plusSeconds(2592000),
    status = SubscriptionStatus.Active,
    cancelledAt = None,
    effectiveEndDate = None,
    createdAt = TimeUtils.now(),
    updatedAt = TimeUtils.now()
  )

  private def createTestSubscriptionWithDates(
    orderId: OrderId,
    userId: UserId,
    startDate: Instant,
    endDate: Instant,
    status: SubscriptionStatus = SubscriptionStatus.Active
  ): Subscription = Subscription(
    id = SubscriptionId(UUID.randomUUID()),
    orderId = orderId,
    userId = userId,
    productId = ProductId("monthly"),
    startDate = TimeUtils.truncateToSeconds(startDate),
    endDate = TimeUtils.truncateToSeconds(endDate),
    status = status,
    cancelledAt = None,
    effectiveEndDate = None,
    createdAt = TimeUtils.now(),
    updatedAt = TimeUtils.now()
  )

  private def createTestOrderCancellation(
    orderId: OrderId,
    reason: CancellationReason = CancellationReason.UserRequest,
    cancellationType: CancellationType = CancellationType.Immediate,
    notes: Option[String] = Some("User requested cancellation")
  ): OrderCancellation =
    val now = TimeUtils.now()
    OrderCancellation(
      id = OrderCancellationId(UUID.randomUUID()),
      orderId = orderId,
      reason = reason,
      cancellationType = cancellationType,
      notes = notes,
      cancelledAt = now,
      cancelledBy = CancelledBy.User,
      effectiveDate = now,
      createdAt = now,
      updatedAt = now
    )
