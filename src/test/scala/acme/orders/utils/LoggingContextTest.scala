package acme.orders.utils

import java.util.UUID

import acme.orders.models._
import munit.FunSuite

class LoggingContextTest extends FunSuite:

  test("empty LoggingContext should create empty map") {
    val context = LoggingContext.empty
    assertEquals(context.toMap, Map.empty[String, String])
  }

  test("withOperation should add operation to context") {
    val context = LoggingContext.withOperation("testOperation")
    assertEquals(context.toMap, Map("operation" -> "testOperation"))
  }

  test("withUserId should add userId to context") {
    val userId = UserId("user123")
    val context = LoggingContext.empty.withUserId(userId)
    assertEquals(context.toMap, Map("userId" -> "user123"))
  }

  test("withOrderId should add orderId to context") {
    val orderId = OrderId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
    val context = LoggingContext.empty.withOrderId(orderId)
    assertEquals(context.toMap, Map("orderId" -> "123e4567-e89b-12d3-a456-426614174000"))
  }

  test("withSubscriptionId should add subscriptionId to context") {
    val subscriptionId = SubscriptionId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
    val context = LoggingContext.empty.withSubscriptionId(subscriptionId)
    assertEquals(context.toMap, Map("subscriptionId" -> "123e4567-e89b-12d3-a456-426614174001"))
  }

  test("withProductId should add productId to context") {
    val productId = ProductId("monthly")
    val context = LoggingContext.empty.withProductId(productId)
    assertEquals(context.toMap, Map("productId" -> "monthly"))
  }

  test("withOrderCancellationId should add cancellationId to context") {
    val cancellationId = OrderCancellationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174002"))
    val context = LoggingContext.empty.withOrderCancellationId(cancellationId)
    assertEquals(context.toMap, Map("cancellationId" -> "123e4567-e89b-12d3-a456-426614174002"))
  }

  test("withCorrelationId should add correlationId to context") {
    val correlationId = CorrelationId("corr-123")
    val context = LoggingContext.empty.withCorrelationId(correlationId)
    assertEquals(context.toMap, Map("correlationId" -> "corr-123"))
  }

  test("withCustom should add custom key-value to context") {
    val context = LoggingContext.empty.withCustom("customKey", "customValue")
    assertEquals(context.toMap, Map("customKey" -> "customValue"))
  }

  test("builder pattern should chain operations correctly") {
    val userId = UserId("user123")
    val orderId = OrderId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
    val productId = ProductId("monthly")
    val correlationId = CorrelationId("corr-123")

    val context = LoggingContext
      .withOperation("createOrder")
      .withUserId(userId)
      .withOrderId(orderId)
      .withProductId(productId)
      .withCorrelationId(correlationId)
      .withCustom("reason", "UserRequest")

    val expected = Map(
      "operation" -> "createOrder",
      "userId" -> "user123",
      "orderId" -> "123e4567-e89b-12d3-a456-426614174000",
      "productId" -> "monthly",
      "correlationId" -> "corr-123",
      "reason" -> "UserRequest"
    )

    assertEquals(context.toMap, expected)
  }

  test("subsequent operations should override previous values for same key") {
    val context = LoggingContext.withOperation("firstOperation").withOperation("secondOperation").withCustom("key1", "value1").withCustom("key1", "value2")

    val expected = Map(
      "operation" -> "secondOperation",
      "key1" -> "value2"
    )

    assertEquals(context.toMap, expected)
  }

  test("LoggingContext should be immutable") {
    val originalContext = LoggingContext.withOperation("original")
    val modifiedContext = originalContext.withOperation("modified")

    assertEquals(originalContext.toMap, Map("operation" -> "original"))
    assertEquals(modifiedContext.toMap, Map("operation" -> "modified"))
  }
