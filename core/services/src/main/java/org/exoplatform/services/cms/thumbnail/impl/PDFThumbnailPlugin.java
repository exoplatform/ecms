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
package org.exoplatform.services.cms.thumbnail.impl;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.Stream;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * OCt 22, 2009
 * 2:20:33 PM
 */
public class PDFThumbnailPlugin implements ComponentPlugin, ThumbnailPlugin {

  private ThumbnailType config;
  private String description;
  private String name;
  private static final Log LOG = ExoLogger.getExoLogger(PDFThumbnailPlugin.class.getName());

  public PDFThumbnailPlugin(InitParams initParams) throws Exception {
    config = initParams.getObjectParamValues(ThumbnailType.class).get(0);
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BufferedImage getBufferedImage(Node contentNode, String nodePath) throws Exception {
    Document document = new Document();

    // Turn off Log of org.icepdf.core.pobjects.Stream to not print error stack trace in case
    // viewing a PDF file including CCITT (Fax format) images
    // TODO: Remove these statement and comments after IcePDF fix ECMS-3765
    Logger.getLogger(Stream.class.toString()).setLevel(Level.OFF);

    try {
      InputStream input = contentNode.getProperty("jcr:data").getStream() ;
      document.setInputStream(input, nodePath);      
    } catch (PDFException ex) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error parsing PDF document " + ex);
      }
    } catch (PDFSecurityException ex) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error encryption not supported " + ex);
      }
    } catch (FileNotFoundException ex) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error file not found " + ex);
      }
    } catch (IOException ex) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Error handling PDF document " + contentNode.getPath());
      }
    }
    // Paint each pages content to an image and write the image to file
    BufferedImage image = (BufferedImage) document.getPageImage(0, GraphicsRenderingHints.SCREEN,
        Page.BOUNDARY_CROPBOX, 0.0f, 1.0f);
    document.dispose();
    return image;
  }

  public List<String> getMimeTypes() {
    return config.getMimeTypes();
  }

}
