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

import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.viewer.ContentReader;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Text files viewer for Cloud Drive.
 */
@ComponentConfig(template = "classpath:groovy/templates/TextViewer.gtmpl")
public class TextViewer extends AbstractFileViewer {

  /** The Constant MAX_FILE_SIZE. */
  public static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2M

  /**
   * Checks if is web document.
   *
   * @return true, if is web document
   */
  public boolean isWebDocument() {
    String mimeType = file.getType();
    return mimeType.startsWith("text/html") || mimeType.startsWith("application/rss+xml")
        || mimeType.startsWith("application/xhtml");
  }

  /**
   * Checks if is xml document.
   *
   * @return true, if is xml document
   */
  public boolean isXmlDocument() {
    String mimeType = file.getType();
    return mimeType.startsWith("text/xml") || mimeType.startsWith("application/xml")
        || (mimeType.startsWith("application/") && mimeType.indexOf("+xml") > 0);
  }

  /**
   * Checks if is formatted text.
   *
   * @return true, if is formatted text
   */
  public boolean isFormattedText() {
    String mimeType = file.getType();
    // text/x- can be used for various programming languages
    return (mimeType.startsWith("text/") && file.getTypeMode() != null)
        || (mimeType.startsWith("application/") && file.getTypeMode() != null) || mimeType.startsWith("text/x-")
        || mimeType.startsWith("application/x-sh") || mimeType.startsWith("text/javascript")
        || mimeType.startsWith("application/javascript") || mimeType.startsWith("text/json")
        || mimeType.startsWith("application/json");
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
