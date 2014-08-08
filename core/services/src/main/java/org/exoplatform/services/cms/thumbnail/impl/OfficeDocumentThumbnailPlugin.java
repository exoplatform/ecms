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
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
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
  private static final Log LOG = ExoLogger.getExoLogger(OfficeDocumentThumbnailPlugin.class.getName());

  public OfficeDocumentThumbnailPlugin(InitParams initParams) throws Exception {
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
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
    Document pdfDocument = new Document();
    if(contentNode.isNodeType(NodetypeConstant.NT_RESOURCE)) contentNode = contentNode.getParent();
    PDFViewerService pdfViewerService = WCMCoreUtils.getService(PDFViewerService.class);
    pdfDocument = pdfViewerService.initDocument(contentNode,repository);
    if (pdfDocument != null) {
      BufferedImage image = (BufferedImage) pdfDocument.getPageImage(0, GraphicsRenderingHints.SCREEN,
                                                                   Page.BOUNDARY_CROPBOX, 0.0f, 1.0f);
      pdfDocument.dispose();
      return image;
    } else return null;
  }


  public List<String> getMimeTypes() {
    return config.getMimeTypes();
  }

}
