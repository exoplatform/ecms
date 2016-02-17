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
package org.exoplatform.wcm.connector.collaboration;

import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.connector.BaseConnector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Created by The eXo Platform SAS
 * Author : Benjamin Paillereau
 * benjamin.paillereau@exoplatform.com
 * March 25, 2011
 */
/**
 * Adds and queries the public tags of a document.
 */
@Path("/contents/tag/")
public class TagConnector extends BaseConnector implements ResourceContainer {

  NewFolksonomyService tagService;

  /**
   * Instantiates a new tag connector.
   */
  public TagConnector(NewFolksonomyService tagService) {
    this.tagService = tagService;
  }


  /**
   * Adds a public tag to a given document.
   * 
   * @param tag The tag to be added.
   * 
   * @param jcrPath The path of the document, e.g. /portal/collaboration/test/document1, 
   *        in which "portal" is the repository, "collaboration" is the workspace.
   * 
   * @request POST: http://localhost:8080/rest/private/contents/tag/add/tag1?jcrPath=/portal/collaboration/test/document1
   * 
   * @response HTTP Status code. 200 on success. 500 if any error during the process.
   * 
   * @anchor TagConnector.addTag
   */
  @POST
  @Path("/add/{tag}")
  public Response addTag(
          @PathParam("tag") String tag,
          @QueryParam("jcrPath") String jcrPath
          ) throws Exception {

    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);
    Node content = getContent(workspaceName, jcrPath, null, false);

//    tagService.addPublicTag("/Application Data/Tags", new String[]{tag}, content,  workspaceName);
    tagService.addPublicTag("/tags", new String[]{tag}, content, workspaceName);

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

  }

  /**
   * Gets the list of public tags of a given document.
   * 
   * @param jcrPath The path of the document, e.g. /portal/collaboration/test/document1, 
   *        in which "portal" is the repository, "collaboration" is the workspace.
   * 
   * @request GET: http://localhost:8080/rest/private/contents/tag/public?jcrPath=/portal/collaboration/test/document1
   * 
   * @format XML
   * 
   * @response The tag names in XML format.
   * {@code
   * <?xml version="1.0" encoding="UTF-8" standalone="no" ?>
   * <tags>
   *  <tag name="gold" />
   *  <tag name="silver" />
   * </tags>
   * }
   * 
   * @anchor TagConnector.getPublicTags
   */
  @GET
  @Path("/public")
  public Response getPublicTags(
          @QueryParam("jcrPath") String jcrPath
          ) throws Exception {

    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);

    try {
      Node content = getContent(workspaceName, jcrPath, null, false);

      List<Node> tags = tagService.getLinkedTagsOfDocumentByScope(NewFolksonomyService.PUBLIC,
                                                                  "",
                                                                  content,
                                                                  workspaceName);

      Document document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element tagsElt = document.createElement("tags");

      for (Node tag:tags) {
        Element element = document.createElement("tag");
        element.setAttribute("name", tag.getName());
        tagsElt.appendChild(element);
      }
      document.appendChild(tagsElt);

      DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
      return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    } catch (Exception e){
      Response.serverError().build();
    }


    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

  }



  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.wcm.connector.fckeditor.BaseConnector#getRootContentStorage
   * (javax.jcr.Node)
   */
  @Override
  protected Node getRootContentStorage(Node parentNode) throws Exception {
    try {
      PortalFolderSchemaHandler folderSchemaHandler =
        webSchemaConfigService.getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
      return folderSchemaHandler.getDocumentStorage(parentNode);
    } catch (Exception e) {
      WebContentSchemaHandler webContentSchemaHandler =
        webSchemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
      return webContentSchemaHandler.getDocumentFolder(parentNode);
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.wcm.connector.fckeditor.BaseConnector#getContentStorageType
   * ()
   */
  @Override
  protected String getContentStorageType() throws Exception {
    return FCKUtils.DOCUMENT_TYPE;
  }

}
