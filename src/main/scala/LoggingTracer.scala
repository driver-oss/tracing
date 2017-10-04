package xyz.driver.tracing

import java.util.UUID
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import spray.json._

class LoggingTracer(logger: String => Unit) extends Tracer {
  import LoggingTracer.JsonProtocol._

  override def submit(span: Span): Unit = logger(span.toJson.compactPrint)
}

object LoggingTracer {

  object JsonProtocol extends DefaultJsonProtocol {
    implicit def uuidFormat: JsonFormat[UUID] = jsonFormat(
      { (js: JsValue) =>
        js match {
          case JsString(str) => UUID.fromString(str)
          case other         => deserializationError("expected string as UUID")
        }
      }, { (uuid: UUID) =>
        JsString(uuid.toString)
      }
    )

    private val timeFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
      .withZone(ZoneId.of("UTC"))

    implicit def instantFormat: JsonFormat[Instant] = jsonFormat(
      { (js: JsValue) =>
        js match {
          case JsString(str) => Instant.parse(str)
          case other         => deserializationError(s"`$other` is not a valid instant")
        }
      }, { (instant: Instant) =>
        JsString(timeFormatter.format(instant))
      }
    )
    implicit def spanFormat: RootJsonFormat[Span] = jsonFormat7(Span.apply)
  }

}
