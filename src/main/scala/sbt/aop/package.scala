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

package sbt

import java.io.{ File ⇒ JFile }
import sbt.aop.argument.Weave
import sbt.Keys._

package object aop {
  val AspectJVersion = "1.8.9"
  /** Entry point for the plugin in user's project. */
  def AOP = Plugin.defaultCompileSettings ++ Plugin.defaultTestSettings ++
    Plugin.dependencySettings ++ compileSettings ++ testSettings
  /** Entry point for the plugin in user's project with RT dependency. */
  def AOPRT = Plugin.defaultCompileSettings ++ Plugin.defaultTestSettings ++
    Plugin.dependencySettingsRT ++ compileSettings ++ testSettings

  // export declarations for end user
  lazy val AOPKey = Keys
  lazy val AOPCompile = Keys.AOPCompileConf
  lazy val AOPTest = Keys.AOPTestConf

  /** Add to log message AspectJ prefix. */
  protected[aop] def logPrefix(name: String) = "[AOP:%s] ".format(name)
  /** Compile settings. */
  protected def compileSettings = inConfig(sbt.Compile)(Seq(
    products <<= (Keys.aopWeaveArg in Keys.AOPCompileConf, products in Compile, Keys.aopGenericArg in Keys.AOPCompileConf) map
      ((a, b, c) ⇒ AspectJ.weave(a, b)(c))))
  /** Test settings. */
  protected def testSettings = inConfig(sbt.Test)(Seq(
    products <<= (Keys.aopWeaveArg in Keys.AOPTestConf, products in Test, Keys.aopGenericArg in Keys.AOPTestConf) map
      ((a, b, c) ⇒ AspectJ.weave(a, b)(c))))

  case class Aspect(file: JFile, binary: Boolean)
  case class Mapping(in: JFile, aspects: Seq[Aspect], out: JFile)
}
