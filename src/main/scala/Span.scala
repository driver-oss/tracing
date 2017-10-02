package xyz.driver.tracing

import java.util.UUID
import java.time._

case class Span(
    name: String,
    traceId: UUID = UUID.randomUUID(),
    spanId: UUID = UUID.randomUUID(),
    parentSpanId: Option[UUID] = None,
    labels: Map[String, String] = Map.empty,
    startTime: Instant = Instant.now,
    endTime: Instant = Instant.now
) {

  def start(clock: Clock = Clock.systemUTC): Span =
    this.copy(startTime = clock.instant())
  def end(clock: Clock = Clock.systemUTC): Span =
    this.copy(endTime = clock.instant())

  def withLabels(extraLabels: (String, String)*) =
    this.copy(labels = this.labels ++ extraLabels)

}
