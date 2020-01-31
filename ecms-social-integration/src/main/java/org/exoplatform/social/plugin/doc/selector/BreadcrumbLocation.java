package org.exoplatform.social.plugin.doc.selector;

import java.util.*;

import javax.jcr.Node;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;

public class BreadcrumbLocation {

  private List<Object> breadCrumb      = new ArrayList<>();

  private List<String> breadCrumbTitle = new ArrayList<>();

  public String getBreadCrumbTitle(Object element) throws Exception {
    if (element instanceof DriveData) {
      return getDriveTitle((DriveData) element);
    } else {
      int index = breadCrumb.indexOf(element);
      if (index >= 0) {
        return breadCrumbTitle.get(index);
      } else {
        return null;
      }
    }
  }

  public void addLocation(Object location) throws Exception {
    if (location == null) {
      throw new IllegalArgumentException("location is mandatory");
    }

    if (location instanceof DriveData) {
      DriveData driveData = (DriveData) location;
      breadCrumb.add(driveData);
      breadCrumbTitle.add(getDriveTitle(driveData));
    } else if (location instanceof String) {
      String selectedPath = (String) location;
      DriveData driveData = (DriveData) breadCrumb.get(0);
      breadCrumb.add(selectedPath);
      breadCrumbTitle.add(getFolderTitle(getNode(driveData.getWorkspace(), selectedPath)));
    } else {
      throw new IllegalStateException("Unhandled location class type: " + location);
    }
  }

  public Node getCurrentFolder() throws Exception {
    if (breadCrumb.isEmpty()) {
      return null;
    } else {
      DriveData drive = (DriveData) breadCrumb.get(0);
      String path = null;

      if (breadCrumb.size() == 1) {
        path = getDriveHomePath(drive);
      } else {
        path = (String) breadCrumb.get(breadCrumb.size() - 1);
      }

      String repositoryName = WCMCoreUtils.getRepository().getConfiguration().getName();
      String folderExpression = repositoryName + ":" + drive.getWorkspace() + ":" + path;
      return NodeLocation.getNodeByExpression(folderExpression);
    }
  }

  public String getCurrentFolderTitle() {
    if (breadCrumbTitle.isEmpty()) {
      return null;
    } else {
      return breadCrumbTitle.get(breadCrumbTitle.size() - 1);
    }
  }

  public String getCurrentFolderBreadcrumb() {
    if (breadCrumbTitle.isEmpty()) {
      return null;
    } else {
      StringBuilder stringBuilder = new StringBuilder(breadCrumbTitle.get(0));
      for (int i = 1; i < breadCrumbTitle.size(); i++) {
        stringBuilder.append(" > ").append(breadCrumbTitle.get(i));
      }
      return stringBuilder.toString();
    }
  }

  public boolean isEmpty() {
    return breadCrumb.isEmpty();
  }

  public String getWorkspace() {
    if (!breadCrumb.isEmpty() && breadCrumb.get(0) instanceof DriveData) {
      return ((DriveData) breadCrumb.get(0)).getWorkspace();
    } else {
      return null;
    }
  }

  public List<Object> getBreadCrumb() {
    return breadCrumb;
  }

  public void subList(int i, int j) {
    breadCrumb = breadCrumb.subList(i, j);
    breadCrumbTitle = breadCrumbTitle.subList(i, j);
  }

  public int size() {
    return breadCrumb.size();
  }

  public boolean isFolder() {
    return !breadCrumb.isEmpty();
  }

  public static String getDriveHomePath(DriveData driveData) throws Exception {
    String homePath = driveData.getHomePath();
    if (homePath.contains("${userId}")) {
      homePath = org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(homePath,
                                                                              Util.getPortalRequestContext().getRemoteUser());
    }
    return homePath;
  }

  public static String getGroupLabel(String groupId, boolean isFull) {
    String ret = groupId.replace(".", " / ");
    if (!isFull) {
      if (ret.startsWith(" / spaces")) {
        return ret.substring(ret.lastIndexOf("/") + 1).trim();
      }
      int count = 0;
      int slashPosition = -1;
      for (int i = 0; i < ret.length(); i++) {
        if ('/' == ret.charAt(i)) {
          if (++count == 4) {
            slashPosition = i;
            break;
          }
        }
      }
      if (slashPosition > 0) {
        ret = ret.substring(0, slashPosition) + "...";
      } else if (ret.length() > 70) {
        ret = ret.substring(0, 70) + "...";
      }
    }
    return ret;
  }

  public static String getLabel(String id) {
    RequestContext context = RequestContext.getCurrentInstance();
    if (context == null) {
      return id;
    }
    ResourceBundle res = context.getApplicationResourceBundle();
    if (res == null) {
      return id;
    }
    try {
      String userDisplayName = "";
      if (ManageDriveServiceImpl.USER_DRIVE_NAME.equals(id)) {
        RequestContext ctx = Util.getPortalRequestContext();
        if (ctx != null) {
          String username = ctx.getRemoteUser();
          try {
            User user = CommonsUtils.getService(OrganizationService.class).getUserHandler().findUserByName(username);
            if (user != null) {
              userDisplayName = user.getDisplayName();
            }
          } catch (Exception ex) {
            userDisplayName = username;
          }
        }
      }
      return res.getString("Drives.label." + id.replace(" ", "")).replace("{0}", userDisplayName);
    } catch (Exception ex) {
      return id;
    }
  }

  public static String getDriveTitle(DriveData drive) throws Exception {
    String name = drive.getName();
    if (name == null) {
      return null;
    } else if (name.startsWith(".")) {
      String groupLabel = getGroupLabel(drive);
      if (groupLabel == null) {
        groupLabel = getGroupLabel(name, !name.startsWith("/spaces"));
      }
      return groupLabel;
    } else {
      return getLabel(name);
    }
  }

  public static String getGroupLabel(DriveData driveData) throws Exception {
    try {
      RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
      NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
      String absPath = groupPath + driveData.getName().replace(".", "/");
      ManageableRepository currentRepository = repoService.getCurrentRepository();
      String workspace = currentRepository.getConfiguration().getDefaultWorkspaceName();

      return getNode(workspace, absPath).getProperty(NodetypeConstant.EXO_LABEL).getString();
    } catch (Exception e) {
      return null;
    }
  }

  public static Node getNode(String workspace, String absPath) throws Exception {
    RepositoryService repoService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository currentRepository = repoService.getCurrentRepository();
    Node groupNode = (Node) WCMCoreUtils.getSystemSessionProvider().getSession(workspace, currentRepository).getItem(absPath);
    return groupNode;
  }

  public static String getFolderTitle(Node folderNode) throws Exception {
    return Utils.getTitle(folderNode);
  }

}
