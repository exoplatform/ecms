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

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * May 28, 2008
 */
public class WebContentSchemaHandler extends BaseWebSchemaHandler {

  protected String getHandlerNodeType() { return "exo:webContent"; }
  protected String getParentNodeType() { return "nt:unstructured"; }

  public boolean matchHandler(SessionProvider sessionProvider, Node node) throws Exception {
    String handlerNodeType = getHandlerNodeType();
    String parentNodeType = getParentNodeType();
    if(!node.isNodeType(handlerNodeType))
      return false;
    if(!node.getParent().isNodeType(parentNodeType))
      return false;
    return true;
  }

  public void onCreateNode(SessionProvider sessionProvider, final Node webContent) throws Exception {
    createSchema(webContent);
    webContent.getParent().save();
  }

  public Node getCSSFolder(final Node webContent) throws Exception {
    return webContent.getNode("css");
  }

  public Node getJSFolder(final Node webContent) throws Exception {
    return webContent.getNode("js");
  }

  public Node getImagesFolders(final Node webContent) throws Exception {
    return webContent.getNode("medias/images");
  }

  public Node getIllustrationImage(final Node webContent) throws Exception {
    return webContent.getNode("medias/images/illustration");
  }

  public Node getVideoFolder(final Node webContent) throws Exception {
    return webContent.getNode("medias/videos");
  }

  public Node getDocumentFolder (final Node webContent) throws Exception {
    return webContent.getNode("documents");
  }

  public void createDefaultSchema(Node webContent) throws Exception{
    addMixin(webContent,"exo:owneable");
    createSchema(webContent);
    createDefautWebData(webContent);
  }

  protected void createSchema(final Node webContent) throws Exception {
    if (!webContent.hasNode("js")) {
      Node js = webContent.addNode("js","exo:jsFolder");
      addMixin(js,"exo:owneable");
    }
    if (!webContent.hasNode("css")) {
      Node css = webContent.addNode("css","exo:cssFolder");
      addMixin(css,"exo:owneable");
    }
    if (!webContent.hasNode("medias")) {
      Node multimedia = webContent.addNode("medias","exo:multimediaFolder");
      addMixin(multimedia,"exo:owneable");
      Node images = multimedia.addNode("images",NT_FOLDER);
      addMixin(images, "exo:pictureFolder");
      addMixin(images,"exo:owneable");
      Node video = multimedia.addNode("videos",NT_FOLDER);
      addMixin(video, "exo:videoFolder");
      addMixin(video,"exo:owneable");
      Node audio = multimedia.addNode("audio",NT_FOLDER);
      addMixin(audio, "exo:musicFolder");
      addMixin(audio,"exo:owneable");
    }
    if (!webContent.hasNode("documents")) {
      Node document = webContent.addNode("documents",NT_UNSTRUCTURED);
      addMixin(document, "exo:documentFolder");
      addMixin(document,"exo:owneable");
    }
    //because exo:webcontent is exo:rss-enable so need set exo:title of the webcontent
    //by default, value of exo:title is webcontent name
    webContent.setProperty("exo:title", webContent.getName());
  }

  public boolean isWebcontentChildNode(Node file) throws Exception{
    Node parent = file.getParent();
    //for sub nodes of the webcontent node
    if(parent.isNodeType("exo:webContent"))
      return true;
    //for subnodes in some folders like css, js, documents, medias
    if(parent.getPath().equals("/"))
      return false;
    Node grantParent = parent.getParent();
    if(grantParent.isNodeType("exo:webContent"))
      return true;
    //for subnodes in some folders like images, videos, audio
    if(grantParent.getPath().equals("/"))
      return false;
    Node ansestor = grantParent.getParent();
    if(ansestor.isNodeType("exo:webContent"))
      return true;
    return false;
  }
}
