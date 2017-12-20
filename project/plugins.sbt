lazy val root = (project in file(".")).dependsOn(sbtJsonPlugin)

lazy val sbtJsonPlugin = RootProject(uri("git://github.com/battermann/sbt-json#develop"))

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")