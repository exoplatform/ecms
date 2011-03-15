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
package org.exoplatform.services.wcm.javascript;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * May 28, 2008
 */
public class JSFileHandler extends BaseWebSchemaHandler {

  protected String getHandlerNodeType() { return "nt:file"; }
  protected String getParentNodeType() { return "exo:jsFolder" ;}
  private boolean isPortalJSFolder = false;

  public boolean matchHandler(SessionProvider sessionProvider, Node node) throws Exception {
    if(!matchNodeType(node))
      return false;
    if(!matchMimeType(node))
      return false;
    isPortalJSFolder = isInPortalJSFolder(sessionProvider, node);
    if(isPortalJSFolder) {
      return true;
    }
    if(!matchParenNodeType(node)) {
      return false;
    }
    return true;
  }

  private boolean matchNodeType(Node node) throws Exception{
    return node.getPrimaryNodeType().getName().equals("nt:file");
  }

  private boolean matchParenNodeType(Node node ) throws Exception{
    return node.getParent().isNodeType("exo:jsFolder");
  }

  private boolean matchMimeType(Node node) throws Exception{
    String mimeType = getFileMimeType(node);
    if("text/javascript".equals(mimeType))
      return true;
    if("application/x-javascript".equals(mimeType))
      return true;
    if("text/ecmascript".equals(mimeType))
      return true;
    if("text/plain".equals(mimeType))
      return true;
    return false;
  }

  private boolean isInPortalJSFolder(SessionProvider sessionProvider, Node file) throws Exception {
    Node portal = findPortalNode(sessionProvider, file);
    if(portal == null)  {
      return false;
    }
    WebSchemaConfigService schemaConfigService = getService(WebSchemaConfigService.class);
    PortalFolderSchemaHandler schemaHandler = schemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
    Node jsFolder = schemaHandler.getJSFolder(portal);
    return file.getPath().startsWith(jsFolder.getPath());
  }

  public void onCreateNode(SessionProvider sessionProvider, Node file) throws Exception {
    addMixin(file, "exo:jsFile") ;
    addMixin(file,"exo:owneable");
    file.setProperty("exo:presentationType","exo:jsFile");
    //If this jsFile belong to jsFolder of portal, the jsFile will be shared jsFile
    if(isPortalJSFolder) {
      file.setProperty("exo:sharedJS",true);
    }
  }

  public void onModifyNode(SessionProvider sessionProvider, Node file) throws Exception {
    if(isPortalJSFolder) {
      Node portal = findPortalNode(sessionProvider, file);
      XJavascriptService javascriptService = getService(XJavascriptService.class);
      javascriptService.updatePortalJSOnModify(portal, file);
    }
  }

  public void onRemoveNode(SessionProvider sessionProvider, Node file) throws Exception {
    if(isPortalJSFolder) {
      Node portal = findPortalNode(sessionProvider, file);
      XJavascriptService javascriptService = getService(XJavascriptService.class);
      javascriptService.updatePortalJSOnRemove(portal, file);
    }
  }

}
