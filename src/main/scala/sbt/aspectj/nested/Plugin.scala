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
    baseOptions <<= baseOptionsSettings,
    inputs := Seq(new File(".")),
    includeFilter := "*.aj",
    excludeFilter := HiddenFileFilter,
    sources <<= sourcesTask,
    binaryAspects := Seq.empty,
    aspects <<= aspectsTask,
    aspectjFilter := { (f, a) => a },
    aspectMappings <<= aspectMappingsTask,
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
  def weaveTask = (cacheDirectory, aspectMappings, baseOptions, aspectjClasspath, copyResources, state, streams, thisProjectRef) map {
    (cacheDirectory, aspectMappings, baseOptions, aspectjClasspath, _, state, streams, thisProjectRef) =>
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
      arg.log.debug(logPrefix(arg.name) + "weave")
      AspectJ.weave(cacheDirectory, aspectMappings, baseOptions, aspectjClasspath)
  }

  def instrumented(input: File, outputDir: File): File = {
    val (base, ext) = input.baseAndExt
    val outputName = {
      if (ext.isEmpty) base + "-instrumented"
      else base + "-instrumented" + "." + ext
    }
    outputDir / outputName
  }

  def copyResourcesTask = (cacheDirectory, aspectMappings, copyResources in Compile) map {
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
    (sbt.Keys.fullClasspath in config, aspectMappings in AspectJConf, Keys.weave in AspectJConf) map {
      (cp, mappings, woven) => AspectJ.insertInstrumentedClasses(cp.files, mappings)
    }
  }

  def compileAspectsTask = (sources, outxml, aspectjClasspath, baseOptions, aspectClassesDirectory, cacheDirectory, name, streams, state, streams, thisProjectRef) map {
    (aspects, outxml, classpath, opts, dir, cacheDir, name, s, state, streams, thisProjectRef) =>
      implicit val arg = TaskArgument(state, thisProjectRef, Some(streams))
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

  /** Consolidated argument with all required information */
  case class TaskArgument(
    /** The data structure representing all command execution information. */
    state: State,
    // It is more reasonable to pass it from SBT than of fetch it directly.
    /** The reference to the current project. */
    thisProjectRef: ProjectRef,
    /** The structure that contains reference to log facilities. */
    streams: Option[sbt.std.TaskStreams[ScopedKey[_]]] = None) {
    /** Extracted state projection */
    lazy val extracted = Project.extract(state)
    /** SBT logger */
    val log = streams.map(_.log) getOrElse {
      // Heh, another feature not bug? SBT 0.12.3
      // MultiLogger and project level is debug, but ConsoleLogger is still info...
      // Don't care about CPU time
      val globalLoggin = (state.getClass().getDeclaredMethods().find(_.getName() == "globalLogging")) match {
        case Some(method) =>
          // SBT 0.12+
          method.invoke(state).asInstanceOf[GlobalLogging]
        case None =>
          // SBT 0.11.x
          CommandSupport.asInstanceOf[{ def globalLogging(s: State): GlobalLogging }].globalLogging(state)
      }
      import globalLoggin._
      full match {
        case logger: AbstractLogger =>
          val level = logLevel in thisScope get extracted.structure.data
          level.foreach(logger.setLevel(_)) // force level
          logger
        case logger =>
          logger
      }
    }
    /** Current project name */
    val name: String = (sbt.Keys.name in thisScope get extracted.structure.data) getOrElse thisProjectRef.project.toString()
    /** Scope of current project */
    lazy val thisScope = Load.projectScope(thisProjectRef)
    /** Scope of current project withing plugin configuration */
    lazy val thisOSGiScope = thisScope.copy(config = Select(AspectJConf))
  }
  class RichOption[T](option: Option[T]) {
    def getOrThrow(onError: String) = option getOrElse { throw new NoSuchElementException(onError) }
  }
}
