/*
 * Copyright (C) 09/01/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.mole

import org.scalatest._
import org.scalatest.junit._

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

class InputStrainerCapsuleSpec extends FlatSpec with Matchers {
  "The input strainer capsule" should "let the data in but not out" in {
    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val strainer = new TestTask {
      val name = "Strainer"
      override def process(context: Context) = {
        context.contains(p) should equal(true)
        context
      }
    }

    val t2 = new TestTask {
      val name = "Test read"
      override def process(context: Context) = {
        context.contains(p) should equal(false)
        context
      }
    }

    val t1c = new Capsule(t1)
    val strainerC = new InputStrainerCapsule(strainer)
    val t2c = new StrainerCapsule(t2)

    val ex = t1c -- strainerC -- t2c
    ex.start.waitUntilEnded
  }
}
