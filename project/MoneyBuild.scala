import com.typesafe.sbt.SbtAspectj.AspectjKeys._
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtScalariform
import sbt.Keys._
import sbt._
import sbtavro.SbtAvro._
import scoverage.ScoverageKeys
import scoverage.ScoverageSbtPlugin._
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.license.Apache2_0

import scala.sys.SystemProperties

object MoneyBuild extends Build {
  import MoneyBuild.Dependencies._

  lazy val copyApiDocsTask = taskKey[Unit]("Copies the scala docs from each project to the doc tree")

  lazy val props = new SystemProperties()

  lazy val money = Project("money", file("."))
  .settings(basicSettings: _*)
  .settings(
    publishLocal := {},
    publish := {}
  )
  .aggregate(moneyApi, moneyCoreScala, moneyAspectj, moneyHttpClient, moneyJavaServlet, moneyKafka, moneySpring, moneySpring3, moneyWire)

  lazy val moneyApi =
    Project("money-api", file("./money-api"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies ++= {
        Seq(
          scalaTest,
          mockito
        )
      }
      )

  lazy val moneyCoreScala =
    Project("money-core-scala", file("./money-core-scala"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies ++= {
          Seq(
            slf4j,
            log4jbinding,
            metricsCore,
            typesafeConfig,
            scalaTest,
            mockito
          )
        }
      ).dependsOn(moneyApi)

  lazy val moneyCore =
    Project("money-core", file("./money-core"))
    .configs( IntegrationTest )
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies <++= (scalaVersion) { v: String =>
        Seq(
          akkaActor(v),
          akkaSlf4j(v),
          akkaTestkit(v),
          slf4j,
          log4jbinding,
          typesafeConfig,
          jodaTime,
          metricsCore,
          scalaTest,
          mockito
        )
      }
    ).dependsOn(moneyApi)

  lazy val moneyAspectj =
    Project("money-aspectj", file("./money-aspectj"))
    .configs( IntegrationTest )
    .settings(aspectjProjectSettings: _*)
    .settings(
      libraryDependencies <++= (scalaVersion) { v: String =>
        Seq(
          typesafeConfig,
          scalaTest,
          mockito
        )
      }
    )
    .dependsOn(moneyCoreScala % "test->test;compile->compile")

  lazy val moneyHttpClient =
    Project("money-http-client", file("./money-http-client"))
      .configs( IntegrationTest )
      .settings(aspectjProjectSettings: _*)
      .settings(
        libraryDependencies <++= (scalaVersion){v: String =>
          Seq(
            apacheHttpClient,
            scalaTest,
            mockito
          )
        }
      )
      .dependsOn(moneyCoreScala % "test->test;compile->compile",moneyAspectj)

  lazy val moneyJavaServlet =
    Project("money-java-servlet", file("./money-java-servlet"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies <++= (scalaVersion){v: String =>
          Seq(
            javaxServlet,
            scalaTest,
            mockito
          )
        }
      )
      .dependsOn(moneyCoreScala)

  lazy val moneyWire =
    Project("money-wire", file("./money-wire"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(sbtavro.SbtAvro.avroSettings : _*)
      .settings(
        libraryDependencies <++= (scalaVersion){v: String =>
          Seq(
            json4sNative,
            json4sJackson,
            scalaTest,
            mockito
          )
        },
        fork := false,
        javacOptions in doc := Seq("-source", "1.6"),
        // Configure the desired Avro version.  sbt-avro automatically injects a libraryDependency.
        (version in avroConfig) := "1.7.6",
        // Look for *.avsc etc. files in src/test/avro
        (sourceDirectory in avroConfig) <<= (sourceDirectory in Compile)(_ / "avro"),
        (stringType in avroConfig) := "String"
      ).dependsOn(moneyCoreScala)

  lazy val moneyKafka =
    Project("money-kafka", file("./money-kafka"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies <++= (scalaVersion){v: String =>
          Seq(
            kafka,
            bijectionCore,
            bijectionAvro,
            chill,
            chillAvro,
            chillBijection,
            commonsIo,
            akkaTestkit(v),
            scalaTest,
            mockito
          )
        }
      )
      .dependsOn(moneyCoreScala, moneyWire)

  lazy val moneySpring =
    Project("money-spring", file("./money-spring"))
      .configs(IntegrationTest)
      .settings(aspectjProjectSettings: _*)
      .settings(
        libraryDependencies <++= (scalaVersion) { v: String =>
          Seq(
            typesafeConfig,
            scalaTest,
            mockito,
            springContext
          )
        }
      )
      .dependsOn(moneyCoreScala)

  lazy val moneySpring3 =
    Project("money-spring3", file("./money-spring3"))
      .configs(IntegrationTest)
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies <++= (scalaVersion) { v: String =>
          Seq(
            typesafeConfig,
            scalaTest,
            mockito,
            springContext3,
            springAop3,
            junit,
            junitInterface,
            springTest,
            mockito,
            springOckito,
            assertj
          )
        },
        testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
      )
      .dependsOn(moneyCoreScala)

  def projectSettings = basicSettings ++ Seq(
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true
  )

  def aspectjProjectSettings = projectSettings ++ aspectjSettings ++ Seq(
    javaOptions in Test <++= weaverOptions in Aspectj // adds javaagent:aspectjweaver to java options, including test
  )

  def basicSettings =  Defaults.itSettings ++ SbtScalariform.scalariformSettings ++ Seq(
    organization := "com.comcast.money",
    version := "0.8.12-SNAPSHOT",
    crossScalaVersions := Seq("2.10.6", "2.11.7"),
    scalaVersion := "2.11.7",
    resolvers ++= Seq(
      "spray repo" at "http://repo.spray.io/",
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"
    ),
    javacOptions in Compile ++= Seq(
      "-source", "1.6",
      "-target", "1.6",
      "-Xlint:unchecked",
      "-Xlint:deprecation",
      "-Xlint:-options"),
    javacOptions in doc := Seq("-source", "1.6"),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:existentials",
      "-language:postfixOps",
      "-language:reflectiveCalls"),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF", "-u", "target/scalatest-reports"),
    fork := true,
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <name>com.comcast:money</name>
      <description>A distributed tracing framework</description>
      <url>https://github.com/Comcast/money</url>
        <licenses>
          <license>
            <name>Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
            <distribution>repo</distribution>
            <comments>A business-friendly OSS license</comments>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:Comcast/money.git</url>
          <connection>scm:git:git@github.com:Comcast/money.git</connection>
        </scm>
        <developers>
          <developer>
            <id>paulcleary</id>
            <name>Paul Clery</name>
            <url>https://github.com/paulcleary</url>
          </developer>
          <developer>
            <id>kristomasette</id>
            <name>Kristofer Tomasette</name>
            <url>https://github.com/kristomasette</url>
          </developer>
        </developers>),
    publishArtifact in Test := false,
    autoAPIMappings := true,
    apiMappings ++= {
      def findManagedDependency(organization: String, name: String): Option[File] = {
        (for {
          entry <- (fullClasspath in Compile).value
          module <- entry.get(moduleID.key) if module.organization == organization && module.name.startsWith(name)
        } yield entry.data).headOption
      }
      val links: Seq[Option[(File, URL)]] = Seq(
        findManagedDependency("org.scala-lang", "scala-library").map(d => d -> url(s"http://www.scala-lang.org/api/2.10.4/")),
        findManagedDependency("com.typesafe.akka", "akka-actor").map(d => d -> url(s"http://doc.akka.io/api/akka/$akkaVersion/")),
        findManagedDependency("com.typesafe", "config").map(d => d -> url("http://typesafehub.github.io/config/latest/api/"))
      )
      val x = links.collect { case Some(d) => d }.toMap
      println("links: " + x)
      x
    },
    headers := Map(
      "scala" -> Apache2_0("2012-2015", "Comcast Cable Communications Management, LLC"),
      "java" -> Apache2_0("2012-2015", "Comcast Cable Communications Management, LLC"),
      "conf" -> Apache2_0("2012-2015", "Comcast Cable Communications Management, LLC", "#")
    )
  ) ++ HeaderPlugin.settingsFor(IntegrationTest) ++ AutomateHeaderPlugin.automateFor(Compile, Test, IntegrationTest)

  object Dependencies {
    val akkaVersion = "2.2.3"
    val codahaleVersion = "3.0.2"
    val apacheHttpClientVersion = "4.3.5"

    // Logging, SlF4J must equal the same version used by akka
    val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
    val log4jbinding = "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "it,test"

    // Akka
    def akkaActor(scalaVersion: String) = "com.typesafe.akka" %% "akka-actor" % getAkkaVersion(scalaVersion)
    def akkaSlf4j(scalaVersion: String) = "com.typesafe.akka" %% "akka-slf4j" % getAkkaVersion(scalaVersion) % "runtime"
    def akkaTestkit(scalaVersion: String) = "com.typesafe.akka" %% "akka-testkit" % getAkkaVersion(scalaVersion) %
      "it,test"

    // Joda
    val jodaTime = "joda-time" % "joda-time" % "2.1"

    // Json
    val json4sNative = "org.json4s" %% "json4s-native" % "3.2.11"
    val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.2.11"

    // Typseafe config
    def typesafeConfig = "com.typesafe" % "config" % "1.2.1"

    // Codahale metrics
    val metricsCore = "com.codahale.metrics" % "metrics-core" % codahaleVersion

    // Apache http client
    val apacheHttpClient = "org.apache.httpcomponents" % "httpclient" % apacheHttpClientVersion

    // Javax servlet - note: the group id and artfacit id have changed in 3.0
    val javaxServlet = "javax.servlet" % "servlet-api" % "2.5"

    // Kafka, exclude dependencies that we will not need, should work for 2.10 and 2.11
    val kafka = ("org.apache.kafka" %% "kafka" % "0.8.2.2")
    .exclude("javax.jms", "jms")
    .exclude("com.sun.jdmk", "jmxtools")
    .exclude("com.sun.jmx", "jmxri")
    .exclude("org.apache.zookeeper", "zookeeper")
    .exclude("javax.mail", "mail")
    .exclude("javax.activation", "activation")

    // Avro and Bijection
    val bijectionCore = "com.twitter" % "bijection-core_2.10" % "0.6.3"
    val bijectionAvro = "com.twitter" % "bijection-avro_2.10" % "0.6.3"
    val chill = "com.twitter" % "chill_2.10" % "0.4.0"
    val chillAvro = "com.twitter" % "chill-avro" % "0.4.0"
    val chillBijection = "com.twitter" % "chill-bijection_2.10" % "0.4.0"

    val commonsIo = "commons-io" % "commons-io" % "2.4"

    // Spring
    val springContext3 = ("org.springframework" % "spring-context" % "3.2.6.RELEASE")
    .exclude("commons-logging", "commons-logging")

    val springAop3 = "org.springframework" % "spring-aop" % "3.2.6.RELEASE"
    val springContext = "org.springframework" % "spring-context" % "4.1.1.RELEASE"

    // Test
    val mockito = "org.mockito" % "mockito-core" % "1.9.5" % "test"
    val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" % "it,test"
    val junit = "junit" % "junit" % "4.11" % "test"
    val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test->default"
    val springTest = ("org.springframework" % "spring-test" % "3.2.6.RELEASE")
      .exclude("commons-logging", "commons-logging")
    val springOckito = "org.kubek2k" % "springockito" % "1.0.9" % "test"
    val assertj = "org.assertj" % "assertj-core" % "1.7.1" % "it,test"

    def getAkkaVersion(scalaVersion: String) = {
      scalaVersion match {
        case version if version.startsWith("2.10") => "2.2.3"
        case version if version.startsWith("2.11") => "2.3.4"
      }
    }
  }
}
