package xyz.driver.tracing

import scala.concurrent.Future

/** Interface to tracing aggregation backends. */
trait Tracer {

  /** Submit a span to be sent to the aggregation backend.
    *
    * Note that submission typically happens asynchronously. Exact semantics are
    * implementation-specific, however to guarantee of successful submission is
    * made when this method returns. */
  def submit(span: Span): Unit

  /** Aggregate any potentially queued submissions and perform any cleanup logic. */
  def close(): Future[_] = Future.unit

}
