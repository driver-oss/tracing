scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "com.pauldijou" %% "jwt-core" % "0.14.0",
  "io.spray" %% "spray-json" % "1.3.3",
  "org.scalatest" %% "scalatest" % "3.0.2" % "test",
)

fork in test := true
