import sbt.Tests.{Group, SubProcess}
import scoverage.ScoverageKeys
// ================================================================================
// Plugins
// ================================================================================
enablePlugins(
  play.sbt.PlayScala,
  SbtAutoBuildPlugin,
  SbtGitVersioning,
  SbtDistributablesPlugin,
  SbtArtifactory
)

// ================================================================================
// Play configuration
// ================================================================================
PlayKeys.playDefaultPort := 8701

val akkaVersion     = "2.5.23"

val akkaHttpVersion = "10.0.15"

dependencyOverrides += "com.typesafe.akka" %% "akka-stream"    % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-actor"     % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
// ================================================================================
// Testing
// ================================================================================
libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core"  %  "jackson-core"        % "2.11.2",
  "com.fasterxml.jackson.core"  %  "jackson-databind"    % "2.11.2",
  "com.github.tomakehurst"      %  "wiremock-jre8"       % "2.27.1",
  "com.typesafe.play"           %% "play-test"           % play.core.PlayVersion.current,
  "org.jsoup"                   %  "jsoup"               % "1.13.1",
  "org.mockito"                 %  "mockito-core"        % "3.4.6",
  "org.pegdown"                 %  "pegdown"             % "1.6.0",
  "org.scalacheck"              %% "scalacheck"          % "1.14.3",
  "org.scalatest"               %% "scalatest"           % "3.0.8",
  "org.scalatestplus.play"      %% "scalatestplus-play"  % "3.1.3",
  "uk.gov.hmrc"                 %% "hmrctest"            % "3.9.0-play-26",
  "uk.gov.hmrc"                 %% "stub-data-generator" % "0.5.3",
  "com.typesafe.akka"           %% "akka-testkit"        % "2.5.23",
  "uk.gov.hmrc"                 %% "reactivemongo-test"  % "4.21.0-play-26"
).map(_ % "test")

// ================================================================================
// Dependencies
// ================================================================================
scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  ws,
  "com.github.fge"            %  "json-schema-validator"         % "2.2.6",
  "com.github.pureconfig"     %% "pureconfig"                    % "0.13.0",
  "com.softwaremill.macwire"  %% "macros"                        % "2.3.7" % "provided",
  "com.softwaremill.macwire"  %% "macrosakka"                    % "2.3.7" % "provided",
  "com.softwaremill.macwire"  %% "proxy"                         % "2.3.7",
  "com.softwaremill.macwire"  %% "util"                          % "2.3.7",
  "org.typelevel"             %% "cats-core"                     % "1.6.1",
  "uk.gov.hmrc"               %% "auth-client"                   % "3.0.0-play-26",
  "uk.gov.hmrc"               %% "bootstrap-backend-play-26"     % "2.24.0",
  "uk.gov.hmrc"               %% "mongo-lock"                    % "6.23.0-play-26",
  "uk.gov.hmrc"               %% "simple-reactivemongo"          % "7.30.0-play-26",
  "org.scala-stm"             %% "scala-stm"                     % "0.9.1"
)

resolvers ++= Seq(
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.jcenterRepo
)

// ================================================================================
// Compiler flags
// ================================================================================

scalacOptions ++= Seq(
//  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.  
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.
  "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused",                     // Warn if an import selector is not referenced.
  "-Ywarn-value-discard",               // Warn when non-Unit expression results are unused.
  "-Xmax-classfile-name", "100"
)


// ================================================================================
// Misc
// ================================================================================

initialCommands in console := "import cats.implicits._"

majorVersion := 0

uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

// ================================================================================
// Testing
// ================================================================================
  import scoverage.ScoverageKeys._
  import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._

  ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.models\.json.*;views\.html;.*\.Routes;.*\.RoutesPrefix;.*\.Reverse[^.]*;testonly"""
  coverageMinimum := 80
  coverageFailOnMinimum := false
  coverageHighlighting := true
  scalafmtOnCompile in Compile := true
  scalafmtOnCompile in Test := true
  ScoverageKeys.coverageExcludedFiles :=
    """<empty>;.*javascript;.*Routes.*;.*testonly.*;
      |.*BuildInfo.scala.*;.*controllers.test.*;.*connectors.TestConnector.*""".stripMargin

disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427