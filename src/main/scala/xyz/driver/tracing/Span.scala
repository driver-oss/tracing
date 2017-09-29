package xyz.driver.tracing

import java.util.UUID
import java.time._

case class Span(
    traceId: UUID,
    spanId: UUID,
    name: String,
    parentSpanId: Option[UUID] = None,
    labels: Map[String, String] = Map.empty,
    startTime: Instant = Instant.now,
    endTime: Instant = Instant.now
) {

  def started(clock: Clock = Clock.systemUTC): Span =
    this.copy(startTime = clock.instant())
  def ended(clock: Clock = Clock.systemUTC): Span =
    this.copy(endTime = clock.instant())

  def withLabels(extraLabels: (String, String)*) =
    this.copy(labels = this.labels ++ extraLabels)

}
