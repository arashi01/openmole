/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.console

import groovy.lang.Binding
import java.io.InputStream
import java.io.OutputStream
import org.codehaus.groovy.ant.Groovy
import org.codehaus.groovy.tools.shell.Command
import org.codehaus.groovy.tools.shell.Groovysh
import org.codehaus.groovy.tools.shell.IO
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.logging.LoggerService
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import scala.annotation.tailrec


object Console {

  @tailrec def initPassword: Unit = {
    val message = (if(Workspace.passwordChoosen) "Enter your OpenMOLE password" else "OpenMOLE Password has not been set yet, choose a  password") + "  (for preferences encryption):"
  
    val password = new jline.ConsoleReader().readLine(message, '*')
    val success = try {
      Workspace.password_=(password)
      true
    } catch {
      case e: UserBadDataError => 
        println("Password incorrect.")
        false
    }
    if(!success) initPassword
  }
  
  val pluginManager = "plugin"
  val workspace = "workspace"
  val registry = "registry"
  val logger = "logger"
  val serializer = "serializer"
  
  val binding = new Binding
  val groovysh = new Groovysh(classOf[Groovy].getClassLoader, binding, new IO())

  setVariable(pluginManager, PluginManager)
  setVariable(workspace, Workspace)
  setVariable(logger, LoggerService)
  setVariable(serializer, new Serializer)

  run("import org.openmole.core.implementation.data.*")
  run("import static org.openmole.core.implementation.data.Prototype.*")
  run("import static org.openmole.core.implementation.data.Data.*")
  run("import org.openmole.core.implementation.execution.*")
  run("import org.openmole.core.implementation.execution.local.*")
  run("import org.openmole.core.implementation.hook.*")
  run("import org.openmole.core.implementation.job.*")
  run("import org.openmole.core.implementation.mole.*")
  run("import org.openmole.core.implementation.sampling.*")
  run("import org.openmole.core.implementation.task.*")
  run("import org.openmole.core.implementation.transition.*")
  
  def setVariable(name: String, value: Object) = binding.setVariable(name, value)

  def run(command: String) = groovysh.run(command)

  def leftShift(cmnd: Command): Object = groovysh.leftShift(cmnd)
  
}
