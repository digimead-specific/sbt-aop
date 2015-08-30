import sbt.aop._

AOPRT

name := "Simple"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Debug


AOPKey.aopInputs in AOPCompile <<= (classDirectory in Compile) map (x => Seq(x))

products in Runtime <<= products in Compile
