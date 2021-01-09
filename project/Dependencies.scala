import sbt._
object Dependencies {
  val metricsV = "3.2.6"
  val apacheHttpClientV = "4.5.6"

  val akkaV = "2.5.30"
  val akkaHttpV = "10.1.8"
  val slf4jV = "1.7.25"
  val jodaV = "2.9.9"
  val json4sV = "3.6.10"
  val typesafeConfigV = "1.3.3"
  val openTelemetryV = "0.14.0-SNAPSHOT"
  val openTelemetryInstV = "0.14.0-SNAPSHOT"

  val akka =            "com.typesafe.akka"         %% "akka-actor"                  % akkaV
  val akkaStream =      "com.typesafe.akka"         %% "akka-stream"                 % akkaV
  val akkaLog =         "com.typesafe.akka"         %% "akka-slf4j"                  % akkaV
  val akkaTestKit =     "com.typesafe.akka"         %% "akka-testkit"                % akkaV
  val akkaHttp =        "com.typesafe.akka"         %% "akka-http"                   % akkaHttpV
  val akkaHttpTestKit = "com.typesafe.akka"         %% "akka-http-testkit"           % akkaHttpV % Test

  // Logging
  val slf4j = "org.slf4j" % "slf4j-api" % slf4jV
  val log4jbinding = "org.slf4j" % "slf4j-log4j12" % slf4jV % Test

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
  val kafka = ("org.apache.kafka" %% "kafka" % "2.4.0")
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

  val openTelemetryApi = "io.opentelemetry" % "opentelemetry-api" % openTelemetryV changing()
  val openTelemetrySemConv = "io.opentelemetry" % "opentelemetry-semconv" % openTelemetryV changing()
  val openTelemetryProp = "io.opentelemetry" % "opentelemetry-extension-trace-propagators" % openTelemetryV changing()
  val openTelemetrySdk = "io.opentelemetry" % "opentelemetry-sdk" % openTelemetryV changing()
  val openTelemetrySdkTesting = "io.opentelemetry" % "opentelemetry-sdk-testing" % openTelemetryV changing()
  val openTelemetryLoggingExporter = "io.opentelemetry" % "opentelemetry-exporter-logging" % openTelemetryInstV changing()
  val openTelemetryOtlpExporter = "io.opentelemetry" % "opentelemetry-exporter-otlp" % openTelemetryInstV changing()
  val openTelemetryZipkinExporter = "io.opentelemetry" % "opentelemetry-exporter-zipkin" % openTelemetryInstV changing()
  val openTelemetryJaegerExporter = "io.opentelemetry" % "opentelemetry-exporter-jaeger" % openTelemetryInstV changing()

  // Spring
  val springWeb = ("org.springframework" % "spring-web" % "4.3.17.RELEASE")
  val springContext = ("org.springframework" % "spring-context" % "4.3.17.RELEASE")
    .exclude("commons-logging", "commons-logging")

  val springAop = "org.springframework" % "spring-aop" % "4.3.17.RELEASE"

  // Test

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2" % Test
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3" % Test
  val scalaTestWordSpec = "org.scalatest" %% "scalatest-wordspec" % "3.2.2" % Test
  val scalaTestPlus = "org.scalatestplus" %% "mockito-3-4" % "3.2.2.0" % Test
  val scalaTestCheck = "org.scalatestplus" %% "scalacheck-1-14" % "3.2.2.0" % Test

  val junit = "junit" % "junit" % "4.12" % Test
  val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test exclude("junit", "junit-dep")
  val powerMock = "org.powermock" % "powermock-module-junit4" % "2.0.7" % Test
  val powerMockApi = "org.powermock" % "powermock-api-mockito2" % "2.0.7" % Test
  val springTest = ("org.springframework" % "spring-test" % "4.3.17.RELEASE")
    .exclude("commons-logging", "commons-logging")
  val springBootTest = "org.springframework.boot" % "spring-boot-starter-test" % "1.5.11.RELEASE" % Test
  val aspectJ = "org.aspectj" % "aspectjweaver" % "1.8.9" % Test
  val assertj = "org.assertj" % "assertj-core" % "3.17.2" % Test
  val awaitility = "org.awaitility" % "awaitility" % "4.0.3" % Test
  val zipkinJunit = "io.zipkin.zipkin2" % "zipkin-junit" % "2.18.3" % Test

  val commonTestDependencies = Seq(
    scalaTest,
    scalaCheck,
    scalaTestWordSpec,
    scalaTestPlus,
    scalaTestCheck
  )
}
