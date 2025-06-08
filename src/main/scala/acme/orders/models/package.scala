package acme.orders.models

import java.time.Instant
import java.util.UUID

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class OrderId(value: UUID) extends AnyVal
case class SubscriptionId(value: UUID) extends AnyVal
case class UserId(value: String) extends AnyVal
case class ProductId(value: String) extends AnyVal
case class OrderCancellationId(value: UUID) extends AnyVal

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

enum CancellationReason:
  case UserRequest
  case PaymentFailure
  case Violation
  case Other

enum CancellationType:
  case Immediate
  case EndOfPeriod

enum CancelledBy:
  case User
  case System
  case Admin

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
  cancelledAt: Option[Instant],
  effectiveEndDate: Option[Instant],
  createdAt: Instant,
  updatedAt: Instant
)

case class CreateOrderRequest(
  userId: String,
  productId: String
)

case class OrderCancellation(
  id: OrderCancellationId,
  orderId: OrderId,
  reason: CancellationReason,
  cancellationType: CancellationType,
  notes: Option[String],
  cancelledAt: Instant,
  cancelledBy: CancelledBy,
  effectiveDate: Instant,
  createdAt: Instant,
  updatedAt: Instant
)

case class CancelOrderRequest(
  reason: Option[CancellationReason],
  cancellationType: Option[CancellationType],
  notes: Option[String]
)

case class UserSubscriptionStatus(
  userId: String,
  isSubscribed: Boolean,
  activeSubscriptions: List[Subscription],
  subscriptionCount: Int
)

object CreateOrderRequest:
  given Decoder[CreateOrderRequest] = deriveDecoder
  given Encoder[CreateOrderRequest] = deriveEncoder

object CancelOrderRequest:
  given Decoder[CancelOrderRequest] = deriveDecoder
  given Encoder[CancelOrderRequest] = deriveEncoder

object OrderCancellation:
  given Encoder[OrderCancellation] = deriveEncoder

object UserSubscriptionStatus:
  given Encoder[UserSubscriptionStatus] = deriveEncoder

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
    case "active"    => Right(OrderStatus.Active)
    case "cancelled" => Right(OrderStatus.Cancelled)
    case other       => Left(s"Invalid order status: $other")
  }

object SubscriptionStatus:
  given Encoder[SubscriptionStatus] = Encoder[String].contramap(_.toString.toLowerCase)
  given Decoder[SubscriptionStatus] = Decoder[String].emap {
    case "active"    => Right(SubscriptionStatus.Active)
    case "expired"   => Right(SubscriptionStatus.Expired)
    case "cancelled" => Right(SubscriptionStatus.Cancelled)
    case other       => Left(s"Invalid subscription status: $other")
  }

object OrderCancellationId:
  given Encoder[OrderCancellationId] = Encoder[UUID].contramap(_.value)
  given Decoder[OrderCancellationId] = Decoder[UUID].map(OrderCancellationId.apply)

object CancellationReason:
  given Encoder[CancellationReason] = Encoder[String].contramap(_.toString.toLowerCase.replace("request", "_request").replace("failure", "_failure"))
  given Decoder[CancellationReason] = Decoder[String].emap {
    case "user_request"    => Right(CancellationReason.UserRequest)
    case "payment_failure" => Right(CancellationReason.PaymentFailure)
    case "violation"       => Right(CancellationReason.Violation)
    case "other"           => Right(CancellationReason.Other)
    case other             => Left(s"Invalid cancellation reason: $other")
  }

object CancellationType:
  given Encoder[CancellationType] = Encoder[String].contramap(_.toString.toLowerCase.replace("endofperiod", "end_of_period"))
  given Decoder[CancellationType] = Decoder[String].emap {
    case "immediate"     => Right(CancellationType.Immediate)
    case "end_of_period" => Right(CancellationType.EndOfPeriod)
    case other           => Left(s"Invalid cancellation type: $other")
  }

object CancelledBy:
  given Encoder[CancelledBy] = Encoder[String].contramap(_.toString.toLowerCase)
  given Decoder[CancelledBy] = Decoder[String].emap {
    case "user"   => Right(CancelledBy.User)
    case "system" => Right(CancelledBy.System)
    case "admin"  => Right(CancelledBy.Admin)
    case other    => Left(s"Invalid cancelled by: $other")
  }
