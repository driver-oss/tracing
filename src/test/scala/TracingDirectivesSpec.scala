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

  def route(tracer: Tracer): Route = trace(tracer, "example.org") {
    pathPrefix("1") {
      trace(tracer, "test-sub-trace-1") {
        Thread.sleep(2)
        trace(tracer, "test-subsub-trace-1") {
          Thread.sleep(2)
          trace(tracer, "test-subsubsub-trace-1") {
            Thread.sleep(10)
            complete("ok")
          }
        }
      }
    } ~
      pathPrefix("2") {
        trace(tracer, "test-sub-trace-2") {
          Thread.sleep(20)
          complete("ok")
        }
      }
  }

  "Google Tracer" should "submit" in {
    val tracer = new GoogleTracer(
      "driverinc-sandbox",
      Paths.get(
        system.settings.config.getString("tracing.google.service-account-file"))
    )

    val futures: Seq[Assertion] = for (i <- 0 until 100) yield {
      Get(s"https://example.org/${i % 2 + 1}") ~> route(tracer) ~> check {
        assert(responseAs[String] == "ok")
      }
    }

    Await.ready(tracer.close(), 30.seconds)
  }

}
