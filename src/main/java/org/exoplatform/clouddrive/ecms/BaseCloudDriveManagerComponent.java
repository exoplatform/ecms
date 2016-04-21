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
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BaseCloudDriveManagerComponent.java 00000 Sep 26, 2012 pnedonosko $
 */
public abstract class BaseCloudDriveManagerComponent extends UIAbstractManagerComponent {

  protected static final Log LOG = ExoLogger.getLogger(BaseCloudDriveManagerComponent.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return UIAbstractManager.class;
  }

  protected void initContext() throws Exception {
    CloudDriveContext.init(this);
  }
}
