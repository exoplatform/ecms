/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.clouddrive.ecms.action;

import java.security.AccessControlException;
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.ecms.CloudDriveContext;
import org.exoplatform.ecm.permission.info.UIPermissionInputSet;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.core.UIPermissionManagerBase;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;

/**
 * The Class UIPermissionForm.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/wcm/webui/core/UIPermissionForm.gtmpl", events = {
    @EventConfig(listeners = UIPermissionForm.SaveActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.ResetActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.CloseActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.SelectUserActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.SelectMemberActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIPermissionForm.AddAnyActionListener.class),
    @EventConfig(phase = Phase.DECODE, listeners = UIPermissionInputSet.OnChangeActionListener.class) })
public class UIPermissionForm extends org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionForm {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(UIPermissionForm.class);

  /**
   * The listener interface for receiving saveAction events. The class that is
   * interested in processing a saveAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addSaveActionListener</code> method. When the saveAction
   * event occurs, that object's appropriate method is invoked.
   */
  public static class SaveActionListener extends
                                         org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionForm.SaveActionListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final Event<org.exoplatform.ecm.webui.component.explorer.popup.info.UIPermissionForm> event) throws Exception {
      UIPermissionForm uiForm = (UIPermissionForm) event.getSource();
      WebuiRequestContext rcontext = event.getRequestContext();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      UIPermissionManagerBase uiParent = uiForm.getParent();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiParent);

      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);

      CloudDriveService cloudDrives = uiForm.getApplicationComponent(CloudDriveService.class);
      Node currentNode = uiForm.getCurrentNode();
      CloudDrive localDrive = cloudDrives.findDrive(currentNode);
      if (localDrive != null) {
        initContext(rcontext, currentNode);

        String userOrGroup = uiForm.getChild(UIPermissionInputSet.class)
                                   .getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP)
                                   .getValue();

        if (Utils.isNameEmpty(userOrGroup)) {
          uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.userOrGroup-required", null, ApplicationMessage.WARNING));
          return;
        }

        ExtendedNode node = (ExtendedNode) currentNode;
        if (PermissionUtil.canChangePermission(node)) {
          if (node.canAddMixin("exo:privilegeable")) {
            node.addMixin("exo:privilegeable");
            // node.setPermission(Utils.getNodeOwner(node),PermissionType.ALL);
          }
          try {
            CloudFileActionService actions = uiForm.getApplicationComponent(CloudFileActionService.class);
            ManageDriveService documentDrives = uiForm.getApplicationComponent(ManageDriveService.class);
            DriveData documentsDrive = null;
            // 1. check by name if it is an org group
            OrganizationService org = uiForm.getApplicationComponent(OrganizationService.class);

            String[] groupIdentity = userOrGroup.split(":");
            if (groupIdentity.length == 2) {
              Group group = org.getGroupHandler().findGroupById(groupIdentity[1]);
              if (group != null) {
                // 2. if group try find its ECMS drive
                String groupId = group.getId();
                documentsDrive = documentDrives.getDriveByName(groupId.replace('/', '.'));
                if (documentsDrive != null) {
                  // 3. if drive found, share with the drive:
                  // we don't use documentsDrive.getAllPermissions() as user
                  // already chosen the permission
                  // identity in the form
                  // check if link(s) in it exist
                  // FIXME links should not exist if not shared to the drive
                  Node newLink, docsDriveRoot;
                  if (actions.getCloudFileLinks(node, groupId, true).getSize() == 0) {
                    // 3.1 if no links create an one in drive root using
                    // linkFile()
                    docsDriveRoot = (Node) node.getSession().getItem(documentsDrive.getHomePath());
                    if (PermissionUtil.canAddNode(docsDriveRoot)) {
                      actions.shareCloudFile(node, localDrive, userOrGroup);
                      newLink = actions.linkFile(node, docsDriveRoot, groupId);
                      actions.setAllPermissions(newLink, userOrGroup);
                      actions.postSharedActivity(node, newLink, "");
                    } else {
                      // user has no access to the group
                      newLink = null;
                      uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.not-change-permission",
                                                              null,
                                                              ApplicationMessage.WARNING));
                      return;
                    }
                  } else {
                    // else // 3.2 if links found - ensure shared properly
                    newLink = docsDriveRoot = null;
                    actions.shareCloudFile(node, localDrive, userOrGroup);
                  }
                  if (newLink != null) {
                    // notification info for user
                    ApplicationMessage title = new ApplicationMessage("CloudFile.msg.LinkCreated", new String[] { "" });
                    ApplicationMessage text;
                    SpaceService spaces = WCMCoreUtils.getService(SpaceService.class);
                    Space space = spaces.getSpaceByGroupId(groupId);
                    if (space != null) {
                      text = new ApplicationMessage("CloudFile.msg.FileLinksSharedInSpace",
                                                    new String[] { "", actions.documentName(docsDriveRoot),
                                                        space.getDisplayName() });
                    } else {
                      text =
                           new ApplicationMessage("CloudFile.msg.FileLinksSharedInSpace",
                                                  new String[] { "", actions.documentName(docsDriveRoot), group.getGroupName() });
                    }
                    ResourceBundle res = rcontext.getApplicationResourceBundle();
                    title.setResourceBundle(res);
                    text.setResourceBundle(res);
                    CloudDriveContext.showInfo(rcontext, title.getMessage(), text.getMessage());
                  }
                }
              }
            }
            if (documentsDrive == null) {
              // 4. if no drive found, then we check is it an user in org
              // service
              User user = org.getUserHandler().findUserByName(userOrGroup);
              if (user != null) {
                // 5. if user found, use linkShareToUser() to share to the user
                // home dir
                Node link = actions.linkShareToUser(node, localDrive, userOrGroup);
                actions.postSharedActivity(node, link, "");
                // notification info for user
                ApplicationMessage title = new ApplicationMessage("CloudFile.msg.LinkCreated", new String[] { "" });
                ApplicationMessage text = new ApplicationMessage("CloudFile.msg.FileLinksSharedWithUser",
                                                                 new String[] { "", actions.documentName(link.getParent()),
                                                                     user.getFirstName() + " " + user.getLastName() });
                ResourceBundle res = rcontext.getApplicationResourceBundle();
                title.setResourceBundle(res);
                text.setResourceBundle(res);
                CloudDriveContext.showInfo(rcontext, title.getMessage(), text.getMessage());
              } else {
                uiApp.addMessage(new ApplicationMessage("CloudFile.msg.CannotFindUser",
                                                        new String[] { userOrGroup },
                                                        ApplicationMessage.WARNING));
                return;
              }
            }
          } catch (AccessDeniedException ade) {
            uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.not-change-permission",
                                                    null,
                                                    ApplicationMessage.WARNING));
            return;
          } catch (AccessControlException accessControlException) {
            uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.not-change-permission",
                                                    null,
                                                    ApplicationMessage.WARNING));
            return;
          }
          UIPermissionInfo permInfo = uiParent.getChild(UIPermissionInfo.class);
          permInfo.updateGrid(permInfo.getChild(UIGrid.class).getUIPageIterator().getCurrentPage());
        } else {
          uiApp.addMessage(new ApplicationMessage("UIPermissionForm.msg.not-change-permission",
                                                  null,
                                                  ApplicationMessage.WARNING));
          return;
        }

        // save all changes in the workspace
        currentNode.getSession().save();
        uiForm.refresh();
        uiExplorer.setIsHidePopup(true);
      } else {
        LOG.warn("Cloud Drive cannot be found for " + currentNode.getPath());
      }
    }

    /**
     * Inits the context.
     *
     * @param context the context
     * @param currentNode the current node
     * @throws RepositoryException the repository exception
     * @throws CloudDriveException the cloud drive exception
     */
    protected void initContext(WebuiRequestContext context, Node currentNode) throws RepositoryException, CloudDriveException {
      String path = currentNode.getPath();
      String workspace = currentNode.getSession().getWorkspace().getName();
      CloudDriveContext.init(context, workspace, path);
    }
  }

  /** The permissions. */
  protected final UIPermissionInputSet permissions;

  /**
   * Instantiates a new UI permission form.
   *
   * @throws Exception the exception
   */
  public UIPermissionForm() throws Exception {
    super();
    // customize form: don't show permission checkboxes
    removeChildById(PERMISSION);
    permissions = new UIPermissionInputSet(PERMISSION, false);
    addChild(permissions);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkAll(boolean check) {
    // this method has no sense for Cloud File sharing permissions, they are
    // READ only always
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fillForm(String user, ExtendedNode node) throws Exception {
    // we don't need set permissions checkboxes, only user/group name
    refresh();
    permissions.getUIStringInput(UIPermissionInputSet.FIELD_USERORGROUP).setValue(user);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void lockForm(boolean isLock) {
    // XXX we expect NPE in permission checkboxes traversing inside, they are
    // last it's OK
    try {
      super.lockForm(isLock);
    } catch (NullPointerException e) {
      // ignore it
    }
  }

}
