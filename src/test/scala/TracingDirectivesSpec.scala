package xyz.driver.tracing

import java.nio.file._

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import xyz.driver.tracing.TracingDirectives._

import scala.concurrent._
import scala.concurrent.duration._

class TracingDirectivesSpec
    extends FlatSpec
    with BeforeAndAfterAll
    with ScalatestRouteTest {

  class DummyTracer extends Tracer {
    private var _spans: List[Span] = Nil
    def spans = synchronized { _spans }

    override def submit(span: Span): Unit = synchronized {
      _spans = span :: _spans
    }
  }

  def route(tracer: Tracer): Route = trace(tracer, Some("example.org")) {
    trace(tracer, Some("trace-1-1")) {
      Thread.sleep(2)
      trace(tracer, Some("trace-1-2")) {
        Thread.sleep(2)
        trace(tracer, Some("trace-1-3")) {
          Thread.sleep(10)
          complete("ok")
        }
      }
    }
  }

  "Tracing directives" should "nest spans correctly" in {
    val tracer = new DummyTracer

    for (i <- 0 until 100) yield {
      Get(s"https://example.org") ~> route(tracer) ~> check {
        assert(responseAs[String] == "ok")
      }
    }

    val traces = tracer.spans.groupBy(_.traceId)
    assert(traces.toSeq.length == 100)

    traces.foreach {
      case (traceId, spans) =>
        def getSpan(name: String) = spans.find(_.name == name).get
        assert(
          getSpan("trace-1-3").parentSpanId.get === getSpan("trace-1-2").spanId)
        assert(
          getSpan("trace-1-2").parentSpanId.get === getSpan("trace-1-1").spanId)
    }

    Await.ready(tracer.close(), 30.seconds)
  }

}
