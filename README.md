[![Build Status](https://travis-ci.org/drivergroup/tracing.svg?branch=master)](https://travis-ci.org/drivergroup/tracing)
[![Download](https://img.shields.io/maven-central/v/xyz.driver/tracing_2.12.svg)](http://search.maven.org/#search|ga|1|xyz.driver%20tracing-)


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
libraryDependencies += "xyz.driver" %% "tracing" % "<latest version>"
```

### Example

The library provides tracing directives which may be used to track and
report traces accross multiple, nested service calls.

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
Various tracing aggregation backends are provided out of the box.

### Logging
The `LoggingTracer` simply logs any traces locally.

### Google Stackdriver Tracin
The `GoogleTracer` interacts with the Google Stack Driver API, as described here:
https://cloud.google.com/trace/docs/reference/v1/rest/v1/projects.traces#Trace

## Copying
Copyright 2017 Driver Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
