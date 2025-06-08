package acme.orders.routes

import java.time.Instant
import java.util.UUID

import acme.orders.OrderService
import acme.orders.models._
import cats.effect._
import io.circe.syntax._
import munit.CatsEffectSuite
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request, Status, Uri}

class OrderRoutesTest extends CatsEffectSuite:

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

  private def createMockOrderService(): IO[OrderService[IO]] = IO.pure(new OrderService[IO] {
    def createOrder(request: CreateOrderRequest): IO[Order] = IO.pure(
      Order(
        OrderId(UUID.randomUUID()),
        UserId(request.userId),
        acme.orders.models.ProductId(request.productId),
        OrderStatus.Active,
        Instant.now(),
        Instant.now()
      )
    )

    def getOrder(orderId: OrderId): IO[Option[Order]] = IO.pure(
      Some(
        Order(
          orderId,
          UserId("user1"),
          acme.orders.models.ProductId("monthly"),
          OrderStatus.Active,
          Instant.now(),
          Instant.now()
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
          Instant.now(),
          Instant.now()
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
          Instant.now(),
          Instant.now().plusSeconds(2592000),
          SubscriptionStatus.Active,
          Instant.now(),
          Instant.now()
        )
      )
    )

    def cancelOrder(orderId: OrderId): IO[Unit] = IO.unit
  })
