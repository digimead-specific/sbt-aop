
sbtPlugin := true

organization := "org.digimead"

name := "sbt-aspectj-nested"

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.7.3"

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m")

sourceGenerators in Compile <+= (sbtVersion, sourceDirectory in Compile, sourceManaged in Compile) map { (v, sourceDirectory, sourceManaged) =>
  val interface = v.split("""\.""").take(2).mkString(".")
  val source = sourceDirectory / ".." / "patch" / interface
  val generated = (PathFinder(source) ***) x Path.rebase(source, sourceManaged)
  IO.copy(generated, true, false)
  generated.map(_._2).filter(_.getName endsWith ".scala")
}
