/**
 * sbt-aspectj-nested - AspectJ for nested projects.
 *
 * Copyright (c) 2013-2015 Alexey Aksenov ezh@ezh.msk.ru
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

import Keys._
import java.io.{ File, IOException }
import sbt._
import sbt.Keys._
import sbt.aspectj.nested.argument.{ Generic, Weave }
import scala.language.implicitConversions

object Plugin extends sbt.Plugin {
  implicit def option2rich[T](option: Option[T]): RichOption[T] = new RichOption(option)

  lazy val defaultSettings = inConfig(AspectJConf)(Seq(
    aspectjAspects <<= aspectsTask,
    aspectjBinary := Seq.empty,
    aspectjClasspath <<= (externalDependencyClasspath in Compile) map { _.files },
    aspectjFilter := { (f, a) ⇒ a },
    aspectjGenericArg <<= aspectjGenericArgTask,
    aspectjInputs := Seq.empty,
    aspectjInputResources <<= copyResources in Compile,
    aspectjMappings <<= aspectMappingsTask,
    aspectjOptions <<= baseOptionsSettings,
    aspectjOutput <<= (crossTarget) { _ / "aspectj" },
    aspectjOutxml := true,
    aspectjShowWeaveInfo := false,
    aspectjSource <<= (sourceDirectory in Compile) { _ / "aspectj" },
    aspectjSourceLevel <<= findSourceLevel,
    aspectjVerbose := false,
    aspectjVersion := "1.8.5",
    aspectjWeaveArg <<= aspectjWeaveArgTask,
    copyResources <<= copyResourcesTask,
    excludeFilter := HiddenFileFilter,
    includeFilter := "*.aj",
    products <<= weaveTask,
    sourceDirectories <<= Seq(aspectjSource).join,
    sources <<= sourcesTask))

  /** AspectJ dependencies */
  def dependencySettings = Seq(
    ivyConfigurations += AspectJConf,
    libraryDependencies <++= (aspectjVersion in AspectJConf) { version ⇒
      Seq(
        "org.aspectj" % "aspectjtools" % version % AspectJConf.name,
        "org.aspectj" % "aspectjweaver" % version % AspectJConf.name,
        "org.aspectj" % "aspectjrt" % version % AspectJConf.name)
    })
  /** AspectJ dependencies with public AspectJRT dependency */
  def dependencySettingsRT = Seq(
    ivyConfigurations += AspectJConf,
    libraryDependencies <++= (aspectjVersion in AspectJConf) { version ⇒
      Seq(
        "org.aspectj" % "aspectjtools" % version % AspectJConf.name,
        "org.aspectj" % "aspectjweaver" % version % AspectJConf.name,
        "org.aspectj" % "aspectjrt" % version)
    })

  /** Collect mappings from available aspects and filters */
  def aspectMappingsTask = (aspectjInputs, aspectjAspects, aspectjFilter, aspectjOutput) map ((in, aspects, filter, dir) ⇒
    in map { input ⇒ Mapping(input, filter(input, aspects), getInstrumentedPath(input, dir)) })
  /** Collect all available aspects */
  def aspectsTask = (sources, aspectjBinary) map ((s, b) ⇒
    (s map { Aspect(_, binary = false) }) ++ (b map { Aspect(_, binary = true) }))
  /** Aggregate parameters for generic task */
  def aspectjGenericArgTask = (state, streams, thisProjectRef) map (
    (state, streams, thisProjectRef) ⇒ Generic(state, thisProjectRef, Some(streams)))
  /** Aggregate parameters that required by 'weave' task */
  def aspectjWeaveArgTask = (aspectjMappings in AJConf, aspectjOptions in AJConf, aspectjClasspath in AJConf, copyResources in AJConf, streams) map (
    (aspectjMappings, aspectjOptions, aspectjClasspath, _, streams) ⇒
      Weave(streams.cacheDirectory, aspectjMappings, aspectjOptions, aspectjClasspath))
  /** Build baseOptions settings value. */
  def baseOptionsSettings = (aspectjShowWeaveInfo, aspectjVerbose, aspectjSourceLevel) map { (info, verbose, level) ⇒
    (if (info) Seq("-showWeaveInfo") else Seq.empty[String]) ++
      (if (verbose) Seq("-verbose") else Seq.empty[String]) ++
      level.toSeq
  }
  def copyResourcesTask = (aspectjMappings in AJConf, aspectjInputResources in AJConf, state, thisProjectRef, streams) map {
    (mappings, aspectjInputResources, state, thisProjectRef, streams) ⇒
      val arg = Generic(state, thisProjectRef, Some(streams))
      arg.log.debug(logPrefix(arg.name) + "Copy resources.")
      val cacheFile = streams.cacheDirectory / "aspectj" / "resource-sync"
      // list of resoirce tuples (old file location -> instrumented file location) per aspectjMappings
      val mapped = mappings flatMap { mapping ⇒
        if (mapping.in.isDirectory) {
          /*
           * rebase resource(_._2)
           *   from mapping.in (.../abc/def/target/scala-2.10/test-classes/...)
           *   to mapping.out (.../target/scala-2.10/aspectj/test-classes-instrumented/...)
           * returns None if aspectjInputResources entry is "foreign" resource,
           *  without relation to current mapping
           */
          val result = aspectjInputResources map (_._2) pair (rebase(mapping.in, mapping.out), false)
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
  def findSourceLevel = (javacOptions, aspectjGenericArg in AspectJConf) map { (javacOptions, aspectjGenericArg) ⇒
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
  /** Collect AspectJ sources */
  def sourcesTask = (sourceDirectories, includeFilter, excludeFilter) map (
    (dirs, include, exclude) ⇒ dirs.descendantsExcept(include, exclude).get)
  /** Weave inputs */
  def weaveTask = (aspectjWeaveArg in AspectJConf, aspectjGenericArg in AspectJConf) map { (aspectjWeaveArg, aspectjGenericArg) ⇒
    aspectjGenericArg.log.debug(logPrefix(aspectjGenericArg.name) + "weave")
    AspectJ.weave(aspectjWeaveArg)(aspectjGenericArg)
  }

  /** Get path to instrumented artifact. */
  protected def getInstrumentedPath(input: File, outputDir: File): File = {
    val (base, ext) = input.baseAndExt
    val outputName = {
      if (ext.isEmpty) base + "-instrumented"
      else base + "-instrumented" + "." + ext
    }
    outputDir / outputName
  }
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
