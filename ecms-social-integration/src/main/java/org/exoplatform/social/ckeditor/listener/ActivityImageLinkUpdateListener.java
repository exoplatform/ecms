/*
 * Copyright (C) 2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.social.ckeditor.listener;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.ckeditor.HTMLUploadImageProcessor;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * This class is used as a listener that detects uploaded images on
 * activity/comment/reply and store them on Personal Documents or Space Documen
 * (if the activity is of type space) Once the file is stored, the activity
 * message will be modified to use the WebDAV URL of image.
 */
public class ActivityImageLinkUpdateListener extends ActivityListenerPlugin {

  private static final String            PERSONAL_DOCUMENTS_DRIVE_NAME_PARAM = "personal.drive.name";

  private static final String            PERSONAL_DOCUMENTS_DRIVE_NAME       = "Personal Documents";

  private static final String            SPACE_DOCUMENTS_FOLDER              = "Activity Stream Documents/Pictures";

  private static final String            PERSONAL_DOCUMENTS_FOLDER           = "Public/Activity Stream Documents/Pictures";

  private static final Log               LOG                                 =
                                             ExoLogger.getLogger(ActivityImageLinkUpdateListener.class);

  private final ActivityManager          activityManager;

  private final IdentityManager          identityManager;

  private final ManageDriveService       driveService;

  private final RepositoryService        repositoryService;

  private final NodeHierarchyCreator     nodeHierarchyCreator;

  private final SpaceService             spaceService;

  private final HTMLUploadImageProcessor imageProcessor;

  private String                         personalDriveName                   = PERSONAL_DOCUMENTS_DRIVE_NAME;

  public ActivityImageLinkUpdateListener(RepositoryService repositoryService,
                                         NodeHierarchyCreator nodeHierarchyCreator,
                                         ActivityManager activityManager,
                                         IdentityManager identityManager,
                                         ManageDriveService driveService,
                                         SpaceService spaceService,
                                         HTMLUploadImageProcessor imageProcessor,
                                         InitParams params) {
    this.activityManager = activityManager;
    this.identityManager = identityManager;
    this.driveService = driveService;
    this.repositoryService = repositoryService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.spaceService = spaceService;
    this.imageProcessor = imageProcessor;
    if (params != null) {
      ValueParam personalDocumentsDriveName = params.getValueParam(PERSONAL_DOCUMENTS_DRIVE_NAME_PARAM);
      if (personalDocumentsDriveName != null) {
        personalDriveName = personalDocumentsDriveName.getValue();
      }
    }
  }

  @Override
  public void saveActivity(ActivityLifeCycleEvent event) {
    try {
      updateImageLink(event);
    } catch (Exception e) {
      LOG.warn("Error while processing activity body for attached images", e);
    }
  }

  @Override
  public void updateActivity(ActivityLifeCycleEvent event) {
    try {
      updateImageLink(event);
    } catch (Exception e) {
      LOG.warn("Error while processing activity body for attached images", e);
    }
  }

  @Override
  public void saveComment(ActivityLifeCycleEvent event) {
    try {
      updateImageLink(event);
    } catch (Exception e) {
      LOG.warn("Error while processing activity body for attached images", e);
    }
  }

  @Override
  public void likeActivity(ActivityLifeCycleEvent event) {
  }

  @Override
  public void updateComment(ActivityLifeCycleEvent event) {
  }

  @Override
  public void likeComment(ActivityLifeCycleEvent event) {
  }

  private void updateImageLink(ActivityLifeCycleEvent event) throws Exception {
    ExoSocialActivity activity = event.getActivity();
    String body = activity.getBody();
    String title = activity.getTitle();
    boolean storeActivity = false;

    Node folderNode = getFolderNode(activity);
    if (folderNode == null) {
      LOG.warn("Can't get folder destination to store attached image of activity with id: {}", activity.getId());
      return;
    }

    // update links in body
    if (StringUtils.isNotBlank(body)) {
      String processedBody = imageProcessor.processImages(body, folderNode, getImagesFolderPath(activity));
      if (!body.equals(processedBody)) {
        activity.setBody(processedBody);
        storeActivity = true;
      }
    }

    // update links in title
    if (StringUtils.isNotBlank(title)) {
      String processedTitle = imageProcessor.processImages(title, folderNode, getImagesFolderPath(activity));
      if (!title.equals(processedTitle)) {
        activity.setTitle(processedTitle);
        storeActivity = true;
      }
    }

    // update links in template params
    Map<String, String> templateParams = activity.getTemplateParams();
    if (templateParams != null) {
      for (String param : templateParams.keySet()) {
        String paramValue = templateParams.get(param);
        String processedParamValue = imageProcessor.processImages(paramValue, folderNode, getImagesFolderPath(activity));
        if (!paramValue.equals(processedParamValue)) {
          templateParams.put(param, processedParamValue);
          activity.setTemplateParams(templateParams);
          storeActivity = true;
        }
      }
    }

    if (storeActivity) {
      activityManager.updateActivity(activity);
    }
  }

  private Node getFolderNode(ExoSocialActivity activity) throws Exception {
    String posterId = activity.getPosterId();
    String userName = identityManager.getIdentity(posterId, false).getRemoteId();

    DriveData selectedDriveData = null;

    if (activity.getActivityStream().getType() != null
        && SpaceIdentityProvider.NAME.equals(activity.getActivityStream().getType().toString())) {
      String streamOwner = activity.getStreamOwner();
      Space space = spaceService.getSpaceByPrettyName(streamOwner);
      if (space == null) {
        LOG.warn("Can't find space with pretty name: {}. The uploaded files on activity {} will be ignored.",
                 streamOwner,
                 activity.getId());
      }
      selectedDriveData = driveService.getDriveByName(space.getGroupId().replaceAll("/", "."));
    } else {
      List<DriveData> personalDrives = driveService.getPersonalDrives(userName);
      if (personalDrives == null || personalDrives.isEmpty()) {
        LOG.warn("The user {} hasn't personal drives, thus the uploaded files will be deleted from temporary folder", userName);
        return null;
      }
      for (DriveData driveData : personalDrives) {
        if (personalDriveName.equals(driveData.getName())) {
          selectedDriveData = driveData;
          break;
        }
      }
      if (selectedDriveData == null) {
        selectedDriveData = personalDrives.get(0);
        LOG.warn("Cannot find configured personal drive with name {}, another drive will be used instead: {}",
                 personalDriveName,
                 selectedDriveData.getName());
      }
    }

    return getNode(selectedDriveData, userName);
  }

  private String getImagesFolderPath(ExoSocialActivity activity) {
    String folderPath;

    YearMonth yearMonth = YearMonth.now();
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthValue();
    String monthString = String.format("%02d", month);

    if (activity.getActivityStream().getType() != null
        && SpaceIdentityProvider.NAME.equals(activity.getActivityStream().getType().toString())) {
      folderPath = SPACE_DOCUMENTS_FOLDER + "/" + year + "/" + monthString;
    } else {
      folderPath = PERSONAL_DOCUMENTS_FOLDER + "/" + year + "/" + monthString;
    }

    return folderPath;
  }

  private Node getNode(DriveData driveData, String userId) throws Exception {
    Session session = getSession(driveData.getWorkspace());
    if (session == null) {
      return null;
    }
    String driveHomePath = driveData.getHomePath();
    String drivePath = driveHomePath;
    if (driveData.getName().equals(personalDriveName)) {
      drivePath = Utils.getPersonalDrivePath(driveHomePath, userId);
    }

    String jcrPath = Text.escapeIllegalJcrChars(drivePath);
    if (!session.itemExists(jcrPath)) {
      // Make sure that user paths are created on JCR
      nodeHierarchyCreator.getUserNode(SessionProviderService.getSystemSessionProvider(), userId);
    }

    if (!session.itemExists(jcrPath)) {
      LOG.warn("Can't get folder destination path {} to store attached image for user activity", jcrPath);
      return null;
    }
    return (Node) session.getItem(jcrPath);
  }

  private Session getSession(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    if (sessionProvider == null) {
      return null;
    }
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    if (manageableRepository == null) {
      manageableRepository = repositoryService.getDefaultRepository();
    }
    return sessionProvider.getSession(workspaceName, manageableRepository);
  }
}
