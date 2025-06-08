package acme.orders

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import acme.orders.db.Store
import acme.orders.models._
import cats.MonadThrow
import cats.effect._
import cats.syntax.all._

trait OrderService[F[_]]:
  def createOrder(request: CreateOrderRequest): F[Order]
  def getOrder(orderId: OrderId): F[Option[Order]]
  def getUserOrders(userId: UserId): F[List[Order]]
  def getUserSubscriptions(userId: UserId): F[List[Subscription]]
  def cancelOrder(orderId: OrderId): F[Unit]

class OrderServiceImpl[F[_]: MonadThrow, G[_]](
  store: Store[F, G],
  clock: Clock[F]
) extends OrderService[F]:

  def createOrder(request: CreateOrderRequest): F[Order] =
    for
      now <- clock.realTimeInstant
      orderId = OrderId(UUID.randomUUID())
      userId = UserId(request.userId)
      productId = ProductId(request.productId)
      _ <- validateProduct(productId)
      order = Order(orderId, userId, productId, OrderStatus.Active, now, now)
      _ <- store.commit(store.createOrder(order))
      subscription <- createSubscriptionForOrder(order, now)
      _ <- store.commit(store.createSubscription(subscription))
    yield order

  def getOrder(orderId: OrderId): F[Option[Order]] = store.commit(store.findOrder(orderId))

  def getUserOrders(userId: UserId): F[List[Order]] = store.commit(store.findOrdersByUser(userId))

  def getUserSubscriptions(userId: UserId): F[List[Subscription]] = store.commit(store.findSubscriptionsByUser(userId))

  def cancelOrder(orderId: OrderId): F[Unit] =
    for
      orderOpt <- store.commit(store.findOrder(orderId))
      order <- orderOpt.liftTo[F](ServiceError.OrderNotFound(orderId))
      now <- clock.realTimeInstant
      cancelledOrder = order.copy(status = OrderStatus.Cancelled, updatedAt = now)
      _ <- store.commit(store.updateOrder(cancelledOrder))
    yield ()

  private def validateProduct(productId: ProductId): F[Unit] = Product.values.find(_.id == productId) match
    case Some(_) => ().pure[F]
    case None    => ServiceError.InvalidProduct(productId).raiseError[F, Unit]

  private def createSubscriptionForOrder(order: Order, now: Instant): F[Subscription] = Product.values.find(_.id == order.productId) match
    case Some(product) =>
      val endDate = product.duration match
        case ProductDuration.Month => now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant
        case ProductDuration.Year  => now.atZone(ZoneOffset.UTC).plusYears(1).toInstant

      val subscription = Subscription(
        id = SubscriptionId(UUID.randomUUID()),
        orderId = order.id,
        userId = order.userId,
        productId = order.productId,
        startDate = now,
        endDate = endDate,
        status = SubscriptionStatus.Active,
        createdAt = now,
        updatedAt = now
      )
      subscription.pure[F]
    case None => ServiceError.InvalidProduct(order.productId).raiseError[F, Subscription]

object OrderService:
  def apply[F[_]: MonadThrow: Clock, G[_]](
    store: Store[F, G]
  ): OrderService[F] = new OrderServiceImpl(store, Clock[F])

  def apply[F[_]: MonadThrow, G[_]](
    store: Store[F, G],
    clock: Clock[F]
  ): OrderService[F] = new OrderServiceImpl(store, clock)
