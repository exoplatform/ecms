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
package org.exoplatform.clouddrive.jcr;

import org.apache.commons.chain.Context;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.ext.action.InvocationContext;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AbstractJCRAction.java 00000 Oct 5, 2012 pnedonosko $
 */
public abstract class AbstractJCRAction implements Action {

  /**
   * Get a component instance from the context's container.
   *
   * @param <C> the generic type
   * @param context the context
   * @param type the type
   * @return the component
   */
  protected <C> C getComponent(Context context, Class<C> type) {
    ExoContainer container = (ExoContainer) context.get(InvocationContext.EXO_CONTAINER);
    return type.cast(container.getComponentInstanceOfType(type));
  }

  /**
   * Start.
   *
   * @param drive the drive
   */
  protected void start(CloudDrive drive) {
    JCRLocalCloudDrive.startAction(drive);
  }

  /**
   * Accept.
   *
   * @param drive the drive
   * @return true, if successful
   */
  protected boolean accept(CloudDrive drive) {
    return JCRLocalCloudDrive.acceptAction(drive);
  }

  /**
   * Done.
   */
  protected void done() {
    JCRLocalCloudDrive.doneAction();
  }

}
