package acme.orders

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import acme.orders.db.Store
import acme.orders.models._
import acme.orders.utils.{Logging, TimeUtils}
import cats.MonadThrow
import cats.effect._
import cats.syntax.all._
import org.typelevel.log4cats.Logger

trait OrderService[F[_]]:
  def createOrder(request: CreateOrderRequest): F[Order]
  def getOrder(orderId: OrderId): F[Option[Order]]
  def getUserOrders(userId: UserId): F[List[Order]]
  def getUserSubscriptions(userId: UserId): F[List[Subscription]]
  def getUserSubscriptionStatus(userId: UserId): F[UserSubscriptionStatus]
  def cancelOrder(orderId: OrderId): F[Unit]
  def cancelOrder(orderId: OrderId, request: CancelOrderRequest): F[Unit]
  def getOrderCancellation(orderId: OrderId): F[Option[OrderCancellation]]

class OrderServiceImpl[F[_]: MonadThrow: Sync, G[_]: MonadThrow](
  store: Store[F, G],
  clock: Clock[F]
)(implicit logger: Logger[F])
    extends OrderService[F]:

  def createOrder(request: CreateOrderRequest): F[Order] =
    for
      _ <- logger.info(
        Logging.logWithContext(
          "Creating order",
          userId = Some(request.userId),
          additionalContext = Map("productId" -> request.productId)
        )
      )
      rawNow <- clock.realTimeInstant
      now = TimeUtils.truncateToSeconds(rawNow)
      orderId = OrderId(UUID.randomUUID())
      userId = UserId(request.userId)
      productId = ProductId(request.productId)
      _ <- validateProduct(productId)
      order = Order(orderId, userId, productId, OrderStatus.Active, now, now)
      subscription <- createSubscriptionForOrder(order, now)
      _ <- store.commit(
        for
          _ <- store.createOrder(order)
          _ <- store.createSubscription(subscription)
        yield ()
      )
      _ <- logger.info(
        Logging.logWithContext(
          "Order created successfully",
          userId = Some(request.userId),
          orderId = Some(orderId.value.toString),
          additionalContext = Map("productId" -> request.productId)
        )
      )
    yield order

  def getOrder(orderId: OrderId): F[Option[Order]] = store.commit(store.findOrder(orderId))

  def getUserOrders(userId: UserId): F[List[Order]] = store.commit(store.findOrdersByUser(userId))

  def getUserSubscriptions(userId: UserId): F[List[Subscription]] = store.commit(store.findSubscriptionsByUser(userId))

  def getUserSubscriptionStatus(userId: UserId): F[UserSubscriptionStatus] = store.commit(store.findActiveSubscriptionsByUser(userId)).map {
    activeSubscriptions =>
      UserSubscriptionStatus(
        userId = userId.value,
        isSubscribed = activeSubscriptions.nonEmpty,
        activeSubscriptions = activeSubscriptions,
        subscriptionCount = activeSubscriptions.length
      )
  }

  def cancelOrder(orderId: OrderId): F[Unit] = cancelOrder(orderId, CancelOrderRequest(None, None, None))

  def cancelOrder(orderId: OrderId, request: CancelOrderRequest): F[Unit] =
    for
      _ <- logger.info(
        Logging.logWithContext(
          "Cancelling order",
          orderId = Some(orderId.value.toString),
          additionalContext = Map(
            "reason" -> request.reason.map(_.toString).getOrElse("UserRequest"),
            "cancellationType" -> request.cancellationType.map(_.toString).getOrElse("Immediate")
          )
        )
      )
      rawNow <- clock.realTimeInstant
      now = TimeUtils.truncateToSeconds(rawNow)
      _ <- store.commit(
        for
          orderOpt <- store.findOrder(orderId)
          order <- orderOpt.liftTo[G](ServiceError.OrderNotFound(orderId))
          _ <- if order.status == OrderStatus.Cancelled then ServiceError.OrderAlreadyCancelled(orderId).raiseError[G, Unit] else ().pure[G]
          subscriptions <- store.findSubscriptionsByOrder(orderId)
          reason = request.reason.getOrElse(CancellationReason.UserRequest)
          cancellationType = request.cancellationType.getOrElse(CancellationType.Immediate)
          effectiveDate = cancellationType match
            case CancellationType.Immediate   => now
            case CancellationType.EndOfPeriod => subscriptions.headOption.map(_.endDate).getOrElse(now)
          cancellation = OrderCancellation(
            id = OrderCancellationId(UUID.randomUUID()),
            orderId = orderId,
            reason = reason,
            cancellationType = cancellationType,
            notes = request.notes,
            cancelledAt = now,
            cancelledBy = CancelledBy.User,
            effectiveDate = effectiveDate,
            createdAt = now,
            updatedAt = now
          )
          cancelledOrder = order.copy(status = OrderStatus.Cancelled, updatedAt = now)
          cancelledSubscriptions = subscriptions.map { subscription =>
            subscription.copy(
              status = SubscriptionStatus.Cancelled,
              cancelledAt = Some(now),
              effectiveEndDate = Some(effectiveDate),
              updatedAt = now
            )
          }
          _ <- store.updateOrder(cancelledOrder)
          _ <- store.createOrderCancellation(cancellation)
          _ <- cancelledSubscriptions.traverse(store.updateSubscription)
        yield ()
      )
      _ <- logger.info(
        Logging.logWithContext(
          "Order cancelled successfully",
          orderId = Some(orderId.value.toString),
          additionalContext = Map(
            "reason" -> request.reason.map(_.toString).getOrElse("UserRequest"),
            "subscriptionCount" -> "updated"
          )
        )
      )
    yield ()

  def getOrderCancellation(orderId: OrderId): F[Option[OrderCancellation]] = store.commit(store.findOrderCancellation(orderId))

  private def validateProduct(productId: ProductId): F[Unit] = Product.values.find(_.id == productId) match
    case Some(_) => ().pure[F]
    case None => logger.warn(
        Logging.logWithContext(
          "Invalid product validation failed",
          additionalContext = Map("productId" -> productId.value)
        )
      ) *> ServiceError.InvalidProduct(productId).raiseError[F, Unit]

  private def createSubscriptionForOrder(order: Order, now: Instant): F[Subscription] = Product.values.find(_.id == order.productId) match
    case Some(product) =>
      val rawEndDate = product.duration match
        case ProductDuration.Month => now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant
        case ProductDuration.Year  => now.atZone(ZoneOffset.UTC).plusYears(1).toInstant
      val endDate = TimeUtils.truncateToSeconds(rawEndDate)

      val subscription = Subscription(
        id = SubscriptionId(UUID.randomUUID()),
        orderId = order.id,
        userId = order.userId,
        productId = order.productId,
        startDate = now,
        endDate = endDate,
        status = SubscriptionStatus.Active,
        cancelledAt = None,
        effectiveEndDate = None,
        createdAt = now,
        updatedAt = now
      )
      subscription.pure[F]
    case None => ServiceError.InvalidProduct(order.productId).raiseError[F, Subscription]

object OrderService:
  def apply[F[_]: MonadThrow: Clock: Sync, G[_]: MonadThrow](
    store: Store[F, G]
  ): OrderService[F] =
    implicit val logger: Logger[F] = Logging.logger[F]
    new OrderServiceImpl(store, Clock[F])

  def apply[F[_]: MonadThrow: Sync, G[_]: MonadThrow](
    store: Store[F, G],
    clock: Clock[F]
  ): OrderService[F] =
    implicit val logger: Logger[F] = Logging.logger[F]
    new OrderServiceImpl(store, clock)
