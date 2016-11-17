import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import de.heikoseeberger.sbtheader._
import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.license.Apache2_0
import play.sbt.PlayImport._
import play.sbt.PlayScala
import sbt.Keys._
import sbt._

object ProjectPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = plugins.JvmPlugin && SbtScalariform

  object autoImport {
  }

  val AkkaVersion = "2.4.12"
  val InMemoryVersion = "1.3.14"

  lazy val defaultSettings: Seq[Setting[_]] = SbtScalariform.scalariformSettings ++ Seq(

    organization := "com.github.dnvriend",
    organizationName := "Dennis Vriend",
    description := "A small study project on akka-cluster-singleton",
    startYear := Some(2016),

    licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),

    libraryDependencies ++= Seq(
      ws,
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
      "com.typesafe.akka" %% "akka-persistence-query-experimental" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion,
      "com.github.dnvriend" %% "akka-persistence-inmemory" % InMemoryVersion,
      "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "org.scalaz" %% "scalaz-core" % "7.2.7",
      "net.cakesolutions" %% "scala-kafka-client-akka" % "0.8.0",
      "net.cakesolutions" %% "scala-kafka-client-testkit" % "0.8.0" % Test,
      "org.mockito" % "mockito-core" % "2.2.12" % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
    ),

    headers := headers.value ++ Map(
      "scala" -> Apache2_0("2016", "Dennis Vriend"),
      "java" -> Apache2_0("2016", "Dennis Vriend"),
      "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
    ),

    resolvers ++= Seq(
      Resolver.bintrayRepo("cakesolutions", "maven")
    ),

    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences
  )

  override def projectSettings: Seq[Setting[_]] =
    defaultSettings

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
      .setPreference(DoubleIndentClassDeclaration, true)
  }
}