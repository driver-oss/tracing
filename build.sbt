scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-core" % "0.14.0",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "io.spray" %% "spray-json" % "1.3.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % "test",
  "org.scalatest" %% "scalatest" % "3.0.2" % "test",
)

fork in test := true
