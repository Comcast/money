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

lazy val money =
  Project("money", file("."))
    .settings(projectSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    )
    .aggregate(moneyApi, moneyAkka, moneyCore, moneyAspectj, moneyHttpClient, moneyJavaServlet, moneyWire, moneyKafka, moneySpring)

lazy val moneyApi =
  Project("money-api", file("./money-api"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(projectSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
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
          typesafeConfig
        ) ++ commonTestDependencies
    ).dependsOn(moneyApi)

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
          typesafeConfig,
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
    .dependsOn(moneyCore)

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
      javacOptions in doc := Seq("-source", "1.6", "-target", "1.6"),
      // Configure the desired Avro version.  sbt-avro automatically injects a libraryDependency.
      (version in AvroConfig) := "1.7.6",
      (stringType in AvroConfig) := "String"
    ).dependsOn(moneyCore)

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
          commonsIo,
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
          springOckito
        ) ++ commonTestDependencies,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
    )
    .dependsOn(moneyCore)

def projectSettings = basicSettings ++ Seq(
  ScoverageKeys.coverageHighlighting := true,
  ScoverageKeys.coverageMinimum := 80,
  ScoverageKeys.coverageFailOnMinimum := true,
  organizationName := "Comcast Cable Communications Management, LLC",
  startYear := Some(2012),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
)

def aspectjProjectSettings = projectSettings ++ Seq(
  javaOptions in Test ++= (aspectjWeaverOptions in Aspectj).value // adds javaagent:aspectjweaver to java options, including test
)

def basicSettings =  Defaults.itSettings ++ Seq(
  organization := "com.comcast.money",
  version := "0.10.9.1-SNAPSHOT",
  scalaVersion := "2.12.12",
  crossScalaVersions := List("2.13.3", "2.12.12"),
  resolvers ++= Seq(
    ("spray repo" at "http://repo.spray.io/").withAllowInsecureProtocol(true),
    "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases/"
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:existentials",
    "-language:postfixOps",
    "-language:reflectiveCalls"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalariformAutoformat := true,
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
    <url>https://github.com/Comcast/money</url>
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
      findManagedDependency("org.scala-lang", "scala-library").map(d => d -> url(s"https://www.scala-lang.org/api/2.12.12/")),
      findManagedDependency("com.typesafe", "config").map(d => d -> url("https://typesafehub.github.io/config/latest/api/"))
    )
    val x = links.collect { case Some(d) => d }.toMap
    println("links: " + x)
    x
  }
)
