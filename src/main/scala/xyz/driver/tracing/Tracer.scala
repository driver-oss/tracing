package xyz.driver.tracing

trait Tracer {
  def submit(span: Span): Unit
}
