package acme.orders.db

import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import acme.orders.models.*
import java.time.Instant
import java.util.UUID

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
      INSERT INTO subscriptions (id, order_id, user_id, product_id, start_date, end_date, status, created_at, updated_at)
      VALUES (${subscription.id}, ${subscription.orderId}, ${subscription.userId}, ${subscription.productId}, 
              ${subscription.startDate}, ${subscription.endDate}, ${subscription.status}, 
              ${subscription.createdAt}, ${subscription.updatedAt})
    """.update

  def selectSubscriptionsByUser(userId: UserId): Query0[Subscription] = sql"""
      SELECT id, order_id, user_id, product_id, start_date, end_date, status, created_at, updated_at
      FROM subscriptions
      WHERE user_id = $userId
      ORDER BY created_at DESC
    """.query[Subscription]
