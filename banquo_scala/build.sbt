scalaVersion := "2.13.12"

val LogbackVersion = "1.4.1"    // Note: Logback 1.4.1 requires JDK 11+.
val postgresJdbcVersion = "42.2.18"

Compile / run / fork := true
Test / parallelExecution := false
Test / logBuffered := false

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"            % "2.0.19",
  "dev.zio"       %% "zio-json"       % "0.6.2",
  "dev.zio"       %% "zio-http"       % "3.0.0-RC2",
  "io.getquill"   %% "quill-zio"      % "4.7.0",
  "io.getquill"   %% "quill-jdbc-zio" % "4.7.0",
  "com.h2database" % "h2"             % "2.2.224",
  "org.postgresql" % "postgresql" % postgresJdbcVersion,
  "ch.qos.logback"  %  "logback-classic"     % LogbackVersion
)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

dockerExposedPorts := Seq(8080)

dockerUsername   := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
