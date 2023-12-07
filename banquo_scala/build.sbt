scalaVersion := "2.13.12"

val postgresJdbcVersion = "42.2.18"
val logbackVersion = "1.4.1"    // Note: Logback 1.4.1 requires JDK 11+.

Compile / mainClass := Some("com.appstract.banquo.main.RunBanquoHttpApp")
Compile / run / fork := true
Test / parallelExecution := false
Test / logBuffered := false

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"              % "2.0.19",
  "dev.zio"       %% "zio-json"         % "0.6.2",
  "dev.zio"       %% "zio-http"         % "3.0.0-RC2",
  "org.postgresql" % "postgresql"       % postgresJdbcVersion,
  "ch.qos.logback" % "logback-classic"  % logbackVersion
)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
