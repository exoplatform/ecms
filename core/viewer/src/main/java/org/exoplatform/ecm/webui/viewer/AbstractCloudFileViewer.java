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
package org.exoplatform.ecm.webui.viewer;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.ws.rs.core.MediaType;

import org.exoplatform.ecm.webui.clouddrives.CloudDriveContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Base class for Cloud Drive file viewers.
 */
public abstract class AbstractCloudFileViewer extends UIAbstractManagerComponent implements CloudFileViewer {

  /** The drive. */
  protected CloudDrive drive;

  /** The file. */
  protected CloudFile  file;

  /** The viewable max size. */
  protected final long viewableMaxSize;

  /**
   * Instantiates a new abstract file viewer.
   *
   * @param viewableMaxSize the viewable max size
   */
  protected AbstractCloudFileViewer(long viewableMaxSize) {
    this.viewableMaxSize = viewableMaxSize;
  }

  /**
   * Instantiates a new abstract file viewer.
   */
  protected AbstractCloudFileViewer() {
    this(Long.MAX_VALUE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    CloudDriveContext.init(this);

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
   * Gets the drive.
   *
   * @return the drive
   */
  public CloudDrive getDrive() {
    return drive;
  }

  /**
   * Gets the file.
   *
   * @return the file
   */
  public CloudFile getFile() {
    return file;
  }

  /**
   * Checks if is viewable.
   *
   * @return true, if is viewable
   */
  public boolean isViewable() {
    String mimeType = file.getType();
    return file.getSize() <= viewableMaxSize && !mimeType.startsWith(MediaType.APPLICATION_OCTET_STREAM);
  }

  /**
   * Gets the resource bundle.
   *
   * @param key the key
   * @return the resource bundle
   */
  public String appRes(String key) {
    Locale locale = Util.getPortalRequestContext().getLocale();
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(
                                                                            new String[] { "locale.clouddrive.CloudDrive",
                                                                                "locale.ecm.views" },
                                                                            locale,
                                                                            this.getClass().getClassLoader());
    return resourceBundle.getString(key);
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
