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

import Plugin.logPrefix
import org.aspectj.bridge.{ AbortException, IMessage }
import org.aspectj.bridge.{ IMessageHandler, MessageHandler }
import org.aspectj.bridge.IMessage.{ ABORT, DEBUG, ERROR, FAIL, INFO, TASKTAG, WARNING, WEAVEINFO }
import org.aspectj.tools.ajc.Main
import sbt._
import sbt.aspectj.nested.argument.{ Generic, Weave }
import scala.language.reflectiveCalls

object AspectJ {
  def weave(weave: Weave)(implicit arg: Generic): Seq[File] = {
    arg.log.info(logPrefix(arg.name) + "Weaving aspects")
    val cacheDir = weave.cache / "aspectj"
    arg.log.debug(logPrefix(arg.name) + "Search for cached aspects in " + cacheDir)
    val cached = FileFunction.cached(cacheDir / "ajc-inputs", FilesInfo.hash) { _ ⇒
      val withPrevious = weave.mappings.zipWithIndex map { case (m, i) ⇒ (weave.mappings.take(i), m) }
      (withPrevious map {
        case (previousMappings, mapping) ⇒
          val classpath = insertInstrumentedJars(weave.classpath, previousMappings)
          val classpathOpts = Seq("-classpath", classpath.absString)
          val options = weave.options ++ classpathOpts
          ajcRun(mapping.in, mapping.aspects, mapping.out, options, cacheDir)
          mapping.out
      }).toSet
    }
    val cacheInputs = weave.mappings.flatMap(mapping ⇒ {
      val input = mapping.in
      val aspects = mapping.aspects.map(_.file)
      if (input.isDirectory)
        (input ** "*.class").get ++ aspects
      else
        input +: aspects
    }).toSet
    cached(cacheInputs).toSeq
  }

  def insertInstrumentedJars(classpath: Seq[File], mappings: Seq[Mapping]) = insertInstrumentedClasses(classpath, mappings)
  def insertInstrumentedClasses(classpath: Seq[File], mappings: Seq[Mapping]) = {
    classpath map { a ⇒ mappings.find(_.in == a).map(_.out).getOrElse(a) }
  }
  protected def ajcOptions(in: File, aspects: Seq[Aspect], out: File, baseOptions: Seq[String]): Seq[String] = {
    val (binaries, sources) = aspects partition (_.binary)
    baseOptions ++
      Seq("-inpath", in.absolutePath) ++
      { if (binaries.nonEmpty) Seq("-aspectpath", binaries.map(_.file).absString) else Seq.empty } ++
      { if (in.isDirectory) Seq("-d", out.absolutePath) else Seq("-outjar", out.absolutePath) } ++
      sources.map(_.file.absolutePath)
  }
  protected def ajcRun(input: File, aspects: Seq[Aspect], output: File, baseOptions: Seq[String], cacheDir: File)(implicit arg: Generic): Unit = {
    IO.createDirectory(output.getParentFile)
    if (aspects.isEmpty) {
      arg.log.info(logPrefix(arg.name) + "No aspects for %s" format input)
      if (input.isDirectory) {
        arg.log.info(logPrefix(arg.name) + "Copying classes to %s" format output)
        val classes = (input ** "*.class") pair rebase(input, output)
        Sync(cacheDir / "ajc-sync")(classes)
      } else if (input.exists && output.exists) {
        arg.log.info(logPrefix(arg.name) + "Copying jar to %s" format output)
        IO.copyFile(input, output, false)
      } else {
        arg.log.info(logPrefix(arg.name) + "Skip %s" format input)
      }
    } else {
      arg.log.info(logPrefix(arg.name) + "Weaving %s with aspects:" format input)
      aspects foreach { a ⇒ arg.log.info(logPrefix(arg.name) + " " + a.file.absolutePath) }
      arg.log.info(logPrefix(arg.name) + "to %s" format output)
      val options = ajcOptions(input, aspects, output, baseOptions).toArray
      ajcRunMain(options)
    }
  }
  def ajcRunMain(options: Array[String])(implicit arg: Generic): Unit = {
    arg.log.debug(logPrefix(arg.name) + "Running AspectJ compiler with:")
    arg.log.debug(logPrefix(arg.name) + "ajc " + options.mkString(" "))
    val ajc = new Main
    val handler = new MessageHandler
    val showWeaveInfo = options contains "-showWeaveInfo"
    val verbose = options contains "-verbose"
    val logger = new IMessageHandler {
      var errors = false
      def handleMessage(message: IMessage): Boolean = {
        import IMessage._
        message.getKind match {
          case WEAVEINFO ⇒ if (showWeaveInfo) arg.log.info(logPrefix(arg.name) + message.toString)
          case INFO ⇒ if (verbose) arg.log.info(logPrefix(arg.name) + message.toString)
          case DEBUG | TASKTAG ⇒ arg.log.debug(logPrefix(arg.name) + message.toString)
          case WARNING ⇒ arg.log.warn(logPrefix(arg.name) + message.toString)
          case ERROR ⇒
            arg.log.error(logPrefix(arg.name) + message.toString); errors = true
          case FAIL | ABORT ⇒ throw new AbortException(logPrefix(arg.name) + message)
        }
        true
      }
      def isIgnoring(kind: IMessage.Kind) = false
      def dontIgnore(kind: IMessage.Kind) = ()
      def ignore(kind: IMessage.Kind) = ()
    }
    handler.setInterceptor(logger)
    ajc.setHolder(handler)
    ajc.runMain(options, false)
    if (logger.errors) throw new AbortException(logPrefix(arg.name) + "AspectJ failed")
  }
}
