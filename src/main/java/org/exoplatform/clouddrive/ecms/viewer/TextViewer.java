/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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

import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.viewer.ContentReader;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Text files viewer for Cloud Drive.
 */
@ComponentConfig(template = "classpath:groovy/templates/TextViewer.gtmpl")
public class TextViewer extends AbstractFileViewer {

  public static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2M

  public boolean isWebDocument() {
    String mimeType = file.getType();
    return mimeType.startsWith("text/html") || mimeType.startsWith("application/rss+xml")
        || mimeType.startsWith("application/xhtml");
  }

  public boolean isXmlDocument() {
    String mimeType = file.getType();
    return mimeType.startsWith("text/xml") || mimeType.startsWith("application/xml")
        || (mimeType.startsWith("application/") && mimeType.indexOf("+xml") > 0);
  }

  public boolean isFormattedText() {
    String mimeType = file.getType();
    return (mimeType.startsWith("text/") && file.getTypeMode() != null)
        || (mimeType.startsWith("application/") && file.getTypeMode() != null)
        // text/x- can be used for various programming languages
        || mimeType.startsWith("text/x-") || mimeType.startsWith("application/x-sh")
        || mimeType.startsWith("text/javascript") || mimeType.startsWith("application/javascript")
        || mimeType.startsWith("text/json") || mimeType.startsWith("application/json");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isViewable() {
    boolean res = super.isViewable();
    if (res) {
      // ensure size OK
      try {
        ContentReader content = ((CloudDriveStorage) drive).getFileContent(file.getId());
        res = content.getLength() <= MAX_FILE_SIZE;
      } catch (Throwable e) {
        LOG.warn("Error getting file content reader for " + file.getId() + " " + file.getPath(), e);
      }
    }
    return res;
  }
}
