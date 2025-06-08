package acme.orders.db

import java.time.Instant
import java.util.UUID

import acme.orders.models._
import doobie._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._

object Statements:

  given Get[OrderId] = Get[UUID].map(OrderId.apply)
  given Put[OrderId] = Put[UUID].contramap(_.value)

  given Get[SubscriptionId] = Get[UUID].map(SubscriptionId.apply)
  given Put[SubscriptionId] = Put[UUID].contramap(_.value)

  given Get[UserId] = Get[String].map(UserId.apply)
  given Put[UserId] = Put[String].contramap(_.value)

  given Get[ProductId] = Get[String].map(ProductId.apply)
  given Put[ProductId] = Put[String].contramap(_.value)

  given Get[OrderStatus] = Get[String].map {
    case "active"    => OrderStatus.Active
    case "cancelled" => OrderStatus.Cancelled
  }
  given Put[OrderStatus] = Put[String].contramap(_.toString.toLowerCase)

  given Get[SubscriptionStatus] = Get[String].map {
    case "active"    => SubscriptionStatus.Active
    case "expired"   => SubscriptionStatus.Expired
    case "cancelled" => SubscriptionStatus.Cancelled
  }
  given Put[SubscriptionStatus] = Put[String].contramap(_.toString.toLowerCase)

  given Get[OrderCancellationId] = Get[UUID].map(OrderCancellationId.apply)
  given Put[OrderCancellationId] = Put[UUID].contramap(_.value)

  given Get[CancellationReason] = Get[String].map {
    case "user_request"    => CancellationReason.UserRequest
    case "payment_failure" => CancellationReason.PaymentFailure
    case "violation"       => CancellationReason.Violation
    case "other"           => CancellationReason.Other
  }
  given Put[CancellationReason] = Put[String].contramap {
    case CancellationReason.UserRequest    => "user_request"
    case CancellationReason.PaymentFailure => "payment_failure"
    case CancellationReason.Violation      => "violation"
    case CancellationReason.Other          => "other"
  }

  given Get[CancellationType] = Get[String].map {
    case "immediate"     => CancellationType.Immediate
    case "end_of_period" => CancellationType.EndOfPeriod
  }
  given Put[CancellationType] = Put[String].contramap {
    case CancellationType.Immediate   => "immediate"
    case CancellationType.EndOfPeriod => "end_of_period"
  }

  given Get[CancelledBy] = Get[String].map {
    case "user"   => CancelledBy.User
    case "system" => CancelledBy.System
    case "admin"  => CancelledBy.Admin
  }
  given Put[CancelledBy] = Put[String].contramap(_.toString.toLowerCase)

  def insertOrder(order: Order): Update0 = sql"""
      INSERT INTO orders (id, user_id, product_id, status, created_at, updated_at)
      VALUES (${order.id}, ${order.userId}, ${order.productId}, ${order.status}, ${order.createdAt}, ${order.updatedAt})
    """.update

  def selectOrder(orderId: OrderId): Query0[Order] = sql"""
      SELECT id, user_id, product_id, status, created_at, updated_at
      FROM orders
      WHERE id = $orderId
    """.query[Order]

  def selectOrdersByUser(userId: UserId): Query0[Order] = sql"""
      SELECT id, user_id, product_id, status, created_at, updated_at
      FROM orders
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """.query[Order]

  def updateOrderStatus(orderId: OrderId, status: OrderStatus, updatedAt: Instant): Update0 = sql"""
      UPDATE orders
      SET status = $status, updated_at = $updatedAt
      WHERE id = $orderId
    """.update

  def insertSubscription(subscription: Subscription): Update0 = sql"""
      INSERT INTO subscriptions (id, order_id, user_id, product_id, start_date, end_date, status, cancelled_at, effective_end_date, created_at, updated_at)
      VALUES (${subscription.id}, ${subscription.orderId}, ${subscription.userId}, ${subscription.productId}, 
              ${subscription.startDate}, ${subscription.endDate}, ${subscription.status}, 
              ${subscription.cancelledAt}, ${subscription.effectiveEndDate}, ${subscription.createdAt}, ${subscription.updatedAt})
    """.update

  def selectSubscriptionsByUser(userId: UserId): Query0[Subscription] = sql"""
      SELECT id, order_id, user_id, product_id, start_date, end_date, status, cancelled_at, effective_end_date, created_at, updated_at
      FROM subscriptions
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """.query[Subscription]

  def selectActiveSubscriptionsByUser(userId: UserId): Query0[Subscription] = sql"""
      SELECT id, order_id, user_id, product_id, start_date, end_date, status, cancelled_at, effective_end_date, created_at, updated_at
      FROM subscriptions
      WHERE user_id = $userId 
        AND status = 'active'
        AND start_date <= NOW()
        AND end_date > NOW()
      ORDER BY created_at DESC
    """.query[Subscription]

  def selectSubscriptionsByOrder(orderId: OrderId): Query0[Subscription] = sql"""
      SELECT id, order_id, user_id, product_id, start_date, end_date, status, cancelled_at, effective_end_date, created_at, updated_at
      FROM subscriptions
      WHERE order_id = $orderId
    """.query[Subscription]

  def updateSubscription(subscription: Subscription): Update0 = sql"""
      UPDATE subscriptions
      SET status = ${subscription.status}, cancelled_at = ${subscription.cancelledAt}, effective_end_date = ${subscription.effectiveEndDate}, updated_at = ${subscription.updatedAt}
      WHERE id = ${subscription.id}
    """.update

  def insertOrderCancellation(cancellation: OrderCancellation): Update0 = sql"""
      INSERT INTO order_cancellations (id, order_id, reason, cancellation_type, notes, cancelled_at, cancelled_by, effective_date, created_at, updated_at)
      VALUES (${cancellation.id}, ${cancellation.orderId}, ${cancellation.reason}, ${cancellation.cancellationType}, 
              ${cancellation.notes}, ${cancellation.cancelledAt}, ${cancellation.cancelledBy}, ${cancellation.effectiveDate},
              ${cancellation.createdAt}, ${cancellation.updatedAt})
    """.update

  def selectOrderCancellation(orderId: OrderId): Query0[OrderCancellation] = sql"""
      SELECT id, order_id, reason, cancellation_type, notes, cancelled_at, cancelled_by, effective_date, created_at, updated_at
      FROM order_cancellations
      WHERE order_id = $orderId
    """.query[OrderCancellation]
