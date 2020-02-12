lazy val catsVersion   = "2.0.0"
lazy val circeVersion  = "0.12.2"
lazy val http4sVersion = "0.20.11"
lazy val tsecVersion = "0.0.1-M11"

name := "todo"
version := "0.1"
scalaVersion := "2.12.10"
resolvers += Resolver.sonatypeRepo("releases")
scalacOptions += "-Ypartial-unification"

fork in run := true

libraryDependencies ++= Seq(

  "org.typelevel"  %% "cats-core"            % catsVersion,
  "org.typelevel"  %% "cats-effect"          % catsVersion,
  "org.tpolecat"   %% "doobie-core"          % "0.7.1",
  "org.xerial"     %  "sqlite-jdbc"          % "3.28.0",
  "io.circe"       %% "circe-core"           % circeVersion,
  "io.circe"       %% "circe-generic"        % circeVersion,
  "io.circe"       %% "circe-parser"         % circeVersion,
  "org.http4s"     %% "http4s-blaze-server"  % http4sVersion,
  "org.http4s"     %% "http4s-circe"         % http4sVersion,
  "org.http4s"     %% "http4s-dsl"           % http4sVersion,
  "org.http4s"     %% "rho-swagger"          % "0.20.0-M1",
  "org.scalatest"  %% "scalatest"            % "3.0.8" % Test,

 "io.github.jmcardon" %% "tsec-common" % tsecVersion,
 "io.github.jmcardon" %% "tsec-password" % tsecVersion,
 "io.github.jmcardon" %% "tsec-cipher-jca" % tsecVersion,
 "io.github.jmcardon" %% "tsec-cipher-bouncy" % tsecVersion,
 "io.github.jmcardon" %% "tsec-mac" % tsecVersion,
 "io.github.jmcardon" %% "tsec-signatures" % tsecVersion,
 "io.github.jmcardon" %% "tsec-hash-jca" % tsecVersion,
 "io.github.jmcardon" %% "tsec-hash-bouncy" % tsecVersion,
 "io.github.jmcardon" %% "tsec-libsodium" % tsecVersion,
 "io.github.jmcardon" %% "tsec-jwt-mac" % tsecVersion,
 "io.github.jmcardon" %% "tsec-jwt-sig" % tsecVersion,
  "io.github.jmcardon" %% "tsec-http4s" % tsecVersion
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
