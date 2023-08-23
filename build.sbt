import Dependencies._
import com.typesafe.sbt.SbtScalariform
import sbt.Keys._
import sbt._
import sbtavro.SbtAvro._
import scoverage.ScoverageKeys
import scoverage.ScoverageSbtPlugin._

import scala.sys.SystemProperties

lazy val copyApiDocsTask = taskKey[Unit]("Copies the scala docs from each project to the doc tree")

lazy val props = new SystemProperties()

ThisBuild / organization := "com.comcast.money"
ThisBuild / organizationName := "Comcast Cable Communications Management, LLC"
ThisBuild / startYear := Some(2012)
ThisBuild / tlBaseVersion := "0.15"
ThisBuild / developers := List(
  Developer(
    "pauljamescleary",
    "Paul James Cleary",
    "pauljamescleary@gmail.com",
    url("https://github.com/pauljamescleary")
  )
)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / tlSonatypeUseLegacyHost := true
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))

lazy val money =
  Project("money", file("."))
    .settings(projectSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    )
    .aggregate(
      moneyApi,
      moneyAkka,
      moneyCore,
      moneyAspectj,
      moneyHttpClient,
      moneyJavaServlet,
      moneyWire,
      moneyKafka,
      moneySpring,
      moneyOtelFormatters,
      moneyOtelHandler,
      moneyOtlpExporter,
      moneyOtlpHttpExporter,
      moneyOtelInMemoryExporter,
      moneyOtelLoggingExporter,
      moneyOtelZipkinExporter,
      moneyOtelJaegerExporter
    )

lazy val moneyApi =
  Project("money-api", file("./money-api"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(javaOnlyProjectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          openTelemetryApi
        ) ++ commonTestDependencies
    )

lazy val moneyCore =
  Project("money-core", file("./money-core"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          slf4j,
          log4jbinding,
          metricsCore,
          openTelemetryApi,
          openTelemetrySemConv,
          typesafeConfig
        ) ++ commonTestDependencies
    ).dependsOn(moneyApi)

lazy val moneyOtelFormatters =
  Project("money-otel-formatters", file("./money-otel-formatters"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          slf4j,
          log4jbinding,
          metricsCore,
          openTelemetryApi,
          openTelemetryProp,
          typesafeConfig
        ) ++ commonTestDependencies
    ).dependsOn(moneyApi, moneyCore % "test->test;compile->compile")

lazy val moneyAkka =
  Project("money-akka", file("./money-akka"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          akkaStream,
          akkaHttp,
          akkaTestKit,
          akkaHttpTestKit,
          akkaLog,
          typesafeConfig
        ) ++ commonTestDependencies
    )
    .dependsOn(moneyCore)

lazy val moneyAspectj =
  Project("money-aspectj", file("./money-aspectj"))
    .enablePlugins(SbtAspectj, AutomateHeaderPlugin)
    .settings(aspectjProjectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig
        ) ++ commonTestDependencies
    )
    .dependsOn(moneyCore % "test->test;compile->compile")

lazy val moneyHttpClient =
  Project("money-http-client", file("./money-http-client"))
    .enablePlugins(SbtAspectj, AutomateHeaderPlugin)
    .settings(aspectjProjectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          apacheHttpClient
        ) ++ commonTestDependencies
    )
    .dependsOn(moneyCore % "test->test;compile->compile", moneyAspectj)

lazy val moneyJavaServlet =
  Project("money-java-servlet", file("./money-java-servlet"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          javaxServlet
        ) ++ commonTestDependencies
    )
    .dependsOn(moneyCore % "test->test;compile->compile")

lazy val moneyWire =
  Project("money-wire", file("./money-wire"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          json4sNative,
          json4sJackson
        ) ++ commonTestDependencies,
      fork := false,
      doc / javacOptions := Seq("-source", "1.6"),
      // Configure the desired Avro version.  sbt-avro automatically injects a libraryDependency.
      AvroConfig / version := "1.7.6",
      AvroConfig / stringType := "String"
    ).dependsOn(moneyCore % "test->test;compile->compile")

lazy val moneyKafka =
  Project("money-kafka", file("./money-kafka"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          kafka,
          bijectionCore,
          bijectionAvro,
          chill,
          chillAvro,
          chillBijection,
          commonsIo
        ) ++ commonTestDependencies
    )
    .dependsOn(moneyCore, moneyWire % "test->test;compile->compile")

lazy val moneySpring =
  Project("money-spring", file("./money-spring"))
    .enablePlugins(AutomateHeaderPlugin)
    .enablePlugins(SbtAspectj)
    .settings(aspectjProjectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          springWeb,
          springAop,
          springContext,
          junit,
          junitInterface,
          assertj,
          springTest,
          springBootTest,
          aspectJ
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore % "test->test;compile->compile")

lazy val moneyOtelHandler =
  Project("money-otel-handler", file("./money-otel-handler"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore)

lazy val moneyOtelZipkinExporter =
  Project("money-otel-zipkin-exporter", file("./money-otel-zipkin-exporter"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          openTelemetryZipkinExporter,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi,
          awaitility,
          zipkinJunit
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore, moneyOtelHandler % "test->test;compile->compile")

lazy val moneyOtelJaegerExporter =
  Project("money-otel-jaeger-exporter", file("./money-otel-jaeger-exporter"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          openTelemetryJaegerExporter,
          grpc,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore, moneyOtelHandler % "test->test;compile->compile")

lazy val moneyOtelInMemoryExporter =
  Project("money-otel-inmemory-exporter", file("./money-otel-inmemory-exporter"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          openTelemetrySdkTesting,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore, moneyOtelHandler % "test->test;compile->compile")

lazy val moneyOtelLoggingExporter =
  Project("money-otel-logging-exporter", file("./money-otel-logging-exporter"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          openTelemetryLoggingExporter,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore, moneyOtelHandler % "test->test;compile->compile")

lazy val moneyOtlpExporter =
  Project("money-otlp-exporter", file("./money-otlp-exporter"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          openTelemetryOtlpExporter,
          grpc,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore, moneyOtelHandler % "test->test;compile->compile")

lazy val moneyOtlpHttpExporter =
  Project("money-otlp-http-exporter", file("./money-otlp-http-exporter"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++=
        Seq(
          typesafeConfig,
          openTelemetryApi,
          openTelemetrySdk,
          openTelemetryOtlpHttpExporter,
          grpc,
          junit,
          junitInterface,
          assertj,
          powerMock,
          powerMockApi
        ) ++ commonTestDependencies,
         testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore, moneyOtelHandler % "test->test;compile->compile")

def aspectjProjectSettings = projectSettings ++ Seq(
  Test / javaOptions ++= (Aspectj / aspectjWeaverOptions).value // adds javaagent:aspectjweaver to java options, including test
)

def javaOnlyProjectSettings = projectSettings ++ Seq(
  autoScalaLibrary := false
)

def projectSettings = basicSettings ++ Seq(
  ScoverageKeys.coverageHighlighting := true,
  ScoverageKeys.coverageMinimumStmtTotal := 80,
  ScoverageKeys.coverageFailOnMinimum := true,
  sonatypeProfileName := "com.comcast",
  homepage := Some(url("https://github.com/Comcast/money")),
)

def basicSettings =  Defaults.itSettings ++ Seq(
  sonatypeProfileName := "com.comcast",
  scalaVersion := "2.12.15",
  crossScalaVersions := List("2.13.11", "2.12.18"),
  resolvers ++= Seq(
    ("spray repo" at "http://repo.spray.io/").withAllowInsecureProtocol(true),
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:existentials",
    "-language:postfixOps",
    "-language:reflectiveCalls"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  doc / javacOptions := Seq("-source", "1.8"),
  scalariformAutoformat := true,
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDF", "-u", "target/scalatest-reports"),
  fork := true,
  Test / publishArtifact := false,
  autoAPIMappings := true
)
