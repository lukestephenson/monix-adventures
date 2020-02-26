name := "monix-adventures"

version := "0.1"

scalaVersion := "2.13.1"

val specs2Version = "4.8.3"
val monixVersion = "3.1.0"

libraryDependencies ++= Seq(
  "io.monix"                        %% "monix"                                 % monixVersion,
  "org.typelevel"                   %% "cats-core"                             % "2.1.1",
  "org.specs2"                      %% "specs2-core"                           % specs2Version        % "test",
  "org.specs2"                      %% "specs2-matcher-extra"                  % specs2Version        % "test"
)

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Xlint",
  "-language:higherKinds",
  "-language:postfixOps")
