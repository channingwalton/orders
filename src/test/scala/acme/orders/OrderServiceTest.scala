package acme.orders

import scala.collection.mutable

import acme.orders.models._
import acme.orders.utils.TimeUtils
import cats.effect._
import munit.CatsEffectSuite
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

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

  test("cancelOrder with request should cancel order and subscriptions") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
      cancelRequest = CancelOrderRequest(Some(CancellationReason.UserRequest), Some(CancellationType.Immediate), Some("User requested cancellation"))
      _ <- service.cancelOrder(order.id, cancelRequest)
      updatedOrder <- service.getOrder(order.id)
      subscriptions <- service.getUserSubscriptions(UserId("user1"))
      cancellation <- service.getOrderCancellation(order.id)
    yield {
      assertEquals(updatedOrder.get.status, OrderStatus.Cancelled)
      assertEquals(subscriptions.head.status, SubscriptionStatus.Cancelled)
      assert(subscriptions.head.cancelledAt.isDefined)
      assert(cancellation.isDefined)
      assertEquals(cancellation.get.reason, CancellationReason.UserRequest)
      assertEquals(cancellation.get.cancellationType, CancellationType.Immediate)
      assertEquals(cancellation.get.notes, Some("User requested cancellation"))
    }
  }

  test("cancelOrder should handle end of period cancellation") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
      cancelRequest = CancelOrderRequest(Some(CancellationReason.UserRequest), Some(CancellationType.EndOfPeriod), None)
      _ <- service.cancelOrder(order.id, cancelRequest)
      subscriptions <- service.getUserSubscriptions(UserId("user1"))
      cancellation <- service.getOrderCancellation(order.id)
    yield {
      assertEquals(subscriptions.head.status, SubscriptionStatus.Cancelled)
      assert(subscriptions.head.effectiveEndDate.isDefined)
      assertEquals(cancellation.get.cancellationType, CancellationType.EndOfPeriod)
    }
  }

  test("cancelOrder should fail for already cancelled order") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
      _ <- service.cancelOrder(order.id)
      result <- service.cancelOrder(order.id).attempt
    yield {
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[ServiceError.OrderAlreadyCancelled]))
    }
  }

  test("cancelOrder should fail for non-existent order") {
    for
      service <- createOrderService()
      nonExistentOrderId = OrderId(java.util.UUID.randomUUID())
      result <- service.cancelOrder(nonExistentOrderId).attempt
    yield {
      assert(result.isLeft)
      assert(result.left.exists(_.isInstanceOf[ServiceError.OrderNotFound]))
    }
  }

  test("getUserSubscriptionStatus should not include cancelled subscriptions") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
      _ <- service.cancelOrder(order.id)
      status <- service.getUserSubscriptionStatus(UserId("user1"))
    yield {
      assertEquals(status.isSubscribed, false)
      assertEquals(status.subscriptionCount, 0)
      assertEquals(status.activeSubscriptions.length, 0)
    }
  }

  test("getOrderCancellation should return None for non-cancelled order") {
    for
      service <- createOrderService()
      request = CreateOrderRequest("user1", "monthly")
      order <- service.createOrder(request)
      cancellation <- service.getOrderCancellation(order.id)
    yield {
      assert(cancellation.isEmpty)
    }
  }

  private def createOrderService(): IO[OrderService[IO]] =
    implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    IO.pure(OrderService[IO, IO](new MockStore(), Clock[IO]))

  private class MockStore extends acme.orders.db.Store[IO, IO]:
    private val orders = mutable.Map[OrderId, Order]()
    private val subscriptions = mutable.Map[SubscriptionId, Subscription]()
    private val cancellations = mutable.Map[OrderId, OrderCancellation]()

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

    def findActiveSubscriptionsByUser(userId: UserId): IO[List[Subscription]] = Clock[IO].realTimeInstant.map { rawNow =>
      val now = TimeUtils.truncateToSeconds(rawNow)
      subscriptions.values.filter { sub =>
        sub.userId == userId &&
        sub.status == SubscriptionStatus.Active &&
        !sub.startDate.isAfter(now) &&
        sub.endDate.isAfter(now)
      }.toList
    }

    def findSubscriptionsByOrder(orderId: OrderId): IO[List[Subscription]] = IO.pure(subscriptions.values.filter(_.orderId == orderId).toList)

    def updateSubscription(subscription: Subscription): IO[Unit] = IO {
      subscriptions += subscription.id -> subscription
    }

    def createOrderCancellation(cancellation: OrderCancellation): IO[OrderCancellationId] = IO {
      cancellations += cancellation.orderId -> cancellation
      cancellation.id
    }

    def findOrderCancellation(orderId: OrderId): IO[Option[OrderCancellation]] = IO.pure(cancellations.get(orderId))

    def commit[A](fa: IO[A]): IO[A] = fa

    def lift: cats.~>[IO, IO] = cats.arrow.FunctionK.id[IO]
