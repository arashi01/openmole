/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.misc.fileservice.internal

import java.io.File
import org.openmole.misc.updater.IUpdatable

class FileServiceGC(fileService: FileService) extends IUpdatable {
  override def update: Boolean = {
    for(execution <- fileService.archiveCache.cacheMaps) {
      for(file <- execution._2) {
        if(!new File(file._1).exists) fileService.archiveCache.invalidateCache(execution._1, file._1)
      }
    }
    
    for(execution <- fileService.hashCache.cacheMaps) {
      for(file <- execution._2) {
        if(!new File(file._1).exists) fileService.archiveCache.invalidateCache(execution._1, file._1)
      }
    }

    true
  }
}
