import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}

lazy val root = (project in file("."))
  .enablePlugins(SbtJsonPlugin)
  .settings(
    inThisBuild(
      List(
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
    libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.28.20",
    libraryDependencies += "com.typesafe" % "config" % "1.3.1",
    libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.7",
    libraryDependencies += "com.openhtmltopdf" % "openhtmltopdf-parent" % "0.0.1-RC12",
    jsValueFilter := allJsValues.exceptEmptyArrays,
    generateConfigJson,
    (compile in Compile) := (compile in Compile)
      .dependsOn(generateConfigJsonTask)
      .value
  )

val generateConfigJsonTask =
  TaskKey[Unit]("generateConfigJson", "Generate JSON config sample.")

val generateConfigJson = generateConfigJsonTask := {
  val config = ConfigFactory.parseFile(
    (baseDirectory in Compile).value / "src" / "main" / "resources" / "application.conf")
  val content = config.root().render(ConfigRenderOptions.concise())
  val file = (baseDirectory in Compile).value / "src" / "main" / "resources" / "json" / "EnvironmentConfig.json"
  IO.write(file, content)
}
