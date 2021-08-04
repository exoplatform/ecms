/*
 * Copyright (C) 2003-2021 eXo Platform SAS.
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
package org.exoplatform.ecms.activity.processor;

import static org.exoplatform.social.plugin.doc.UIDocActivity.*;
import static org.exoplatform.wcm.ext.component.activity.FileUIActivity.NODE_PATH;
import static org.exoplatform.wcm.ext.component.activity.FileUIActivity.SEPARATOR_REGEX;

import java.util.ArrayList;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.connector.dlp.FileDlpConnector;
import org.exoplatform.services.attachments.model.ActivityFileAttachment;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ActivityAttachmentProcessor extends BaseActivityProcessorPlugin {
  private static final Log LOG = ExoLogger.getLogger(ActivityAttachmentProcessor.class);

  private TrashService     trashService;

  public ActivityAttachmentProcessor(TrashService trashService, InitParams initParams) {
    super(initParams);
    this.trashService = trashService;
  }

  @Override
  public void processActivity(ExoSocialActivity activity) {
    Map<String, String> activityParams = activity.getTemplateParams();
    if (activityParams == null || activityParams.isEmpty() || !activityParams.containsKey(WORKSPACE)) {
      return;
    }

    String[] repositories = getParameterValues(activityParams, REPOSITORY);
    String[] workspaces = getParameterValues(activityParams, WORKSPACE);
    String[] mimeTypes = getParameterValues(activityParams, MIME_TYPE);
    String[] nodeUUIDs = getParameterValues(activityParams, ID);
    String[] docPaths = activityParams.containsKey(DOCPATH) ? getParameterValues(activityParams, DOCPATH)
                                                            : getParameterValues(activityParams, NODE_PATH);
    if (docPaths == null) {
      return;
    }

    activity.setFiles(new ArrayList<>());
    for (int i = 0; i < docPaths.length; i++) {
      String docPath = docPaths[i];
      ActivityFileAttachment fileAttachment = new ActivityFileAttachment();
      try {
        String repository = "repository";
        if (repositories != null && repositories.length == docPaths.length && StringUtils.isNotBlank(repositories[i])) {
          repository = repositories[i];
        }
        String workspace = "collaboration";
        if (workspaces != null && workspaces.length == docPaths.length && StringUtils.isNotBlank(workspaces[i])) {
          workspace = workspaces[i];
        }
        String mimeType = null;
        if (mimeTypes != null && mimeTypes.length == docPaths.length && StringUtils.isNotBlank(mimeTypes[i])) {
          mimeType = mimeTypes[i];
        }
        String nodeUUID = null;
        if (nodeUUIDs != null && nodeUUIDs.length == docPaths.length && StringUtils.isNotBlank(nodeUUIDs[i])) {
          nodeUUID = nodeUUIDs[i];
        }
        fileAttachment.setRepository(repository);
        fileAttachment.setWorkspace(workspace);
        fileAttachment.setId(nodeUUID);
        fileAttachment.setDocPath(docPath);
        fileAttachment.setMimeType(mimeType);
        activity.getFiles().add(fileAttachment);

        NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath, nodeUUID, true);
        Node contentNode = NodeLocation.getNodeByLocation(nodeLocation);
        if (contentNode == null || !contentNode.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
          fileAttachment.setDeleted(true);
          continue;
        }
        if (nodeUUID == null) {
          fileAttachment.setId(contentNode.getUUID());
        }
        fileAttachment.setName(getTitle(contentNode));
        fileAttachment.setDeleted(trashService.isInTrash(contentNode) || isQuarantinedItem(contentNode));
      } catch (Exception e) {
        fileAttachment.setDeleted(true);
        LOG.warn("Error while geting attached file: {}. Continue retrieving the other attachments anyway.", docPath, e);
      }
    }
  }

  private String getTitle(Node contentNode) throws RepositoryException {
    String nodeTitle;
    try {
      nodeTitle = org.exoplatform.ecm.webui.utils.Utils.getTitle(contentNode);
    } catch (Exception e) {
      nodeTitle = contentNode.getName();
    }
    return nodeTitle;
  }

  private boolean isQuarantinedItem(Node node) throws RepositoryException {
    return node.getPath().startsWith("/" + FileDlpConnector.DLP_QUARANTINE_FOLDER + "/");
  }

  private String[] getParameterValues(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if (value == null) {
      value = activityParams.get(paramName.toLowerCase());
    }
    if (value != null) {
      values = value.split(SEPARATOR_REGEX);
    }
    return values;
  }

}
