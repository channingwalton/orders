package acme.orders.utils

import java.util.UUID

import cats.syntax.all._

case class CorrelationId(value: String) extends AnyVal

object CorrelationId:
  def generate: CorrelationId = CorrelationId(UUID.randomUUID().toString)
