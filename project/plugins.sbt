// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-aspectj" % "0.9.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

addSbtPlugin("com.cavorite" % "sbt-avro" % "0.3.2")

