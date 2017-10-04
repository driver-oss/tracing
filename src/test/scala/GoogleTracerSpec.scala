package xyz.driver.tracing

import java.nio.file._

import akka.actor._
import akka.stream._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._
import xyz.driver.tracing.TracingDirectives._

import scala.concurrent._
import scala.concurrent.duration._

class GoogleTracerSpec extends FlatSpec {

  implicit val system = ActorSystem(this.getClass.getSimpleName)
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  "Google Tracer" should "submit" in {
    val tracer = new GoogleTracer(
      "driverinc-sandbox",
      Paths.get(
        system.settings.config.getString("tracing.google.service-account-file"))
    )

    // create 100 traces, each with 10 nested spans
    for (_ <- 0 until 100) {
      (0 until 10).foldLeft(Span(name = "trace-test.driver.xyz")) {
        case (previous, i) =>
          val p: Span = previous
          val span = Span(
            name = s"trace-test.driver.xyz-$i",
            traceId = previous.traceId,
            parentSpanId = Some(previous.spanId)
          )
          Thread.sleep(2)
          val done = span.end()
          tracer.submit(done)
          done
      }
    }

    // TODO: automatically verify that traces were created on google stack driver

    Await.ready(tracer.close(), 30.seconds)
  }

}
