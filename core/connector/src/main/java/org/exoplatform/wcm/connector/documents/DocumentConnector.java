/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wcm.connector.documents;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.services.cms.impl.Utils;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.cms.drives.DriveData;
import org.codehaus.groovy.util.ListHashMap;
import org.exoplatform.wcm.connector.BaseConnector;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.rest.resource.ResourceContainer;

/**
 * The DocumentConnector aims to manage and use comments for the content.
 *
 * @LevelAPI Experimental
 *
 * @anchor CommentConnector
 */
@Path("/document/")
public class DocumentConnector extends BaseConnector implements ResourceContainer {
    
    private DocumentService documentService;
    private DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    
    public DocumentConnector(DocumentService documentService) {
      this.documentService = documentService;
    }
    
    @GET
    @Path("/docOpenUri")
    public Response getDocOpenUri(@QueryParam("nodePath") String nodePath) {
        try {
            Map<String, List<String>> uris = new ListHashMap<>();
            if (nodePath.startsWith("/")) {
              nodePath = nodePath.substring(1);
            }
            if (nodePath.endsWith("/")) {
              nodePath = nodePath.substring(0, nodePath.length() - 1);
            }
            String path = "";
            for (String nodeName : nodePath.split("/")) {
              path += "/" + nodeName;
              try {
                DriveData drive = documentService.getDriveOfNode(path);
                Node docNode = NodeLocation.getNodeByExpression(
                        WCMCoreUtils.getRepository().getConfiguration().getName() + ":" +
                        drive.getWorkspace() + ":" + path);
                String nodeTitle = Utils.getTitle(docNode);
                
                String docLink = documentService.getLinkInDocumentsApp(path);
                
                List<String> titleAndLink = new ArrayList<>();
                titleAndLink.add(nodeTitle);
                titleAndLink.add(docLink);
                
                uris.put(path, titleAndLink);
              } catch (Exception e) {
                //normal case, ok
              }
            }
            
            return Response.ok(uris, MediaType.APPLICATION_JSON_TYPE)
                    .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                    .build();
        } catch (Exception e){
            return Response.serverError().build();
        }
    }

    @Override
    protected Node getRootContentStorage(Node node) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getContentStorageType() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
