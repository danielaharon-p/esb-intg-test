ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.10"

val AkkaVersion = "2.7.0"
val TestcontainersVersion = "0.39.12"
val ScalaTestVersion = "3.2.10"

val ScalaTest = "org.scalatest" %% "scalatest" % ScalaTestVersion
val AkkaActorTestkitTyped =
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion
val AkkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
val AkkaPersistenceJdbc =
  "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.4"
val AkkaSerializationJackson =
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion
val AkkaPersistenceTestkit =
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion
val TestcontainersScalatest =
  "com.dimafeng" %% "testcontainers-scala-scalatest" % TestcontainersVersion
val TestcontainersPostgresql =
  "com.dimafeng" %% "testcontainers-scala-postgresql" % TestcontainersVersion
val Postgresql = "org.postgresql" % "postgresql" % "42.4.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "esb-intg-test",
    idePackagePrefix := Some("com.example"),
    libraryDependencies ++= Seq(
      AkkaActorTyped,
      AkkaPersistenceJdbc,
      AkkaSerializationJackson,
      Postgresql,
      ScalaTest % Test,
      AkkaActorTestkitTyped % Test,
      AkkaPersistenceTestkit % Test,
      TestcontainersScalatest % Test,
      TestcontainersPostgresql % Test,
    )
  )
