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

import sbt._
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main
import sbt.Keys._
import sbt.Project.Initialize

import java.io.File

object Keys {
  def AspectJConf = config("aspectj") hide

  val aspectjFilter = SettingKey[(File, Seq[Aspect]) => Seq[Aspect]]("aspect-filter", "Filter for aspects. Used to create aspect mappings.")
  val aspectjSource = SettingKey[File]("aspectj-source", "Source directory for aspects.")
  val aspectjVersion = SettingKey[String]("aspectj-version", "AspectJ version to use.")
  val outputDirectory = SettingKey[File]("output-directory", "Output directory for AspectJ instrumentation.")
  val showWeaveInfo = SettingKey[Boolean]("show-weave-info", "Enable the -showWeaveInfo AspectJ option.")
  val sourceLevel = SettingKey[String]("source-level", "The AspectJ source level option.")
  val verbose = SettingKey[Boolean]("verbose", "Enable the -verbose AspectJ option.")

//  val compiledClasses = TaskKey[File]("compiled-classes", "The compile classes directory (after compile).")
  // -> inputClasses
  val aspectjClasspath = TaskKey[Seq[File]]("aspectj-classpath", "The classpath used for running AspectJ.")
  val baseOptions = SettingKey[Seq[String]]("base-options", "The showWeaveInfo, verbose, and sourceLevel settings as options.")
  // -> inputRules
  val inputs = TaskKey[Seq[File]]("aspectj-inputs", "The jars or classes directories to weave.")
  val binaryAspects = TaskKey[Seq[File]]("binary-aspects", "Binary aspects passed to the -aspectpath AspectJ option.")
  val aspects = TaskKey[Seq[Aspect]]("aspectj-aspects", "All aspects, both source and binary.")
  val aspectMappings = TaskKey[Seq[Mapping]]("aspectj-mappings", "Mappings from inputs, through aspects, to outputs.")

  val weave = TaskKey[Seq[File]]("weave", "Weave with AspectJ.")

  // load-time weaving support - compiling and including aspects in package-bin
  val aspectClassesDirectory = SettingKey[File]("aspect-classes-directory")
  val outxml = SettingKey[Boolean]("outxml")
  val compileAspects = TaskKey[File]("compile-aspects", "Compile aspects for load-time weaving.")
  val enableProducts = TaskKey[Boolean]("enableProducts", "Enable or disable compiled aspects in compile products.")
  val aspectProducts = TaskKey[Seq[File]]("aspect-products", "Optionally compiled aspects (if produce-aspects).")
  val weaveAgentJar = TaskKey[Option[File]]("weave-agent-jar", "Location of AspectJ weaver.")
  val weaveAgentOptions = TaskKey[Seq[String]]("weave-agent-options", "JVM options for AspectJ java agent.")
}
