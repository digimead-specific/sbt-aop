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
  val AspectJVersion = "1.8.6"
  /** Entry point for the plugin in user's project. */
  def AOP = Plugin.defaultSettings ++ Plugin.dependencySettings ++ compileSettings ++ testSettings
  /** Entry point for the plugin in user's project with RT dependency. */
  def AOPRT = Plugin.defaultSettings ++ Plugin.dependencySettingsRT ++ compileSettings ++ testSettings

  // export declarations for end user
  lazy val AOPKey = Keys
  lazy val AOPConf = Keys.AOPConf

  /** Add to log message AspectJ prefix. */
  protected[aop] def logPrefix(name: String) = "[AOP:%s] ".format(name)
  /** Compile settings. */
  protected def compileSettings = inConfig(sbt.Compile)(Seq(
    Keys.aopClasspath <<= (externalDependencyClasspath in Compile) map { _.files },
    // copyResources will drop foreign input resources
    Keys.aopInputResources <<= copyResources in Compile,
    Keys.aopMappings <<= (Keys.aopInputs in AOPConf, Keys.aopAspects in AOPConf,
      Keys.aopFilter in AOPConf, Keys.aopOutput in AOPConf) map ((in, aspects, filter, dir) ⇒
        in map { input ⇒ Mapping(input, filter(input, aspects), Plugin.getInstrumentedPath(input, dir)) }),
    Keys.aopWeaveArg <<= (Keys.aopOptions in AOPConf, Keys.aopClasspath in Compile,
      Keys.aopMappings in Compile, Keys.aopInputResources in Compile, streams) map
      ((aspectjOptions, aspectjClasspath, aspectjMappings, aspectjInputResources, streams) ⇒
        Weave(streams.cacheDirectory, aspectjClasspath, aspectjMappings, aspectjOptions, aspectjInputResources)),
    products <<= (Keys.aopWeaveArg in Compile, products in Compile, Keys.aopGenericArg in AOPConf) map
      ((a, b, c) ⇒ AspectJ.weave(a, b)(c))))
  /** Test settings. */
  protected def testSettings = inConfig(sbt.Test)(Seq(
    Keys.aopClasspath <<= (externalDependencyClasspath in Compile, externalDependencyClasspath in Test) map { (a, b) ⇒ (a ++ b).files },
    //  copyResources will drop foreign input resources
    Keys.aopInputResources <<= (copyResources in Compile, copyResources in Test) map { (a, b) ⇒ a ++ b },
    Keys.aopMappings <<= (Keys.aopInputs in AOPConf, Keys.aopAspects in AOPConf,
      Keys.aopFilter in AOPConf, Keys.aopOutput in AOPConf) map ((in, aspects, filter, dir) ⇒
        in map { input ⇒ Mapping(input, filter(input, aspects), Plugin.getInstrumentedPath(input, dir)) }),
    Keys.aopWeaveArg <<= (Keys.aopOptions in AOPConf, Keys.aopClasspath in Test,
      Keys.aopMappings in Test, Keys.aopInputResources in Test, streams) map
      ((aspectjOptions, aspectjClasspath, aspectjMappings, aspectjInputResources, streams) ⇒
        Weave(streams.cacheDirectory, aspectjClasspath, aspectjMappings, aspectjOptions, aspectjInputResources)),
    products <<= (Keys.aopWeaveArg in Test, products in Test, Keys.aopGenericArg in AOPConf) map
      ((a, b, c) ⇒ AspectJ.weave(a, b)(c))))

  case class Aspect(file: JFile, binary: Boolean)
  case class Mapping(in: JFile, aspects: Seq[Aspect], out: JFile)
}
