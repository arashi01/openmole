/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.file

import java.io.File
import monocle.macros.Lenses
import org.openmole.tool.stream._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.mole.MoleExecutionContext
import org.openmole.core.workflow.validation.ValidateHook
import org.openmole.core.workflow.dsl._

object AppendToFileHook {

  implicit def isBuilder = new HookBuilder[AppendToFileHook] {
    override def name = AppendToFileHook.name
    override def outputs = AppendToFileHook.outputs
    override def inputs = AppendToFileHook.inputs
    override def defaults = AppendToFileHook.defaults
  }

  def apply(fileName: ExpandedString, content: ExpandedString) =
    new AppendToFileHook(
      fileName,
      content,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    )

}

@Lenses case class AppendToFileHook(
    fileName: ExpandedString,
    content:  ExpandedString,
    inputs:   PrototypeSet,
    outputs:  PrototypeSet,
    defaults: DefaultSet,
    name:     Option[String]
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]): Seq[Throwable] =
    fileName.validate(inputs) ++ content.validate(inputs)

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val file = new File(fileName.from(context))
    file.createParentDir
    file.withLock(_.append(content.from(context)))
    context
  }

}
