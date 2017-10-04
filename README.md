# Driver Tracing Library

A vendor-neutral tracing library for Akka-HTTP.

This library provides tracing directives for Akka-HTTP. It specifies
common client abstractions and implementations that may be used with
different tracing solutions, such as Google Stackdriver Tracing.

## Getting Started

Driver tracing is published to maven central as a standard Scala
library. Include it in sbt by adding the following snippet to your
build.sbt:

```scala
libraryDependencies += "xyz.driver" %% "tracing" % "0.0.1"
```

### Example
```scala
import xyz.driver.tracing._
import xyz.driver.tracing.TracingDirectives._

val tracer = new LoggingTracer(println)

val route =
  // report how long any request to this service takes
  trace(tracer) {
    path("my-service-endpoint") {
      get {
        // include a sub-trace with a custom name
        trace(tracer, Some("complex-call")) {
          // do something that takes time
          complete("done")
        }
      }
    } ~
    path("proxy-service-endpoint") {
      // extract tracing headers so they may be injected into nested requests
      extractTraceHeaders{ headers =>
        // call external service with the existing parent tracing headers
        complete("done")
      }
    }
  }
```

## Tracing Backends

### Logging
TODO

### Google Stackdriver Tracing
https://cloud.google.com/trace/docs/reference/v1/rest/v1/projects.traces#Trace
TODO


