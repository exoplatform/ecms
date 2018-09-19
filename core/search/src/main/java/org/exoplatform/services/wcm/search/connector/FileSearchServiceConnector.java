/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.search.connector;

import org.apache.commons.lang.LocaleUtils;
import org.codehaus.groovy.util.ListHashMap;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.ElasticSearchServiceConnector;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.search.base.EcmsSearchResult;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.controller.QualifiedName;
import org.json.simple.JSONObject;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Search connector for files
 */
public class FileSearchServiceConnector extends ElasticSearchServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger(FileSearchServiceConnector.class.getName());

  private RepositoryService repositoryService;

  private DocumentService documentService;

  public FileSearchServiceConnector(InitParams initParams, ElasticSearchingClient client, RepositoryService repositoryService, DocumentService documentService) {
    super(initParams, client);
    this.repositoryService = repositoryService;
    this.documentService = documentService;
  }

  @Override
  protected String getSourceFields() {
    List<String> fields = Arrays.asList("name",
            "title",
            "workspace",
            "path",
            "author",
            "createdDate",
            "lastUpdatedDate",
            "fileType",
            "fileSize");

    return fields.stream().map(field -> "\"" + field + "\"").collect(Collectors.joining(","));
  }

  @Override
  protected SearchResult buildHit(JSONObject jsonHit, SearchContext searchContext) {
    SearchResult searchResult = super.buildHit(jsonHit, searchContext);

    JSONObject hitSource = (JSONObject) jsonHit.get("_source");
    String workspace = (String) hitSource.get("workspace");
    String nodePath = (String) hitSource.get("path");
    String fileType = (String) hitSource.get("fileType");
    String fileSize = (String) hitSource.get("fileSize");

    String driveName = "";
    try {
      DriveData driveOfNode = documentService.getDriveOfNode(nodePath);
      if(driveOfNode != null) {
        driveName = driveOfNode.getName() + " - ";
      }
    } catch (Exception e) {
      LOG.warn("Cannot get drive of node " + nodePath, e);
    }

    String lang = searchContext.getParamValue(SearchContext.RouterParams.LANG.create());
    String detail = driveName + getFormattedFileSize(fileSize) + " - " + getFormattedDate(searchResult.getDate(), lang);

    SearchResult ecmsSearchResult = new EcmsSearchResult(getUrl(nodePath),
            getPreviewUrl(jsonHit, searchContext),
            searchResult.getTitle(),
            searchResult.getExcerpt(),
            detail,
            getImageUrl(workspace, nodePath),
            searchResult.getDate(),
            searchResult.getRelevancy(),
            fileType,
            nodePath,
            getBreadcrumb(nodePath));

    return ecmsSearchResult;
  }

  protected String getUrl(String nodePath) {
    String url = "";
    try {
      url = documentService.getLinkInDocumentsApp(nodePath);
    } catch (Exception e) {
      LOG.error("Cannot get url of document " + nodePath, e);
    }
    return url;
  }

  protected String getFormattedDate(long createdDateTime, String lang) {
    try {
      Locale locale = LocaleUtils.toLocale(lang);
      DateTimeFormatter df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(locale).withZone(ZoneId.systemDefault());
      return df.format(Instant.ofEpochMilli(createdDateTime));
    } catch (Exception e) {
      LOG.error("Cannot format date for timestamp " + createdDateTime, e);
      return "";
    }
  }

  protected String getFormattedFileSize(String fileSize) {
    try {
      Long size = Long.parseLong(fileSize);
      return Utils.formatSize(size);
    } catch (Exception e) {
      LOG.error("Cannot format file size " + fileSize, e);
      return "";
    }
  }

  protected String getPreviewUrl(JSONObject jsonHit, SearchContext context) {
    JSONObject hitSource = (JSONObject) jsonHit.get("_source");

    String id = (String) jsonHit.get("_id");
    String author = (String) hitSource.get("author");
    String title = (String) hitSource.get("title");
    String workspace = (String) hitSource.get("workspace");
    String nodePath = (String) hitSource.get("path");
    String fileType = (String) hitSource.get("fileType");

    String restContextName =  WCMCoreUtils.getRestContextName();
    String repositoryName = null;
    try {
      repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
    } catch (RepositoryException e) {
      LOG.error("Cannot get repository name", e);
    }

    StringBuffer downloadUrl = new StringBuffer();
    downloadUrl.append('/').append(restContextName).append("/jcr/").
            append(repositoryName).append('/').
            append(workspace).append(nodePath);

    StringBuilder url = new StringBuilder("javascript:require(['SHARED/documentPreview'], function(documentPreview) {documentPreview.init({doc:{");
    url.append("id:'").append(id).append("',");
    url.append("fileType:'").append(fileType).append("',");
    url.append("title:'").append(title).append("',");
    String linkInDocumentsApp = "";
    try {
      linkInDocumentsApp = documentService.getLinkInDocumentsApp(nodePath);
    } catch (Exception e) {
      LOG.error("Cannot get link in document app for node " + nodePath, e);
    }
    url.append("path:'").append(nodePath)
            .append("', repository:'").append(repositoryName)
            .append("', workspace:'").append(workspace)
            .append("', downloadUrl:'").append(downloadUrl.toString())
            .append("', openUrl:'").append(linkInDocumentsApp)
            .append("'}");
    if(author != null) {
      url.append(",author:{username:'").append(author).append("'}");
    }
    //add void(0) to make firefox execute js
    url.append("})});void(0);");

    return url.toString();
  }

  protected String getImageUrl(String workspace, String nodePath) {
    try {
      String path = nodePath.replaceAll("'", "\\\\'");
      String encodedPath = URLEncoder.encode(path, "utf-8");
      encodedPath = encodedPath.replaceAll ("%2F", "/");    //we won't encode the slash characters in the path
      String restContextName = WCMCoreUtils.getRestContextName();
      String repositoryName = null;
      try {
        repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
      } catch (RepositoryException e) {
        LOG.error("Cannot get repository name", e);
      }
      String thumbnailImage = "/" + restContextName + "/thumbnailImage/medium/" +
                              repositoryName + "/" + workspace + encodedPath;
      return thumbnailImage;
    } catch (UnsupportedEncodingException e) {
      LOG.error("Cannot encode path " + nodePath, e);
      return "";
    }
  }

  /**
   * Build the breadcrumb of the file.
   * The map keys contains the node path and the map value contains the node title and the node link.
   * @param nodePath Path of the node to build the breadcrumb
   * @return The breadcrumb
   */
  protected Map<String, List<String>> getBreadcrumb(String nodePath) {
    Map<String, List<String>> uris = new ListHashMap<>();

    try {
      if (nodePath.endsWith("/")) {
        nodePath = nodePath.substring(0, nodePath.length() - 1);
      }

      DriveData drive = documentService.getDriveOfNode(nodePath);

      String nodePathFromDrive = nodePath;

      String driveHomePath = drive.getResolvedHomePath();
      if(nodePath.startsWith(driveHomePath)) {
        nodePathFromDrive = nodePath.substring(driveHomePath.length());
      }

      if (nodePathFromDrive.startsWith("/")) {
        nodePathFromDrive = nodePathFromDrive.substring(1);
      }

      String path = driveHomePath;
      for (String nodeName : nodePathFromDrive.split("/")) {
        path += "/" + nodeName;
        try {
          Node docNode = NodeLocation.getNodeByExpression(
                  WCMCoreUtils.getRepository().getConfiguration().getName() + ":" +
                          drive.getWorkspace() + ":" + path);
          if(docNode != null) {
            String nodeTitle = Utils.getTitle(docNode);

            String docLink = documentService.getLinkInDocumentsApp(path, drive);

            List<String> titleAndLink = new ArrayList<>();
            titleAndLink.add(nodeTitle);
            titleAndLink.add(docLink);

            uris.put(path, titleAndLink);
          }
        } catch (Exception e) {
          LOG.error("Cannot get title and link of node " + nodeName, e);
        }
      }

    } catch (Exception e){
      LOG.error("Error while building breadcrumb of file " + nodePath, e);
    }

    return uris;
  }
}
