
sbtPlugin := true

organization := "org.digimead"

name := "sbt-aspectj-nested"

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.7.2"

publishMavenStyle := false

publishTo <<= (version) { v =>
  def scalasbt(repo: String) = ("scalasbt " + repo, "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-" + repo)
  val (name, repo) = if (v.endsWith("-SNAPSHOT")) scalasbt("snapshots") else scalasbt("releases")
  Some(Resolver.url(name, url(repo))(Resolver.ivyStylePatterns))
}

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m")
