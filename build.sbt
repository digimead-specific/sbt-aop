//
// Copyright (c) 2013-2015 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

sbtPlugin := true

organization := "org.digimead"

name := "sbt-aspectj-nested"

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

libraryDependencies += "org.aspectj" % "aspectjtools" % "1.8.4"

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := Seq("-Xms512m", "-Xmx512m", "-XX:MaxPermSize=256m")

sourceGenerators in Compile <+= (sbtVersion, sourceDirectory in Compile, sourceManaged in Compile) map { (v, sourceDirectory, sourceManaged) =>
  val interface = v.split("""\.""").take(2).mkString(".")
  val source = sourceDirectory / ".." / "patch" / interface
  val generated = (PathFinder(source) ***) x Path.rebase(source, sourceManaged)
  IO.copy(generated, true, false)
  generated.map(_._2).filter(_.getName endsWith ".scala")
}
