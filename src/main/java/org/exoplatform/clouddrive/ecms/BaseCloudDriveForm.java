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
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * The Class BaseCloudDriveForm.
 */
public abstract class BaseCloudDriveForm extends UIForm implements UIPopupComponent {

  protected static final Log LOG = ExoLogger.getLogger(BaseCloudDriveForm.class);

  protected void initContext() throws Exception {
    CloudDriveContext.init(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void activate() {
    // nothing

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deActivate() {
    // nothing
  }
}
