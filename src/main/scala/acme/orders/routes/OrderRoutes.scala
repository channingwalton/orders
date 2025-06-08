package acme.orders.routes

import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import io.circe.syntax.*
import acme.orders.{OrderService}
import acme.orders.models.*
import java.util.UUID

object OrderRoutes:

  def routes[F[_]: Async](orderService: OrderService[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] {
      case req @ POST -> Root / "orders" =>
        for
          createOrderRequest <- req.as[CreateOrderRequest]
          order <- orderService.createOrder(createOrderRequest)
          response <- Created(order.asJson)
        yield response

      case GET -> Root / "orders" / UUIDVar(orderId) => orderService.getOrder(OrderId(orderId)).flatMap {
          case Some(order) => Ok(order.asJson)
          case None        => NotFound()
        }

      case GET -> Root / "users" / userId / "orders" =>
        for
          orders <- orderService.getUserOrders(UserId(userId))
          response <- Ok(orders.asJson)
        yield response

      case GET -> Root / "users" / userId / "subscriptions" =>
        for
          subscriptions <- orderService.getUserSubscriptions(UserId(userId))
          response <- Ok(subscriptions.asJson)
        yield response

      case PUT -> Root / "orders" / UUIDVar(orderId) / "cancel" =>
        for
          _ <- orderService.cancelOrder(OrderId(orderId))
          response <- NoContent()
        yield response
    }
