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
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.connector.BaseConnector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The CommentConnector aims to manage and use comments for the content.
 *
 * @LevelAPI Experimental
 *
 * @anchor CommentConnector
 */
@Path("/contents/comment/")
public class CommentConnector extends BaseConnector implements ResourceContainer {

  CommentsService commentsService;

  /**
   * Instantiates a new tag connector.
   *
   * @param commentsService Service instantiation.
   */
  public CommentConnector(CommentsService commentsService) {
    this.commentsService = commentsService;
  }


  /**
   *
   * Adds a new comment to the content.
   *
   * @param jcrPath The JCR path of the content.
   * @param comment The comment to add.
   * @return The last modified date as a property to check the result.
   * @throws Exception The exception
   *
   * @anchor CommentConnector.addComment
   */
  @POST
  @Path("/add")
  public Response addComment(
          @FormParam("jcrPath") String jcrPath,
          @FormParam("comment") String comment
          ) throws Exception {

    Node content = getNode(jcrPath);

    commentsService.addComment(content, content.getSession().getUserID(), null, null, comment,null);

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();

  }

  /**
   * Gets all comments for a specific content.
   *
   * @param jcrPath The JCR path of the content.
   * @return All comments
   * @throws Exception The exception
   *
   * @anchor CommentConnector.getComments
   */
  @GET
  @Path("/all")
  public Response getComments(
          @QueryParam("jcrPath") String jcrPath
          ) throws Exception {

    try {
      Node content = getNode(jcrPath);

      List<Node> comments = commentsService.getComments(content, null);

      Document document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element tagsElt = document.createElement("comments");

      for (Node comment:comments) {
        Element tagElt = document.createElement("comment");
        //exo:name
        Element id = document.createElement("id");
        id.setTextContent(comment.getName());
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
        date.setTextContent(DateFormat.getDateTimeInstance().format(comment.getProperty("exo:commentDate").getDate().getTime()));

        //exo:commentContent
        Element commentElt = document.createElement("content");
        commentElt.setTextContent(comment.getProperty("exo:commentContent").getString());

        tagElt.appendChild(id);
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

  /**
   *
   * Delete a comment of a content.
   *
   * @param jcrPath The JCR path of the content.
   * @param commentId The id of the comment to delete.
   * @return
   * @throws Exception The exception
   *
   * @anchor CommentConnector.deleteComment
   */
  @DELETE
  @Path("/delete")
  public Response deleteComment(
          @QueryParam("jcrPath") String jcrPath,
          @QueryParam("commentId") String commentId
  ) throws Exception {

    if(StringUtils.isEmpty(jcrPath) || StringUtils.isEmpty(commentId)) {
      return Response.status(400).entity("jcrPath and commentId query parameters are mandatory").build();
    }

    Node content = getNode(jcrPath);

    if(content.hasNode("comments")) {
      Node commentsNode = content.getNode("comments");
      if(commentsNode.hasNode(commentId)) {
        commentsService.deleteComment(commentsNode.getNode(commentId));
      } else {
        return Response.noContent().build();
      }
    } else {
      return Response.noContent().build();
    }

    return Response.ok().build();
  }

  /**
   * Get the jcr node of the given jcr path
   * @param jcrPath
   * @return The node of the given jcr path
   * @throws Exception
   */
  protected Node getNode(@QueryParam("jcrPath") String jcrPath) throws Exception {
    if (jcrPath.contains("%20")) {
      jcrPath = URLDecoder.decode(jcrPath, "UTF-8");
    }
    String[] path = jcrPath.split("/");
    String repositoryName = path[1];
    String workspaceName = path[2];
    jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
    if (jcrPath.charAt(1)=='/') {
      jcrPath.substring(1);
    }
    return getContent(workspaceName, jcrPath, null, false);
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
