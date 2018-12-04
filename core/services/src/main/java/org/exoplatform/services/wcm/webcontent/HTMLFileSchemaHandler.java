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
package org.exoplatform.services.wcm.webcontent;

import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.link.LiveLinkManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham
 * hoa.pham@exoplatform.com
 * Jun 23, 2008
 */

public class HTMLFileSchemaHandler extends BaseWebSchemaHandler {

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getHandlerNodeType()
   */
  protected String getHandlerNodeType() {   return "nt:file"; }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getParentNodeType()
   */
  protected String getParentNodeType() { return "exo:webFolder"; }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#matchHandler(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public boolean matchHandler(SessionProvider sessionProvider, Node node) throws Exception {
    if(!matchNodeType(node))
      return false;
    if(!matchMimeType(node))
      return false;
    if(!matchParentNodeType(node)) {
      if(!isInWebContent(node))
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
    if("text/html".equals(mimeType))
      return true;
    if("text/plain".equals(mimeType))
      return true;
    return false;
  }

  /**
   * Checks if is in web content.
   *
   * @param file the file
   *
   * @return true, if is in web content
   *
   * @throws Exception the exception
   */
  public boolean isInWebContent(Node file) throws Exception{
    if(file.getParent().isNodeType("exo:webContent")) {
      return file.isNodeType("exo:htmlFile");
    }
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
  private boolean matchParentNodeType(Node file) throws Exception{
    return file.getParent().isNodeType("exo:webFolder");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onCreateNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onCreateNode(SessionProvider sessionProvider, final Node file) throws Exception {
    Session session = file.getSession();
    Node webFolder = file.getParent();
    String fileName = file.getName();
    //create temp folder
    addMixin(file, "exo:htmlFile");
    file.setProperty("exo:presentationType","exo:htmlFile");
    String tempFolderName = fileName + this.hashCode();
    Node tempFolder = webFolder.addNode(tempFolderName, NT_UNSTRUCTURED);
    String tempPath = tempFolder.getPath() + "/" +file.getName();
    session.move(file.getPath(),tempPath);
    webFolder.save();
    //rename the folder
    Node webContent = webFolder.addNode(fileName, "exo:webContent");
    addMixin(webContent,"exo:privilegeable");
    addMixin(webContent,"exo:owneable");
    // need check why WebContentSchemaHandler doesn't run for this case
    WebSchemaConfigService schemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    WebContentSchemaHandler contentSchemaHandler = schemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    contentSchemaHandler.createSchema(webContent);
    webFolder.save();
    //the htmlFile become default.html file for the web content
    String htmlFilePath = webContent.getPath() + "/default.html";
    session.move(tempPath, htmlFilePath);
    tempFolder.remove();
    createDefautWebData(webContent);
    webFolder.save();
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.core.BaseWebSchemaHandler#onModifyNode(javax
   * .jcr.Node, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void onModifyNode(final SessionProvider sessionProvider, final Node node) throws Exception {
    if(node.hasNode("jcr:content")) {
      Node contentNode = node.getNode("jcr:content");
      contentNode.setProperty("jcr:lastModified",new GregorianCalendar());
      contentNode.setProperty("jcr:mimeType", "text/html");
    }

    Node parent = node.getParent();
    if(!parent.isNodeType("exo:webContent"))
      return;
    if (!parent.isCheckedOut() || parent.isLocked() || !node.isCheckedOut()) {
      return;
    }
    LiveLinkManagerService liveLinkManagerService = WCMCoreUtils.getService(LiveLinkManagerService.class);
    List<String> newLinks = liveLinkManagerService.extractLinks(node);
    liveLinkManagerService.updateLinkDataForNode(parent,newLinks);
  }

}
