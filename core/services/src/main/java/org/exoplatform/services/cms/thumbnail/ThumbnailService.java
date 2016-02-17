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
package org.exoplatform.services.cms.thumbnail;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 10, 2008 1:59:21 PM
 */
/**
 * This service will be support to create thumbnail for node
 * Get image and any file type in node
 */
public interface ThumbnailService {

  final public static String EXO_THUMBNAILS = "exo:thumbnails";
  final public static String EXO_THUMBNAIL = "exo:thumbnail";
  final public static String SMALL_SIZE = "exo:smallSize";
  final public static String MEDIUM_SIZE = "exo:mediumSize";
  final public static String BIG_SIZE = "exo:bigSize";
  final public static String THUMBNAIL_LAST_MODIFIED = "exo:thumbnailLastModified";
  final public static String EXO_THUMBNAILS_FOLDER = "exo:thumbnails";
  final public static String HIDDENABLE_NODETYPE = "exo:hiddenable";

  /**
   * Return all nt:file node at current node
   * @param node Current node
   * @return all nt:file nodes
   * @throws RepositoryException
   */
  public List<Node> getAllFileInNode(Node node) throws RepositoryException;

  /**
   * Return the list of node in the current node with mimetype specified
   * @param node Current node
   * @param jcrMimeType Mime type of node will be retrieve
   * @return node list
   * @throws Exception
   */
  public List<Node> getFileNodesByType(Node node, String jcrMimeType) throws Exception;
  /**
   * Return a list image in node
   * @param node Current node
   * @return images nodes list
   * @throws Exception
   */
  public List<Node> getFlowImages(Node node) throws Exception;
  /**
   * To setup status of node is allow thumbnail or not
   * @param isEnable
   */
  public void setEnableThumbnail(boolean isEnable);
  /**
   * Return the status of node is enable thumbnail or not
   * @return Boolean value
   */
  public boolean isEnableThumbnail();
  /**
   * Create thumbnail for node with default size:
   * Small size, medium size, big size
   * @param node Current node which will be added thumbnail
   * @param image BufferedImage which contain the original image
   * @param mimeType File type
   * @throws Exception
   */
  public void createThumbnailImage(Node node, BufferedImage image, String mimeType) throws Exception;
  /**
   * Return the data of thumbnail with specified type
   * @param node Current node which will be added thumbnail
   * @param thumbnailType Type of thumbnail will be return (small, medium, big or specified if has)
   * @throws Exception
   */
  public InputStream getThumbnailImage(Node node, String thumbnailType) throws Exception;
  /**
   * Create thumbnail node
   * @param node Current node which included thumbnail
   * @return Node
   * @throws Exception
   */
  public Node addThumbnailNode(Node node) throws Exception;
  /**
   * Get thumbnail node
   * @param node
   * @return Node
   * @throws Exception
   */
  public Node getThumbnailNode(Node node) throws Exception;
  /**
   * Create a thumbnail for node with size specified
   * @param node Current node which will be added thumbnail
   * @param image BufferedImage which contain the original image
   * @param propertyName Data will be set to this property
   * @throws Exception
   */
  public void createSpecifiedThumbnail(Node node, BufferedImage image, String propertyName) throws Exception;
  /**
   * Add a thumbnail image to node
   * @param node Current node which will be added thumbnail
   * @param image BufferedImage which contain the original image
   * @param propertyName Data will be set to this property
   * @throws Exception
   */
  public void addThumbnailImage(Node node, BufferedImage image, String propertyName) throws Exception;
  /**
   * Process thumbnail with list nodes
   * @param listNodes List node which will be process to add thumbnail
   * @param type Type of thumbnail image
   * @throws Exception
   */
  public void processThumbnailList(List<Node> listNodes, String type) throws Exception;
  /**
   * Get mime types which allow to view
   * @return mimetypes list
   */
  public List<String> getMimeTypes();
  /**
   * Process to remove thumbnail
   * @param showingNode Node contain the thumbnail
   * @throws Exception
   */
  public void processRemoveThumbnail(Node showingNode) throws Exception;

  /**
   * Copy thumbnail node to destination node after moving or copy/paste.
   *
   * @param srcThumbnailNode thumbnailNode of source node
   * @param destNode destination Node
   * @throws Exception
   */
  public void copyThumbnailNode(Node srcThumbnailNode, Node destNode) throws Exception;

  /**
   * Add Thumbnail Plugin
   * @param plugin ComponentPlugin
   */
  public void addPlugin(ComponentPlugin plugin);

  /**
   * Return a list of Thumbnail plugin
   * @return ComponentPlugin list
   */
  public List<ComponentPlugin> getComponentPlugins();
}
