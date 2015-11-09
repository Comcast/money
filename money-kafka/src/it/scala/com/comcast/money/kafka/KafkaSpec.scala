package com.comcast.money.kafka

import java.util.Properties

import _root_.kafka.message.MessageAndMetadata
import _root_.kafka.utils.{Logging, ZKStringSerializer}
import com.comcast.money.core.Money
import com.comcast.money.wire.avro._
import com.twitter.bijection.Injection
import com.twitter.bijection.avro.SpecificAvroCodecs
import kafka.admin.AdminUtils
import org.I0Itec.zkclient.ZkClient
import org.scalatest._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.reflectiveCalls

class KafkaSpec extends FunSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with Logging {

  implicit val specificAvroBinaryInjectionForTweet = SpecificAvroCodecs.toBinary[Span]

  private val testTopic = "money"
  private val testTopicNumPartitions = 1
  private val testTopicReplicationFactor = 1
  //private val zookeeperPort = InstanceSpec.getRandomPort
  private val zookeeperPort = 2181
  //private val kafkaPort = InstanceSpec.getRandomPort
  private val kafkaPort = 9092

  private var zookeeperEmbedded: Option[ZooKeeperEmbedded] = None
  private var zkClient: Option[ZkClient] = None
  private var kafkaEmbedded: Option[KafkaEmbedded] = None

  override def beforeAll() {
    // Start embedded ZooKeeper server
    zookeeperEmbedded = Some(new ZooKeeperEmbedded(zookeeperPort))

    for {z <- zookeeperEmbedded} {
      // Start embedded Kafka broker
      val brokerConfig = new Properties
      brokerConfig.put("zookeeper.connect", z.connectString)
      brokerConfig.put("port", kafkaPort.toString)
      kafkaEmbedded = Some(new KafkaEmbedded(brokerConfig))
      for {k <- kafkaEmbedded} k.start()

      // Create test topic
      val sessionTimeout = 30.seconds
      val connectionTimeout = 30.seconds
      zkClient = Some(new ZkClient(z.connectString, sessionTimeout.toMillis.toInt, connectionTimeout.toMillis.toInt,
        ZKStringSerializer))
      for {
        zc <- zkClient
      } {
        val topicConfig = new Properties
        AdminUtils.createTopic(zc, testTopic, testTopicNumPartitions, testTopicReplicationFactor, topicConfig)
      }
    }

    Money.tracer
  }

  override def afterAll() {
    for {k <- kafkaEmbedded} k.stop()

    for {
      zc <- zkClient
    } {
      info("ZooKeeper client: shutting down...")
      zc.close()
      info("ZooKeeper client: shutdown completed")
    }

    for {z <- zookeeperEmbedded} z.stop()
  }

  import scala.collection.JavaConversions._
  val fixture = {
    new {
      val notes = seqAsJavaList(Seq(new Note("foo", 1L, new NoteValue(NoteType.String, "foo")), new Note("foo", 1L, new NoteValue(NoteType.Double, "4.0"))))
      val s1 = new Span("root", "money", "localhost", 1000L, true, 1L, new SpanId("1", 2L, 3L), notes)
      val s2 = new Span("service-1", "money", "com.service1", 1000L, true, 1L, new SpanId("1", 3L, 4L), notes)
      val s3 = new Span("service-2", "money", "com.service2", 1000L, true, 1L, new SpanId("1", 3L, 5L), notes)

      val messages = Seq(s1, s2, s3)
    }
  }

  describe("Kafka") {

    it("should asynchronously send and receive a Span in Avro format") {
      for {
        z <- zookeeperEmbedded
        k <- kafkaEmbedded
      } {
        Given("a ZooKeeper instance")
        And("a Kafka broker instance")
        And("some spans")
        val f = fixture
        val spans = f.messages
        And("a single-threaded Kafka consumer group")
        // The Kafka consumer group must be running before the first messages are being sent to the topic.
        val consumer = {
          val numConsumerThreads = 1
          val config = {
            val c = new Properties
            c.put("group.id", "test-consumer")
            c
          }
          new KafkaConsumerApp(testTopic, z.connectString, numConsumerThreads, config)
        }
        val actualSpans = new mutable.SynchronizedQueue[Span]
        consumer.startConsumers(
          (m: MessageAndMetadata[Array[Byte], Array[Byte]], c: ConsumerTaskContext) => {
            val span = Injection.invert(m.message)
            for {t <- span} {
              println(s"Consumer thread ${c.threadId}: received Span $t from partition ${m.partition} of topic ${m.topic} (offset: ${m.offset})")
              actualSpans += t
            }
          })
        val waitForConsumerStartup = 300.millis
        debug(s"Waiting $waitForConsumerStartup for Kafka consumer threads to launch")
        Thread.sleep(waitForConsumerStartup.toMillis)
        debug("Finished waiting for Kafka consumer threads to launch")

        When("I send a trace using the kafka emitter")
        Money.tracer.startSpan("foo")
        Money.tracer.stopSpan()

        Then("the consumer app should receive the spans")
        // XXX: REMEMBER, SPANS WAIT ONE FULL SECOND AFTER STOPPING TO EMIT!!!
        val waitForConsumerToReadStormOutput = 2000.millis
        debug(s"Waiting $waitForConsumerToReadStormOutput for Kafka consumer threads to read messages")
        Thread.sleep(waitForConsumerToReadStormOutput.toMillis)
        debug("Finished waiting for Kafka consumer threads to read messages")

        And("the span that was sent from money should have been received")
        actualSpans(0).getName shouldEqual "foo"

        // Cleanup
        debug("Shutting down Kafka consumer threads")
        consumer.shutdown()
      }
    }
  }
}