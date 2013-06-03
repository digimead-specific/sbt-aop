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
import sbt.std._
import sbt.Configurations.Compile
import sbt.Keys._
import sbt.Project.Initialize
import Keys._
import java.io.File
import org.aspectj.bridge.{ AbortException, IMessageHandler, IMessage, MessageHandler }
import org.aspectj.tools.ajc.Main
import sbt.aspectj.nested.argument.Weave
import sbt.aspectj.nested.argument.Generic
import sbt.aspectj.nested.argument.Weave

object Plugin extends sbt.Plugin {
  implicit def option2rich[T](option: Option[T]): RichOption[T] = new RichOption(option)
  def logPrefix(name: String) = "[AspectJ nested:%s] ".format(name)

  lazy val defaultSettings = inConfig(AspectJConf)(Seq(
    aspectjVersion := "1.7.2",
    showWeaveInfo := false,
    verbose := false,
    sourceLevel := "-1.6",
    aspectjSource <<= (sourceDirectory in Compile) / "aspectj",
    sourceDirectories <<= Seq(aspectjSource).join,
    outputDirectory <<= crossTarget / "aspectj",
    //managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars,
    //dependencyClasspath <<= dependencyClasspath in Compile,
    //compiledClasses <<= compileClasses,
    aspectjClasspath <<= (fullClasspath in Compile) map { _.files },
    aspectjWeaveArg <<= aspectjWeaveArgTask,
    aspectjGenericArg <<= aspectjGenericArgTask,
    aspectjOptions <<= baseOptionsSettings,
    inputs := Seq.empty,
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources <<= sourcesTask,
    binaryAspects := Seq.empty,
    aspects <<= aspectsTask,
    aspectjFilter := { (f, a) => a },
    aspectjMappings <<= aspectMappingsTask,
    copyResources <<= copyResourcesTask,
    weave <<= weaveTask,
    aspectClassesDirectory <<= outputDirectory / "classes",
    outxml := true,
    compileAspects <<= compileAspectsTask,
    enableProducts := false,
    aspectProducts <<= compileIfEnabled,
    //products in Compile <<= combineProducts,
    weaveAgentJar <<= javaAgent,
    weaveAgentOptions <<= javaAgentOptions)) ++ dependencySettings
  def dependencySettings = Seq(
    ivyConfigurations += AspectJConf,
    libraryDependencies <++= (aspectjVersion in AspectJConf) { version =>
      Seq(
        "org.aspectj" % "aspectjtools" % version % AspectJConf.name,
        "org.aspectj" % "aspectjweaver" % version % AspectJConf.name,
        "org.aspectj" % "aspectjrt" % version % AspectJConf.name)
    })

  //def compileClasses = (compile in Compile, compileInputs in Compile) map {
  //  (_, inputs) => inputs.config.classesDirectory
  //}

  //def combineClasspaths = (managedClasspath, dependencyClasspath, compiledClasses) map {
  //  (mcp, dcp, classes) => Attributed.blank(classes) +: (mcp ++ dcp)
  //}

  /** Collect mappings from available aspects and filters */
  def aspectMappingsTask = (inputs, aspects, aspectjFilter, outputDirectory) map ((in, aspects, filter, dir) =>
    in map { input => Mapping(input, filter(input, aspects), instrumented(input, dir)) })
  /** Collect all available aspects */
  def aspectsTask = (sources, binaryAspects) map ((s, b) =>
    (s map { Aspect(_, binary = false) }) ++ (b map { Aspect(_, binary = true) }))
  /** Build baseOptions settings value. */
  def baseOptionsSettings = (showWeaveInfo, verbose, sourceLevel) { (info, verbose, level) =>
    (if (info) Seq("-showWeaveInfo") else Seq.empty[String]) ++
      (if (verbose) Seq("-verbose") else Seq.empty[String]) ++
      Seq(level)
  }
  /** Collect AspectJ sources */
  def sourcesTask = (sourceDirectories, includeFilter, excludeFilter) map (
    (dirs, include, exclude) => dirs.descendantsExcept(include, exclude).get)
  /** Weave inputs */
  def weaveTask = (aspectjWeaveArg, aspectjGenericArg) map { (aspectjWeaveArg, aspectjGenericArg) =>
    aspectjGenericArg.log.debug(logPrefix(aspectjGenericArg.name) + "weave")
    AspectJ.weave(aspectjWeaveArg)(aspectjGenericArg)
  }
  /** Aggregate parameters that required by 'weave' task */
  def aspectjWeaveArgTask = (cacheDirectory, aspectjMappings, aspectjOptions, aspectjClasspath, copyResources) map (
    (cacheDirectory, aspectjMappings, aspectjOptions, aspectjClasspath, _) =>
      Weave(cacheDirectory, aspectjMappings, aspectjOptions, aspectjClasspath))
  /** Aggregate parameters for generic task */
  def aspectjGenericArgTask = (state, streams, thisProjectRef) map (
    (state, streams, thisProjectRef) => Generic(state, thisProjectRef, Some(streams)))

  def instrumented(input: File, outputDir: File): File = {
    val (base, ext) = input.baseAndExt
    val outputName = {
      if (ext.isEmpty) base + "-instrumented"
      else base + "-instrumented" + "." + ext
    }
    outputDir / outputName
  }

  def copyResourcesTask = (cacheDirectory, aspectjMappings, copyResources in Compile) map {
    (cache, mappings, resourceMappings) =>
      val cacheFile = cache / "aspectj" / "resource-sync"
      val mapped = mappings flatMap { mapping =>
        if (mapping.in.isDirectory) {
          resourceMappings map (_._2) x rebase(mapping.in, mapping.out)
        } else Seq.empty
      }
      Sync(cacheFile)(mapped)
      mapped
  }

  def useInstrumentedJars(config: Configuration) = useInstrumentedClasses(config)
  def useInstrumentedClasses(config: Configuration) = {
    (sbt.Keys.fullClasspath in config, aspectjMappings in AspectJConf, Keys.weave in AspectJConf) map {
      (cp, mappings, woven) => AspectJ.insertInstrumentedClasses(cp.files, mappings)
    }
  }

  def compileAspectsTask = (sources, outxml, aspectjClasspath, aspectjOptions, aspectClassesDirectory, cacheDirectory, name, streams, state, streams, thisProjectRef) map {
    (aspects, outxml, classpath, opts, dir, cacheDir, name, s, state, streams, thisProjectRef) =>
      implicit val arg = Generic(state, thisProjectRef, Some(streams))
      val cachedCompile = FileFunction.cached(cacheDir / "ajc-compile", FilesInfo.hash) { _ =>
        val sourceCount = Util.counted("AspectJ source", "", "s", aspects.length)
        sourceCount foreach { count => s.log.info("Compiling %s to %s..." format (count, dir)) }
        val options = ajcCompileOptions(aspects, outxml, classpath, opts, dir).toArray
        AspectJ.ajcRunMain(options)
        dir.***.get.toSet
      }
      val inputs = aspects.toSet
      cachedCompile(inputs)
      dir
  }

  def ajcCompileOptions(aspects: Seq[File], outxml: Boolean, classpath: Seq[File], baseOptions: Seq[String], output: File): Seq[String] = {
    baseOptions ++
      Seq("-XterminateAfterCompilation") ++
      Seq("-classpath", classpath.absString) ++
      Seq("-d", output.absolutePath) ++
      (if (outxml) Seq("-outxml") else Seq.empty[String]) ++
      aspects.map(_.absolutePath)
  }

  def compileIfEnabled = (enableProducts, compileAspects.task) flatMap {
    (enable, compile) => if (enable) (compile map { dir => Seq(dir) }) else task { Seq.empty[File] }
  }

  //def combineProducts = (products in Compile, aspectProducts) map { _ ++ _ }

  def javaAgent = update map { report =>
    report.matching(moduleFilter(organization = "org.aspectj", name = "aspectjweaver")) headOption
  }

  def javaAgentOptions = weaveAgentJar map { weaver => weaver.toSeq map { "-javaagent:" + _ } }

  class RichOption[T](option: Option[T]) {
    def getOrThrow(onError: String) = option getOrElse { throw new NoSuchElementException(onError) }
  }
}
