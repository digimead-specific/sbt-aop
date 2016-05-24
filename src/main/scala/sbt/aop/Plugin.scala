/**
 * sbt-aop - AspectJ for nested projects.
 *
 * Copyright (c) 2013-2016 Alexey Aksenov ezh@ezh.msk.ru
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

import Keys._
import java.io.{ File, IOException }
import sbt._
import sbt.Keys._
import sbt.aop.argument.{ Generic, Weave }
import scala.language.implicitConversions

object Plugin extends sbt.Plugin {
  implicit def option2rich[T](option: Option[T]): RichOption[T] = new RichOption(option)

  lazy val defaultCompileSettings = inConfig(AOPCompileConf)(Seq(
    aopAspects <<= aspectsTask,
    aopBinary := Seq.empty,
    aopClasspath <<= (externalDependencyClasspath in Compile, classDirectory in Compile) map { (a, b) ⇒ a.files :+ b },
    aopFilter := { (f, a) ⇒ a },
    aopGenericArg <<= (state, streams, thisProjectRef) map (
      (state, streams, thisProjectRef) ⇒ Generic(state, thisProjectRef, Some(streams))),
    aopInputs := Seq.empty,
    // copyResources will drop foreign input resources
    aopInputResources <<= sbt.Keys.copyResources in Compile,
    aopMappings <<= (aopInputs, aopAspects, aopFilter, aopOutput) map ((in, aspects, filter, dir) ⇒
      in map { input ⇒ Mapping(input, filter(input, aspects), Plugin.getInstrumentedPath(input, dir)) }),
    aopOptions <<= baseOptionsSettings,
    aopOutput <<= (crossTarget) { _ / "aop" },
    aopOutxml := true,
    aopShowWeaveInfo := false,
    aopSource <<= (sourceDirectory in Compile) { _ / "aop" },
    aopSourceLevel <<= findSourceLevel,
    aopVerbose := false,
    aopWeaveArg <<= (aopOptions, aopClasspath, aopMappings, aopInputResources, streams) map
      ((aspectjOptions, aspectjClasspath, aspectjMappings, aspectjInputResources, streams) ⇒
        Weave(streams.cacheDirectory, aspectjClasspath, aspectjMappings, aspectjOptions, aspectjInputResources)),
    excludeFilter := HiddenFileFilter,
    includeFilter := "*.aj",
    sourceDirectories <<= Seq(aopSource).join,
    sources <<= sourcesTask))

  lazy val defaultTestSettings = inConfig(AOPTestConf)(Seq(
    aopAspects <<= aspectsTask,
    aopBinary := Seq.empty,
    aopClasspath <<= (externalDependencyClasspath in Compile, externalDependencyClasspath in Test,
      classDirectory in Compile, classDirectory in Test) map { (a, b, c, d) ⇒ (a ++ b).files :+ c :+ d },
    aopFilter := { (f, a) ⇒ a },
    aopGenericArg <<= (state, streams, thisProjectRef) map (
      (state, streams, thisProjectRef) ⇒ Generic(state, thisProjectRef, Some(streams))),
    aopInputs := Seq.empty,
    // copyResources will drop foreign input resources
    aopInputResources <<= (sbt.Keys.copyResources in Compile, sbt.Keys.copyResources in Test) map { (a, b) ⇒ a ++ b },
    aopMappings <<= (aopInputs, aopAspects, aopFilter, aopOutput) map ((in, aspects, filter, dir) ⇒
      in map { input ⇒ Mapping(input, filter(input, aspects), Plugin.getInstrumentedPath(input, dir)) }),
    aopOptions <<= baseOptionsSettings,
    aopOutput <<= (crossTarget) { _ / "aop" },
    aopOutxml := true,
    aopShowWeaveInfo := false,
    aopSource <<= (sourceDirectory in Compile) { _ / "aop" },
    aopSourceLevel <<= findSourceLevel,
    aopVerbose := false,
    aopWeaveArg <<= (aopOptions, aopClasspath, aopMappings, aopInputResources, streams) map
      ((aspectjOptions, aspectjClasspath, aspectjMappings, aspectjInputResources, streams) ⇒
        Weave(streams.cacheDirectory, aspectjClasspath, aspectjMappings, aspectjOptions, aspectjInputResources)),
    excludeFilter := HiddenFileFilter,
    includeFilter := "*.aj",
    sourceDirectories <<= Seq(aopSource).join,
    sources <<= sourcesTask))

  /** AspectJ dependencies */
  def dependencySettings = Seq(
    ivyConfigurations ++= Seq(AOPCompileConf, AOPTestConf),
    libraryDependencies ++= Seq(
      "org.aspectj" % "aspectjtools" % AspectJVersion % AOPCompileConf.name,
      "org.aspectj" % "aspectjweaver" % AspectJVersion % AOPCompileConf.name,
      "org.aspectj" % "aspectjrt" % AspectJVersion % AOPCompileConf.name))

  /** AspectJ dependencies with public AspectJRT dependency */
  def dependencySettingsRT = Seq(
    ivyConfigurations ++= Seq(AOPCompileConf, AOPTestConf),
    libraryDependencies ++= Seq(
      "org.aspectj" % "aspectjtools" % AspectJVersion % AOPCompileConf.name,
      "org.aspectj" % "aspectjweaver" % AspectJVersion % AOPCompileConf.name,
      "org.aspectj" % "aspectjrt" % AspectJVersion))

  /** Collect all available aspects */
  def aspectsTask = (sources, aopBinary) map ((s, b) ⇒
    (s map { Aspect(_, binary = false) }) ++ (b map { Aspect(_, binary = true) }))
  /** Build baseOptions settings value. */
  def baseOptionsSettings = (aopShowWeaveInfo, aopVerbose, aopSourceLevel) map { (info, verbose, level) ⇒
    (if (info) Seq("-showWeaveInfo") else Seq.empty[String]) ++
      (if (verbose) Seq("-verbose") else Seq.empty[String]) ++
      level.toSeq
  }
  def copyResources(weave: Weave)(implicit arg: Generic) = {
    arg.log.debug(logPrefix(arg.name) + "Copy resources.")
    val cacheFile = weave.cache / "aop" / "resource-sync"
    // list of resoirce tuples (old file location -> instrumented file location) per aspectjMappings
    val mapped = weave.mappings flatMap { mapping ⇒
      if (mapping.in.isDirectory) {
        /*
           * rebase resource(_._2)
           *   from mapping.in (.../abc/def/target/scala-2.10/test-classes/...)
           *   to mapping.out (.../target/scala-2.10/aspectj/test-classes-instrumented/...)
           * returns None if aspectjInputResources entry is "foreign" resource,
           *  without relation to current mapping
           */
        val result = weave.resources map (_._2) pair (rebase(mapping.in, mapping.out), false)
        result.map {
          case (from, to) ⇒ arg.log.debug(logPrefix(arg.name) + "Copy resource %s for mapping '%s'.".format(from, mapping.in.name))
        }
        result
      } else Seq.empty
    }
    sync(cacheFile)(mapped)
    mapped
  }
  /** Find source level for aspect4j. */
  def findSourceLevel = (javacOptions, aopGenericArg) map { (javacOptions, aspectjGenericArg) ⇒
    def default = System.getProperty("java.runtime.version") match {
      case version if version startsWith "1.8" ⇒ Some("-1.8")
      case version if version startsWith "1.7" ⇒ Some("-1.7")
      case version if version startsWith "1.6" ⇒ Some("-1.6")
      case _ ⇒ None
    }
    val index = javacOptions.indexOf("-source")
    val level = if (index >= 0) javacOptions.drop(index + 1).headOption else None
    level match {
      case Some("1.8") ⇒
        Some("-1.8")
      case Some("1.7") ⇒
        Some("-1.7")
      case Some("1.6") ⇒
        Some("-1.6")
      case None ⇒
        default
      case Some(unsupported) ⇒
        aspectjGenericArg.log.warn(logPrefix(aspectjGenericArg.name) + " unable to find correct AspectJ source level: unsupported value of -source " + unsupported)
        default
    }
  }
  /** Get path to instrumented artifact. */
  def getInstrumentedPath(input: File, outputDir: File): File = {
    val (base, ext) = input.baseAndExt
    val outputName = {
      if (ext.isEmpty) base + "-instrumented"
      else base + "-instrumented" + "." + ext
    }
    outputDir / outputName
  }
  /** Collect AspectJ sources */
  def sourcesTask = (sourceDirectories, includeFilter, excludeFilter) map (
    (dirs, include, exclude) ⇒ dirs.descendantsExcept(include, exclude).get)

  /**
   * Fixed sbt.Sync.apply
   * https://github.com/sbt/sbt/issues/1559
   */
  protected def sync(cacheFile: File, inStyle: FileInfo.Style = FileInfo.lastModified, outStyle: FileInfo.Style = FileInfo.exists): Traversable[(File, File)] ⇒ Relation[File, File] =
    mappings ⇒
      {
        val relation = Relation.empty ++ mappings
        Sync.noDuplicateTargets(relation)
        val currentInfo = relation._1s.map(s ⇒ (s, inStyle(s))).toMap

        val (previousRelation, previousInfo) = readInfo(cacheFile)(inStyle.format)
        val removeTargets = previousRelation._2s -- relation._2s

        def outofdate(source: File, target: File): Boolean =
          !previousRelation.contains(source, target) ||
            (previousInfo get source) != (currentInfo get source) ||
            !target.exists ||
            target.isDirectory != source.isDirectory

        val updates = relation filter outofdate

        val (cleanDirs, cleanFiles) = (updates._2s ++ removeTargets).partition(_.isDirectory)

        IO.delete(cleanFiles)
        IO.deleteIfEmpty(cleanDirs)
        updates.all.foreach((Sync.copy _).tupled)

        Sync.writeInfo(cacheFile, relation, currentInfo)(inStyle.format)
        relation
      }
  /**
   * Fixed sbt.Sync.readInfo
   * https://github.com/sbt/sbt/issues/1559
   */
  protected def readInfo[F <: FileInfo](file: File)(implicit infoFormat: sbinary.Format[F]): Sync.RelationInfo[F] =
    try { Sync.readUncaught(file)(infoFormat) }
    catch {
      case e: IOException ⇒ (Relation.empty, Map.empty)
      case e: TranslatedIOException ⇒ (Relation.empty, Map.empty)
    }

  class RichOption[T](option: Option[T]) {
    def getOrThrow(onError: String) = option getOrElse { throw new NoSuchElementException(onError) }
  }
}
