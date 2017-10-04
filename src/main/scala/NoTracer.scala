package xyz.driver.tracing

object NoTracer extends Tracer {
  override def submit(span: Span): Unit = ()
}
