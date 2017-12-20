lazy val root = (project in file(".")).dependsOn(sbtJsonPlugin)

lazy val sbtJsonPlugin = RootProject(uri("git://github.com/battermann/sbt-json#develop"))
