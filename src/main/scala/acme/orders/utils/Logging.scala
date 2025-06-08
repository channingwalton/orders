package acme.orders.utils

import java.util.UUID

import cats.effect._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

case class CorrelationId(value: String) extends AnyVal

object CorrelationId:
  def generate: CorrelationId = CorrelationId(UUID.randomUUID().toString)

object Logging:
  def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def logWithContext[F[_]: Sync](
    message: String,
    correlationId: Option[CorrelationId] = None,
    userId: Option[String] = None,
    orderId: Option[String] = None,
    additionalContext: Map[String, String] = Map.empty
  ): String =
    val context = Map(
      "correlationId" -> correlationId.map(_.value),
      "userId" -> userId,
      "orderId" -> orderId
    ).collect { case (k, Some(v)) => s"$k=$v" } ++
      additionalContext.map { case (k, v) => s"$k=$v" }

    if context.nonEmpty then s"$message [${context.mkString(", ")}]"
    else message

  def formatDuration(startTime: Long): String =
    val duration = System.currentTimeMillis() - startTime
    s"${duration}ms"
