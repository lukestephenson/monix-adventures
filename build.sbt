name := "monix-adventures"

version := "0.1"

scalaVersion := "2.12.5"

val specs2Version = "3.9.1"
val monixVersion = "2.3.2"

libraryDependencies ++= Seq(
  "io.monix"                        %% "monix"                                 % monixVersion,
  "io.monix"                        %% "monix-cats"                            % monixVersion,
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
