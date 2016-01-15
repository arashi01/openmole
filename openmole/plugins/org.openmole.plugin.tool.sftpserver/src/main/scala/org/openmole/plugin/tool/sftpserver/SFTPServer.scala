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

package org.openmole.plugin.tool.sftpserver

import java.io.File
import org.apache.sshd.common.file.root.{ RootedFileSystemProvider }
import org.apache.sshd.server.SshServer
import org.apache.sshd.common.file._
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.apache.sshd.common.session.Session

import org.apache.sshd.server.command.ScpCommandFactory
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import collection.JavaConversions._

object SFTPServer extends Logger

import SFTPServer.Log._

class SFTPServer(path: File, login: String, password: String, port: Int) {
  logger.fine(s"Starting sftp server on port $port with path $path")

  def fileSystem = new RootedFileSystemProvider().newFileSystem(path.toPath, Map.empty[String, Object])

  val sshd = SshServer.setUpDefaultServer

  {
    sshd.setPort(port)
    sshd.setSubsystemFactories(List(new SftpSubsystemFactory))
    sshd.setCommandFactory(new ScpCommandFactory)
    sshd.setFileSystemFactory(new FileSystemFactory {
      override def createFileSystem(s: Session) = fileSystem
    })

    sshd.setPasswordAuthenticator(new PasswordAuthenticator {
      override def authenticate(username: String, pass: String, session: ServerSession) = {
        username == login && pass == password
      }
    })
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider)

    start
  }

  override def finalize = background { stop }

  def start = sshd.start

  def stop = sshd.stop

}
