package com.example

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.SeqHasAsJava
import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.jdbc.testkit.scaladsl.SchemaUtils
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.testkit.scaladsl.PersistenceInit
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.ForAllTestContainer
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpecLike

trait TestSerializable

object PersistentBehavior {
  import akka.actor.typed.Behavior
  import akka.persistence.typed.scaladsl.EventSourcedBehavior
  import akka.persistence.typed.PersistenceId
  import akka.persistence.typed.scaladsl.Effect

  val persistenceId: PersistenceId = PersistenceId.ofUniqueId("test")

  sealed trait Command extends TestSerializable
  final case object Increment extends Command

  sealed trait Event extends TestSerializable
  final case object Incremented extends Event

  final case class State(count: Long) extends TestSerializable

  def apply(): Behavior[Command] = EventSourcedBehavior[Command, Event, State](
    persistenceId,
    emptyState = State(0),
    commandHandler = (_, command) =>
      command match {
        case Increment => Effect.persist(Incremented)
      },
    eventHandler = (state, event) =>
      event match {
        case Incremented => State(state.count + 1)
      }
  )
}

object IntegrationTestSpec {
  private final def PostgresPort = 53888
}

import IntegrationTestSpec.PostgresPort

class IntegrationTestSpec
    extends ScalaTestWithActorTestKit(
      ConfigFactory
        .load("application-test.conf")
        .withFallback(EventSourcedBehaviorTestKit.config)
        .resolve()
    )
    with AnyFlatSpecLike
    with BeforeAndAfterEach
    with ForAllTestContainer {

  private val adminUsername = "postgres"
  private val adminPassword = "password"
  private val databaseName = "test"

  override val container: PostgreSQLContainer = PostgreSQLContainer(
    databaseName = databaseName,
    username = adminUsername,
    password = adminPassword,
    mountPostgresDataToTmpfs = true
  ).configure { c =>
    c.addEnv("DB_PORT", PostgresPort.toString)
    c.setPortBindings(List(s"$PostgresPort:5432").asJava)
  }

  private val eventSourcedTestKit = EventSourcedBehaviorTestKit[
    PersistentBehavior.Command,
    PersistentBehavior.Event,
    PersistentBehavior.State
  ](
    system,
    PersistentBehavior(),
    EventSourcedBehaviorTestKit.SerializationSettings.enabled
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    // eventSourcedTestKit.clear()

    implicit val ec: ExecutionContext = system.executionContext
    Await.result(
      for {
        _ <- SchemaUtils.dropIfExists()
        _ <- SchemaUtils.createIfNotExists()
        _ <- PersistenceInit.initializeDefaultPlugins(system, 5.seconds)
      } yield Done,
      5.seconds
    ): Unit
  }

  behavior of "Event sourced behavior using real database integration test"

  it should "persist events to the database" in {
    val result = eventSourcedTestKit.runCommand(PersistentBehavior.Increment)
    result.state shouldBe PersistentBehavior.State(1)
    Thread.sleep(5_000L)
//    eventSourcedTestKit.persistenceTestKit
//      .expectNextPersistedType[PersistentBehavior.Event](
//        PersistentBehavior.persistenceId.toString
//      )
    eventSourcedTestKit.persistenceTestKit
      .expectNextPersisted[PersistentBehavior.Event](
        PersistentBehavior.persistenceId.toString(),
        PersistentBehavior.Incremented
      )
  }
}
