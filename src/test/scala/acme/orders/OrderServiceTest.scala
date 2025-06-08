package acme.orders

import scala.collection.mutable

import acme.orders.models._
import cats.effect._
import munit.CatsEffectSuite

class OrderServiceTest extends CatsEffectSuite:

  test("createOrder should create order and subscription") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
    yield {
      assertEquals(order.userId, UserId("user1"))
      assertEquals(order.productId, ProductId("monthly"))
      assertEquals(order.status, OrderStatus.Active)
    }
  }

  test("createOrder should fail for invalid product") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "invalid")
      result <- service.createOrder(request).attempt
    yield {
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[ServiceError.InvalidProduct]))
    }
  }

  test("getUserOrders should return user's orders") {
    for
      service <- createOrderService()
      request1 = CreateOrderRequest("user1", "monthly")
      request2 = CreateOrderRequest("user1", "annual")
      _ <- service.createOrder(request1)
      _ <- service.createOrder(request2)
      orders <- service.getUserOrders(UserId("user1"))
    yield {
      assertEquals(orders.length, 2)
      assert(orders.forall(_.userId == UserId("user1")))
    }
  }

  test("getUserSubscriptions should return user's subscriptions") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      _ <- service.createOrder(request)
      subscriptions <- service.getUserSubscriptions(UserId("user1"))
    yield {
      assertEquals(subscriptions.length, 1)
      assertEquals(subscriptions.head.userId, UserId("user1"))
      assertEquals(subscriptions.head.status, SubscriptionStatus.Active)
    }
  }

  test("cancelOrder should update order status") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
      _ <- service.cancelOrder(order.id)
      updatedOrder <- service.getOrder(order.id)
    yield {
      assert(updatedOrder.isDefined)
      assertEquals(updatedOrder.get.status, OrderStatus.Cancelled)
    }
  }

  test("getUserSubscriptionStatus should return correct status for subscribed user") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      _ <- service.createOrder(request)
      status <- service.getUserSubscriptionStatus(UserId("user1"))
    yield {
      assertEquals(status.userId, "user1")
      assertEquals(status.isSubscribed, true)
      assertEquals(status.subscriptionCount, 1)
      assertEquals(status.activeSubscriptions.length, 1)
    }
  }

  test("getUserSubscriptionStatus should return correct status for unsubscribed user") {
    for
      service <- createOrderService()
      status <- service.getUserSubscriptionStatus(UserId("user2"))
    yield {
      assertEquals(status.userId, "user2")
      assertEquals(status.isSubscribed, false)
      assertEquals(status.subscriptionCount, 0)
      assertEquals(status.activeSubscriptions.length, 0)
    }
  }

  private def createOrderService(): IO[OrderService[IO]] = IO.pure(OrderService[IO, IO](new MockStore(), Clock[IO]))

  private class MockStore extends acme.orders.db.Store[IO, IO]:
    private val orders = mutable.Map[OrderId, Order]()
    private val subscriptions = mutable.Map[SubscriptionId, Subscription]()

    def createOrder(order: Order): IO[OrderId] = IO {
      orders += order.id -> order
      order.id
    }

    def findOrder(orderId: OrderId): IO[Option[Order]] = IO.pure(orders.get(orderId))

    def findOrdersByUser(userId: UserId): IO[List[Order]] = IO.pure(orders.values.filter(_.userId == userId).toList)

    def updateOrder(order: Order): IO[Unit] = IO {
      orders += order.id -> order
    }

    def createSubscription(subscription: Subscription): IO[SubscriptionId] = IO {
      subscriptions += subscription.id -> subscription
      subscription.id
    }

    def findSubscriptionsByUser(userId: UserId): IO[List[Subscription]] = IO.pure(subscriptions.values.filter(_.userId == userId).toList)

    def findActiveSubscriptionsByUser(userId: UserId): IO[List[Subscription]] = Clock[IO].realTimeInstant.map { now =>
      subscriptions.values.filter { sub =>
        sub.userId == userId &&
        sub.status == SubscriptionStatus.Active &&
        !sub.startDate.isAfter(now) &&
        sub.endDate.isAfter(now)
      }.toList
    }

    def commit[A](fa: IO[A]): IO[A] = fa

    def lift: cats.~>[IO, IO] = cats.arrow.FunctionK.id[IO]
