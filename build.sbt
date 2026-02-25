val Http4sVersion = "0.23.33"
val CirceVersion = "0.14.14"
val MunitVersion = "1.2.2"
val LogbackVersion = "1.5.32"
val MunitCatsEffectVersion = "2.1.0"
val Fs2CsvVersion = "1.12.0"
val Circe = "0.14.15"

lazy val root = (project in file("."))
  .settings(
    organization := "tickets4sale",
    name := "stagekeeper",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "3.3.6",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.gnieh"       %% "fs2-data-csv"        % Fs2CsvVersion,
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect"   % MunitCatsEffectVersion % Test,
      "io.circe"        %% "circe-parser"        % Circe                  % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion         % Runtime,
    ),
    assembly / assemblyMergeStrategy := {
      case "module-info.class" => MergeStrategy.discard
      case x => (assembly / assemblyMergeStrategy).value.apply(x)
    }
  )
