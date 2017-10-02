package xyz.driver.tracing

import google._
import java.nio.file.Path
import akka.stream._
import akka.stream.scaladsl._
import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import scala.util.control._
import scala.concurrent.duration._
import spray.json.DefaultJsonProtocol._
import java.util.UUID

class GoogleTracer(projectId: String,
                   serviceAccountFile: Path,
                   bufferSize: Int = 1000,
                   concurrentConnections: Int = 1)(implicit system: ActorSystem,
                                                   materializer: Materializer)
    extends Tracer {

  import system.dispatcher

  lazy val connectionPool = Http().superPool[Unit]()

  private val batchingPipeline: Flow[Span, Traces, _] =
    Flow[Span]
      .groupedWithin(bufferSize, 1.second)
      .map { spans =>
        val traces: Seq[Trace] = spans
          .groupBy(_.traceId)
          .map {
            case (traceId, spans) =>
              Trace(
                traceId,
                projectId,
                spans.map(span => TraceSpan.fromSpan(span))
              )
          }
          .toSeq
        Traces(traces)
      }

  lazy val queue: SourceQueueWithComplete[Span] = {
    Source
      .queue[Span](bufferSize, OverflowStrategy.dropNew)
      .log("debug")
      .viaMat(batchingPipeline)(Keep.left)
      .mapAsync(concurrentConnections) { (traces: Traces) =>
        println(traces)
        Marshal(traces).to[RequestEntity].map { entity =>
          HttpRequest(
            method = HttpMethods.PATCH,
            uri =
              s"https://cloudtrace.googleapis.com/v1/projects/${projectId}/traces",
            entity = entity
          )
        }
      }
      .viaMat(
        OAuth2.authenticatedFlow(
          Http(),
          serviceAccountFile,
          Seq(
            "https://www.googleapis.com/auth/trace.append"
          )))(Keep.left)
      .map(req => (req, ()))
      .viaMat(connectionPool)(Keep.left)
      .mapError {
        case NonFatal(e) =>
          system.log.error(s"Exception encountered while submitting trace", e)
          e.printStackTrace
          e
      }
      .to(Sink.ignore)
      .run()
  }

  override def submit(span: Span): Unit = queue.offer(span)

}
