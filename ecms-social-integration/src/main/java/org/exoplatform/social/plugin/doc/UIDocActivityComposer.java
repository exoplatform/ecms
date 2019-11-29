/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.social.plugin.doc;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.plugin.doc.selector.BreadcrumbLocation;
import org.exoplatform.social.plugin.doc.selector.UIComposerMultiUploadSelector;
import org.exoplatform.social.webui.composer.*;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.social.webui.profile.UIUserActivitiesDisplay;
import org.exoplatform.wcm.connector.fckeditor.DriverConnector;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * The templateParamsProcessor to process an activity. Replace template key by
 * template value in activity's title.
 * 
 * @author Zun
 * @since Apr 19, 2010
 */
@ComponentConfig(template = "classpath:groovy/social/plugin/doc/UIDocActivityComposer.gtmpl", events = {
    @EventConfig(listeners = UIActivityComposer.CloseActionListener.class),
    @EventConfig(listeners = UIActivityComposer.SubmitContentActionListener.class),
    @EventConfig(listeners = UIActivityComposer.ActivateActionListener.class),
    @EventConfig(listeners = UIDocActivityComposer.SelectDocumentActionListener.class),
    @EventConfig(listeners = UIDocActivityComposer.SelectDestinationFolderActionListener.class),
    @EventConfig(listeners = UIDocActivityComposer.RemoveDestinationFolderActionListener.class),
    @EventConfig(listeners = UIDocActivityComposer.RemoveDocumentActionListener.class) })
public class UIDocActivityComposer extends UIActivityComposer implements UISelectable {
  private static final Log                          LOG                       = ExoLogger.getLogger(UIDocActivityComposer.class);

  public static final String                        REPOSITORY                = "repository";

  public static final String                        WORKSPACE                 = "collaboration";

  protected static final String                     UI_COMPOSER_MULTIUPLOAD   = "UIComposerMultiUpload";

  private static final String                       FILE_SPACES               = "files:spaces";

  private final static String                       POPUP_COMPOSER            = "UIPopupComposer";

  private final String                              docActivityTitle          = "<a href=\"${" + UIDocActivity.DOCLINK
      + "}\">" + "${" + UIDocActivity.DOCNAME + "}</a>";

  private SpaceService                              spaceService;

  private IdentityManager                           identityManager;

  private ActivityManager                           activityManager;

  private List<ComposerFileItem>                    selectedFileItems         = new ArrayList<>();

  private String                                    currentUser;

  private int                                       maxFilesCount             = 20;

  private int                                       maxFileSize               = 5;

  private int                                       filesCounter              = 5;

  private boolean                                   duplicatedFilesSelection  = false;

  private List<String>                              duplicatedFileNames       = new ArrayList<>();

  private Map<String, UIAbstractSelectFileComposer> uiFileSelectors           = new HashMap<>();

  private BreadcrumbLocation                        selectedDestinationFolder = null;

  /**
   * constructor
   * 
   * @throws Exception exception thrown when adding children
   */
  public UIDocActivityComposer() throws Exception {
    DriverConnector driverConnector = getApplicationComponent(DriverConnector.class);
    String maxFilesCountString = PropertyManager.getProperty("exo.social.composer.maxToUpload");
    if (StringUtils.isNotBlank(maxFilesCountString)) {
      maxFilesCount = Integer.parseInt(maxFilesCountString);
    }
    String maxFileSizeString = PropertyManager.getProperty("exo.social.composer.maxFileSizeInMB");
    if (StringUtils.isNotBlank(maxFileSizeString)) {
      maxFileSize = Integer.parseInt(maxFileSizeString);
    } else {
      maxFileSize = driverConnector.getLimitSize();
    }

    spaceService = getApplicationComponent(SpaceService.class);
    identityManager = getApplicationComponent(IdentityManager.class);
    activityManager = getApplicationComponent(ActivityManager.class);

    addChild(new UIFormStringInput("InputDoc", "InputDoc", null));

    UIComposerMultiUploadSelector uiMultiUpload = addChild(UIComposerMultiUploadSelector.class,
                                                           null,
                                                           UIComposerMultiUploadSelector.CONTAINER_ID);
    uiMultiUpload.init(maxFilesCount, maxFileSize);

    uiFileSelectors.put(uiMultiUpload.getId(), uiMultiUpload);

    resetValues();
  }

  @Override
  public boolean isReadyForPostingActivity() {
    return !getSelectedFileItems().isEmpty();
  }

  public int getAndIncrementFilesCounter() {
    return filesCounter++;
  }

  public String getDestinationBreadCrumb() throws Exception {
    if (selectedDestinationFolder == null) {
      return null;
    } else {
      return selectedDestinationFolder.getCurrentFolderBreadcrumb();
    }
  }

  public String getDestinationTitle() throws Exception {
    if (selectedDestinationFolder == null) {
      return null;
    } else {
      return selectedDestinationFolder.getCurrentFolderTitle();
    }
  }

  private void resetValues() {
    selectedFileItems.clear();
    for (UIAbstractSelectFileComposer selectFileComposer : getUIFileSelectors().values()) {
      selectFileComposer.resetSelection();
    }
    selectedDestinationFolder = null;
    setReadyForPostingActivity(false);
  }

  /**
   * @return the currentUser
   */
  public String getCurrentUser() {
    return currentUser;
  }

  /**
   * @param currentUser the currentUser to set
   */
  public void setCurrentUser(String currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  protected void onActivate(Event<UIActivityComposer> event) {
    setCurrentUser(event.getRequestContext().getRemoteUser());
  }

  @Override
  protected void onClose(Event<UIActivityComposer> event) {
    resetValues();
  }

  @Override
  protected void onSubmit(Event<UIActivityComposer> event) {
  }

  @Override
  public void onPostActivity(PostContext postContext,
                             UIComponent source,
                             WebuiRequestContext requestContext,
                             String postedMessage) throws Exception {
  }

  @Override
  public ExoSocialActivity onPostActivity(PostContext postContext, String postedMessage) throws Exception {
    ExoSocialActivity activity = null;
    if (!isReadyForPostingActivity()) {
      getAncestorOfType(UIPortletApplication.class).addMessage(new ApplicationMessage("UIComposer.msg.error.Must_select_file",
                                                                                      null,
                                                                                      ApplicationMessage.INFO));
    } else {
      Map<String, String> activityParams = new LinkedHashMap<String, String>();
      List<ComposerFileItem> selectedFileItemsList = new ArrayList<>(getSelectedFileItems());
      Collections.sort(selectedFileItemsList);

      for (ComposerFileItem composerFileItem : selectedFileItemsList) {
        UIAbstractSelectFileComposer uiSelectFileComposer = getResolver(composerFileItem.getResolverType());
        if (composerFileItem.getDestinationLocation() == null) {
          composerFileItem.setDestinationLocation(selectedDestinationFolder);
        }
        Object obj = uiSelectFileComposer.preActivitySave(composerFileItem, postContext);
        uiSelectFileComposer.putActivityParams(obj, composerFileItem, activityParams);
      }

      activityParams.put(UIDocActivity.MESSAGE, postedMessage);

      //
      if (postContext == UIComposer.PostContext.SPACE) {
        Space space = spaceService.getSpaceByUrl(SpaceUtils.getSpaceUrlByContext());
        Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
        //
        activity = saveActivity(activityParams, identityManager, spaceIdentity);
      } else if (postContext == UIComposer.PostContext.USER) {
        activity = postActivityToUser(activityParams);
      }

      for (ComposerFileItem composerFileItem : selectedFileItemsList) {
        UIAbstractSelectFileComposer uiSelectFileComposer = getResolver(composerFileItem.getResolverType());
        uiSelectFileComposer.postActivitySave(composerFileItem, postContext, activity);
      }
    }
    resetValues();
    return activity;
  }

  public Map<String, UIAbstractSelectFileComposer> getUIFileSelectors() {
    return uiFileSelectors;
  }

  private UIAbstractSelectFileComposer getResolver(String resolverType) {
    if (resolverType == null) {
      return null;
    }
    UIAbstractSelectFileComposer uiSelectFileComposer = null;
    for (UIAbstractSelectFileComposer uiFileSelector : uiFileSelectors.values()) {
      if (StringUtils.equals(uiFileSelector.getResolverType(), resolverType)) {
        uiSelectFileComposer = uiFileSelector;
        break;
      }
    }
    return uiSelectFileComposer;
  }

  private ExoSocialActivity postActivityToUser(Map<String, String> activityParams) throws Exception {
    String ownerName = ((UIUserActivitiesDisplay) getActivityDisplay()).getOwnerName();
    IdentityManager identityManager = getApplicationComponent(IdentityManager.class);
    Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, ownerName, true);
    //
    return saveActivity(activityParams, identityManager, ownerIdentity);
  }

  @SuppressWarnings("unchecked")
  public void doSelect(String selectField, Object value) throws Exception {
    if (StringUtils.isBlank(selectField)) {
      LOG.warn("No selection is not retrieved with expected type");
    } else if (selectField.equals(UIAbstractSelectFileComposer.COMPOSER_SELECTION_TYPE)) {
      Set<ComposerFileItem> selectedComposerChildItems = (Set<ComposerFileItem>) value;
      Set<ComposerFileItem> duplicatedItems = new HashSet<>(selectedComposerChildItems);
      duplicatedItems.retainAll(selectedFileItems);
      for (ComposerFileItem composerFileItem : duplicatedItems) {
        addDuplicatedFileName(composerFileItem.getName());
      }

      selectedComposerChildItems.removeAll(selectedFileItems);
      int remainingFilesToSelect = getRemainingFilesToSelect();
      if (remainingFilesToSelect >= selectedComposerChildItems.size()) {
        for (ComposerFileItem composerFileItem : selectedComposerChildItems) {
          if (!selectedFileItems.contains(composerFileItem)) {
            selectedFileItems.add(composerFileItem);
          }
        }
      } else {
        ArrayList<ComposerFileItem> selectedItemList = new ArrayList<>(selectedComposerChildItems);
        List<ComposerFileItem> subList = selectedItemList.subList(0, remainingFilesToSelect);
        selectedFileItems.addAll(subList);
      }
      Collections.sort(selectedFileItems);
    } else if (selectField.equals(UIAbstractSelectFileComposer.COMPOSER_DESTINATION_FOLDER)) {
      this.selectedDestinationFolder = (BreadcrumbLocation) value;
    } else if (selectedFileItems != null && !selectedFileItems.isEmpty()) {
      ComposerFileItem selectedComposerFileItem = getFileItem(selectField);
      if (selectedComposerFileItem == null) {
        LOG.warn("Unknown selection field '{}'", selectField);
      } else {
        BreadcrumbLocation selectedFileLocation = (BreadcrumbLocation) value;
        selectedComposerFileItem.setDestinationLocation(selectedFileLocation);
      }
    } else {
      LOG.warn("Unknown selection field '{}'", selectField);
    }
  }

  public boolean testAndSetMaxCountReached(boolean duplicated) {
    if (duplicatedFilesSelection) {
      duplicatedFilesSelection = duplicated;
      return true;
    } else {
      duplicatedFilesSelection = duplicated;
      return false;
    }
  }

  public boolean hasDuplicatedFilesInSelection() {
    return !duplicatedFileNames.isEmpty();
  }

  public void addDuplicatedFileName(String duplicatedFileName) {
    this.duplicatedFileNames.add(duplicatedFileName);
  }

  public String getAndClearDuplicatedFiles() {
    String duplicatedFiles = StringUtils.join(duplicatedFileNames, ",");
    duplicatedFileNames.clear();
    return duplicatedFiles;
  }

  public void removeFileItem(ComposerFileItem fileItem) {
    for (UIAbstractSelectFileComposer selectFileComposer : uiFileSelectors.values()) {
      selectFileComposer.removeFile(fileItem);
    }
    selectedFileItems.remove(fileItem);
  }

  public void removeDestinationFolder(String fileId) {
    if (StringUtils.isBlank(fileId)) {
      selectedDestinationFolder = null;
    } else {
      ComposerFileItem fileItem = getFileItem(fileId);
      if (fileItem != null) {
        fileItem.setDestinationLocation(null);
      }
    }
  }

  public List<ComposerFileItem> getSelectedFileItems() {
    return getSelectedFileItems(true);
  }

  public List<ComposerFileItem> getSelectedFileItems(boolean computeFromSelectors) {
    if (computeFromSelectors) {
      for (UIAbstractSelectFileComposer selectFileComposer : uiFileSelectors.values()) {
        Set<ComposerFileItem> selectFiles = selectFileComposer.getSelectFiles();
        if (selectFiles != null && !selectFiles.isEmpty()) {
          for (ComposerFileItem composerFileItem : selectFiles) {
            if (!selectedFileItems.contains(composerFileItem)) {
              selectedFileItems.add(composerFileItem);
            }
          }
          Collections.sort(selectedFileItems);
        }
      }
    }
    return selectedFileItems;
  }

  private ComposerFileItem getFileItem(String fileId) {
    return selectedFileItems.stream()
                            .filter(item -> StringUtils.equals(item.getId(), fileId))
                            .findFirst()
                            .orElse(null);
  }

  private ExoSocialActivity saveActivity(Map<String, String> activityParams,
                                         IdentityManager identityManager,
                                         Identity ownerIdentity) throws RepositoryException {
    String activity_type = FILE_SPACES;
    String remoteUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteUser, true);
    String title = activityParams.get(UIDocActivity.MESSAGE);
    if (title == null || title.length() == 0) {
      title = docActivityTitle;
    }
    ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(), activity_type, title, null);
    activity.setTemplateParams(activityParams);
    //
    activityManager.saveActivityNoReturn(ownerIdentity, activity);

    //
    return activityManager.getActivity(activity.getId());
  }

  public static class SelectDocumentActionListener extends EventListener<UIDocActivityComposer> {
    @Override
    public void execute(Event<UIDocActivityComposer> event) throws Exception {
      UIDocActivityComposer docActivityComposer = event.getSource();
      PopupContainer popupContainer = docActivityComposer.getAncestorOfType(UIPortletApplication.class)
                                                         .findFirstComponentOfType(PopupContainer.class);
      UIDocActivityPopup uiDocActivityPopup = popupContainer.createUIComponent(UIDocActivityPopup.class, null, null);
      popupContainer.activate(uiDocActivityPopup, 570, 0, false, POPUP_COMPOSER);
      for (UIAbstractSelectFileComposer uiSelector : uiDocActivityPopup.getUIFileSelectors()) {
        docActivityComposer.getUIFileSelectors().put(uiSelector.getId(), uiSelector);
      }
      uiDocActivityPopup.setMaxFilesCount(docActivityComposer.getRemainingFilesToSelect());
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
    }
  }

  public static class SelectDestinationFolderActionListener extends EventListener<UIDocActivityComposer> {
    @Override
    public void execute(Event<UIDocActivityComposer> event) throws Exception {
      UIDocActivityComposer docActivityComposer = event.getSource();
      String destinationFileId = event.getRequestContext().getRequestParameter(OBJECTID);

      PopupContainer popupContainer = docActivityComposer.getAncestorOfType(UIPortletApplication.class)
                                                         .findFirstComponentOfType(PopupContainer.class);
      UIFolderActivityPopup uiFolderActivityPopup = popupContainer.createUIComponent(UIFolderActivityPopup.class, null, null);
      uiFolderActivityPopup.setDestinationFileId(destinationFileId);
      popupContainer.activate(uiFolderActivityPopup, 570, 0, false, POPUP_COMPOSER);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
    }
  }

  public static class RemoveDestinationFolderActionListener extends EventListener<UIDocActivityComposer> {
    @Override
    public void execute(Event<UIDocActivityComposer> event) throws Exception {
      UIDocActivityComposer docActivityComposer = event.getSource();
      String destinationFileId = event.getRequestContext().getRequestParameter(OBJECTID);
      docActivityComposer.removeDestinationFolder(destinationFileId);
      event.getRequestContext().addUIComponentToUpdateByAjax(docActivityComposer);
    }
  }

  public static class RemoveDocumentActionListener extends EventListener<UIDocActivityComposer> {
    @Override
    public void execute(Event<UIDocActivityComposer> event) throws Exception {
      final UIDocActivityComposer docActivityComposer = event.getSource();
      String selectedId = event.getRequestContext().getRequestParameter(OBJECTID);
      List<ComposerFileItem> selectedFileItems = docActivityComposer.getSelectedFileItems(false);
      Iterator<ComposerFileItem> iterator = selectedFileItems.iterator();
      while (iterator.hasNext()) {
        ComposerFileItem composerFileItem = (ComposerFileItem) iterator.next();
        if (composerFileItem.getId().equals(selectedId)) {
          iterator.remove();
          for (UIAbstractSelectFileComposer uiAbstractSelectFileComposer : docActivityComposer.getUIFileSelectors().values()) {
            uiAbstractSelectFileComposer.removeFile(composerFileItem);
          }
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(docActivityComposer);
    }
  }

  protected Node getDocNode(String repository, String workspace, String docPath) {
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  public int getRemainingFilesToSelect() {
    return getMaxUploadCount() - getSelectedFileItems().size();
  }

  public int getMaxUploadCount() {
    return maxFilesCount;
  }

  protected void clearComposerData() {
    resetValues();
  }

  public int getLimitFileSize() {
    return maxFileSize;
  }
}
