/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.social.plugin.doc.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.plugin.doc.ComposerFileItem;
import org.exoplatform.social.plugin.doc.UIAbstractSelectFileComposer;
import org.exoplatform.social.plugin.doc.UIDocActivityPopup;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.cssfile.CssClassManager;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(template = "classpath:groovy/social/plugin/doc/selector/UIDocumentSelector.gtmpl", events = {
    @EventConfig(listeners = UIDocumentSelector.SelectActionListener.class)
})
public class UIDocumentSelector extends UIAbstractSelectFileComposer {

  public static final String  ACTIVITY_PARAMS_SEPARATOR     = "/@/";

  public static final String  DOCUMENTS_BREADCRUMB_SELECTOR = "DocumentsBreadcumbSelector";

  private static final String ALL_GROUP_PERMISSION          = "*:${groupId}";

  private static final String ALL_USER_PERMISSION           = "${userId}";

  private static final String UPLOAD_RESOLVER_TYPE          = "JCR";

  private static final Log    LOG                           = ExoLogger.getLogger(UIDocumentSelector.class.getName());

  protected String            documentSelectorTitle;

  private ManageDriveService  driveService;

  private SpaceService        spaceService;

  private LinkManager         linkManager;

  private List<String>        selectedFilePaths             = new ArrayList<>();

  private List<String>        selectedFileTitles             = new ArrayList<>();

  private List<String>        validSelectedFilePaths        = new ArrayList<>();

  private String              lastSelectedDocumentTitle     = null;

  private boolean             documentAlreadySelectedError  = false;

  private boolean             folderSelection               = false;

  private BreadcrumbLocation  breadcrumbLocation            = new BreadcrumbLocation();

  public UIDocumentSelector() throws Exception {
    super();
    driveService = getApplicationComponent(ManageDriveService.class);
    spaceService = getApplicationComponent(SpaceService.class);
    linkManager = getApplicationComponent(LinkManager.class);

    Iterator<DriveData> drives = getDrives().iterator();

    DriveData selectedDriveData = null;
    try {
      if (StringUtils.isBlank(SpaceUtils.getSpaceUrlByContext())) {
        while (drives.hasNext() && selectedDriveData == null) {
          DriveData driveData = (DriveData) drives.next();
          if (driveData.getName().equals(ManageDriveServiceImpl.PERSONAL_DRIVE_NAME)) {
            selectedDriveData = driveData;
          }
        }
      } else {
        Space space = spaceService.getSpaceByUrl(SpaceUtils.getSpaceUrlByContext());
        while (drives.hasNext() && selectedDriveData == null) {
          DriveData driveData = (DriveData) drives.next();
          if (driveData.getHomePath().contains(space.getGroupId())) {
            selectedDriveData = driveData;
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error while initializing Popup", e);
    }

    if (selectedDriveData != null) {
      breadcrumbLocation = new BreadcrumbLocation();
      breadcrumbLocation.addLocation(selectedDriveData);
    }

    addChild(UIDocumentSelectorUpdate.class, null, null);
  }

  public boolean isFolderSelection() {
    return folderSelection;
  }

  public void setFolderSelection(boolean folderSelection) {
    this.folderSelection = folderSelection;
  }

  public String getLastSelectedDocumentTitle() {
    return lastSelectedDocumentTitle;
  }

  public boolean isDocumentAlreadySelectedError() {
    return documentAlreadySelectedError;
  }

  public void setDocumentAlreadySelectedError(boolean documentAlreadySelectedError) {
    this.documentAlreadySelectedError = documentAlreadySelectedError;
  }

  public void setTitle(String documentSelectorTitle) {
    this.documentSelectorTitle = documentSelectorTitle;
  }

  public String getTitle() {
    return documentSelectorTitle;
  }

  public List<DriveData> getDrives() throws Exception {
    if (breadcrumbLocation.isEmpty()) {
      List<DriveData> driveDatas = new ArrayList<>();
      driveDatas.addAll(driveService.getMainDrives(Util.getPortalRequestContext().getRemoteUser(), Utils.getMemberships()));
      driveDatas.addAll(driveService.getGroupDrives(Util.getPortalRequestContext().getRemoteUser(), Utils.getMemberships()));
      driveDatas.addAll(driveService.getPersonalDrives(Util.getPortalRequestContext().getRemoteUser()));
      return driveDatas;
    } else {
      return Collections.emptyList();
    }
  }

  public List<Node> getFolders() throws Exception {
    Node currentFolder = breadcrumbLocation.getCurrentFolder();
    if (currentFolder == null) {
      return Collections.emptyList();
    } else {
      List<Node> folderNodes = new ArrayList<>();

      NodeIterator nodesIterator = currentFolder.getNodes();
      while (nodesIterator.hasNext()) {
        Node node = nodesIterator.nextNode();
        if (node.isNodeType(FCKUtils.EXO_HIDDENABLE))
          continue;
        if (node.isNodeType("exo:symlink") && node.hasProperty("exo:uuid") && node.hasProperty("exo:workspace")) {
          node = linkManager.getTarget(node);
        }
        if (node.isNodeType(NodetypeConstant.NT_FOLDER) || node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)) {
          folderNodes.add(node);
        }
      }
      return folderNodes;
    }
  }

  public List<Node> getFiles() throws Exception {
    if (folderSelection || breadcrumbLocation.isEmpty()) {
      return Collections.emptyList();
    } else {
      Node docNode = breadcrumbLocation.getCurrentFolder();
      List<Node> fileNodes = new ArrayList<>();

      NodeIterator nodesIterator = docNode.getNodes();
      while (nodesIterator.hasNext()) {
        Node node = (Node) nodesIterator.nextNode();
        if (node.isNodeType(FCKUtils.EXO_HIDDENABLE))
          continue;
        if (node.isNodeType("exo:symlink") && node.hasProperty("exo:uuid") && node.hasProperty("exo:workspace")) {
          node = linkManager.getTarget(node);
        }
        if (node.isNodeType(NodetypeConstant.NT_FILE) && !fileNodes.contains(node)) {
          fileNodes.add(node);
        }
      }
      return fileNodes;
    }
  }

  public String getDriveCSSClasses(DriveData drive) throws Exception {
    if (drive.getName().startsWith(".")) {
      return "uiIconEcms24x24Drive" + drive.getName().replaceAll(" ", "") + " uiIconEcms24x24DriveGroup";
    } else if (drive.getHomePath().contains("${userId}")) {
      return "uiIconEcms24x24Drive" + drive.getName().replaceAll(" ", "") + " uiIconEcms24x24DrivePrivate";
    } else {
      return "uiIconEcms24x24Drive" + drive.getName().replaceAll(" ", "") + " uiIconEcms24x24DriveGeneral";
    }
  }

  public String getFileTitle(Node fileNode) throws Exception {
    return Utils.getTitle(fileNode);
  }

  public String getFilePath(Node fileNode) throws Exception {
    return fileNode.getSession().getWorkspace().getName() + "@" + fileNode.getPath();
  }

  public String getFolderNodeIcon(Node node) throws Exception {
    return Utils.getNodeTypeIcon(node, "uiIcon24x24");
  }

  public double getFileSize(Node node) throws Exception {
    // get file size
    if (node.hasNode(Utils.JCR_CONTENT)) {
      Node contentNode = node.getNode(Utils.JCR_CONTENT);
      if (contentNode.hasProperty(Utils.JCR_DATA)) {
        return contentNode.getProperty(Utils.JCR_DATA).getLength();
      }
    }
    return 0;
  }

  public String getFileTypeCSSClass(Node fileNode) throws Exception {
    String cssClass = CssClassUtils
                                   .getCSSClassByFileNameAndFileType(fileNode.getName(),
                                                                     getFileMimeType(fileNode),
                                                                     CssClassManager.ICON_SIZE.ICON_64)
                                   .replace("uiIcon", "uiBgd");
    return cssClass;
  }

  public static String getFileMimeType(Node node) throws Exception {
    if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
      if (node.hasNode(NodetypeConstant.JCR_CONTENT))
        return node.getNode(NodetypeConstant.JCR_CONTENT).getProperty(NodetypeConstant.JCR_MIME_TYPE).getString();
    }
    return "";
  }

  public boolean isFileSelected(Node fileNode) throws Exception {
    return !breadcrumbLocation.isEmpty()
        && StringUtils.equals(breadcrumbLocation.getWorkspace(), fileNode.getSession().getWorkspace().getName())
        && selectedFilePaths.contains(getFilePath(fileNode));
  }

  public List<Object> getBreadCrumb() {
    return breadcrumbLocation.getBreadCrumb();
  }

  public BreadcrumbLocation getBreadcrumbLocation() {
    return breadcrumbLocation;
  }

  public String getBreadCrumbTitle(Object obj) throws Exception {
    return breadcrumbLocation.getBreadCrumbTitle(obj);
  }
  
  public String getDriveTitle(DriveData driveData) throws Exception {
    return BreadcrumbLocation.getDriveTitle(driveData);
  }

  public String getFolderTitle(Node folderNode) throws Exception {
    return Utils.getTitle(folderNode);
  }

  public static class SelectActionListener extends EventListener<UIDocumentSelector> {

    public void execute(Event<UIDocumentSelector> event) throws Exception {
      UIDocumentSelector component = event.getSource();

      UIComponent componentToUpdate = component;

      String selectedElement = event.getRequestContext().getRequestParameter(OBJECTID);
      String[] params = selectedElement.split(ACTIVITY_PARAMS_SEPARATOR);
      if (params.length < 3) {
        LOG.warn("Number of parameters must be 3 or greater (separated by /@/ ): {} ", selectedElement);
        return;
      }

      String selectionType = params[0];
      String elementType = params[1];
      String selectedPath = params[2];
      if (selectionType.equals("BREADCRUMB")) {
        if (StringUtils.isBlank(selectedPath)) {
          LOG.warn("No entry was selected: {}", selectedElement);
          return;
        } else if (elementType.equals("DRIVES")) {
          component.breadcrumbLocation = new BreadcrumbLocation();
        } else {
          int breadCrumbIndex = Integer.parseInt(selectedPath);
          if (elementType.equals("DRIVE")) {
            if (breadCrumbIndex != 0) {
              LOG.warn("Selected index isn't a drive: {} ", breadCrumbIndex);
              return;
            }
            component.breadcrumbLocation.subList(0, 1);
          } else if (elementType.equals("FOLDER")) {
            if (component.breadcrumbLocation.size() < 2) {
              LOG.warn("No folder was selected: {}", breadCrumbIndex);
              return;
            }
            component.breadcrumbLocation.subList(0, breadCrumbIndex + 1);
          } else {
            LOG.warn("Invalid breadcrumb element type: {} ", elementType);
            return;
          }
        }
      } else if (selectionType.equals("SELECTIONBOX")) {
        if (elementType.equals("DRIVE")) {
          if (!component.getBreadCrumb().isEmpty()) {
            LOG.warn("Can't switch to another Drive while a drive is already selected: {} ", selectedPath);
            return;
          }
          DriveData driveData = component.driveService.getDriveByName(selectedPath);
          if (driveData != null && component.hasPermissionOnDrive(driveData)) {
            component.breadcrumbLocation = new BreadcrumbLocation();
            component.breadcrumbLocation.addLocation(driveData);
          } else {
            LOG.warn("Can't find drive with name {}", selectedPath);
            return;
          }
        } else if (elementType.equals("FOLDER")) {
          if (component.getBreadCrumb().isEmpty()) {
            LOG.warn("Can't find the selected drive for selected folder: {} ", selectedPath);
            return;
          }
          component.breadcrumbLocation.addLocation(selectedPath);
        } else if (elementType.equals("FILE")) {
          UIDocActivityPopup docActivityPopup = component.getAncestorOfType(UIDocActivityPopup.class);
          componentToUpdate = component.getChild(UIDocumentSelectorUpdate.class);

          if (params.length != 4) {
            LOG.warn("Number of parameters must be equal to 4 (separated by /@/ ): {} ", selectedElement);
            return;
          }
          String selectedFileTitle = params[3];

          if (component.selectedFilePaths.contains(selectedPath)) {
            component.selectedFilePaths.remove(selectedPath);
            component.selectedFileTitles.remove(selectedFileTitle);
            docActivityPopup.setLimitReached(component.selectedFilePaths.size() >= docActivityPopup.getMaxFilesCount());
          } else {
            component.lastSelectedDocumentTitle = selectedFileTitle;

            if (component.selectedFileTitles.contains(selectedFileTitle)) {
              component.documentAlreadySelectedError = true;
            } else {
              docActivityPopup.setLimitReached(component.selectedFilePaths.size() >= docActivityPopup.getMaxFilesCount());
              if (!docActivityPopup.isLimitReached()) {
                component.selectedFilePaths.add(selectedPath);
                if (StringUtils.isNotBlank(selectedFileTitle)) {
                  component.selectedFileTitles.add(selectedFileTitle);
                }
              }
            }

          }
        }
      } else {
        LOG.warn("Invalid selection type: {} ", selectionType);
        return;
      }

      event.getRequestContext().addUIComponentToUpdateByAjax(componentToUpdate);
    }
  }

  protected boolean hasPermissionOnDrive(DriveData drive) throws Exception {
    List<String> userMemberships = Utils.getMemberships();
    String[] allPermission = drive.getAllPermissions();
    if (ALL_GROUP_PERMISSION.equals(allPermission[0]) || ALL_USER_PERMISSION.equals(allPermission[0])) {
      return true;
    }
    for (String membership : userMemberships) {
      if (drive.hasPermission(allPermission, membership)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void validateSelection() {
    validSelectedFilePaths.clear();
    validSelectedFilePaths.addAll(selectedFilePaths);
    selectedFilePaths.clear();
    selectedFileTitles.clear();
  }

  public boolean hasSelectedFiles() {
    return !selectedFilePaths.isEmpty();
  }

  @Override
  public Set<ComposerFileItem> getSelectFiles() {
    if (validSelectedFilePaths == null || validSelectedFilePaths.isEmpty()) {
      return Collections.emptySet();
    }
    Set<ComposerFileItem> composerFileItems = new HashSet<>();
    for (String selectedFile : validSelectedFilePaths) {
      try {
        String selectedFileWS = selectedFile.substring(0, selectedFile.indexOf("@"));
        String selectedFilePath = selectedFile.substring(selectedFile.indexOf("@") + 1);

        Node node = BreadcrumbLocation.getNode(selectedFileWS, selectedFilePath);

        ComposerFileItem composerFileItem = new ComposerFileItem();
        composerFileItem.setName(node.getName());
        composerFileItem.setMimeType(getFileMimeType(node));
        composerFileItem.setId(getFilePath(node));
        composerFileItem.setTitle(getFileTitle(node));
        double fileSize = getFileSize(node);

        String mbString = "MB";
        try {
          ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
          mbString = resourceBundle.getString("UIComposerDocumentExtension.label.megabyte");
        } catch (Exception e) {
        }
        String fileSizeMB = (((int) (Math.abs(fileSize / (1024 * 1024)) * 100)) / 100d) + " " + mbString;

        composerFileItem.setSizeInBytes(fileSize);
        composerFileItem.setSize(fileSizeMB);
        composerFileItem.setNodeIcon(getFileTypeCSSClass(node));
        composerFileItem.setResolverType("JCR");

        composerFileItems.add(composerFileItem);
      } catch (Exception e) {
        LOG.warn("Error occurred while proceeding selection of file " + selectedFile, e);
      }
    }
    return composerFileItems;
  }

  @Override
  public String getResolverType() {
    return UPLOAD_RESOLVER_TYPE;
  }

  @Override
  public Object preActivitySave(Object resource, PostContext postContext) throws Exception {
    ComposerFileItem fileItem = (ComposerFileItem) resource;
    String path = fileItem.getId();
    String selectedFileWS = path.substring(0, path.indexOf("@"));
    String selectedFilePath = path.substring(path.indexOf("@") + 1);
    return BreadcrumbLocation.getNode(selectedFileWS, selectedFilePath);
  }

  @Override
  public void resetSelection() {
    breadcrumbLocation = new BreadcrumbLocation();
    selectedFilePaths.clear();
    validSelectedFilePaths.clear();
    selectedFileTitles.clear();
  }

  @Override
  protected void removeSelectedFile(ComposerFileItem fileItem) {
    // Nothing to do
  }
}
