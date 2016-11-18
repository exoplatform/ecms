/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive;

import org.exoplatform.clouddrive.CloudDrive.Command;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Support different environments for Cloud Drive commands execution. Three phases can be implemented:
 * preparation of a {@link Command}, cleanup on success or failure on error.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveEnvironment.java 00000 Jan 15, 2014 pnedonosko $
 * 
 */
public abstract class CloudDriveEnvironment extends BaseComponentPlugin {

  /** The Constant LOG. */
  protected static final Log      LOG = ExoLogger.getLogger(CloudDriveEnvironment.class);

  /** The next. */
  protected CloudDriveEnvironment next;

  /**
   * Basic constructor.
   */
  public CloudDriveEnvironment() {
  }

  /**
   * Configure environment for a command execution. If this configuration will fail the command execution will
   * fail also with this exception.<br>
   * This operation should gather required settings to apply them later in
   * {@link CloudDriveEnvironment#prepare(Command)} method.
   *
   * @param command the command
   * @throws CloudDriveException the cloud drive exception
   */
  public void configure(Command command) throws CloudDriveException {
    if (next != null) {
      next.configure(command);
    }
  }

  /**
   * Prepare environment for a command execution. If this preparation will fail the command execution will
   * fail also with this exception. This method should be invoked in a thread where the command runs.<br>
   * Preparation does apply settings gathered in {@link CloudDriveEnvironment#configure(Command)} method.
   *
   * @param command the command
   * @throws CloudDriveException the cloud drive exception
   */
  public void prepare(Command command) throws CloudDriveException {
    if (next != null) {
      next.prepare(command);
    }
  }

  /**
   * Clean environment after a command execution. This cleanup will be called in case of command success. This
   * method should be invoked in a thread where the command runs.<br>
   * Cleanup does restore setting applied in {@link CloudDriveEnvironment#prepare(Command)} method.
   *
   * @param command {@link Command}
   * @throws CloudDriveException the cloud drive exception
   */
  public void cleanup(Command command) throws CloudDriveException {
    if (next != null) {
      next.cleanup(command);
    }
  }

  /**
   * Command failed and here it can be reported or some special action applied regarding the error. This
   * method should be invoked in a thread where the command runs.<br>
   * Fail <strong>should not</strong> restore setting applied in
   * {@link CloudDriveEnvironment#prepare(Command)} method, this should
   * be done by {@link #cleanup(Command)} call from try-finally block for example.
   *
   * @param command {@link Command}
   * @param error {@link Throwable}
   * @throws CloudDriveException the cloud drive exception
   */
  public void fail(Command command, Throwable error) throws CloudDriveException {
    if (next != null) {
      next.fail(command, error);
    }
  }

  /**
   * Chain next environment support to this environment. Next env will create or continue a chain of already
   * chained. It's not possible to chain to itself but no checks will be performed for repeated sub-chains.
   * 
   * @param next {@link CloudDriveEnvironment} a next env in chain.
   */
  public void chain(CloudDriveEnvironment next) {
    if (this != next) {
      if (this.next == null) {
        this.next = next;
      } else {
        this.next.chain(next);
      }
    }
  }
}
