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
package org.exoplatform.services.wcm.core;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com May 28, 2008
 */
public abstract class BaseWebSchemaHandler extends BaseComponentPlugin implements WebSchemaHandler {

  protected final String EXO_OWNABLE = "exo:owneable";
  protected final String NT_FOLDER = "nt:folder";
  protected final String NT_UNSTRUCTURED = "nt:unstructured";
  protected final String NT_FILE = "nt:file" ;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.WebSchemaHandler#matchHandler(javax.jcr.Node)
   */
  public boolean matchHandler(SessionProvider sessionProvider, Node node) throws Exception {
    String handlerNodeType = getHandlerNodeType();
    String parentNodeType = getParentNodeType();
    if(!node.getPrimaryNodeType().getName().equals(handlerNodeType))
      return false;
    if(!node.getParent().isNodeType(parentNodeType))
      return false;
    return true;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.WebSchemaHandler#onCreateNode(javax.jcr.Node)
   */
  public void onCreateNode(SessionProvider sessionProvider, Node node) throws Exception { }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.WebSchemaHandler#onModifyNode(javax.jcr.Node)
   */
  public void onModifyNode(SessionProvider sessionProvider, Node node) throws Exception { }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.WebSchemaHandler#onRemoveNode(javax.jcr.Node)
   */
  public void onRemoveNode(SessionProvider sessionProvider, Node node) throws Exception { }

  /**
   * Gets the handler node type.
   *
   * @return the handler node type
   */
  protected abstract String getHandlerNodeType() ;

  /**
   * Gets the parent node type.
   *
   * @return the parent node type
   */
  protected abstract String getParentNodeType();

  /**
   * Adds the mixin.
   *
   * @param node the node
   * @param mixin the mixin
   * @throws RepositoryException 
   * @throws LockException 
   * @throws ConstraintViolationException 
   * @throws VersionException 
   * @throws NoSuchNodeTypeException 
   * @throws Exception the exception
   */
  protected void addMixin(Node node, String mixin) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    if (!node.isNodeType(mixin)) node.addMixin(mixin);
  }

  protected <T> T getService(Class<T> clazz) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    return clazz.cast(container.getComponentInstanceOfType(clazz));
  }

  protected Node findPortalNode(SessionProvider sessionProvider, Node child) throws Exception{
    LivePortalManagerService livePortalManagerService = getService(LivePortalManagerService.class);
    String portalName = null;
    for(String portalPath: livePortalManagerService.getLivePortalsPath()) {
      if(child.getPath().startsWith(portalPath)) {
        portalName = livePortalManagerService.getPortalNameByPath(portalPath);
        break;
      }
    }
    if(portalName == null) return null;
    return livePortalManagerService.getLivePortal(sessionProvider, portalName);
  }

  protected String getFileMimeType(Node file) throws Exception{
    String mimeType = null;
    try {
      mimeType = file.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    } catch (Exception e) {
      MimeTypeResolver resolver = new MimeTypeResolver();
      resolver.setDefaultMimeType("text/plain");
      mimeType = resolver.getMimeType(file.getName());
    }
    return mimeType;
  }

  protected void createDefautWebData(Node webContent) throws Exception{
    //create empty css file:
    Node defaultCSS = addNodeAsNTFile(webContent.getNode("css"), "default.css", "text/css", "");
    addMixin(defaultCSS, "exo:cssFile");
    addMixin(defaultCSS,"exo:owneable");

    Node defaultJS = addNodeAsNTFile(webContent.getNode("js"), "default.js", "application/x-javascript", "");
    addMixin(defaultJS, "exo:jsFile");
    addMixin(defaultJS,"exo:owneable");

    if(!webContent.hasNode("default.html")){
      Node defaultHTML = addNodeAsNTFile(webContent, "default.html", "text/html", "");
      addMixin(defaultHTML, "exo:htmlFile");
      addMixin(defaultHTML,"exo:owneable");
    }

    Node illustration = addNodeAsNTFile(webContent.getNode("medias/images"), "illustration", "", "");
    addMixin(illustration, "exo:owneable");
  }

  private Node addNodeAsNTFile(Node home, String fileName,String mimeType,String data) throws Exception{
    Node file = home.addNode(fileName,"nt:file");
    Node jcrContent = file.addNode("jcr:content","nt:resource");
    jcrContent.addMixin("dc:elementSet");
    jcrContent.setProperty("jcr:encoding", "UTF-8");
    jcrContent.setProperty("jcr:lastModified", Calendar.getInstance());
    jcrContent.setProperty("jcr:mimeType", mimeType);
    jcrContent.setProperty("jcr:data", data);
    return file;
  }

}
