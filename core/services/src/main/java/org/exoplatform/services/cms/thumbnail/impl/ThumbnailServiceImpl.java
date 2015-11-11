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
package org.exoplatform.services.cms.thumbnail.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.ImageUtils;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 10, 2008 1:58:10 PM
 */
public class ThumbnailServiceImpl implements ThumbnailService {

  final private static String JCR_CONTENT = "jcr:content";
  final private static String JCR_MIMETYPE = "jcr:mimeType";
  final private static String JCR_DATA = "jcr:data";
  final private static String NT_FILE = "nt:file";

  private boolean isEnableThumbnail_ = false;
  private String smallSize_;
  private String mediumSize_;
  private String bigSize_;
  private String mimeTypes_;
  private List<ComponentPlugin> plugins_ = new ArrayList<ComponentPlugin>();

  public ThumbnailServiceImpl(InitParams initParams) throws Exception {
    smallSize_ = initParams.getValueParam("smallSize").getValue();
    mediumSize_ = initParams.getValueParam("mediumSize").getValue();
    bigSize_ = initParams.getValueParam("bigSize").getValue();
    mimeTypes_ = initParams.getValueParam("mimetypes").getValue();
    isEnableThumbnail_ = Boolean.parseBoolean(initParams.getValueParam("enable").getValue());
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getFlowImages(Node node) throws Exception {
    NodeIterator nodeIter = node.getNodes();
    List<Node> listNodes = new ArrayList<Node>();
    Node thumbnailNode = null;
    while(nodeIter.hasNext()) {
      Node childNode = nodeIter.nextNode();
      thumbnailNode = addThumbnailNode(childNode);
      if(thumbnailNode != null && thumbnailNode.isNodeType(EXO_THUMBNAIL) && thumbnailNode.hasProperty(BIG_SIZE)) {
        listNodes.add(childNode);
      }
    }
    return listNodes;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAllFileInNode(Node node) throws RepositoryException {
    List<Node> fileListNodes = new ArrayList<Node>();
    NodeIterator nodeIter = node.getNodes();
    Node childNode = null;
    while(nodeIter.hasNext()) {
      childNode = nodeIter.nextNode();
      if(childNode.isNodeType(NT_FILE)) {
        fileListNodes.add(childNode);
      }
    }
    return fileListNodes;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getFileNodesByType(Node node, String jcrMimeType) throws RepositoryException {
    List<Node> fileListNodes = getAllFileInNode(node);
    List<Node> listNodes = new ArrayList<Node>();
    Node contentNode = null;
    for(Node childNode : fileListNodes) {
      contentNode = childNode.getNode(JCR_CONTENT);
      if(contentNode.getProperty(JCR_MIMETYPE).getString().equals(jcrMimeType)) {
        listNodes.add(childNode);
      }
    }
    return listNodes;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEnableThumbnail() {
    return isEnableThumbnail_;
  }

  /**
   * {@inheritDoc}
   */
  public void setEnableThumbnail(boolean isEnable) {
    isEnableThumbnail_ = isEnable;
  }

  /**
   * {@inheritDoc}
   */
  public InputStream getThumbnailImage(Node node, String thumbnailType) throws Exception {
    Node thumbnailNode = addThumbnailNode(node);
    if(thumbnailNode != null && thumbnailNode.hasProperty(thumbnailType)) {
      return thumbnailNode.getProperty(thumbnailType).getStream();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void addThumbnailImage(Node thumbnailNode, BufferedImage image, String propertyName) throws Exception {
    if(propertyName.equals(SMALL_SIZE)) parseImageSize(thumbnailNode, image, smallSize_, SMALL_SIZE);
    else if(propertyName.equals(MEDIUM_SIZE)) parseImageSize(thumbnailNode, image, mediumSize_, MEDIUM_SIZE);
    else if(propertyName.equals(BIG_SIZE)) parseImageSize(thumbnailNode, image, bigSize_, BIG_SIZE);
    else parseImageSize(thumbnailNode, image, propertyName.substring(4), propertyName, false);
  }

  /**
   * {@inheritDoc}
   */
  public void createSpecifiedThumbnail(Node node, BufferedImage image, String propertyName) throws Exception {
    addThumbnailImage(addThumbnailNode(node), image, propertyName);
  }

  /**
   * {@inheritDoc}
   */
  public void createThumbnailImage(Node node, BufferedImage image, String mimeType) throws Exception {
    Node thumbnailNode = addThumbnailNode(node);
    if(thumbnailNode != null) {
      if(mimeType.startsWith("image")) processImage2Image(thumbnailNode, image);
      thumbnailNode.setProperty(THUMBNAIL_LAST_MODIFIED, new GregorianCalendar());
      thumbnailNode.getSession().save();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void processThumbnailList(List<Node> listNodes, String type) throws Exception {
    for(Node node : listNodes) {
      Node thumbnailNode = addThumbnailNode(node);
      if(thumbnailNode != null && !thumbnailNode.hasProperty(THUMBNAIL_LAST_MODIFIED) &&
          node.isNodeType(NT_FILE)) {
        Node contentNode = node.getNode(JCR_CONTENT);
        if(contentNode.getProperty(JCR_MIMETYPE).getString().startsWith("image")) {
          BufferedImage image = ImageIO.read(contentNode.getProperty(JCR_DATA).getStream());
          addThumbnailImage(thumbnailNode, image, type);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getMimeTypes() {
    return Arrays.asList(mimeTypes_.split(";"));
  }

  /**
   * {@inheritDoc}
   */
  public Node addThumbnailNode(Node node) throws Exception {
    Node parentNode = node.getParent();
    Node thumbnailFolder = ThumbnailUtils.getThumbnailFolder(parentNode);
    String identifier = ((NodeImpl) node).getInternalIdentifier();
    Node thumbnailNode = ThumbnailUtils.getThumbnailNode(thumbnailFolder, identifier);
    return thumbnailNode;
  }

  /**
   * {@inheritDoc}
   */
  public Node getThumbnailNode(Node node) throws Exception {
    try {
      String nodePath = node.getPath();
      String thumPath = nodePath.replace(nodePath.substring(nodePath.lastIndexOf("/") + 1), EXO_THUMBNAILS_FOLDER);
      Node thumbnailFolder = (Node)node.getSession().getItem(thumPath);
      return thumbnailFolder.getNode(((NodeImpl) node).getInternalIdentifier());
    } catch(Exception e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void processRemoveThumbnail(Node showingNode) throws Exception {
    Node parentNode = showingNode.getParent();
    if(parentNode.hasNode(EXO_THUMBNAILS_FOLDER)) {
      Node thumbnailFolder = parentNode.getNode(EXO_THUMBNAILS_FOLDER);
      try {
        String workspace = parentNode.getSession().getWorkspace().getName();
        RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
        Session systemSession = WCMCoreUtils.getSystemSessionProvider().getSession(workspace, repositoryService.getCurrentRepository());
        thumbnailFolder = (Node) systemSession.getItem(thumbnailFolder.getPath());
        thumbnailFolder.getNode(((NodeImpl) showingNode).getInternalIdentifier()).remove();
        thumbnailFolder.getSession().save();
      } catch(PathNotFoundException path) {
        return;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void copyThumbnailNode(Node srcThumbnailNode, Node destNode) throws Exception {

    // Validate arguments
    if (srcThumbnailNode == null || destNode == null) {
      return;
    }

    // Copy thumbnail to destination node
    Node destThumbnailNode = this.addThumbnailNode(destNode);
    if (srcThumbnailNode.hasProperty(SMALL_SIZE)) {
      destThumbnailNode.setProperty(SMALL_SIZE, srcThumbnailNode.getProperty(SMALL_SIZE).getValue());
    }

    if (srcThumbnailNode.hasProperty(MEDIUM_SIZE)) {
      destThumbnailNode.setProperty(MEDIUM_SIZE, srcThumbnailNode.getProperty(MEDIUM_SIZE).getValue());
    }

    if (srcThumbnailNode.hasProperty(BIG_SIZE)) {
      destThumbnailNode.setProperty(BIG_SIZE, srcThumbnailNode.getProperty(BIG_SIZE).getValue());
    }

    destThumbnailNode.save();
  }

  public void addPlugin(ComponentPlugin plugin) {
    if(plugin instanceof ThumbnailPlugin) plugins_.add(plugin);
  }

  public List<ComponentPlugin> getComponentPlugins() {
    return plugins_;
  }

  /**
   * Put data from image to 3 property : exo:smallSizes, exo:mediumSizes, exo:bigSizes
   * with each property, image is parsed to correlative size
   * @param node
   * @param image
   * @throws Exception
   */
  private void processImage2Image(Node node, BufferedImage image) throws Exception {
    parseImageSize(node, image, smallSize_, SMALL_SIZE);
    parseImageSize(node, image, mediumSize_, MEDIUM_SIZE);
    parseImageSize(node, image, bigSize_, BIG_SIZE);
  }

  /**
   * Put image data to property name of node with given height and width
   * @param thumbnailNode
   * @param image
   * @param width
   * @param height
   * @param propertyName
   * @throws Exception
   */
  private void createThumbnailImage(Node thumbnailNode, BufferedImage image, int width, int height,
      String propertyName, boolean crop) throws Exception {
    if (width>1600) width=1600;
    if (height>1600) height=1600;
    InputStream thumbnailStream = ImageUtils.scaleImage(image, width, height, crop);
    try {
      thumbnailNode.setProperty(propertyName, thumbnailStream);
      thumbnailNode.getSession().save();
      thumbnailNode.setProperty(THUMBNAIL_LAST_MODIFIED, new GregorianCalendar());
      thumbnailNode.getSession().save();
    } catch (ItemExistsException e) {
      return;
    } finally {
      thumbnailStream.close();
    }
  }

  /**
   * Analysis size which has format (width x height) and call method createThumbnailImage
   * to put data into propertyName of node
   * @param node
   * @param image
   * @param size
   * @param propertyName
   * @throws Exception
   */
  private void parseImageSize(Node node, BufferedImage image, String size, String propertyName) throws Exception {
    parseImageSize(node, image, size, propertyName, false);
  }
  /**
   * Analysis size which has format (width x height) and call method createThumbnailImage
   * to put data into propertyName of node
   * @param node
   * @param image
   * @param size
   * @param propertyName
   * @throws Exception
   */
  private void parseImageSize(Node node, BufferedImage image, String size, String propertyName, boolean crop) throws Exception {
    int width = 0;
    int height = 0;
    if (size.startsWith("x")) {
      height = Integer.parseInt(size.substring(1));
    } else if (size.endsWith("x")) {
      width = Integer.parseInt(size.substring(0, size.length()-1));
    } else if(size.indexOf("x") > -1) {
      String[] imageSize = size.split("x");
      width = Integer.parseInt(imageSize[0]);
      height = Integer.parseInt(imageSize[1]);
    }
    createThumbnailImage(node, image, width, height, propertyName, crop);
  }
}
