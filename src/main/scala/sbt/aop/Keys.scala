/**
 * sbt-aop - AspectJ for nested projects.
 *
 * Copyright (c) 2013-2015 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.aop

import java.io.File

import sbt._
import sbt.aop.argument.Generic
import sbt.aop.argument.Weave

object Keys {
  def AOPCompileConf = config("aopCompile").hide
  def AOPTestConf = config("aopTest").hide

  val aopAspects = TaskKey[Seq[Aspect]]("aopAspects", "All aspects, both source and binary.")
  val aopBinary = TaskKey[Seq[File]]("aopBinary", "Binary aspects passed to the -aspectpath AspectJ option.")
  val aopClasspath = TaskKey[Seq[File]]("aopClasspath", "The classpath used for running AspectJ.")
  val aopFilter = SettingKey[(File, Seq[Aspect]) ⇒ Seq[Aspect]]("aop-filter", "Filter for aspects. Used to create aspect mappings.")
  val aopGenericArg = TaskKey[Generic]("aopGenericArguments", "Project settings for generic task.")
  val aopInputs = TaskKey[Seq[File]]("aopInputs", "The jars or classes directories to weave.")
  val aopInputResources = TaskKey[Seq[(File, File)]]("aopInputResources", "The list of project resources that are copied to weaved directories with respect to aspectjMappings.")
  val aopMappings = TaskKey[Seq[Mapping]]("aopMappings", "Mappings from inputs, through aspects, to outputs.")
  val aopOptions = TaskKey[Seq[String]]("aopOptions", "The showWeaveInfo, verbose, and sourceLevel settings as options.")
  val aopOutput = SettingKey[File]("aopOutput", "Output directory for AspectJ instrumentation.")
  val aopOutxml = SettingKey[Boolean]("aopOutXML")
  val aopShowWeaveInfo = SettingKey[Boolean]("aopShowWeaveInfo", "Enable the -showWeaveInfo AspectJ option.")
  val aopSource = SettingKey[File]("aopSource", "Source directory for aspects.")
  val aopSourceLevel = TaskKey[Option[String]]("aopSourceLevel", "The AspectJ source level option.")
  val aopVerbose = SettingKey[Boolean]("aopVerbose", "Enable the -verbose AspectJ option.")
  val aopWeaveArg = TaskKey[Weave]("aopWeaveArguments", "Project settings for weave task.")
}
