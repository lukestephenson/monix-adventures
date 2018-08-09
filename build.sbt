name := "monix-adventures"

version := "0.1"

scalaVersion := "2.12.6"

val specs2Version = "4.3.2"
val monixVersion = "3.0.0-RC1"

libraryDependencies ++= Seq(
  "io.monix"                        %% "monix"                                 % monixVersion,
  "org.typelevel"                   %% "cats-core"                             % "1.1.0",
  "org.specs2"                      %% "specs2-core"                           % specs2Version        % "test",
  "org.specs2"                      %% "specs2-matcher-extra"                  % specs2Version        % "test"
)

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-Xfatal-warnings",
  "-Ywarn-unused-import",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Xlint",
  "-language:higherKinds",
  "-language:postfixOps")
