import sbt.Keys._
import sbt.Keys.libraryDependencies
import sbt.Keys.name
import sbt.Keys.scalaVersion
import sbt.Keys.version

name := """samples-play"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies += "com.comcast.money" %% "money-core" % "0.8.8-SNAPSHOT"

fork in run := true

javaOptions in run += "-J-XX:+UseConcMarkSweepGC -J-Xms4096M -J-Xmx4096M"

