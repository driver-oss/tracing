package xyz.driver.tracing

import akka.actor._
import akka.stream._
import java.nio.file._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import TracingDirectives._
import scala.concurrent._
import scala.concurrent.duration._

import org.scalatest._

class TracingDirectivesSpec extends FlatSpec with BeforeAndAfterAll with ScalatestRouteTest {

  implicit val tracer = new GoogleTracer(
    "driverinc-sandbox",
    Paths.get(
      system.settings.config.getString("tracing.google.service-account-file"))
  )

  val route: Route = trace(tracer, "example.org") {
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

  "Tracer" should "submit" in {
    for (i <- 0 until 100) {
      Get(s"https://example.org/${i % 2 + 1}") ~> route ~> check {
        assert(responseAs[String] == "ok")
      }
    }
  }

  override def afterAll() = {
    tracer.queue.complete()
    Await.ready(tracer.queue.watchCompletion(), Duration.Inf)
    super.afterAll()
  }

}
