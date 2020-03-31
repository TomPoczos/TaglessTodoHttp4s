lazy val catsVersion   = "2.0.0"
lazy val circeVersion  = "0.12.2"
lazy val http4sVersion = "0.20.11"
lazy val tsecVersion = "0.0.1-M11"

name := "todo"
version := "0.1"
scalaVersion := "2.12.10"
resolvers += Resolver.sonatypeRepo("releases")
scalacOptions += "-Ypartial-unification"

addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel"  %% "kind-projector"     % "0.10.3")
addCompilerPlugin("org.augustjune" %% "context-applied"    % "0.1.2")


fork in run := true

libraryDependencies ++= Seq(

  "org.typelevel"      %% "cats-core"            % catsVersion,
  "org.typelevel"      %% "cats-effect"          % catsVersion,
  "org.tpolecat"       %% "doobie-core"          % "0.7.1",
  "org.xerial"         %  "sqlite-jdbc"          % "3.28.0",
  "io.circe"           %% "circe-core"           % circeVersion,
  "io.circe"           %% "circe-generic"        % circeVersion,
  "io.circe"           %% "circe-parser"         % circeVersion,
  "org.http4s"         %% "http4s-blaze-server"  % http4sVersion,
  "org.http4s"         %% "http4s-circe"         % http4sVersion,
  "org.http4s"         %% "http4s-dsl"           % http4sVersion,
  "org.http4s"         %% "rho-swagger"          % "0.20.0-M1",
  "org.scalatest"      %% "scalatest"            % "3.0.8" % Test,
  "org.reactormonk"    %% "cryptobits"           % "1.3",
  "com.github.t3hnar"  %% "scala-bcrypt"         % "4.1"
)