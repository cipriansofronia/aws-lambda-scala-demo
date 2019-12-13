javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file("."))
  .settings(
    name := "hack9-lambda-1",
    version := "1.0",
    scalaVersion := "2.12.9",
    retrieveManaged := true,
    libraryDependencies += "io.github.mkotsur" %% "aws-lambda-scala" % "0.1.1"
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*)                        => MergeStrategy.discard
  case _                                                  => MergeStrategy.first
}