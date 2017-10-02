package xyz.driver.tracing
package google

import java.nio.ByteBuffer
import java.time._
import java.time.format._
import java.util.UUID

import spray.json.DefaultJsonProtocol.{LongJsonFormat => _, _}
import spray.json._

case class TraceSpan(
    spanId: Long,
    kind: TraceSpan.SpanKind,
    name: String,
    startTime: Instant,
    endTime: Instant,
    parentSpanId: Option[Long],
    labels: Map[String, String]
)

object TraceSpan {

  sealed trait SpanKind
  // Unspecified
  case object Unspecified extends SpanKind
  // Indicates that the span covers server-side handling of an RPC or other remote network request.
  case object RpcServer extends SpanKind
  // Indicates that the span covers the client-side wrapper around an RPC or other remote request.
  case object RpcClient extends SpanKind

  object SpanKind {
    implicit val format: JsonFormat[SpanKind] = new JsonFormat[SpanKind] {
      override def write(x: SpanKind): JsValue = x match {
        case Unspecified => JsString("SPAN_KIND_UNSPECIFIED")
        case RpcServer   => JsString("RPC_SERVER")
        case RpcClient   => JsString("RPC_CLIENT")
      }
      override def read(x: JsValue): SpanKind = x match {
        case JsString("SPAN_KIND_UNSPECIFIED") => Unspecified
        case JsString("RPC_SERVER")            => RpcServer
        case JsString("RPC_CLIENT")            => RpcClient
        case other =>
          spray.json.deserializationError(s"`$other` is not a valid span kind")
      }
    }
  }

  implicit val instantFormat = new JsonFormat[Instant] {
    val formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
      .withZone(ZoneId.of("UTC"))
    override def write(x: Instant): JsValue = JsString(formatter.format(x))
    override def read(x: JsValue): Instant = x match {
      case JsString(x) => Instant.parse(x)
      case other =>
        spray.json.deserializationError(s"`$other` is not a valid instant")
    }
  }

  implicit val longFormat: JsonFormat[Long] = new JsonFormat[Long] {
    override def write(x: Long): JsValue = {
      JsString(java.lang.Long.toUnsignedString(x))
    }
    override def read(x: JsValue): Long = x match {
      case JsString(num) => num.toLong
      case other =>
        spray.json.deserializationError("expected long")
    }
  }

  implicit val format: JsonFormat[TraceSpan] = jsonFormat7(TraceSpan.apply)

  def fromSpan(span: Span) = TraceSpan(
    span.spanId.getLeastSignificantBits,
    Unspecified,
    span.name,
    span.startTime,
    span.endTime,
    span.parentSpanId.map(_.getLeastSignificantBits),
    span.labels
  )

}

case class Trace(
    traceId: UUID,
    projectId: String = "",
    spans: Seq[TraceSpan] = Seq.empty
)

object Trace {

  implicit val uuidFormat = new JsonFormat[UUID] {
    override def write(x: UUID) = {
      val buffer = ByteBuffer.allocate(16)
      buffer.putLong(x.getMostSignificantBits)
      buffer.putLong(x.getLeastSignificantBits)
      val array = buffer.array()
      val string = new StringBuilder
      for (i <- 0 until 16) {
        string ++= f"${array(i) & 0xff}%02x"
      }
      JsString(string.result)
    }
    override def read(x: JsValue): UUID = x match {
      case JsString(str) if str.length == 32 =>
        val (msb, lsb) = str.splitAt(16)
        new UUID(java.lang.Long.decode(msb), java.lang.Long.decode(lsb))
      case JsString(str) =>
        spray.json.deserializationError(
          "128-bit id string must be exactly 32 characters long")
      case other =>
        spray.json.deserializationError("expected 32 character hex string")
    }
  }

  implicit val format: JsonFormat[Trace] = jsonFormat3(Trace.apply)

}

case class Traces(traces: Seq[Trace])
object Traces {
  implicit val format: RootJsonFormat[Traces] = jsonFormat1(Traces.apply)
}
