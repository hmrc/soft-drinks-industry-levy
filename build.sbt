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

// ================================================================================
// Testing
// ================================================================================
libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core"  %  "jackson-core"        % "2.8.11",
  "com.fasterxml.jackson.core"  %  "jackson-databind"    % "2.8.11.2",
  "com.github.tomakehurst"      %  "wiremock"            % "2.18.0",
  "com.typesafe.play"           %% "play-test"           % play.core.PlayVersion.current,
  "org.jsoup"                   %  "jsoup"               % "1.11.3",
  "org.mockito"                 %  "mockito-core"        % "2.19.1",
  "org.pegdown"                 %  "pegdown"             % "1.6.0",
  "org.scalacheck"              %% "scalacheck"          % "1.14.0",
  "org.scalatest"               %% "scalatest"           % "2.2.6",
  "org.scalatestplus.play"      %% "scalatestplus-play"  % "2.0.1",
  "uk.gov.hmrc"                 %% "hmrctest"            % "3.0.0",
  "uk.gov.hmrc"                 %% "stub-data-generator" % "0.5.3"
).map(_ % "test")

// ================================================================================
// Dependencies
// ================================================================================
scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  ws,
  "com.github.fge"            %  "json-schema-validator" % "2.2.6",
  "com.github.pureconfig"     %% "pureconfig"            % "0.9.1",
  "com.softwaremill.macwire"  %% "macros"                % "2.3.1" % "provided",
  "com.softwaremill.macwire"  %% "macrosakka"            % "2.3.1" % "provided",
  "com.softwaremill.macwire"  %% "proxy"                 % "2.3.1",
  "com.softwaremill.macwire"  %% "util"                  % "2.3.1",
  "org.typelevel"             %% "cats-core"             % "1.1.0",
  "uk.gov.hmrc"               %% "auth-client"           % "2.6.0",
  "uk.gov.hmrc"               %% "bootstrap-play-25"     % "4.6.0",
  "uk.gov.hmrc"               %% "mongo-lock"            % "5.1.0",
  "uk.gov.hmrc"               %% "simple-reactivemongo"  % "6.1.0",
  "org.scala-stm"             %% "scala-stm"             % "0.7"
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
