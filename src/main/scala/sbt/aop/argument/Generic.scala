/**
 * sbt-aop - AspectJ for nested projects.
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
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

package sbt.aop.argument

import sbt._
import sbt.Keys._

/** Consolidated argument with all required information */
case class Generic(
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
    val globalLoggin = sbt.aop.patch.Patch.getGlobalLogging(state)
    import globalLoggin._
    full match {
      case logger: AbstractLogger ⇒
        val level = logLevel in thisScope get extracted.structure.data
        level.foreach(logger.setLevel(_)) // force level
        logger
      case logger ⇒
        logger
    }
  }
  /** Current project name */
  val name: String = (sbt.Keys.name in thisScope get extracted.structure.data) getOrElse thisProjectRef.project.toString()
  /** Scope of current project */
  lazy val thisScope = Load.projectScope(thisProjectRef)
}
