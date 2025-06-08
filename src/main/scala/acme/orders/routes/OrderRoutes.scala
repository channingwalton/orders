package acme.orders.routes

import acme.orders.OrderService
import acme.orders.models._
import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

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

      case GET -> Root / "users" / userId / "subscription-status" =>
        for
          status <- orderService.getUserSubscriptionStatus(UserId(userId))
          response <- Ok(status.asJson)
        yield response

      case req @ PUT -> Root / "orders" / UUIDVar(orderId) / "cancel" =>
        for
          cancelRequest <- req.as[CancelOrderRequest].recoverWith { case _: Exception =>
            CancelOrderRequest(None, None, None).pure[F]
          }
          _ <- orderService.cancelOrder(OrderId(orderId), cancelRequest)
          response <- NoContent()
        yield response

      case GET -> Root / "orders" / UUIDVar(orderId) / "cancellation" => orderService.getOrderCancellation(OrderId(orderId)).flatMap {
          case Some(cancellation) => Ok(cancellation.asJson)
          case None               => NotFound()
        }
    }
