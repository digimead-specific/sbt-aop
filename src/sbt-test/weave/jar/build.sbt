import sbt.aop._

AOPRT

name := "Simple"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

logLevel := Level.Debug


AOPKey.aopInputs in AOPCompile <<= update map { report =>
  report.matching(moduleFilter(organization = "com.typesafe.akka", name = "akka-actor*"))
}

fullClasspath in Runtime <<= (products in Compile, fullClasspath in Runtime) map { (a,b) => a.map(Attributed.blank) ++ b }
