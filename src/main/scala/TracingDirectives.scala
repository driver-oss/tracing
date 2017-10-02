package xyz.driver.tracing

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.collection.immutable.Seq

trait TracingDirectives {
  import TracingDirectives._

  def optionalTraceContext: Directive1[Option[TraceContext]] =
    extractRequest.map { req =>
      TraceContext.fromHeaders(req.headers)
    }

  def withTraceContext(ctx: TraceContext): Directive0 =
    mapRequest(req => req.withHeaders(ctx.headers))

  def trace(tracer: Tracer,
            name: String,
            extraLabels: Map[String, String] = Map.empty): Directive0 =
    extractRequest.flatMap { request =>
      val labels = Map(
        "/http/user_agent" -> "driver-tracer",
        "/http/method" -> request.method.name,
        "/http/url" -> request.uri.path.toString,
        "/http/host" -> request.uri.authority.host.toString
      ) ++ extraLabels

      val span: Span = TraceContext.fromHeaders(request.headers) match {
        case None => // no parent span, create new trace
          Span(
            name = name,
            labels = labels
          )
        case Some(TraceContext(traceId, parentSpanId)) =>
          Span(
            name = name,
            traceId = traceId,
            parentSpanId = parentSpanId,
            labels = labels
          )
      }

      withTraceContext(TraceContext.fromSpan(span)) & mapRouteResult { res =>
        tracer.submit(span.end())
        res
      }

    }

  /*
  def span2(name2: String, tracer: Tracer): Directive0 = {
    val f: RouteResult ⇒ RouteResult = ???
    Directive { inner ⇒ ctx ⇒
      inner(())(ctx).map(f)(ctx.executionContext)
    }
  }
 */

}

object TracingDirectives extends TracingDirectives {

  case class TraceContext(traceId: UUID, parentSpanId: Option[UUID]) {
    import TraceContext._

    def headers: Seq[HttpHeader] =
      Seq(RawHeader(TraceHeaderName, traceId.toString)) ++
        parentSpanId.toSeq.map(id => RawHeader(SpanHeaderName, id.toString))
  }
  object TraceContext {
    val TraceHeaderName = "Tracing-Trace-Id"
    val SpanHeaderName = "Tracing-Span-Id"

    def fromHeaders(headers: Seq[HttpHeader]): Option[TraceContext] = {
      val traceId = headers
        .find(_.name == TraceHeaderName)
        .map(_.value)
        .map(UUID.fromString)
      val parentSpanId =
        headers.find(_.name == SpanHeaderName).map(_.value).map(UUID.fromString)
      traceId.map { tid =>
        TraceContext(tid, parentSpanId)
      }
    }
    def fromSpan(span: Span) = TraceContext(span.traceId, Some(span.spanId))
  }

}
