import sbt.aop._

AOPRT

name := "Simple"

version := "1.0.0.0-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit")

//logLevel := Level.Debug


AOPKey.aopInputs in AOPCompile <<= (classDirectory in Compile) map {(a) => Seq(a)}
AOPKey.aopInputs in AOPTest <<= (classDirectory in Compile, classDirectory in Test) map {(a,b) => Seq(a,b)}

AOPKey.aopFilter in AOPCompile := { (input, aspects) =>
  input.name match {
    case "classes" => aspects filter (_.toString.contains("SampleAspect.aj"))
    case "test-classes" => aspects filter (_.toString.contains("SampleAspect.aj"))
    case other => Seq.empty
  }
}

AOPKey.aopFilter in AOPTest := { (input, aspects) =>
  input.name match {
    case "classes" => aspects filter (_.toString.contains("SampleAspect.aj"))
    case "test-classes" => aspects filter (_.toString.contains("SampleAspect.aj"))
    case other => Seq.empty
  }
}

products in Runtime <<= products in Compile

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
