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
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.cms.comments.CommentsService;
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
@Path("/contents/comment/")
public class CommentConnector extends BaseConnector implements ResourceContainer {

  CommentsService commentsService;

  /**
   * Instantiates a new tag connector.
   */
  public CommentConnector(CommentsService commentsService) {
    this.commentsService = commentsService;
  }


  /**
   * to complete
   */
  @POST
  @Path("/add")
  public Response addComment(
          @FormParam("jcrPath") String jcrPath,
          @FormParam("comment") String comment
          ) throws Exception {

    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);
    Node content = getContent(repositoryName, workspaceName, jcrPath, null, false);

    commentsService.addComment(content, content.getSession().getUserID(), null, null, comment,null);

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

  }

  /**
   * to complete
   */
  @GET
  @Path("/all")
  public Response getComments(
          @QueryParam("jcrPath") String jcrPath
          ) throws Exception {

    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') jcrPath.substring(1);

    try {
      Node content = getContent(repositoryName, workspaceName, jcrPath, null, false);

      List<Node> comments = commentsService.getComments(content, null);

      Document document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element tagsElt = document.createElement("comments");

      for (Node comment:comments) {
        Element tagElt = document.createElement("comment");
        //exo:commentor
        Element commentor = document.createElement("commentor");
        commentor.setTextContent(comment.getProperty("exo:commentor").getString());
        //exo:commentorEmail
        Element commentorEmail = document.createElement("email");
        if (comment.hasProperty("exo:commentorEmail")) {
          commentorEmail.setTextContent(comment.getProperty("exo:commentorEmail").getString());
        }
        //exo:commentDate
        Element date = document.createElement("date");
        date.setTextContent(comment.getProperty("exo:commentDate").getDate().getTime().toLocaleString());
        //exo:commentContent
        Element commentElt = document.createElement("content");
        commentElt.setTextContent(comment.getProperty("exo:commentContent").getString());

        tagElt.appendChild(commentor);
        tagElt.appendChild(commentorEmail);
        tagElt.appendChild(date);
        tagElt.appendChild(commentElt);
        tagsElt.appendChild(tagElt);
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
