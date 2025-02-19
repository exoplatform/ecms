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
package org.exoplatform.social.plugin.doc;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.web.application.Parameter;

import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:22:08 PM
 */

public interface NodePresentation {
  
  public static final String MEDIA_STATE_DISPLAY = "DISPLAY";
  public static final String MEDIA_STATE_NONE = "NONE";

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
   * Gets the viewable link.
   *
   * @return the viewable URL
   * @throws Exception the exception
   */
  public String getViewableLink(Node attNode, Parameter[] params) throws Exception;

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

  /**
   * gets the status of display audio description for accessible media
   * @return status of display audio description for accessible media
   */
  public String getMediaState();
  
  /**
   * switches the status of display audio description for accessible media
   */
  public void switchMediaState();
  
  /**
   * 
   * @return true if node can display audio description
   */
  public boolean isDisplayAlternativeText();
  
  /**
   * 
   * @return true if can switch to play audio description of the media
   */
  public boolean playAudioDescription();
  
  /**
   * 
   * @return true if can switch back from original node from audio description
   */
  public boolean switchBackAudioDescription();
  
  public String getActionOpenDocInDesktop() throws Exception;
  
}