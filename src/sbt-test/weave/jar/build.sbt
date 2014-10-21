import sbt.aspectj.nested._

AspectJNestedRT

name := "Simple"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

logLevel := Level.Debug


AJKey.aspectjInputs in AJConf <<= update map { report =>
  report.matching(moduleFilter(organization = "com.typesafe.akka", name = "akka-actor*"))
}

fullClasspath in Runtime <<= (products in AJConf, fullClasspath in Runtime) map { (a,b) => a.map(Attributed.blank) ++ b }
