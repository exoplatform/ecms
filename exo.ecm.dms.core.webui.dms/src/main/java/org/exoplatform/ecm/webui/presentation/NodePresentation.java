/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.presentation;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:22:08 PM
 */

public interface NodePresentation {
  
  /**
   * Sets the node.
   * 
   * @param node the new node
   */
  public void setNode(Node node);

  /**
   * Gets the node.
   * 
   * @return the node
   * @throws Exception the exception
   */
  public Node getNode() throws Exception;
  
  /**
   * Gets the original node.
   * 
   * @return the original node
   * @throws Exception the exception
   */
  public Node getOriginalNode() throws Exception;
  
  /**
   * Gets the node type.
   * 
   * @return the node type
   * @throws Exception the exception
   */
  public String getNodeType() throws Exception;

  /**
   * Checks if is node type supported.
   * 
   * @return true, if is node type supported
   */
  public boolean isNodeTypeSupported();

  /**
   * Gets the template path.
   * 
   * @return the template path
   * @throws Exception the exception
   */
  public String getTemplatePath() throws Exception;

  /**
   * Gets the relations.
   * 
   * @return the relations
   * @throws Exception the exception
   */
  public List<Node> getRelations() throws Exception;

  /**
   * Gets the attachments.
   * 
   * @return the attachments
   * @throws Exception the exception
   */
  public List<Node> getAttachments() throws Exception;

  /**
   * Checks if is rss link.
   * 
   * @return true, if is rss link
   */
  public boolean isRssLink();

  /**
   * Gets the rss link.
   * 
   * @return the rss link
   */
  public String getRssLink();

  /**
   * Gets the supported localise.
   * 
   * @return the supported localise
   * @throws Exception the exception
   */
  public List getSupportedLocalise() throws Exception;
  
  /**
   * Sets the language.
   * 
   * @param language the new language
   */
  public void setLanguage(String language);
  
  /**
   * Gets the language.
   * 
   * @return the language
   */
  public String getLanguage();
  
  /**
   * Gets the component instance of type.
   * 
   * @param className the class name
   * @return the component instance of type
   */
  public Object getComponentInstanceOfType(String className);
  
  /**
   * Gets the web dav server prefix.
   * 
   * @return the web dav server prefix
   * @throws Exception the exception
   */
  public String getWebDAVServerPrefix() throws Exception;
  
  /**
   * Gets the image.
   * 
   * @param node the node
   * @return the image
   * @throws Exception the exception
   */
  public String getImage(Node node) throws Exception;
  
  /**
   * Gets the portal name.
   * 
   * @return the portal name
   */
  public String getPortalName();
  
  /**
   * Gets the repository.
   * 
   * @return the repository
   * @throws Exception the exception
   */
  public String getRepository() throws Exception; 
  
  /**
   * Gets the workspace name.
   * 
   * @return the workspace name
   * @throws Exception the exception
   */
  public String getWorkspaceName() throws Exception;
  
  /**
   * Gets the view template.
   * 
   * @param nodeTypeName the node type name
   * @param templateName the template name
   * @return the view template
   * @throws Exception the exception
   */
  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception;
  
  /**
   * Get the skin of template if it's existing
   * @param nodeTypeName The node type name
   * @param skinName  Skin name
   * @return The skin template
   * @throws Exception
   */
  public String getTemplateSkin(String nodeTypeName, String skinName) throws Exception;
  
  /**
   * Get UIComponent for comment
   * @return
   * @throws Exception
   */
  public UIComponent getCommentComponent() throws Exception;
  
  /**
   * Get UIComponent to remove attachment in document
   * @return
   * @throws Exception
   */
  public UIComponent getRemoveAttach() throws Exception;

  /**
   * Get UIComponent to remove comment in document
   * @return
   * @throws Exception
   */
  
  public UIComponent getRemoveComment() throws Exception;
  
  /**
   * Gets the comments.
   * 
   * @return the comments
   * @throws Exception the exception
   */
  public List<Node> getComments() throws Exception;
  
  /**
   * Gets the download link.
   * 
   * @param node the node
   * @return the download link
   * @throws Exception the exception
   */
  public String getDownloadLink(Node node) throws Exception;
  
  /**
   * Encode html.
   * 
   * @param text the text
   * @return the string
   * @throws Exception the exception
   */
  public String encodeHTML(String text) throws Exception;
  
  /**
   * Gets the icons.
   * 
   * @param node the node
   * @param size the size
   * @return the icons
   * @throws Exception the exception
   */
  public String getIcons(Node node, String size) throws Exception;

  /**
   * Get the UIComponent which to display file
   * @param mimeType
   * @return
   * @throws Exception
   */
  public UIComponent getUIComponent(String mimeType) throws Exception;
}
