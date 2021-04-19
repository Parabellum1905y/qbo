name := "akka-http-rest"
organization := "me.luger"
version := "1.0.0"
scalaVersion := "2.13.5"

resolvers += "jitpack" at "https://jitpack.io"
resolvers += "Sonatype OSS releases" at "https://oss.sonatype.org/service/local/repositories/releases/content/"

libraryDependencies ++= {
  val akkaV = "2.6.14"
  val akkaHttpV = "10.2.4"
  val scalaTestV = "3.2.5"
  val circeV = "0.12.3"
  val sttpV = "3.2.0"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    // HTTP server
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,

    // Support of CORS requests, version depends on akka-http
    "ch.megard" %% "akka-http-cors" % "1.1.1",

    // Encoding decoding sugar, used in passwords hashing
    "com.github.fdietze.hasher" %% "hasher" % "75be8ed",

    // Parsing and generating of JWT tokens
    "com.pauldijou" %% "jwt-core" % "5.0.0",

    // Config file parser
    "com.github.pureconfig" %% "pureconfig" % "0.14.1",

    // JSON serialization library
    "io.circe" %% "circe-core" % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parser" % circeV,

  // Sugar for serialization and deserialization in akka-http with circe
    "de.heikoseeberger" %% "akka-http-circe" % "1.36.0",

    // Validation library
    "com.wix" %% "accord-core" % "0.7.6",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
    "net.debasishg" %% "redisclient" % "3.30",

    // Http client
    "com.softwaremill.sttp.client3" %% "core" % sttpV,
    "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpV,
    "com.softwaremill.sttp.client3" %% "circe" % sttpV,

    "com.typesafe.akka" %% "akka-testkit" % akkaV % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "org.mockito" %% "mockito-scala-scalatest" % "1.16.32" % Test,
    "com.github.sebruck" %% "scalatest-embedded-redis" % "0.4.0" % Test
  )
}

enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)

// Needed for Heroku deployment, can be removed
enablePlugins(JavaAppPackaging)
