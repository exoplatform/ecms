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
package org.exoplatform.services.ecm.publication.REST.presentation.document.edit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.utils.comparator.PropertyValueComparator;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * May 17, 2009
 */


@Path("/presentation/document/edit/")
public class GetEditedDocumentRESTService implements ResourceContainer {

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  private RepositoryService   repositoryService;

  private TemplateService     templateService;

  private NewFolksonomyService   newFolksonomyService;

  private ManageDriveService   manageDriveService;

  private static final String DATE_MODIFIED   = "exo:dateModified";

  private static final String JCR_PRIMARYTYPE = "jcr:primaryType";

  private static final String NT_BASE         = "nt:base";

  private static final String EXO_OWNER       = "exo:owner";

  private static final int    NO_PER_PAGE     = 5;

  private static final String QUERY_STATEMENT = "SELECT * FROM $0 WHERE $1 ORDER BY $2 DESC";

  private static final String GADGET          = "gadgets";

  private boolean             show_gadget     = false;

  private Log LOG = ExoLogger.getLogger("cms.GetEditedDocumentRESTService");


  public GetEditedDocumentRESTService(RepositoryService repositoryService,
      TemplateService templateService, NewFolksonomyService newFolksonomyService, ManageDriveService manageDriveService) {
    this.repositoryService = repositoryService;
    this.templateService = templateService;
    this.newFolksonomyService = newFolksonomyService;
    this.manageDriveService = manageDriveService;
  }

  @Path("/{repository}/")
  @GET
  public Response getLastEditedDoc(@PathParam("repository") String repository,
      @QueryParam("showItems") String showItems, @QueryParam("showGadgetWs") String showGadgetWs) throws Exception {
    List<Node> lstLastEditedNode = getLastEditedNode(showItems, showGadgetWs);
    List<DocumentNode> lstDocNode = getDocumentData(lstLastEditedNode);
    ListEditDocumentNode listEditDocumentNode = new ListEditDocumentNode();
    listEditDocumentNode.setLstDocNode(lstDocNode);

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok(listEditDocumentNode, new MediaType("application", "json"))
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

  private List<Node> getLastEditedNode(String noOfItem, String showGadgetWs) throws Exception{
    if (showGadgetWs != null && showGadgetWs.length() > 0) {
      show_gadget = Boolean.parseBoolean(showGadgetWs);
    }
    ArrayList<Node>  lstNode = new ArrayList<Node>();
    StringBuffer bf = new StringBuffer(1024);
    List<String> lstNodeType = templateService.getDocumentTemplates();
    if (lstNodeType != null) {
      for (String nodeType : lstNodeType) {
        bf.append("(").append(JCR_PRIMARYTYPE).append("=").append("'").append(nodeType).append("'")
            .append(")").append(" OR ");
      }
    }

    if (bf.length() == 1) return null;
    bf.delete(bf.lastIndexOf("OR") - 1, bf.length());
    if (noOfItem == null || noOfItem.trim().length() == 0) noOfItem = String.valueOf(NO_PER_PAGE);
    String queryStatement = StringUtils.replace(QUERY_STATEMENT, "$0", NT_BASE);
    queryStatement = StringUtils.replace(queryStatement, "$1", bf.toString());
    queryStatement = StringUtils.replace(queryStatement, "$2", DATE_MODIFIED);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    try {
      String[] workspaces = manageableRepository.getWorkspaceNames();
      List<String> lstWorkspace = new ArrayList<String>();
      //Arrays.asList() return fixed size list;
      lstWorkspace.addAll(Arrays.asList(workspaces));
      if (!show_gadget && lstWorkspace.contains(GADGET)) {
        lstWorkspace.remove(GADGET);
      }
      SessionProvider provider = WCMCoreUtils.createAnonimProvider();
      QueryImpl query = null;
      Session session = null;
      QueryResult queryResult = null;
      QueryManager queryManager = null;
      for (String workspace : lstWorkspace) {
        session = provider.getSession(workspace, manageableRepository);
        queryManager = session.getWorkspace().getQueryManager();
        query = (QueryImpl) queryManager.createQuery(queryStatement, Query.SQL);
        query.setLimit(Integer.parseInt(noOfItem));
        query.setOffset(0);
        queryResult = query.execute();
        puttoList(lstNode, queryResult.getNodes());
        session.logout();
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when execute SQL " + queryStatement, e);
      }
    }
    return lstNode;
  }

  private void puttoList(List<Node> lstNode, NodeIterator nodeIter) {
    if (nodeIter != null) {
      while (nodeIter.hasNext()) {
        lstNode.add(nodeIter.nextNode());
      }
    }
  }

  private List<DocumentNode> getDocumentData(List<Node> lstNode) throws Exception {
    return getDocumentData(lstNode, String.valueOf(NO_PER_PAGE));
  }

  private String getDateFormat(Calendar date) {
    return String.valueOf(date.getTimeInMillis());
  }

  private List<DocumentNode> getDocumentData(List<Node> lstNode, String noOfItem) throws Exception {
    if (lstNode == null || lstNode.size() == 0) return null;
    List<DocumentNode> lstDocNode = new ArrayList<DocumentNode>();
    DocumentNode docNode = null;
    StringBuilder tags = null;

    Collections.sort(lstNode, new PropertyValueComparator(DATE_MODIFIED, PropertyValueComparator.DESCENDING_ORDER));
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    List<DriveData> lstDrive = manageDriveService.getAllDrives();
    for (Node node : lstNode) {
      docNode = new DocumentNode();
      docNode.setName(node.getName());
      docNode.setPath(node.getPath());
      docNode.setLastAuthor(node.getProperty(EXO_OWNER).getString());
      docNode.setLstAuthor(node.getProperty(EXO_OWNER).getString());
      docNode.setDateEdited(getDateFormat(node.getProperty(DATE_MODIFIED).getDate()));
      tags = new StringBuilder(1024);

      List<Node> tagList = newFolksonomyService.getLinkedTagsOfDocumentByScope(NewFolksonomyService.PUBLIC,
                                                                               "",
                                                                               node,
                                                                               manageableRepository.getConfiguration()
                                                                                                   .getDefaultWorkspaceName());
      for(Node tag : tagList) {
        tags.append(tag.getName()).append(", ");
      }

      if (tags.lastIndexOf(",") > 0) {
        tags.delete(tags.lastIndexOf(","), tags.length());
      }

      docNode.setTags(tags.toString());
      docNode.setDriveName(getDriveName(lstDrive, node));
      if (lstDocNode.size() < Integer.parseInt(noOfItem))  lstDocNode.add(docNode);
    }
    return lstDocNode;
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

  public class DocumentNode {

    private String nodeName_;

    private String nodePath_;

    private String driveName_;

    private String   dateEdited_;

    private String tags;

    private String   lastAuthor;

    private String lstAuthor;

    public String getTags() {
      return tags;
    }

    public void setTags(String tags) {
      this.tags = tags;
    }

    public String getLastAuthor() {
      return lastAuthor;
    }

    public void setLastAuthor(String lastAuthor) {
      this.lastAuthor = lastAuthor;
    }

    public String getLstAuthor() {
      return lstAuthor;
    }

    public void setLstAuthor(String lstAuthor) {
      this.lstAuthor = lstAuthor;
    }

    public void setName(String nodeName) {
      nodeName_ = nodeName;
    }

    public String getName() {
      return nodeName_;
    }

    public void setPath(String nodePath) {
      nodePath_ = nodePath;
    }

    public String getPath() {
      return nodePath_;
    }

    public void setDriveName(String driveName) {
      driveName_ = driveName;
    }

    public String getDriveName() {
      return driveName_;
    }

    public String getDateEdited() {
      return dateEdited_;
    }

    public void setDateEdited(String dateEdited_) {
      this.dateEdited_ = dateEdited_;
    }
  }

  public class ListEditDocumentNode {

    private List<DocumentNode> lstDocNode;

    public List<DocumentNode> getLstDocNode() {
      return lstDocNode;
    }

    public void setLstDocNode(List<DocumentNode> lstDocNode) {
      this.lstDocNode = lstDocNode;
    }
  }

}
