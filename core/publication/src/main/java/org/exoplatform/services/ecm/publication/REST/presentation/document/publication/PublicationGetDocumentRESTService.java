/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.REST.presentation.document.publication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL Author : Ly Dinh Quang
 * quang.ly@exoplatform.com xxx5669@gmail.com Feb 19, 2009
 * modified hunghvit@gmail.com
 * May 17, 2009
 */
@Path("/publication/presentation/")
public class PublicationGetDocumentRESTService implements ResourceContainer {

  private RepositoryService  repositoryService_;

  private PublicationService publicationService_;

  private ManageDriveService manageDriveService_;

  final static public String DEFAULT_ITEM = "5";

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  private static final Log LOG  = ExoLogger.getLogger(PublicationGetDocumentRESTService.class);

  public PublicationGetDocumentRESTService(RepositoryService repositoryService,
      PublicationService publicationService, ManageDriveService manageDriveService) {
    repositoryService_ = repositoryService;
    publicationService_ = publicationService;
    manageDriveService_ = manageDriveService;
  }

  /**
   * <p>Get the document which is published</p>
   * ex:
   * /portal/rest/publication/presentation/{repository}/{workspace}/{state}?showItems={numberOfItem}
   *
   * @param repoName      Repository name
   * @param wsName        Workspace name
   * @param state         The state is specified to classify the process
   * @return
   * @throws Exception
   */
  @Path("/{repository}/{workspace}/{state}/")
  @GET
//  @OutputTransformer(Bean2JsonOutputTransformer.class)
  public Response getPublishDocument(@PathParam("repository") String repoName, @PathParam("workspace")
  String wsName, @PathParam("state") String state, @QueryParam("showItems")
  String showItems) throws Exception {
    return getPublishDocuments(wsName, state, null, showItems);
  }

  /**
   * <p>Get the document which is published</p>
   * ex: /portal/rest/publication/presentation/{repository}/{workspace}/{publicationPluginName}/
   * {state}?showItems={numberOfItem}
   *
   * @param repoName                Repository name
   * @param wsName                  Workspace name
   * @param publicationPluginName   Plugin name
   * @param state                   The state is specified to classify the process
   * @return
   * @throws Exception
   */
  @Path("/{repository}/{workspace}/{publicationPluginName}/{state}/")
  @GET
//  @OutputTransformer(Bean2JsonOutputTransformer.class)
  public Response getPublishedListDocument(@PathParam("repository") String repoName, @PathParam("workspace")
  String wsName, @PathParam("publicationPluginName") String pluginName, @PathParam("state")
  String state, @QueryParam("showItems")
  String showItems) throws Exception {
    return getPublishDocuments(wsName, state, pluginName, showItems);
  }

  @SuppressWarnings("unused")
  private Response getPublishDocuments(String wsName,
                                       String state,
                                       String pluginName,
                                       String itemPage) throws Exception {
    List<PublishedNode> publishedNodes = new ArrayList<PublishedNode>();
    PublishedListNode publishedListNode = new PublishedListNode();
    if(itemPage == null) itemPage = DEFAULT_ITEM;
    int item = Integer.parseInt(itemPage);
    String queryStatement = "select * from publication:publication order by exo:dateModified ASC";
    SessionProvider provider = WCMCoreUtils.createAnonimProvider();
    Session session = provider.getSession(wsName, repositoryService_.getCurrentRepository());
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    QueryImpl query = (QueryImpl) queryManager.createQuery(queryStatement, Query.SQL);
    query.setLimit(item);
    query.setOffset(0);
    QueryResult queryResult = query.execute();
    NodeIterator iter = queryResult.getNodes();
    List<Node> listNode = getNodePublish(iter, pluginName);
    Collections.sort(listNode, new DateComparator());
    if(listNode.size() < item) item = listNode.size();
    List<DriveData> lstDrive = manageDriveService_.getAllDrives();
    for (int i = 0; i < item; i++) {
      PublishedNode publishedNode = new PublishedNode();
      Node node = listNode.get(i);
      publishedNode.setName(node.getName());
      publishedNode.setPath(node.getPath());
      publishedNode.setDatePublished(getPublishedDateTime(node));
      publishedNode.setDriveName(getDriveName(lstDrive, node));
      publishedNodes.add(publishedNode);
    }
    publishedListNode.setPublishedListNode(publishedNodes);
    session.logout();
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(publishedListNode, new MediaType("application", "json"))
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

  private List<Node> getNodePublish(NodeIterator iter, String pluginName) throws Exception {
    List<Node> listNode = new ArrayList<Node>();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      Node nodecheck = publicationService_.getNodePublish(node, pluginName);
      if (nodecheck != null) {
        listNode.add(node);
      }
    }
    return listNode;
  }

  private String getPublishedDateTime(Node currentNode) throws Exception {
    Value[] history = currentNode.getProperty("publication:history").getValues();
    String time = currentNode.getProperty("exo:dateModified").getString();
    for (Value value : history) {
      String[] arrHistory = value.getString().split(";");
      time = arrHistory[3];
    }
    return String.valueOf(ISO8601.parse(time).getTimeInMillis());
  }

  private static class DateComparator implements Comparator<Node> {
    public int compare(Node node1, Node node2) {
      try {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        Date date1 = formatter.parse(getDateTime(node1));
        Date date2 = formatter.parse(getDateTime(node2));
        return date2.compareTo(date1);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
      return 0;
    }

    private String getDateTime(Node currentNode) throws Exception {
      Value[] history = currentNode.getProperty("publication:history").getValues();
      for (Value value : history) {
        String[] arrHistory = value.getString().split(";");
        return arrHistory[3];
      }
      return currentNode.getProperty("exo:dateModified").getString();
    }
  }

  private String getDriveName(List<DriveData> lstDrive, Node node) throws RepositoryException{
    String driveName = "";
    for (DriveData drive : lstDrive) {
      if (node.getSession().getWorkspace().getName().equals(drive.getWorkspace())
          && node.getPath().contains(drive.getHomePath()) && drive.getHomePath().equals("/")) {
        driveName = drive.getName();
        break;
      }
    }
    return driveName;
  }

  public class PublishedNode {

    private String nodeName_;
    private String nodePath_;
    private String driveName_;
    private String datePublished_;

    public void setName(String nodeName) { nodeName_ = nodeName; }
    public String getName() { return nodeName_; }

    public void setPath(String nodePath) { nodePath_ = nodePath; }
    public String getPath() { return nodePath_; }

    public void setDriveName(String driveName) { driveName_ = driveName; }
    public String getDriveName() { return driveName_; }

    public void setDatePublished(String datePublished) { datePublished_ = datePublished; }
    public String getDatePublished() { return datePublished_; }

  }

  public class PublishedListNode {

    private List<PublishedNode> publishedListNode_;

    public void setPublishedListNode(List<PublishedNode> publishedListNode) {
      publishedListNode_ = publishedListNode;
    }
    public List<PublishedNode> getPublishedListNode() { return publishedListNode_; }

  }
}
