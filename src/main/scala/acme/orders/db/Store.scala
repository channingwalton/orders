package acme.orders.db

import cats.data.{NonEmptyList, OptionT}
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all.*
import cats.{Applicative, FlatMap, ~>}
import acme.orders.models.*

trait Store[F[_], G[_]]:
  def createOrder(order: Order): G[OrderId]
  def findOrder(orderId: OrderId): G[Option[Order]]
  def findOrdersByUser(userId: UserId): G[List[Order]]
  def updateOrder(order: Order): G[Unit]
  def createSubscription(subscription: Subscription): G[SubscriptionId]
  def findSubscriptionsByUser(userId: UserId): G[List[Subscription]]
  def commit[A](ga: G[A]): F[A]
