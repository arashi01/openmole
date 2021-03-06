/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.domain

import org.openmole.core.context._

object DomainInputs {

  /**
   * By default implicitly no domain inputs
   * @tparam T
   * @return
   */
  implicit def empty[T] = new DomainInputs[T] {
    def inputs(domain: T): PrototypeSet = PrototypeSet.empty
  }
}

/**
 * Property of having inputs for a domain
 * @tparam D
 */
trait DomainInputs[-D] {
  def inputs(domain: D): PrototypeSet
}
