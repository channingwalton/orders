package acme.orders.utils

import acme.orders.models._
import org.typelevel.log4cats.SelfAwareStructuredLogger

case class LoggingContext private (context: Map[String, String]):
  def withOperation(operation: String): LoggingContext = copy(context = context + ("operation" -> operation))

  def withCorrelationId(correlationId: CorrelationId): LoggingContext = copy(context = context + ("correlationId" -> correlationId.value))

  def withUserId(userId: UserId): LoggingContext = copy(context = context + ("userId" -> userId.value))

  def withOrderId(orderId: OrderId): LoggingContext = copy(context = context + ("orderId" -> orderId.value.toString))

  def withSubscriptionId(subscriptionId: SubscriptionId): LoggingContext = copy(context = context + ("subscriptionId" -> subscriptionId.value.toString))

  def withProductId(productId: ProductId): LoggingContext = copy(context = context + ("productId" -> productId.value))

  def withOrderCancellationId(cancellationId: OrderCancellationId): LoggingContext =
    copy(context = context + ("cancellationId" -> cancellationId.value.toString))

  def withCustom(key: String, value: String): LoggingContext = copy(context = context + (key -> value))

  def toMap: Map[String, String] = context

object LoggingContext:
  def empty: LoggingContext = LoggingContext(Map.empty)

  def withOperation(operation: String): LoggingContext = empty.withOperation(operation)

  extension [F[_]](logger: SelfAwareStructuredLogger[F])
    def infoWithContext(ctx: LoggingContext)(message: => String): F[Unit] = logger.info(ctx.toMap)(message)

    def errorWithContext(ctx: LoggingContext, throwable: Throwable)(message: => String): F[Unit] = logger.error(ctx.toMap, throwable)(message)

    def errorWithContext(ctx: LoggingContext)(message: => String): F[Unit] = logger.error(ctx.toMap)(message)

    def warnWithContext(ctx: LoggingContext)(message: => String): F[Unit] = logger.warn(ctx.toMap)(message)
