/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.model.transition

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.data.IData
import org.openmole.core.model.mole.ISubMoleExecution

trait ITransition {

  /**
   *
   * Get the starting capsule of this transition.
   *
   * @return the starting capsule of this transition
   */
  def start: ICapsule

  /**
   *
   * Get the ending capsule of this transition.
   *
   * @return the ending capsule of this transition
   */
  def end: ISlot

  /**
   *
   * Get the condition under which this transition is performed.
   *
   * @return the condition under which this transition is performed
   */
  def condition: ICondition

  /**
   *
   * Get the value of the condition under which this transition is performed.
   * @param context the context in which this condition is evaluated
   *
   * @return the value of the condition under which this transition is performed
   */
  def isConditionTrue(context: IContext): Boolean

  /**
   *
   * Get the names of the variables which are filtred by this transition.
   *
   * @return the names of the variables which are filtred by this transition
   */
  def filter: Set[String]

  /**
   * Get the unfiltred user output data of the starting capsule going through
   * this transition
   *
   * @return the unfiltred output data of the staring capsule
   */
  def unFiltred: Iterable[IData[_]]

  /**
   *
   * Perform the transition and submit the jobs for the following capsules in the mole.
   *
   * @param from      context generated by the previous job
   * @param ticket    ticket of the previous job
   * @param toClone   variable to clone in the transition
   * @param subMole   current submole
   */
  def perform(from: IContext, ticket: ITicket, subMole: ISubMoleExecution)

}
