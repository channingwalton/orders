ThisBuild / organization := "acme"
ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "orders",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % "0.23.24",
      "org.http4s" %% "http4s-ember-client" % "0.23.24",
      "org.http4s" %% "http4s-circe" % "0.23.24",
      "org.http4s" %% "http4s-dsl" % "0.23.24",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC9",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC9",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC9",
      "org.flywaydb" % "flyway-core" % "9.21.2",
      "org.postgresql" % "postgresql" % "42.6.0",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "is.cir" %% "ciris" % "3.4.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
      "org.tpolecat" %% "doobie-munit" % "1.0.0-RC9" % Test,
      "com.dimafeng" %% "testcontainers-scala-munit" % "0.41.0" % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.41.0" % Test
    ),
    Test / testFrameworks += new TestFramework("munit.Framework"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Ykind-projector:underscores",
      "-Xfatal-warnings"
    ),
    Compile / mainClass := Some("acme.orders.Main"),
    addCommandAlias("ci", "clean; compile; test")
  )