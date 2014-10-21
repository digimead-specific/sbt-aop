import sbt.aspectj.nested._

AspectJNestedRT

name := "Simple"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

logLevel := Level.Debug


AJKey.aspectjInputs in AJConf <<= (classDirectory in Compile) map (x => Seq(x))

products in Compile <<= products in AJConf

products in Runtime <<= products in Compile
