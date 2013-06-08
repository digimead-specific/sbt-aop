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
  def AspectJConf = config("aspectj") hide

  val aspectjAspects = TaskKey[Seq[Aspect]]("aspectj-aspects", "All aspects, both source and binary.")
  val aspectjBinary = TaskKey[Seq[File]]("aspectj-binary", "Binary aspects passed to the -aspectpath AspectJ option.")
  val aspectjClasspath = TaskKey[Seq[File]]("aspectj-classpath", "The classpath used for running AspectJ.")
  val aspectjFilter = SettingKey[(File, Seq[Aspect]) => Seq[Aspect]]("aspectj-filter", "Filter for aspects. Used to create aspect mappings.")
  val aspectjGenericArg = TaskKey[Generic]("aspectj-generic-arguments", "Project settings for generic task.")
  val aspectjInputs = TaskKey[Seq[File]]("aspectj-inputs", "The jars or classes directories to weave.")
  val aspectjInputResources = TaskKey[Seq[(File,File)]]("aspectj-input-resources", "The list of project resources that are copied to weaved directories with respect to aspectjMappings.")
  val aspectjMappings = TaskKey[Seq[Mapping]]("aspectj-mappings", "Mappings from inputs, through aspects, to outputs.")
  val aspectjOptions = SettingKey[Seq[String]]("aspectj-options", "The showWeaveInfo, verbose, and sourceLevel settings as options.")
  val aspectjOutput = SettingKey[File]("aspectj-output", "Output directory for AspectJ instrumentation.")
  val aspectjOutxml = SettingKey[Boolean]("aspectj-outxml")
  val aspectjShowWeaveInfo = SettingKey[Boolean]("aspectj-show-weave-info", "Enable the -showWeaveInfo AspectJ option.")
  val aspectjSource = SettingKey[File]("aspectj-source", "Source directory for aspects.")
  val aspectjSourceLevel = SettingKey[String]("aspectj-source-level", "The AspectJ source level option.")
  val aspectjVerbose = SettingKey[Boolean]("aspectj-verbose", "Enable the -verbose AspectJ option.")
  val aspectjVersion = SettingKey[String]("aspectj-version", "AspectJ version to use.")
  val aspectjWeave = TaskKey[Seq[File]]("aspectj-weave", "Weave with AspectJ.")
  val aspectjWeaveAgentJar = TaskKey[Option[File]]("aspectj-weave-agent-jar", "Location of AspectJ weaver.")
  val aspectjWeaveAgentOptions = TaskKey[Seq[String]]("aspectj-weave-agent-options", "JVM options for AspectJ java agent.")
  val aspectjWeaveArg = TaskKey[Weave]("aspectj-weave-arguments", "Project settings for weave task.")
}
