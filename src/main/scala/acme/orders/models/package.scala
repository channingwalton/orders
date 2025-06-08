package acme.orders.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import java.time.Instant
import java.util.UUID

case class OrderId(value: UUID) extends AnyVal
case class SubscriptionId(value: UUID) extends AnyVal
case class UserId(value: String) extends AnyVal
case class ProductId(value: String) extends AnyVal

enum Product(val id: ProductId, val duration: ProductDuration):
  case Monthly extends Product(ProductId("monthly"), ProductDuration.Month)
  case Annual extends Product(ProductId("annual"), ProductDuration.Year)

enum ProductDuration:
  case Month
  case Year

enum OrderStatus:
  case Active
  case Cancelled

enum SubscriptionStatus:
  case Active
  case Expired
  case Cancelled

case class Order(
  id: OrderId,
  userId: UserId,
  productId: ProductId,
  status: OrderStatus,
  createdAt: Instant,
  updatedAt: Instant
)

case class Subscription(
  id: SubscriptionId,
  orderId: OrderId,
  userId: UserId,
  productId: ProductId,
  startDate: Instant,
  endDate: Instant,
  status: SubscriptionStatus,
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateOrderRequest(
  userId: String,
  productId: String
)

object CreateOrderRequest:
  given Decoder[CreateOrderRequest] = deriveDecoder
  given Encoder[CreateOrderRequest] = deriveEncoder

object Order:
  given Encoder[Order] = deriveEncoder
  
object Subscription:
  given Encoder[Subscription] = deriveEncoder

object OrderId:
  given Encoder[OrderId] = Encoder[UUID].contramap(_.value)
  given Decoder[OrderId] = Decoder[UUID].map(OrderId.apply)

object SubscriptionId:
  given Encoder[SubscriptionId] = Encoder[UUID].contramap(_.value)
  given Decoder[SubscriptionId] = Decoder[UUID].map(SubscriptionId.apply)

object UserId:
  given Encoder[UserId] = Encoder[String].contramap(_.value)
  given Decoder[UserId] = Decoder[String].map(UserId.apply)

object ProductId:
  given Encoder[ProductId] = Encoder[String].contramap(_.value)
  given Decoder[ProductId] = Decoder[String].map(ProductId.apply)

object OrderStatus:
  given Encoder[OrderStatus] = Encoder[String].contramap(_.toString.toLowerCase)
  given Decoder[OrderStatus] = Decoder[String].emap {
    case "active" => Right(OrderStatus.Active)
    case "cancelled" => Right(OrderStatus.Cancelled)
    case other => Left(s"Invalid order status: $other")
  }

object SubscriptionStatus:
  given Encoder[SubscriptionStatus] = Encoder[String].contramap(_.toString.toLowerCase)
  given Decoder[SubscriptionStatus] = Decoder[String].emap {
    case "active" => Right(SubscriptionStatus.Active)
    case "expired" => Right(SubscriptionStatus.Expired)
    case "cancelled" => Right(SubscriptionStatus.Cancelled)
    case other => Left(s"Invalid subscription status: $other")
  }