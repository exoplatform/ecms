/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.social.plugin.doc.selector;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.plugin.doc.ComposerFileItem;
import org.exoplatform.social.plugin.doc.UIAbstractSelectFileComposer;
import org.exoplatform.social.plugin.doc.UIDocActivityComposer;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.cssfile.CssClassManager;
import org.exoplatform.webui.cssfile.CssClassManager.ICON_SIZE;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.input.UIUploadInput;

@ComponentConfigs({ @ComponentConfig(lifecycle = UIContainerLifecycle.class),
    @ComponentConfig(type = UIUploadInput.class, id = UIComposerMultiUploadSelector.UI_MULTI_UPLOAD_INPUT, template = "classpath:/groovy/social/plugin/doc/selector/UIMultiUploadSelectorInput.gtmpl", events = {
        @EventConfig(listeners = UIComposerMultiUploadSelector.SelectUploadIdActionListener.class),
        @EventConfig(listeners = UIUploadInput.CreateUploadIdActionListener.class),
        @EventConfig(listeners = UIUploadInput.RemoveUploadIdActionListener.class) }) })
public class UIComposerMultiUploadSelector extends UIAbstractSelectFileComposer {
  public static final String  UI_MULTI_UPLOAD_INPUT     = "UIMultiUploadInput";

  private static final Log    LOG                       = ExoLogger.getLogger(UIComposerMultiUploadSelector.class);

  public static final String  UPLOAD_RESOLVER_TYPE      = "UPLOAD";

  private final static String PUBLIC_ALIAS              = "userPublic";

  private static final String FOLDER_UPLOAD_PARENT_NAME = "Activity Stream Documents";

  public static final String  CONTAINER_ID              = "ComposerMultiUploadImportTab";

  private String              title;

  private int                 maxFileSize;

  private int                 maxFileCount;

  private UIUploadInput       uiUploadInput;

  private SpaceService        spaceService;

  public UIComposerMultiUploadSelector() throws Exception {
    spaceService = getApplicationComponent(SpaceService.class);
    setId(CONTAINER_ID);
  }

  public void init(int limitFilesCount, int maxUploadSize) {
    maxFileCount = limitFilesCount;
    maxFileSize = maxUploadSize;
    resetSelection();
  }

  @Override
  public void resetSelection() {
    if (uiUploadInput != null) {
      removeChild(UIUploadInput.class);
    }
    uiUploadInput = new UIUploadInput(UI_MULTI_UPLOAD_INPUT, UI_MULTI_UPLOAD_INPUT, maxFileCount, maxFileSize) {
      @Override
      public String getTemplate() {
        return "war:/groovy/social/plugin/doc/selector/UIMultiUploadSelectorInput.gtmpl";
      }
    };
    uiUploadInput.setComponentConfig(UIUploadInput.class, UI_MULTI_UPLOAD_INPUT);
    addChild(uiUploadInput);
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    renderChildren();
  }

  public UIUploadInput getUIUploadInput() {
    return uiUploadInput;
  }

  public int getLimitFileSize() {
    return maxFileSize;
  }

  public int getMaxUploadCount() {
    return maxFileCount;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public Set<ComposerFileItem> getSelectFiles() {
    String[] uploadIds = uiUploadInput.getUploadIds();
    if (uploadIds == null || uploadIds.length == 0) {
      return Collections.emptySet();
    }

    Set<ComposerFileItem> fileItems = new HashSet<>();
    for (String uploadId : uploadIds) {
      UploadResource uploadedResource = uiUploadInput.getUploadResource(uploadId);
      if (uploadedResource == null) {
        continue;
      }
      if (uploadedResource.getStatus() == UploadResource.FAILED_STATUS
          || (uploadedResource.getStatus() == UploadResource.UPLOADING_STATUS
              && uploadedResource.getEstimatedSize() != uploadedResource.getUploadedSize())) {
        continue;
      }
      ComposerFileItem fileItem = new ComposerFileItem();
      fileItem.setId(uploadId);
      fileItem.setTitle(uploadedResource.getFileName());
      fileItem.setName(uploadedResource.getFileName());
      fileItem.setMimeType(uploadedResource.getMimeType());
      fileItem.setNodeIcon(getFileTypeCSSClass(fileItem.getName(), fileItem.getMimeType(), CssClassManager.ICON_SIZE.ICON_64));
      double fileSize = uploadedResource.getUploadedSize();
      fileItem.setSizeInBytes(fileSize);

      String mbString = "MB";
      try {
        ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
        mbString = resourceBundle.getString("UIComposerDocumentExtension.label.megabyte");
      } catch (Exception e) {
      }
      String fileSizeMB = (((int) (Math.abs(fileSize / (1024 * 1024)) * 100)) / 100d) + " " + mbString;

      fileItem.setSize(fileSizeMB);
      fileItem.setResolverType(UPLOAD_RESOLVER_TYPE);
      fileItems.add(fileItem);
    }
    return fileItems;
  }

  public String getFileTypeCSSClass(String fileName, String mimeType, ICON_SIZE iconSize) {
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(fileName, mimeType, iconSize).replace("uiIcon", "uiBgd");
    return cssClass;
  }

  @Override
  public String getResolverType() {
    return UPLOAD_RESOLVER_TYPE;
  }

  @Override
  public Object preActivitySave(Object resource, PostContext postContext) throws Exception {
    if (!(resource instanceof ComposerFileItem)) {
      return null;
    }
    if (!postContext.equals(PostContext.SPACE) && !postContext.equals(PostContext.USER)) {
      LOG.warn("PostContext '" + postContext + "' is not supported ");
      return null;
    }
    ComposerFileItem fileItem = (ComposerFileItem) resource;
    String uploadId = fileItem.getId();
    UploadResource uploadedResource = uiUploadInput.getUploadResource(uploadId);
    if (uploadedResource == null) {
      LOG.warn("Cannot proceed uploaded file {}, it may not exists", fileItem.getTitle());
    }
    ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);

    Node parentUploadNode = null;
    Session session = null;
    if (fileItem.getDestinationLocation() != null) {
      parentUploadNode = fileItem.getDestinationLocation().getCurrentFolder();
      session = parentUploadNode.getSession();
    } else {
      Node parentNode = null;

      RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
      NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);

      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      ManageableRepository currentRepository = repoService.getCurrentRepository();
      String workspaceName = currentRepository.getConfiguration().getDefaultWorkspaceName();
      session = sessionProvider.getSession(workspaceName, currentRepository);

      if (postContext.equals(PostContext.SPACE)) {
        Space space = spaceService.getSpaceByUrl(SpaceUtils.getSpaceUrlByContext());
        String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
        String spaceParentPath = groupPath + space.getGroupId() + "/Documents";
        if (!session.itemExists(spaceParentPath)) {
          throw new IllegalStateException("Root node of space '" + spaceParentPath + "' doesn't exist");
        }
        parentNode = (Node) session.getItem(spaceParentPath);
      } else {
        String remoteUser = Util.getPortalRequestContext().getRemoteUser();
        if (StringUtils.isBlank(remoteUser)) {
          throw new IllegalStateException("Remote user is empty");
        }
        Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, remoteUser);
        String publicPath = nodeHierarchyCreator.getJcrPath(PUBLIC_ALIAS);
        if (userNode == null || !userNode.hasNode(publicPath)) {
          throw new IllegalStateException("User '" + remoteUser + "' hasn't public folder");
        }
        parentNode = userNode.getNode(publicPath);
      }

      if (!parentNode.hasNode(FOLDER_UPLOAD_PARENT_NAME)) {
        parentNode.addNode(FOLDER_UPLOAD_PARENT_NAME);
        session.save();
      }

      parentUploadNode = parentNode.getNode(FOLDER_UPLOAD_PARENT_NAME);
    }

    String nodeName = Utils.cleanName(fileItem.getName());
    if (!parentUploadNode.getDefinition().allowsSameNameSiblings()) {
      nodeName = getFileName(parentUploadNode, nodeName, nodeName, 1);
    }
    Node node = null;
    try {
      node = parentUploadNode.addNode(nodeName, NodetypeConstant.NT_FILE);
    } catch (ItemExistsException e) {
      nodeName = getFileName(parentUploadNode, nodeName, nodeName, 1);
      node = parentUploadNode.addNode(nodeName, NodetypeConstant.NT_FILE);
    }
    node.setProperty(NodetypeConstant.EXO_TITLE, fileItem.getName());
    node.addMixin(NodetypeConstant.MIX_VERSIONABLE);
    activityService.setCreating(node, true);
    Node resourceNode = node.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.NT_RESOURCE);
    resourceNode.setProperty(NodetypeConstant.JCR_MIMETYPE, fileItem.getMimeType());
    resourceNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, Calendar.getInstance());
    String fileDiskLocation = uploadedResource.getStoreLocation();
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(fileDiskLocation);
      resourceNode.setProperty(NodetypeConstant.JCR_DATA, inputStream);
      session.save();
      node = (Node) session.getItem(node.getPath());
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
    activityService.setCreating(node, false);
    fileItem.setPath(node.getPath());
    return node;
  }

  private String getFileName(Node parentUploadNode,
                             String originalNodeName,
                             String nodeName,
                             int fileIndex) throws RepositoryException {
    if (parentUploadNode.hasNode(nodeName)) {
      int pointIndex = originalNodeName.lastIndexOf('.');
      if (pointIndex > 0) {
        nodeName = originalNodeName.substring(0, pointIndex) + "(" + fileIndex + ")" + originalNodeName.substring(pointIndex);
      } else {
        nodeName = originalNodeName + "(" + fileIndex + ")";
      }
      return getFileName(parentUploadNode, originalNodeName, nodeName, ++fileIndex);
    }
    return nodeName;
  }

  @Override
  public void postActivitySave(Object obj, PostContext postContext, ExoSocialActivity activity) throws Exception {
    if (obj instanceof ComposerFileItem) {
      ComposerFileItem fileItem = (ComposerFileItem) obj;
      if (!StringUtils.isBlank(fileItem.getPath())) {
        UploadService uploadService = getApplicationComponent(UploadService.class);
        uploadService.removeUploadResource(fileItem.getId());

        SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
        RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
        ManageableRepository currentRepository = repoService.getCurrentRepository();
        String workspaceName = currentRepository.getConfiguration().getDefaultWorkspaceName();

        Session session = sessionProvider.getSession(workspaceName, currentRepository);
        obj = (Node) session.getItem(fileItem.getPath());
      }
    }
    super.postActivitySave(obj, postContext, activity);
  }

  @Override
  protected void removeSelectedFile(ComposerFileItem fileItem) {
    UploadService service = getApplicationComponent(UploadService.class);
    service.removeUploadResource(fileItem.getId());
  }

  public static class SelectUploadIdActionListener extends EventListener<UIUploadInput> {
    public void execute(Event<UIUploadInput> event) throws Exception {
      UIUploadInput uiUploadInput = event.getSource();
      UIComposerMultiUploadSelector multiUploadSelector = uiUploadInput.getParent();

      UIDocActivityComposer activityComposer = multiUploadSelector.getParent();

      // Refresh List of selected files
      activityComposer.getSelectedFileItems(true);

      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadInput);
    }
  }

}
