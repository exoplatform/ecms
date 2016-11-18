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
package org.exoplatform.clouddrive.ecms.viewer;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.ecms.BaseCloudDriveForm;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.ws.rs.core.MediaType;

/**
 * Base support for WebUI forms based on Cloud Drive file.
 */
public abstract class AbstractFileForm extends BaseCloudDriveForm implements CloudFileViewer {

  /** The drive. */
  protected CloudDrive drive;

  /** The file. */
  protected CloudFile  file;

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
   * @return <code>true</code> if file can be represented as Web document.
   */
  public boolean isViewable() {
    String mimeType = file.getType();
    return !mimeType.startsWith(MediaType.APPLICATION_OCTET_STREAM);
  }

  /**
   * Gets the resource bundle.
   *
   * @param key the key
   * @return the resource bundle
   */
  public String getResourceBundle(String key) {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(localeFile(),
                                                                            locale,
                                                                            this.getClass().getClassLoader());
    return resourceBundle.getString(key);
  }

  /**
   * Locale file.
   *
   * @return the string
   */
  protected abstract String localeFile();

}
