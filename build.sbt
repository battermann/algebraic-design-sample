lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      scalaVersion := "2.12.4",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "Transaction Analyzer",
    scalacOptions += "-Ypartial-unification",
    scalacOptions += "-feature",
    libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.0-RC1",
    libraryDependencies += "com.github.alexarchambault" %% "case-app" % "1.2.0-M4",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "0.5",
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.18.0",
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.28.14"
  )
