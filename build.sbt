name := "WoWRobot"

version := "0.1"

val theScalaVersion = "3.0.1"

val commonSettings = List(
  scalaVersion := theScalaVersion,
  libraryDependencies += "org.scalameta" %%% "munit" % "0.7.26" % Test,
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.3" % Test
)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .in(file("./shared"))
  .settings(
    commonSettings,
    SharedDependencies.addDependencies()
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" % "scala-java-time_sjs1_2.13" % "2.3.0",
    libraryDependencies += "io.github.cquiroz" % "scala-java-time-tzdb_sjs1_2.13" % "2.3.0",
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )

lazy val frontend = project
  .in(file("./frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := theScalaVersion,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= List(
      "com.raquo" %%% "laminar" % "0.13.0"
    )
  )
  .dependsOn(shared.js)

val AkkaVersion     = "2.6.16"
val AkkaHttpVersion = "10.2.6"

lazy val server = project
  .in(file("./server"))
  .settings(
    commonSettings,
    libraryDependencies ++= List(
      "com.typesafe.akka" % "akka-actor-typed_2.13" % AkkaVersion,
      "com.typesafe.akka" % "akka-stream-typed_2.13" % AkkaVersion,
      "com.typesafe.akka" % "akka-stream_2.13" % AkkaVersion,
      "com.typesafe.akka" % "akka-http_2.13" % AkkaHttpVersion,
      "com.typesafe" % "config" % "1.4.1",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
  .dependsOn(shared.jvm)
