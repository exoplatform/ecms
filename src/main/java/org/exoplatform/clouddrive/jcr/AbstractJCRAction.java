/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.jcr;

import org.apache.commons.chain.Context;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.ext.action.InvocationContext;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AbstractJCRAction.java 00000 Oct 5, 2012 pnedonosko $
 * 
 */
public abstract class AbstractJCRAction implements Action {

  /**
   * Get a component instance from the context's container.
   * 
   * @param context
   * @param type
   * @return
   */
  protected <C> C getComponent(Context context, Class<C> type) {
    ExoContainer container = (ExoContainer) context.get(InvocationContext.EXO_CONTAINER);
    return type.cast(container.getComponentInstanceOfType(type));
  }

  protected void start(CloudDrive drive) {
    JCRLocalCloudDrive.startAction(drive);
  }

  protected boolean accept(CloudDrive drive) {
    return JCRLocalCloudDrive.acceptAction(drive);
  }

  protected void done() {
    JCRLocalCloudDrive.doneAction();
  }

}
