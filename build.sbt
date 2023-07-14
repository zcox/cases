Global / onChangedBuildSource := ReloadOnSourceChanges

val Http4sVersion = "0.23.14"
val CirceVersion = "0.14.2"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.11"
val MunitCatsEffectVersion = "1.0.7"
val ScalacheckMagnoliaVersion = "0.6.0"
val Messagedb4sVersion = "0.1.0-SNAPSHOT"
val FlywayVersion = "9.0.2"
val PostgresVersion = "42.5.0"

lazy val root = (project in file("."))
  .settings(
    organization := "com.banno",
    name := "cases",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "default"         %% "messagedb4s"         % Messagedb4sVersion,
      "org.flywaydb"    %  "flyway-core"         % FlywayVersion,
      "org.postgresql"  %  "postgresql"          % PostgresVersion,
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.scalameta"   %% "munit-scalacheck"    % MunitVersion           % Test,
      "com.github.chocpanda" %% "scalacheck-magnolia" % ScalacheckMagnoliaVersion % Test,
      "org.typelevel"   %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion         % Runtime,
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
