/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;

/**
 * Created by The eXo Platform SAS
 * 
 * Code copied from Onlyoffice add-on.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ContainerCommand.java 00000 Aug 26, 2019 pnedonosko $
 */
public abstract class ContainerCommand implements Runnable {

  /** The container name. */
  final String containerName;

  /**
   * Instantiates a new container command.
   *
   * @param containerName the container name
   */
  ContainerCommand(String containerName) {
    this.containerName = containerName;
  }

  /**
   * Execute actual work of the commend (in extending class).
   *
   * @param exoContainer the exo container
   */
  abstract void execute(ExoContainer exoContainer);

  /**
   * Callback to execute on container error.
   *
   * @param error the error
   */
  abstract void onContainerError(String error);

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    // Do the work under eXo container context (for proper work of eXo apps and JPA storage)
    ExoContainer exoContainer = ExoContainerContext.getContainerByName(containerName);
    if (exoContainer != null) {
      ExoContainer contextContainer = ExoContainerContext.getCurrentContainerIfPresent();
      try {
        // Container context
        ExoContainerContext.setCurrentContainer(exoContainer);
        RequestLifeCycle.begin(exoContainer);
        // do the work here
        execute(exoContainer);
      } finally {
        // Restore context
        RequestLifeCycle.end();
        ExoContainerContext.setCurrentContainer(contextContainer);
      }
    } else {
      onContainerError("Container not found");
    }
  }
}
