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

package sbt.aspectj.nested.argument

import java.io.File

import sbt.aspectj.nested.Mapping

case class Weave(
  /** Directory used for caching task data (cacheDirectory). */
  val cache: File,
  /** Mappings from inputs, through aspects, to outputs (aspectjMappings). */
  val mappings: Seq[Mapping],
  /** The showWeaveInfo, verbose, and sourceLevel settings as options (aspectjOptions). */
  val options: Seq[String],
  /** The classpath used for running AspectJ (aspectjClasspath). */
  val classpath: Seq[File])
