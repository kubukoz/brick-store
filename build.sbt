val macroParadise    = ("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)
val kindProjector    = "org.spire-math" %% "kind-projector" % "0.9.10"
val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.0"

val plugins = List(macroParadise, kindProjector, betterMonadicFor)

val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

val http4s = Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.20.0",
  "org.http4s" %% "http4s-circe"        % "0.20.0",
  "org.http4s" %% "http4s-dsl"          % "0.20.0",
  "io.circe"   %% "circe-derivation"    % "0.11.0-M1" % Compile,
  "io.circe"   %% "circe-fs2"           % "0.11.0"
)

val cats =
  Seq(
    "org.typelevel"     %% "cats-core"      % "1.6.0",
    "io.chrisdavenport" %% "cats-par"       % "0.2.1",
    "org.typelevel"     %% "cats-effect"    % "1.2.0",
    "io.chrisdavenport" %% "log4cats-slf4j" % "0.3.0"
  )

val doobie = Seq(
  "org.tpolecat"   %% "doobie-core"      % "0.6.0",
  "org.tpolecat"   %% "doobie-postgres"  % "0.6.0",
  "org.tpolecat"   %% "doobie-hikari"    % "0.6.0",
  "org.tpolecat"   %% "doobie-scalatest" % "0.6.0",
  "org.postgresql" % "postgresql"        % "42.2.5",
  "org.flywaydb"   % "flyway-core"       % "5.2.4"
)

val chimney = "io.scalaland" %% "chimney" % "0.3.1"

val pureconfig = Seq(
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.10.2",
  "com.github.pureconfig" %% "pureconfig-enumeratum"  % "0.10.2",
  "com.github.pureconfig" %% "pureconfig-generic"     % "0.10.2"
)

val enumeratum = Seq(
  "com.beachape" %% "enumeratum-circe" % "1.5.21"
)

val macwire = Seq(
  "com.softwaremill.macwire" %% "macros" % "2.3.1" % Provided,
  "com.softwaremill.macwire" %% "util"   % "2.3.1",
  "com.softwaremill.macwire" %% "proxy"  % "2.3.1"
)

lazy val root = (project in file(".")).settings(
  organization := "org.typelevel",
  name := "brick-store",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.7",
  scalacOptions ++= Options.flags,
  libraryDependencies ++= plugins
    .map(compilerPlugin) ++ http4s ++ doobie ++ cats ++ macwire ++ enumeratum ++ pureconfig ++ Seq(logback, chimney)
)
