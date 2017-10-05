package xyz.driver.tracing

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.collection.immutable.Seq

trait TracingDirectives {
  import TracingDirectives._

  def extractTraceHeaders: Directive1[Seq[HttpHeader]] =
    extractRequest.map { request =>
      request.headers.filter(h =>
        h.name == TraceHeaderName || h.name == SpanHeaderName)
    }

  def trace(tracer: Tracer,
            name: Option[String] = None,
            extraLabels: Map[String, String] = Map.empty): Directive0 =
    extractRequest.flatMap { request =>
      def getHeader(name: String): Option[String] =
        request.headers.find(_.name == name).map(_.value)

      // get existing trace or start a new one
      val traceId = getHeader(TraceHeaderName)
        .map(UUID.fromString)
        .getOrElse(UUID.randomUUID())

      val parentSpanId = getHeader(SpanHeaderName).map(UUID.fromString)

      val labels = Map(
        "/http/user_agent" -> "driver-tracer",
        "/http/method" -> request.method.name,
        "/http/url" -> request.uri.path.toString,
        "/http/host" -> request.uri.authority.host.toString
      ) ++ extraLabels

      val span = Span(
        name = name.getOrElse(request.uri.path.toString),
        traceId = traceId,
        parentSpanId = parentSpanId,
        labels = labels
      )

      val childHeaders = Seq(
        RawHeader(TraceHeaderName, span.traceId.toString),
        RawHeader(SpanHeaderName, span.spanId.toString)
      )

      mapRequest(childRequest =>
        childRequest
          .withHeaders(childRequest.headers
            .filterNot(h =>
              h.name() == TraceHeaderName ||
                h.name() == SpanHeaderName) ++ childHeaders)) & mapRouteResult {
        result =>
          tracer.submit(span.end())
          result
      }
    }

}

object TracingDirectives extends TracingDirectives {
  val TraceHeaderName = "Tracing-Trace-Id"
  val SpanHeaderName = "Tracing-Span-Id"
}
