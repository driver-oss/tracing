organization := "xyz.driver"
licenses in ThisBuild := Seq(
  ("Apache 2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
homepage in ThisBuild := Some(url("https://github.com/drivergroup/tracing"))
publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scmInfo := Some(
  ScmInfo(
    url("https://github.com/drivergroup/tracing"),
    "scm:git@github.com:drivergroup/tracing.git"
  )
)

developers := List(
  Developer(
    id = "jakob@driver.xyz",
    name = "Jakob Odersky",
    email = "jakob@driver.xyz",
    url = url("https://driver.xyz")
  ),
  Developer(
    id = "john@driver.xyz",
    name = "John St john",
    email = "john@driver.xyz",
    url = url("https://driver.xyz")
  )
)

useGpg := true
pgpSigningKey := Some(0x1E373AB444C7D4C7l)
