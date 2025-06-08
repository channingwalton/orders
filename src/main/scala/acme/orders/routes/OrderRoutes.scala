package acme.orders.routes

import acme.orders.OrderService
import acme.orders.models._
import acme.orders.utils.{CorrelationId, Logging}
import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

object OrderRoutes:

  def routes[F[_]: Async](orderService: OrderService[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    implicit val logger: Logger[F] = Logging.logger[F]

    def logRequest(method: Method, uri: Uri, correlationId: CorrelationId, startTime: Long): F[Unit] = logger.info(
      Logging.logWithContext(
        s"HTTP Request: ${method.name} ${uri.path}",
        correlationId = Some(correlationId),
        additionalContext = Map(
          "method" -> method.name,
          "uri" -> uri.toString,
          "timestamp" -> startTime.toString
        )
      )
    )

    def logResponse(status: Status, correlationId: CorrelationId, startTime: Long): F[Unit] = logger.info(
      Logging.logWithContext(
        s"HTTP Response: ${status.code}",
        correlationId = Some(correlationId),
        additionalContext = Map(
          "statusCode" -> status.code.toString,
          "duration" -> Logging.formatDuration(startTime)
        )
      )
    )

    def handleServiceError(error: ServiceError, correlationId: CorrelationId): F[Response[F]] = logger.error(
      Logging.logWithContext(
        s"Service error: ${error.getMessage}",
        correlationId = Some(correlationId),
        additionalContext = Map("errorType" -> error.getClass.getSimpleName)
      )
    ) *> (error match
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
              Logging.logWithContext(
                "Unexpected error in POST /orders",
                correlationId = Some(correlationId)
              )
            ) *> InternalServerError("Unexpected error")
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
              Logging.logWithContext(
                "Unexpected error in GET /orders/{id}",
                correlationId = Some(correlationId),
                orderId = Some(orderId.toString)
              )
            ) *> InternalServerError("Unexpected error")
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
              Logging.logWithContext(
                "Unexpected error in GET /users/{userId}/orders",
                correlationId = Some(correlationId),
                userId = Some(userId)
              )
            ) *> InternalServerError("Unexpected error")
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
              Logging.logWithContext(
                "Unexpected error in GET /users/{userId}/subscriptions",
                correlationId = Some(correlationId),
                userId = Some(userId)
              )
            ) *> InternalServerError("Unexpected error")
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
              Logging.logWithContext(
                "Unexpected error in GET /users/{userId}/subscription-status",
                correlationId = Some(correlationId),
                userId = Some(userId)
              )
            ) *> InternalServerError("Unexpected error")
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
              Logging.logWithContext(
                "Unexpected error in PUT /orders/{orderId}/cancel",
                correlationId = Some(correlationId),
                orderId = Some(orderId.toString)
              )
            ) *> InternalServerError("Unexpected error")
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
              Logging.logWithContext(
                "Unexpected error in GET /orders/{orderId}/cancellation",
                correlationId = Some(correlationId),
                orderId = Some(orderId.toString)
              )
            ) *> InternalServerError("Unexpected error")
        }
    }
