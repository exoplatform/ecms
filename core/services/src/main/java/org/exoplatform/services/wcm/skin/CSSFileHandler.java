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
package org.exoplatform.services.wcm.skin;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * May 28, 2008
 */
public class CSSFileHandler extends BaseWebSchemaHandler {

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getHandlerNodeType()
   */
  protected String getHandlerNodeType() { return "nt:file"; }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getParentNodeType()
   */
  protected String getParentNodeType() { return "exo:cssFolder" ; }

  /** The is in portal css folder. */
  private boolean isInPortalCSSFolder = false;

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#matchHandler(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public boolean matchHandler(SessionProvider sessionProvider, Node node) throws Exception {
    if (!matchNodeType(node))
      return false;
    if (!matchMimeType(node))
      return false;
    isInPortalCSSFolder = matchPortalCSSFolder(sessionProvider, node);
    if (isInPortalCSSFolder)
      return true;
    if (!matchParentNodeType(node)) {
      return false;
    }
    return true;
  }

  /**
   * Match node type.
   *
   * @param node the node
   *
   * @return true, if successful
   *
   * @throws Exception the exception
   */
  private boolean matchNodeType(Node node) throws Exception{
    return node.getPrimaryNodeType().getName().equals("nt:file");
  }

  /**
   * Match mime type.
   *
   * @param node the node
   *
   * @return true, if successful
   *
   * @throws Exception the exception
   */
  private boolean matchMimeType(Node node) throws Exception {
    String mimeType = getFileMimeType(node);
    if ("text/css".equals(mimeType))
      return true;
    if ("text/plain".equals(mimeType))
      return true;
    return false;
  }

  /**
   * Match parent node type.
   *
   * @param file the file
   *
   * @return true, if successful
   *
   * @throws Exception the exception
   */
  private boolean matchParentNodeType(Node file) throws Exception {
    return file.getParent().isNodeType("exo:cssFolder");
  }

  /**
   * Match portal css folder.
   *
   * @param file the file
   * @param sessionProvider the session provider
   *
   * @return true, if successful
   *
   * @throws Exception the exception
   */
  private boolean matchPortalCSSFolder(SessionProvider sessionProvider, Node file) throws Exception {
    Node portal = findPortalNode(sessionProvider, file);
    if (portal == null)
      return false;
    WebSchemaConfigService schemaConfigService = getService(WebSchemaConfigService.class);
    PortalFolderSchemaHandler schemaHandler = schemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
    Node cssFolder = schemaHandler.getCSSFolder(portal);
    return file.getPath().startsWith(cssFolder.getPath());
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onCreateNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onCreateNode(SessionProvider sessionProvider, Node file) throws Exception {
    addMixin(file, "exo:cssFile");
    addMixin(file,"exo:owneable");
    file.setProperty("exo:presentationType","exo:cssFile");
    if(isInPortalCSSFolder) {
      file.setProperty("exo:sharedCSS",true);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onModifyNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onModifyNode(SessionProvider sessionProvider, Node file) throws Exception {
    if(isInPortalCSSFolder) {
      Node portal = findPortalNode(sessionProvider, file);
      XSkinService skinService = getService(XSkinService.class);
      skinService.updatePortalSkinOnModify(portal, file);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onRemoveNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onRemoveNode(SessionProvider sessionProvider, Node file) throws Exception {
    if (isInPortalCSSFolder) {
      XSkinService skinService = getService(XSkinService.class);
      Node portal = findPortalNode(sessionProvider, file);
      skinService.updatePortalSkinOnRemove(portal, file);
    }
  }

}
