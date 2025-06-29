package acme.orders.routes

import java.util.UUID

import acme.orders.OrderService
import acme.orders.models._
import acme.orders.utils.TimeUtils
import cats.effect._
import io.circe.syntax._
import munit.CatsEffectSuite
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request, Status, Uri}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class OrderRoutesTest extends CatsEffectSuite:

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  test("POST /orders should create order") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      request = Request[IO](Method.POST, uri"/orders").withEntity(CreateOrderRequest("user1", "monthly").asJson)
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.Created)
    }
  }

  test("GET /orders/{id} should return order") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      orderId = UUID.randomUUID()
      request = Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/$orderId"))
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.Ok)
    }
  }

  test("GET /users/{userId}/orders should return user orders") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      request = Request[IO](Method.GET, uri"/users/user1/orders")
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.Ok)
    }
  }

  test("GET /users/{userId}/subscriptions should return user subscriptions") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      request = Request[IO](Method.GET, uri"/users/user1/subscriptions")
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.Ok)
    }
  }

  test("GET /users/{userId}/subscription-status should return subscription status") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      request = Request[IO](Method.GET, uri"/users/user1/subscription-status")
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.Ok)
    }
  }

  test("PUT /orders/{id}/cancel should cancel order") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      orderId = UUID.randomUUID()
      request = Request[IO](Method.PUT, Uri.unsafeFromString(s"/orders/$orderId/cancel"))
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.NoContent)
    }
  }

  test("PUT /orders/{id}/cancel with request body should cancel order with details") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      orderId = UUID.randomUUID()
      cancelRequest = CancelOrderRequest(Some(CancellationReason.UserRequest), Some(CancellationType.Immediate), Some("User requested"))
      request = Request[IO](Method.PUT, Uri.unsafeFromString(s"/orders/$orderId/cancel")).withEntity(cancelRequest.asJson)
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.NoContent)
    }
  }

  test("GET /orders/{id}/cancellation should return cancellation details") {
    for
      service <- createMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      orderId = UUID.randomUUID()
      request = Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/$orderId/cancellation"))
      response <- routes.orNotFound.run(request)
    yield {
      assertEquals(response.status, Status.Ok)
    }
  }

  test("POST /orders should return 400 for invalid product") {
    for
      service <- createErrorMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      request = Request[IO](Method.POST, uri"/orders").withEntity(CreateOrderRequest("user1", "invalid").asJson)
      response <- routes.orNotFound.run(request)
      body <- response.as[String]
    yield {
      assertEquals(response.status, Status.BadRequest)
      assert(body.contains("Invalid product"))
    }
  }

  test("GET /orders/{id} should return 404 for non-existent order") {
    for
      service <- createErrorMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      orderId = UUID.randomUUID()
      request = Request[IO](Method.GET, Uri.unsafeFromString(s"/orders/$orderId"))
      response <- routes.orNotFound.run(request)
      body <- response.as[String]
    yield {
      assertEquals(response.status, Status.NotFound)
      assert(body.contains("Order not found"))
    }
  }

  test("PUT /orders/{id}/cancel should return 409 for already cancelled order") {
    for
      service <- createErrorMockOrderService()
      routes = OrderRoutes.routes[IO](service)
      orderId = UUID.randomUUID()
      request = Request[IO](Method.PUT, Uri.unsafeFromString(s"/orders/$orderId/cancel"))
      response <- routes.orNotFound.run(request)
      body <- response.as[String]
    yield {
      assertEquals(response.status, Status.Conflict)
      assert(body.contains("Order already cancelled"))
    }
  }

  private def createMockOrderService(): IO[OrderService[IO]] = IO.pure(new OrderService[IO] {
    def createOrder(request: CreateOrderRequest): IO[Order] = IO.pure(
      Order(
        OrderId(UUID.randomUUID()),
        UserId(request.userId),
        acme.orders.models.ProductId(request.productId),
        OrderStatus.Active,
        TimeUtils.now(),
        TimeUtils.now()
      )
    )

    def getOrder(orderId: OrderId): IO[Option[Order]] = IO.pure(
      Some(
        Order(
          orderId,
          UserId("user1"),
          acme.orders.models.ProductId("monthly"),
          OrderStatus.Active,
          TimeUtils.now(),
          TimeUtils.now()
        )
      )
    )

    def getUserOrders(userId: UserId): IO[List[Order]] = IO.pure(
      List(
        Order(
          OrderId(UUID.randomUUID()),
          userId,
          acme.orders.models.ProductId("monthly"),
          OrderStatus.Active,
          TimeUtils.now(),
          TimeUtils.now()
        )
      )
    )

    def getUserSubscriptions(userId: UserId): IO[List[Subscription]] = IO.pure(
      List(
        Subscription(
          SubscriptionId(UUID.randomUUID()),
          OrderId(UUID.randomUUID()),
          userId,
          acme.orders.models.ProductId("monthly"),
          TimeUtils.now(),
          TimeUtils.now().plusSeconds(2592000),
          SubscriptionStatus.Active,
          None,
          None,
          TimeUtils.now(),
          TimeUtils.now()
        )
      )
    )

    def getUserSubscriptionStatus(userId: UserId): IO[UserSubscriptionStatus] = IO.pure(
      UserSubscriptionStatus(
        userId = userId.value,
        isSubscribed = true,
        activeSubscriptions = List(
          Subscription(
            SubscriptionId(UUID.randomUUID()),
            OrderId(UUID.randomUUID()),
            userId,
            acme.orders.models.ProductId("monthly"),
            TimeUtils.now(),
            TimeUtils.now().plusSeconds(2592000),
            SubscriptionStatus.Active,
            None,
            None,
            TimeUtils.now(),
            TimeUtils.now()
          )
        ),
        subscriptionCount = 1
      )
    )

    def cancelOrder(orderId: OrderId): IO[Unit] = IO.unit

    def cancelOrder(orderId: OrderId, request: CancelOrderRequest): IO[Unit] = IO.unit

    def getOrderCancellation(orderId: OrderId): IO[Option[OrderCancellation]] = IO.pure(
      Some(
        OrderCancellation(
          OrderCancellationId(UUID.randomUUID()),
          orderId,
          CancellationReason.UserRequest,
          CancellationType.Immediate,
          Some("User requested cancellation"),
          TimeUtils.now(),
          CancelledBy.User,
          TimeUtils.now(),
          TimeUtils.now(),
          TimeUtils.now()
        )
      )
    )
  })

  private def createErrorMockOrderService(): IO[OrderService[IO]] = IO.pure(new OrderService[IO] {
    def createOrder(request: CreateOrderRequest): IO[Order] = IO.raiseError(ServiceError.InvalidProduct(ProductId(request.productId)))

    def getOrder(orderId: OrderId): IO[Option[Order]] = IO.raiseError(ServiceError.OrderNotFound(orderId))

    def getUserOrders(userId: UserId): IO[List[Order]] = IO.pure(List.empty)

    def getUserSubscriptions(userId: UserId): IO[List[Subscription]] = IO.pure(List.empty)

    def getUserSubscriptionStatus(userId: UserId): IO[UserSubscriptionStatus] = IO.pure(
      UserSubscriptionStatus(userId.value, false, List.empty, 0)
    )

    def cancelOrder(orderId: OrderId): IO[Unit] = IO.raiseError(ServiceError.OrderAlreadyCancelled(orderId))

    def cancelOrder(orderId: OrderId, request: CancelOrderRequest): IO[Unit] = IO.raiseError(ServiceError.OrderAlreadyCancelled(orderId))

    def getOrderCancellation(orderId: OrderId): IO[Option[OrderCancellation]] = IO.pure(None)
  })
