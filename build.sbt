name := "tracing"
version in ThisBuild := {
  import sys.process._
  ("git describe --always --dirty=-SNAPSHOT --match v[0-9].*" !!).tail.trim
}

crossScalaVersions := Seq("2.11.12", "2.12.6")
scalaVersion := crossScalaVersions.value.last

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-core" % "0.16.0",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
  "com.typesafe.akka" %% "akka-actor" % "2.5.13",
  "com.typesafe.akka" %% "akka-stream" % "2.5.13",
  "io.spray" %% "spray-json" % "1.3.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
)

fork in test := true
