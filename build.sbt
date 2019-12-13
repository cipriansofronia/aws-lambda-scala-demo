javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file("."))
  .settings(
    name := "hack9-lambda-1",
    version := "1.0",
    scalaVersion := "2.12.9",
    retrieveManaged := true,
    libraryDependencies ++= Seq(
      "io.github.howardjohn" %% "http4s-lambda" % "0.4.0",
      "org.http4s" %% "http4s-dsl" % "0.21.0-M6"
    )
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*)                        => MergeStrategy.discard
  case _                                                  => MergeStrategy.first
}