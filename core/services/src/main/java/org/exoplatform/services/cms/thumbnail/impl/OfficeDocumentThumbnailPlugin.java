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
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Node;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharSet;
import org.apache.tika.io.IOUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.pdfviewer.PDFViewerService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * OCt 22, 2009
 * 2:20:33 PM
 */
public class OfficeDocumentThumbnailPlugin implements ComponentPlugin, ThumbnailPlugin {

  private ThumbnailType config;
  private String description;
  private String name;
  private JodConverterService jodConverter_;

  private static final Log LOG = ExoLogger.getExoLogger(OfficeDocumentThumbnailPlugin.class.getName());

  public OfficeDocumentThumbnailPlugin(JodConverterService jodConverter, InitParams initParams) throws Exception {
    config = initParams.getObjectParamValues(ThumbnailType.class).get(0);
    this.jodConverter_ = jodConverter;
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
    if(contentNode.isNodeType(NodetypeConstant.NT_RESOURCE)) contentNode = contentNode.getParent();
    String mimeType = contentNode.getProperty("jcr:content").getString();
    String extension = DMSMimeTypeResolver.getInstance().getExtension(mimeType);
    File in = File.createTempFile(name + "_tmp", "." + extension);
    FileUtils.writeByteArrayToFile(in, contentNode.getProperty("jcr:data").getString().getBytes());
    File out = File.createTempFile(name + "_tmp", ".jpg");
    boolean success = jodConverter_.convert(in, out,"jpg");
    if (success) {
      return ImageIO.read(out);
    } else {
      return null;
    }
  }


  public List<String> getMimeTypes() {
    return config.getMimeTypes();
  }

}
