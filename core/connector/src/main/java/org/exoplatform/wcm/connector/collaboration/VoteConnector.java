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
 * July 10, 2009
 */
@Path("/contents/vote/")
public class VoteConnector extends BaseConnector implements ResourceContainer {

  /**
   * Instantiates a new vote connector.
   */
  public VoteConnector() {}


  /**
   * post a Vote for a content
   *
   * @param jcrPath the jcr path
   *
   * @return http code
   *
   * @throws Exception the exception
   */
  @POST
  @Path("/star/")
//  @InputTransformer(PassthroughInputTransformer.class)
  public Response postStarVote(
          @FormParam("jcrPath") String jcrPath,
          @FormParam("vote") String vote
          ) throws Exception {

    if (jcrPath.contains("%20")) jcrPath = URLDecoder.decode(jcrPath, "UTF-8");

    return postVote(null, null,  jcrPath,  vote,  "en" );
  }

    /**
   * get a Vote for a content
   *
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   *
   * @return http code
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/star/")
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response getStarVote(
      @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("jcrPath") String jcrPath) throws Exception {

    return getVote(repositoryName,  workspaceName,  jcrPath);
  }


  /**
   * post a Vote for a content
   *
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   *
   * @return http code
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/postVote/")
//  @InputTransformer(PassthroughInputTransformer.class)
  public Response postVote(
      @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("jcrPath") String jcrPath,
      @QueryParam("vote") String vote,
      @QueryParam("lang") String lang
      ) throws Exception {
    try {

      if (repositoryName==null && workspaceName==null) {
        String[] path = jcrPath.split("/");
        repositoryName = path[1];
        workspaceName = path[2];
        jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
        if (jcrPath.charAt(1)=='/') jcrPath.substring(1);
      }
      Node content = getContent(repositoryName, workspaceName, jcrPath, null, false);
      if (content.isNodeType("mix:votable")) {
        String userName = content.getSession().getUserID();
        votingService.vote(content, Double.parseDouble(vote), userName, lang);
      }
    } catch (Exception e) {
      Response.serverError().build();
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  /**
   * get a Vote for a content
   *
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param jcrPath the jcr path
   *
   * @return http code
   *
   * @throws Exception the exception
   */
  @GET
  @Path("/getVote/")
//  @OutputTransformer(XMLOutputTransformer.class)
  public Response getVote(
      @QueryParam("repositoryName") String repositoryName,
      @QueryParam("workspaceName") String workspaceName,
      @QueryParam("jcrPath") String jcrPath) throws Exception {
    try {

      if (repositoryName==null && workspaceName==null) {
        String[] path = jcrPath.split("/");
        repositoryName = path[1];
        workspaceName = path[2];
        jcrPath = jcrPath.substring(repositoryName.length()+workspaceName.length()+2);
        if (jcrPath.charAt(1)=='/') jcrPath.substring(1);
      }
      Node content = getContent(repositoryName, workspaceName, jcrPath);
      if (content.isNodeType("mix:votable")) {
        String votingRate = "";
        if (content.hasProperty("exo:votingRate"))
          votingRate = content.getProperty("exo:votingRate").getString();
        String votingTotal = "";
        if (content.hasProperty("exo:voteTotalOfLang"))
          votingTotal = content.getProperty("exo:voteTotalOfLang").getString();

        Document document =
          DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element element = document.createElement("vote");
        Element rate = document.createElement("rate");
        rate.setTextContent(votingRate);
        Element total = document.createElement("total");
        total.setTextContent(votingTotal);
        element.appendChild(rate);
        element.appendChild(total);
        document.appendChild(element);

        DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
        return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                       .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
      }
    } catch (Exception e) {
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
