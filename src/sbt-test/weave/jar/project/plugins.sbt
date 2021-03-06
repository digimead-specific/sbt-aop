resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "oss sonatype" at "https://oss.sonatype.org/content/groups/public/",
  "digimead-maven" at "http://commondatastorage.googleapis.com/maven.repository.digimead.org/"
)

libraryDependencies <+= (sbtBinaryVersion in update, scalaBinaryVersion in update, baseDirectory) { (sbtV, scalaV, base) =>
  Defaults.sbtPluginExtra("org.digimead" % "sbt-aop" %
    scala.io.Source.fromFile(base / Seq("..", "version").mkString(java.io.File.separator)).mkString.trim, sbtV, scalaV) }
