/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.ui.console

import jline.console.ConsoleReader
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.batch.authentication._
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.implementation.execution.local._
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.{ ExecutionContext, IMole, IMoleExecution }
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IExplorationTransition
import org.openmole.core.serializer.SerialiserService
import org.openmole.misc.workspace.Workspace
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.core.implementation.mole.MoleExecution

class Command {

  def print(environment: LocalEnvironment): Unit = {
    println("Queued jobs: " + environment.nbJobInQueue)
    println("Number of threads: " + environment.nbThreads)
  }

  def print(environment: BatchEnvironment): Unit = {
    val accounting = new Array[AtomicInteger](ExecutionState.values.size)

    for (state ← ExecutionState.values) {
      accounting(state.id) = new AtomicInteger
    }

    val executionJobs = environment.executionJobs

    for (executionJob ← executionJobs) {
      accounting(executionJob.state.id).incrementAndGet
    }

    for (state ← ExecutionState.values) {
      println(state.toString + ": " + accounting(state.id))
    }
  }

  def print(mole: IMole): Unit = {
    println("root: " + mole.root)
    mole.transitions.foreach(println)
    mole.dataChannels.foreach(println)
  }

  def print(moleExecution: IMoleExecution): Unit = {
    val toDisplay = new Array[AtomicInteger](State.values.size)
    for (state ← State.values) toDisplay(state.id) = new AtomicInteger
    for (job ← moleExecution.moleJobs) toDisplay(job.state.id).incrementAndGet
    for (state ← State.values) System.out.println(state.toString + ": " + toDisplay(state.id))
  }

  def verify(mole: IMole): Unit = Validation(mole).foreach(println)

  def encrypted: String = Workspace.encrypt(askPassword)

  def load[T](f: File) = SerialiserService.deserialise[T](f)
  def save(o: Any, f: File) = SerialiserService.serialise(o, f)

  def bundles = PluginManager.bundles
  def dependencies(file: File) = PluginManager.dependencies(file)

}

