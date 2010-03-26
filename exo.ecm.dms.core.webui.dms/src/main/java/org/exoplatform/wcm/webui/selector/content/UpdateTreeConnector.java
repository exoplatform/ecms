/*
 * Copyright (C) 2003-2007 eXo Platform SEA.
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
package org.exoplatform.wcm.webui.selector.content;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.wcm.webui.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SEA
 * Author : Ha Mai Van
 * maivanha1610@gmail.com
 * Sep 7, 2009
 */
@Path("/wcmTreeContent/")
public class UpdateTreeConnector implements ResourceContainer {
  
  /** The Constant FILE_TYPE_WEBCONTENT. */
  public static final String FILE_TYPE_WEBCONTENT                  = "Web Contents"; 
  
  /** The Constant FILE_TYPE_DMSDOC. */
  public static final String FILE_TYPE_DMSDOC                      = "DMS Documents"; 
  
  /** The Constant FILE_TYPE_MEDIAS. */
  public static final String FILE_TYPE_MEDIAS                      = "Medias"; 
  
  @GET
  @Path("/getChildNodes/")
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response getChildNodes(@QueryParam("nodePath") String nodePath,
                              @QueryParam("workspaceName") String workspaceName,
                              @QueryParam("repositoryName") String repositoryName) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    try {
      SessionProvider sessionProvider = Utils.getSystemSessionProvider();
      RepositoryService repositoryService = (RepositoryService)ExoContainerContext.getCurrentContainer()
        .getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getRepository(repositoryName);
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
      Node node = (Node)session.getItem(nodePath);
      NodeIterator nodeIterator = node.getNodes();
      int index = 0;
      Element parentNode = document.createElement("childNodes");
      document.appendChild(parentNode);
      StringBuilder buffer;
      Element childNode;
      Element workSpaceEle;
      Element repositoryEle;
      Element nameEle;
      Element nodePathEle;
      while(nodeIterator.hasNext()){
        node = nodeIterator.nextNode();
        if (!node.isNodeType(NodetypeConstant.EXO_WEBCONTENT) && !node.isNodeType(NodetypeConstant.EXO_HIDDENABLE) &&
            (node.isNodeType(NodetypeConstant.EXO_TAXONOMY) || node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED) || node.isNodeType(NodetypeConstant.NT_FOLDER)) ) {
          buffer = new StringBuilder(128);
          buffer.append(node.getName());
          index = node.getIndex();
          if (index > 1) {
            buffer.append('[');
            buffer.append(index);
            buffer.append(']');
          }
          childNode = document.createElement("childNode");
          parentNode.appendChild(childNode);
          workSpaceEle = document.createElement("workspaceName");
          workSpaceEle.setTextContent(workspaceName);
          repositoryEle = document.createElement("repositoryName");
          repositoryEle.setTextContent(repositoryName);
          nameEle = document.createElement("name");
          nameEle.setTextContent(buffer.toString());
          nodePathEle = document.createElement("nodePath");
          nodePathEle.setTextContent(node.getPath());
          
          childNode.appendChild(workSpaceEle);
          childNode.appendChild(repositoryEle);
          childNode.appendChild(nameEle);
          childNode.appendChild(nodePathEle);
        }
      }
    } catch (Exception e) {
    }
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
  }
  
}
