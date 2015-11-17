import com.typesafe.sbt.SbtAspectj.AspectjKeys._
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtScalariform
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin._
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.license.Apache2_0

import scala.sys.SystemProperties

object MoneyBuild extends Build {
  import MoneyBuild.Dependencies._

  lazy val props = new SystemProperties()

  lazy val money = Project("money", file("."))
  .settings(basicSettings: _*)
  .settings(
    publishLocal := {},
    publish := {}
  )
  .aggregate(moneyCore,moneyAspectj)

  lazy val moneyCore =
    Project("money-core", file("./money-core"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          slf4j,
          log4jbinding,
          typesafeConfig,
          junit,
          scalaTest,
          mockito
        )
      )

  lazy val moneyAspectj =
    Project("money-aspectj", file("./money-aspectj"))
      .configs( IntegrationTest )
      .settings(aspectjProjectSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          slf4j,
          log4jbinding,
          typesafeConfig,
          junit,
          scalaTest,
          mockito,
          assertj
        )
      ).dependsOn(moneyCore % "compile->compile;it->it;test->test")

  lazy val moneyJavaServlet =
    Project("money-java-servlet", file("./money-java-servlet"))
      .configs( IntegrationTest )
      .settings(projectSettings: _*)
      .settings(
        libraryDependencies ++= Seq(
          javaxServlet,
          junit,
          scalaTest,
          mockito
        )
      )
      .dependsOn(moneyCore)

  def projectSettings = basicSettings ++ Seq(
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true
  )

  def aspectjProjectSettings = projectSettings ++ aspectjSettings ++ Seq(
    javaOptions in IntegrationTest <++= weaverOptions in Aspectj // adds javaagent:aspectjweaver to java options, including test
  )

  def basicSettings =  Defaults.itSettings ++ SbtScalariform.scalariformSettings ++ Seq(
    organization := "com.comcast.money",
    version := "0.9.0-SNAPSHOT",
    crossScalaVersions := Seq("2.10.6", "2.11.7"),
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      "spray repo" at "http://repo.spray.io/",
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"
    ),
    javacOptions in Compile ++= Seq(
      "-source", "1.6",
      "-target", "1.6",
      "-Xlint:unchecked",
      "-Xlint:deprecation",
      "-Xlint:-options",
      "-g"),
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
    headers := Map(
      "scala" -> Apache2_0("2012-2015", "Comcast Cable Communications Management, LLC"),
      "java" -> Apache2_0("2012-2015", "Comcast Cable Communications Management, LLC"),
      "conf" -> Apache2_0("2012-2015", "Comcast Cable Communications Management, LLC", "#")
    )
  ) ++ HeaderPlugin.settingsFor(IntegrationTest) ++ AutomateHeaderPlugin.automateFor(Compile, Test, IntegrationTest)

  object Dependencies {
    val codahaleVersion = "3.0.2"
    val apacheHttpClientVersion = "4.3.5"

    // Logging, SlF4J must equal the same version used by akka
    val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
    val log4jbinding = "org.slf4j" % "slf4j-log4j12" % "1.7.5" % "it,test"

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

    // Javax servlet - note: the group id and artifact id have changed in 3.0
    val javaxServlet = "javax.servlet" % "servlet-api" % "2.5"

    // Kafka, exclude dependencies that we will not need, should work for 2.10 and 2.11
    val kafka = ("org.apache.kafka" % "kafka_2.10" % "0.8.1.1")
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

    val curator = ("org.apache.curator" % "curator-test" % "2.4.0")
    .exclude("org.slf4j", "slf4j-log4j12")

    val zkClient = ("com.101tec" % "zkclient" % "0.4")
    .exclude("org.apache.zookeeper", "zookeeper")

    // Test
    val mockito = "org.mockito" % "mockito-core" % "1.9.5" % "it,test"
    val scalaTest = "org.scalatest" %% "scalatest" % "2.2.3" % "it,test"
    val junit = "junit" % "junit" % "4.11" % "test"
    val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test->default"
    val springTest = ("org.springframework" % "spring-test" % "3.2.6.RELEASE")
      .exclude("commons-logging", "commons-logging")
    val springOckito = "org.kubek2k" % "springockito" % "1.0.9" % "test"
    val assertj = "org.assertj" % "assertj-core" % "2.2.0" % "it,test"
  }
}
