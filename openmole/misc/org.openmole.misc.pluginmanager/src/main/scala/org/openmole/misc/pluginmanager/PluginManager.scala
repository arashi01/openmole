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

package org.openmole.misc.pluginmanager

import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import org.openmole.misc.pluginmanager.internal.Activator
import org.openmole.misc.tools.service.IHash
import org.osgi.framework.Bundle
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import org.osgi.framework.BundleEvent
import org.osgi.framework.BundleListener
import scala.collection.JavaConversions._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.hashservice.HashService

object PluginManager {

  private val defaultPatern = ".*\\.jar"

  private var files = Map.empty[File, (Long, IHash)]
  private var resolvedDirectDependencies = HashMap.empty[Long, HashSet[Long]]
  private var resolvedPluginDependenciesCache = HashMap.empty[Long, Iterable[Long]]
  private var providedDependencies = Set.empty[Long]

  updateDependencies

  Activator.contextOrException.addBundleListener(new BundleListener {
    override def bundleChanged(event: BundleEvent) = {
      val b = event.getBundle
      if (event.getType == BundleEvent.RESOLVED || event.getType == BundleEvent.UNRESOLVED || event.getType == BundleEvent.UPDATED) updateDependencies
    }
  })

  val OpenMOLEScope = "OpenMOLE-Scope"
  private implicit def bundleDecorator(b: Bundle) = new {

    def isSystem = b.getLocation.toLowerCase.contains("system bundle") || b.getLocation.startsWith("netigso:")

    def isProvided = b.getHeaders.toMap.exists { case (k, v) ⇒ k.toString.toLowerCase.contains("openmole-scope") && v.toString.toLowerCase.contains("provided") }

    def file = {
      val url = if (b.getLocation.startsWith("reference:"))
        b.getLocation.substring("reference:".length)
      else if (b.getLocation.startsWith("initial@reference:")) b.getLocation.substring("initial@reference:".length)
      else b.getLocation

      val location = {
        val protocol = url.indexOf(':')
        val noProtocol = if (protocol == -1) url else url.substring(protocol + 1)
        if (noProtocol.endsWith("/")) noProtocol.substring(0, noProtocol.length - 1)
        else noProtocol
      }
      new File(location)
    }
  }

  def isClassProvidedByAPlugin(c: Class[_]) = {
    val b = Activator.packageAdmin.getBundle(c)
    if (b != null) !providedDependencies.contains(b.getBundleId)
    else false
  }

  def pluginsForClass(c: Class[_]): Iterable[File] = synchronized {
    allPluginDependencies(Activator.packageAdmin.getBundle(c).getBundleId).map { l ⇒ Activator.contextOrException.getBundle(l).file }
  }

  def load(files: Iterable[File]) = synchronized {
    val bundles = files.map { installBundle }.toList
    bundles.foreach { _.start }
  }

  def loadIfNotAlreadyLoaded(plugins: Iterable[File]) = synchronized {
    val bundles = plugins.filterNot(f ⇒ files.contains(f)).map(installBundle).toList
    bundles.foreach { _.start }
  }

  def load(path: File): Unit = synchronized { installBundle(path).start }

  def loadDir(path: String): Unit = loadDir(new File(path))
  def loadDir(path: File): Unit = loadDir(path, defaultPatern)
  def loadDir(path: File, pattern: String): Unit = {
    loadDir(path, new FileFilter {
      override def accept(file: File): Boolean = {
        file.isFile && file.exists && file.getName().matches(pattern)
      }
    })
  }

  def loadDir(path: File, fiter: FileFilter): Unit =
    if (path.exists && path.isDirectory) load(path.listFiles(fiter))

  def bundle(file: File) = files.get(file.getAbsoluteFile).map { id ⇒ Activator.contextOrException.getBundle(id._1) }

  private def dependencies(bundles: Iterable[Long]): Iterable[Long] = synchronized {
    val ret = new HashSet[Long]
    var toProceed = new ListBuffer[Long] ++ bundles

    while (!toProceed.isEmpty) {
      val cur = toProceed.remove(0)
      ret += cur
      toProceed ++= resolvedDirectDependencies.getOrElse(cur, Iterable.empty).filter(b ⇒ !ret.contains(b))
    }

    ret
  }

  private def allPluginDependencies(b: Long) = synchronized {
    resolvedPluginDependenciesCache.getOrElseUpdate(b, dependencies(List(b)).filter(b ⇒ !providedDependencies.contains(b)))
  }

  private def installBundle(f: File) = {
    if (!f.exists) throw new UserBadDataError("Bundle file " + f + " doesn't exists.")
    val file = f.getAbsoluteFile

    files.get(file) match {
      case None ⇒
        val ret = Activator.contextOrException.installBundle(file.toURI.toString)
        files += file -> ((ret.getBundleId, HashService.computeHash(file)))
        ret
      case Some(bundleId) ⇒
        val bundle = Activator.contextOrException.getBundle(bundleId._1)
        if (HashService.computeHash(file) != bundleId._2) {
          val is = new FileInputStream(f)
          try bundle.update(is)
          finally is.close
        }
        bundle
    }
  }

  private def updateDependencies = synchronized {
    //Activator.contextOrException.getBundles.foreach{b => println(b.getLocation)}

    val bundles = Activator.contextOrException.getBundles.filter(!_.isSystem)

    resolvedDirectDependencies = new HashMap[Long, HashSet[Long]]
    bundles.foreach {
      b ⇒
        dependingBundles(b).foreach {
          db ⇒
            {
              resolvedDirectDependencies.getOrElseUpdate(db.getBundleId, new HashSet[Long]) += b.getBundleId
            }
        }
    }

    resolvedPluginDependenciesCache = new HashMap[Long, Iterable[Long]]
    providedDependencies = dependencies(bundles.filter(b ⇒ b.isProvided).map { _.getBundleId }).toSet
    files = bundles.map(b ⇒ b.file.getAbsoluteFile -> ((b.getBundleId, HashService.computeHash(b.file)))).toMap
  }

  private def dependingBundles(b: Bundle): Iterable[Bundle] = {
    val exportedPackages = Activator.packageAdmin.getExportedPackages(b)

    if (exportedPackages != null) {
      for (exportedPackage ← exportedPackages; ib ← exportedPackage.getImportingBundles) yield ib
    } else Iterable.empty
  }

  def load(path: String): Unit = load(new File(path))
}
