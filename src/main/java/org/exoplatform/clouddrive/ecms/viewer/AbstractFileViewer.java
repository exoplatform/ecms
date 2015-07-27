/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.ecms.viewer;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.ecms.BaseCloudDriveManagerComponent;
import org.exoplatform.webui.application.WebuiRequestContext;

import javax.ws.rs.core.MediaType;

/**
 * Base class for Cloud Drive file viewers.
 */
public abstract class AbstractFileViewer extends BaseCloudDriveManagerComponent implements CloudFileViewer {

  protected CloudDrive drive;

  protected CloudFile  file;

  protected final long viewableMaxSize;

  protected AbstractFileViewer(long viewableMaxSize) {
    this.viewableMaxSize = viewableMaxSize;
  }

  protected AbstractFileViewer() {
    this(Long.MAX_VALUE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    initContext();

    Object obj = context.getAttribute(CloudDrive.class);
    if (obj != null) {
      CloudDrive drive = (CloudDrive) obj;
      obj = context.getAttribute(CloudFile.class);
      if (obj != null) {
        initFile(drive, (CloudFile) obj);
      }
    }

    super.processRender(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initFile(CloudDrive drive, CloudFile file) {
    this.drive = drive;
    this.file = file;
  }

  /**
   * @return the drive
   */
  public CloudDrive getDrive() {
    return drive;
  }

  /**
   * @return the file
   */
  public CloudFile getFile() {
    return file;
  }

  public boolean isViewable() {
    String mimeType = file.getType();
    return file.getSize() <= viewableMaxSize && !mimeType.startsWith(MediaType.APPLICATION_OCTET_STREAM);
  }

}
