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
package org.exoplatform.services.cms.clouddrives.webui.viewer;

import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * MPEG files viewer for Cloud Drive.
 */
@ComponentConfig(template = "classpath:groovy/templates/MpegVideoViewer.gtmpl")
public class MpegVideoViewer extends AbstractFileViewer {

  /**
   * Instantiates a new mpeg video viewer.
   *
   * @throws Exception the exception
   */
  public MpegVideoViewer() throws Exception {
  }
}
