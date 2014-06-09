/**
 * sbt-aspectj-nested - AspectJ for nested projects.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
 * Based on aspectj-sbt-plugin by Typesafe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.aspectj.nested

import java.io.File

import sbt._
import sbt.aspectj.nested.argument.Generic
import sbt.aspectj.nested.argument.Weave

//val aspectjClassesDirectory = SettingKey[File]("aspectj-classes-directory")
//  val compiledClasses = TaskKey[File]("compiled-classes", "The compile classes directory (after compile).")
// -> inputClasses
// -> inputRules
// load-time weaving support - compiling and including aspects in package-bin
//val compileAspects = TaskKey[File]("compile-aspects", "Compile aspects for load-time weaving.")
//val enableProducts = TaskKey[Boolean]("enableProducts", "Enable or disable compiled aspects in compile products.")
//val aspectProducts = TaskKey[Seq[File]]("aspect-products", "Optionally compiled aspects (if produce-aspects).")

object Keys {
  def AspectJConf = config("aspectj").hide

  val aspectjAspects = TaskKey[Seq[Aspect]]("aspectjAspects", "All aspects, both source and binary.")
  val aspectjBinary = TaskKey[Seq[File]]("aspectjBinary", "Binary aspects passed to the -aspectpath AspectJ option.")
  val aspectjClasspath = TaskKey[Seq[File]]("aspectjClasspath", "The classpath used for running AspectJ.")
  val aspectjFilter = SettingKey[(File, Seq[Aspect]) => Seq[Aspect]]("aspectj-filter", "Filter for aspects. Used to create aspect mappings.")
  val aspectjGenericArg = TaskKey[Generic]("aspectjGenericArguments", "Project settings for generic task.")
  val aspectjInputs = TaskKey[Seq[File]]("aspectjInputs", "The jars or classes directories to weave.")
  val aspectjInputResources = TaskKey[Seq[(File,File)]]("aspectjInputResources", "The list of project resources that are copied to weaved directories with respect to aspectjMappings.")
  val aspectjMappings = TaskKey[Seq[Mapping]]("aspectjMappings", "Mappings from inputs, through aspects, to outputs.")
  val aspectjOptions = TaskKey[Seq[String]]("aspectjOptions", "The showWeaveInfo, verbose, and sourceLevel settings as options.")
  val aspectjOutput = SettingKey[File]("aspectjOutput", "Output directory for AspectJ instrumentation.")
  val aspectjOutxml = SettingKey[Boolean]("aspectjOutXML")
  val aspectjShowWeaveInfo = SettingKey[Boolean]("aspectjShowWeaveInfo", "Enable the -showWeaveInfo AspectJ option.")
  val aspectjSource = SettingKey[File]("aspectjSource", "Source directory for aspects.")
  val aspectjSourceLevel = TaskKey[Option[String]]("aspectjSourceLevel", "The AspectJ source level option.")
  val aspectjVerbose = SettingKey[Boolean]("aspectjVerbose", "Enable the -verbose AspectJ option.")
  val aspectjVersion = SettingKey[String]("aspectjVersion", "AspectJ version to use.")
  val aspectjWeave = TaskKey[Seq[File]]("aspectjWeave", "Weave with AspectJ.")
  val aspectjWeaveAgentJar = TaskKey[Option[File]]("aspectjWeaveAgentJar", "Location of AspectJ weaver.")
  val aspectjWeaveAgentOptions = TaskKey[Seq[String]]("aspectjWeaveAgentOptions", "JVM options for AspectJ java agent.")
  val aspectjWeaveArg = TaskKey[Weave]("aspectjWeaveArguments", "Project settings for weave task.")
}
