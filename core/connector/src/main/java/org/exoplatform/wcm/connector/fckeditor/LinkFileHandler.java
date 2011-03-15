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
package org.exoplatform.wcm.connector.fckeditor;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.connector.fckeditor.FCKFileHandler;

/*
 * Created by The eXo Platform SAS
 * Author : Anh Do Ngoc
 *          anh.do@exoplatform.com
 * Sep 12, 2008
 */
public class LinkFileHandler extends FCKFileHandler {

  /**
   * Instantiates a new link file handler.
   *
   * @param container the container
   */
  public LinkFileHandler() {
    super(ExoContainerContext.getCurrentContainer());
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.connector.fckeditor.FCKFileHandler#getFileURL(javax.jcr.Node)
   */
  protected String getFileURL(final Node file) throws Exception {
    return file.getProperty("exo:linkURL").getString();
  }
}
