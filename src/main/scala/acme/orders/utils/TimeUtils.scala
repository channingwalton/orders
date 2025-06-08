package acme.orders.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

object TimeUtils:
  def truncateToSeconds(instant: Instant): Instant = instant.truncatedTo(ChronoUnit.SECONDS)

  def now(): Instant = truncateToSeconds(Instant.now())
