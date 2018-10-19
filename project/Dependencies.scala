import sbt._
object Dependencies {
  val metricsV = "3.2.6"
  val apacheHttpClientV = "4.5.6"

  val akkaV = "2.5.12"
  val akkaHttpV = "10.1.5"
  val slf4jV = "1.7.25"
  val jodaV = "2.9.9"
  val json4sV = "3.6.1"
  val typesafeConfigV = "1.3.3"

  val akka =            "com.typesafe.akka"         %% "akka-actor"                  % akkaV
  val akkaStream =      "com.typesafe.akka"         %% "akka-stream"                 % akkaV
  val akkaLog =         "com.typesafe.akka"         %% "akka-slf4j"                  % akkaV
  val akkaHttp =        "com.typesafe.akka"         %% "akka-http"                   % akkaHttpV
  val akkaHttpTestKit = "com.typesafe.akka"         %% "akka-http-testkit"           % akkaHttpV % "test"

  // Logging
  val slf4j = "org.slf4j" % "slf4j-api" % slf4jV
  val log4jbinding = "org.slf4j" % "slf4j-log4j12" % slf4jV % "test"

  // Joda
  val jodaTime = "joda-time" % "joda-time" % jodaV

  // Json
  val json4sNative = "org.json4s" %% "json4s-native" % json4sV
  val json4sJackson = "org.json4s" %% "json4s-jackson" % json4sV

  // Typseafe config
  def typesafeConfig = "com.typesafe" % "config" % typesafeConfigV

  // Codahale metrics
  val metricsCore = "io.dropwizard.metrics" % "metrics-core" % metricsV

  // Apache http client
  val apacheHttpClient = "org.apache.httpcomponents" % "httpclient" % apacheHttpClientV

  // Javax servlet - note: the group id and artfacit id have changed in 3.0
  val javaxServlet = "javax.servlet" % "servlet-api" % "2.5"

  // Kafka, exclude dependencies that we will not need, should work for 2.10 and 2.11
  val kafka = ("org.apache.kafka" %% "kafka" % "0.10.2.2")
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
  val springWeb3 = ("org.springframework" % "spring-web" % "3.2.18.RELEASE")
  val springContext3 = ("org.springframework" % "spring-context" % "3.2.18.RELEASE")
  .exclude("commons-logging", "commons-logging")

  val springAop3 = "org.springframework" % "spring-aop" % "3.2.18.RELEASE"
  val springContext = "org.springframework" % "spring-context" % "4.3.17.RELEASE"

  // Test
  val mockito = "org.mockito" % "mockito-core" % "1.9.5" % "test"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
  val junit = "junit" % "junit" % "4.11" % "test"
  val junitInterface = "com.novocode" % "junit-interface" % "0.11" % "test->default"
  val springTest = ("org.springframework" % "spring-test" % "3.2.18.RELEASE")
    .exclude("commons-logging", "commons-logging")
  val springOckito = "org.kubek2k" % "springockito" % "1.0.9" % "test"
  val assertj = "org.assertj" % "assertj-core" % "1.7.1" % "test"
}
