package acme.orders.db

import acme.orders.models._

trait Store[F[_], G[_]]:
  def createOrder(order: Order): G[OrderId]
  def findOrder(orderId: OrderId): G[Option[Order]]
  def findOrdersByUser(userId: UserId): G[List[Order]]
  def updateOrder(order: Order): G[Unit]
  def createSubscription(subscription: Subscription): G[SubscriptionId]
  def findSubscriptionsByUser(userId: UserId): G[List[Subscription]]
  def findActiveSubscriptionsByUser(userId: UserId): G[List[Subscription]]
  def findSubscriptionsByOrder(orderId: OrderId): G[List[Subscription]]
  def updateSubscription(subscription: Subscription): G[Unit]
  def createOrderCancellation(cancellation: OrderCancellation): G[OrderCancellationId]
  def findOrderCancellation(orderId: OrderId): G[Option[OrderCancellation]]
  def commit[A](ga: G[A]): F[A]
