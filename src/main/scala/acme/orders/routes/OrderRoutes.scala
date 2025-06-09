package acme.orders.routes

import acme.orders.OrderService
import acme.orders.models._
import acme.orders.utils.LoggingContext._
import acme.orders.utils.{CorrelationId, LoggingContext}
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

    def logRequest(method: Method, uri: Uri, correlationId: CorrelationId, startTime: Long): F[Unit] =
      val context = LoggingContext
        .withOperation("httpRequest")
        .withCorrelationId(correlationId)
        .withCustom("method", method.name)
        .withCustom("uri", uri.toString)
        .withCustom("timestamp", startTime.toString)
      logger.infoWithContext(context)(s"HTTP Request: ${method.name} ${uri.path}")

    def logResponse(status: Status, correlationId: CorrelationId, startTime: Long): F[Unit] =
      val context = LoggingContext
        .withOperation("httpResponse")
        .withCorrelationId(correlationId)
        .withCustom("statusCode", status.code.toString)
        .withCustom("duration", s"${System.currentTimeMillis() - startTime}ms")
      logger.infoWithContext(context)(s"HTTP Response: ${status.code}")

    def handleServiceError(error: ServiceError, correlationId: CorrelationId): F[Response[F]] =
      val context = LoggingContext.withOperation("handleServiceError").withCorrelationId(correlationId).withCustom("errorType", error.getClass.getSimpleName)
      logger.errorWithContext(context)(s"Service error: ${error.getMessage}") *> (error match
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
          case _ =>
            val context = LoggingContext.withOperation("POST /orders").withCorrelationId(correlationId)
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
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
          case _ =>
            val context = LoggingContext.withOperation("GET /orders/{id}").withCorrelationId(correlationId).withOrderId(OrderId(orderId))
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
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
          case _ =>
            val context = LoggingContext.withOperation("GET /users/{userId}/orders").withCorrelationId(correlationId).withUserId(UserId(userId))
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
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
          case _ =>
            val context = LoggingContext.withOperation("GET /users/{userId}/subscriptions").withCorrelationId(correlationId).withUserId(UserId(userId))
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
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
          case _ =>
            val context = LoggingContext.withOperation("GET /users/{userId}/subscription-status").withCorrelationId(correlationId).withUserId(UserId(userId))
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
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
          case _ =>
            val context = LoggingContext.withOperation("PUT /orders/{orderId}/cancel").withCorrelationId(correlationId).withOrderId(OrderId(orderId))
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
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
          case _ =>
            val context = LoggingContext.withOperation("GET /orders/{orderId}/cancellation").withCorrelationId(correlationId).withOrderId(OrderId(orderId))
            logger.errorWithContext(context)("Unexpected error") *> InternalServerError("Unexpected error")
        }
    }
