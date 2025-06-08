package acme.orders.routes

import acme.orders.OrderService
import acme.orders.models._
import acme.orders.utils.CorrelationId
import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}

object OrderRoutes:

  def routes[F[_]: Async: LoggerFactory](orderService: OrderService[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    val logger: SelfAwareStructuredLogger[F] = LoggerFactory[F].getLogger

    def logRequest(method: Method, uri: Uri, correlationId: CorrelationId, startTime: Long): F[Unit] = logger.info(
      Map(
        "operation" -> "httpRequest",
        "method" -> method.name,
        "uri" -> uri.toString,
        "correlationId" -> correlationId.value,
        "timestamp" -> startTime.toString
      )
    )(s"HTTP Request: ${method.name} ${uri.path}")

    def logResponse(status: Status, correlationId: CorrelationId, startTime: Long): F[Unit] = logger.info(
      Map(
        "operation" -> "httpResponse",
        "statusCode" -> status.code.toString,
        "correlationId" -> correlationId.value,
        "duration" -> s"${System.currentTimeMillis() - startTime}ms"
      )
    )(s"HTTP Response: ${status.code}")

    def handleServiceError(error: ServiceError, correlationId: CorrelationId): F[Response[F]] = logger.error(
      Map(
        "operation" -> "handleServiceError",
        "correlationId" -> correlationId.value,
        "errorType" -> error.getClass.getSimpleName
      )
    )(s"Service error: ${error.getMessage}") *> (error match
      case ServiceError.OrderNotFound(_)         => NotFound(error.getMessage)
      case ServiceError.InvalidProduct(_)        => BadRequest(error.getMessage)
      case ServiceError.OrderAlreadyCancelled(_) => Conflict(error.getMessage)
      case ServiceError.DatabaseError(_)         => InternalServerError("Internal server error"))

    HttpRoutes.of[F] {
      case req @ POST -> Root / "orders" =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          createOrderRequest <- req.as[CreateOrderRequest]
          order <- orderService.createOrder(createOrderRequest)
          response <- Created(order.asJson)
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "POST /orders",
                "correlationId" -> correlationId.value
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }

      case req @ GET -> Root / "orders" / UUIDVar(orderId) =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          orderOpt <- orderService.getOrder(OrderId(orderId))
          response <- orderOpt match
            case Some(order) => Ok(order.asJson)
            case None        => NotFound()
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "GET /orders/{id}",
                "correlationId" -> correlationId.value,
                "orderId" -> orderId.toString
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }

      case req @ GET -> Root / "users" / userId / "orders" =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          orders <- orderService.getUserOrders(UserId(userId))
          response <- Ok(orders.asJson)
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "GET /users/{userId}/orders",
                "correlationId" -> correlationId.value,
                "userId" -> userId
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }

      case req @ GET -> Root / "users" / userId / "subscriptions" =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          subscriptions <- orderService.getUserSubscriptions(UserId(userId))
          response <- Ok(subscriptions.asJson)
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "GET /users/{userId}/subscriptions",
                "correlationId" -> correlationId.value,
                "userId" -> userId
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }

      case req @ GET -> Root / "users" / userId / "subscription-status" =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          status <- orderService.getUserSubscriptionStatus(UserId(userId))
          response <- Ok(status.asJson)
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "GET /users/{userId}/subscription-status",
                "correlationId" -> correlationId.value,
                "userId" -> userId
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }

      case req @ PUT -> Root / "orders" / UUIDVar(orderId) / "cancel" =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          cancelRequest <- req.as[CancelOrderRequest].recoverWith { case _: Exception => CancelOrderRequest(None, None, None).pure[F] }
          _ <- orderService.cancelOrder(OrderId(orderId), cancelRequest)
          response <- NoContent()
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "PUT /orders/{orderId}/cancel",
                "correlationId" -> correlationId.value,
                "orderId" -> orderId.toString
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }

      case req @ GET -> Root / "orders" / UUIDVar(orderId) / "cancellation" =>
        val correlationId = CorrelationId.generate
        val startTime = System.currentTimeMillis()
        (for
          _ <- logRequest(req.method, req.uri, correlationId, startTime)
          cancellationOpt <- orderService.getOrderCancellation(OrderId(orderId))
          response <- cancellationOpt match
            case Some(cancellation) => Ok(cancellation.asJson)
            case None               => NotFound()
          _ <- logResponse(response.status, correlationId, startTime)
        yield response).handleErrorWith {
          case error: ServiceError => handleServiceError(error, correlationId)
          case _ => logger.error(
              Map(
                "operation" -> "GET /orders/{orderId}/cancellation",
                "correlationId" -> correlationId.value,
                "orderId" -> orderId.toString
              )
            )("Unexpected error") *> InternalServerError("Unexpected error")
        }
    }
