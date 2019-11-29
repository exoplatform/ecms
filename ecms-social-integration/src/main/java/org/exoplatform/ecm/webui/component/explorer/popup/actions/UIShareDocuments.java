/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import static org.exoplatform.wcm.notification.plugin.FileActivityChildPlugin.EXO_RESOURCES_URI;
import static org.exoplatform.wcm.notification.plugin.FileActivityChildPlugin.ICON_FILE_EXTENSION;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.*;
import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.access.*;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.*;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;
import org.exoplatform.wcm.notification.plugin.ShareFileToSpacePlugin;
import org.exoplatform.wcm.notification.plugin.ShareFileToUserPlugin;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.cssfile.*;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.*;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 18, 2014 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "classpath:groovy/ecm/social-integration/share-document/UIShareDocuments.gtmpl",
    events = {
        @EventConfig(listeners = UIShareDocuments.ConfirmActionListener.class),
        @EventConfig(listeners = UIShareDocuments.CancelActionListener.class),
        @EventConfig(listeners = UIShareDocuments.TextChangeActionListener.class),
        @EventConfig(listeners = UIShareDocuments.ChangeActionListener.class),
        @EventConfig(listeners = UIShareDocuments.ChangePermissionActionListener.class)
    }
)
public class UIShareDocuments extends UIForm implements UIPopupComponent{

  private static final Log    LOG                 = ExoLogger.getLogger(UIShareDocuments.class);
  private static final String SHARECONTENT_BUNDLE_LOCATION = "locale.extension.SocialIntegration";
  private static final String SHARE_OPTION_CANVEW          = "UIShareDocuments.label.option.read";
  private static final String SHARE_OPTION_CANMODIFY       = "UIShareDocuments.label.option.modify";
  private static final String SHARE_PERMISSION_VIEW        = PermissionType.READ;
  private static final String SHARE_PERMISSION_MODIFY      = "modify";
  private static final String SPACE_PREFIX1 = "space::";
  private static final String SPACE_PREFIX2 = "*:/spaces/";
  private static final String LOGIN_INITIALURI = "/login?initialURI=/";

  private String permission = SHARE_PERMISSION_VIEW;
  private boolean permDropDown = false;

  public boolean hasPermissionDropDown() {
    return permDropDown;
  }

  public void setPermissionDropDown(boolean permDropDown) {
    this.permDropDown = permDropDown;
  }

  public void removePermission(String id) {
    this.permissions.remove(id);
    if (this.entries.contains(id)) {
      this.entries.remove(id);
    }
  }

  public void updatePermission(String id, String permission) {
    this.permissions.put(id,permission);
  }

  /**
   * @return true if given name is a Group type, not a Space
   */
  public boolean isGroupType(String name) {
    if (name != null && name.startsWith("*:/") && !name.startsWith(SPACE_PREFIX2)) {
      return true;
    }
    return false;
  }

  public static class ChangeActionListener extends EventListener<UIShareDocuments> {

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      String permission = "read";
      UIShareDocuments uiform = event.getSource();
      if (uiform.getChild(UIFormSelectBox.class).getValue().equals(SHARE_PERMISSION_MODIFY)) {
        uiform.getChild(UIFormSelectBox.class).setValue(SHARE_PERMISSION_VIEW);
      } else {
        uiform.getChild(UIFormSelectBox.class).setValue(SHARE_PERMISSION_MODIFY);
        permission = SHARE_PERMISSION_MODIFY;
      }
      UIWhoHasAccess uiWhoHasAccess = uiform.getParent();
      uiWhoHasAccess.updateEntry(uiform.getId(), permission);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiform);
    }
  }

  public static class CancelActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
    }
  }
  public static class TextChangeActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uiform = event.getSource();
      uiform.comment = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getChild(UIFormTextAreaInput.class));
    }
  }

  public static class ConfirmActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uiform = event.getSource();
      IShareDocumentService service = WCMCoreUtils.getService(IShareDocumentService.class);
      SpaceService spaceService = WCMCoreUtils.getService(SpaceService.class);
      DocumentService documentService = WCMCoreUtils.getService(DocumentService.class);

      UIApplication uiApp = uiform.getAncestorOfType(UIApplication.class);

      try {
        uiform.addPermission();
      } catch (PermissionException ex) {
        switch (ex.getError()) {
          case INVALID_OWNER:
            uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.InvalidOwner", null,
                    ApplicationMessage.WARNING));
            break;
          case NOT_FOUND:
            uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.Invalid", new String[]{ex.getData()
                    .replace("[","").replace("]","")}, ApplicationMessage.WARNING));
            break;
          case NO_PERMISSION:
            uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoPermission", null,
                    ApplicationMessage.WARNING));
            break;
          default:
            uiApp.addMessage(new ApplicationMessage("Error during add permission", null,
                    ApplicationMessage.WARNING));
        }
        return;
      }

      List<String> entries = uiform.entries;
      Map<String,String> permissions = uiform.permissions;
      Set<String> accessList = uiform.getWhoHasAccess();
      Node node = uiform.getNode();
      String message = "";
      Identity identity = ConversationState.getCurrent().getIdentity();
      boolean isShared = false;
      if (uiform.isOwner(identity.getUserId()) || uiform.canEdit(identity)) {
        if (uiform.getChild(UIFormTextAreaInput.class).getValue() != null)
          message = uiform.getChild(UIFormTextAreaInput.class).getValue();
        for (String name : accessList) {
          try {
            if (IdentityConstants.ANY.equals(name)
                || IdentityConstants.SYSTEM.equals(name)
                || uiform.hasPermission(name, uiform.getPermission(name))
                || uiform.isOwner(name) || uiform.isGroupType(name)) {
              continue;
            } else if (permissions.containsKey(name)) {
              String perm = permissions.get(name);
              if (!name.startsWith(SPACE_PREFIX2)) {
                service.unpublishDocumentToUser(name, (ExtendedNode) node);
                service.publishDocumentToUser(name, node, message, perm);
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToUserPlugin.NODE, node)
                    .append(ShareFileToUserPlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToUserPlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, documentService.getDocumentUrlInPersonalDocuments(node, name))
                    .append(ShareFileToUserPlugin.RECEIVER, name)
                    .append(ShareFileToUserPlugin.PERM, perm)
                    .append(ShareFileToUserPlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToUserPlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToUserPlugin.MESSAGE, message);
                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToUserPlugin.ID))).execute(ctx);
                isShared = true;
              } else {
                String groupId = name.substring("*:".length());
                service.unpublishDocumentToSpace(groupId, (ExtendedNode) node);
                String activityId = service.publishDocumentToSpace(groupId, node, message, perm);
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToSpacePlugin.NODE, node)
                    .append(ShareFileToSpacePlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToSpacePlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, documentService.getDocumentUrlInSpaceDocuments(node, groupId))
                    .append(ShareFileToSpacePlugin.RECEIVER, groupId)
                    .append(ShareFileToSpacePlugin.PERM, perm)
                    .append(ShareFileToSpacePlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToSpacePlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToSpacePlugin.ACTIVITY_ID, activityId)
                    .append(ShareFileToSpacePlugin.MESSAGE, message);
                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToSpacePlugin.ID))).execute(ctx);
                isShared = true;
              }
            } else if (!name.startsWith(SPACE_PREFIX2)) {
              service.unpublishDocumentToUser(name, (ExtendedNode) node);
            } else {
              String groupId = name.substring("*:".length());
              service.unpublishDocumentToSpace(groupId, (ExtendedNode) node);
            }
          } catch (RepositoryException e) {
            uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.InvalidEntry", null,
                ApplicationMessage.WARNING));
          }
        }
        if (entries.size() > 0) {
          for (String entry : entries) {
            if (entry.equals("") || uiform.isOwner(entry)) continue;
            else {
              String perm = permissions.get(entry);
              String activityId = "";
              if (entry.startsWith(SPACE_PREFIX2)) {
                String groupId = spaceService.getSpaceByPrettyName(entry.substring(SPACE_PREFIX2.length())).getGroupId();
                activityId = service.publishDocumentToSpace(groupId, node, message, perm);
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToSpacePlugin.NODE, node)
                    .append(ShareFileToSpacePlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToSpacePlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, documentService.getDocumentUrlInSpaceDocuments(node, groupId))
                    .append(ShareFileToSpacePlugin.RECEIVER, groupId)
                    .append(ShareFileToSpacePlugin.PERM, perm)
                    .append(ShareFileToSpacePlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToSpacePlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToSpacePlugin.ACTIVITY_ID, activityId)
                    .append(ShareFileToSpacePlugin.MESSAGE, message);
                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToSpacePlugin.ID))).execute(ctx);
                isShared = true;
              } else {
                service.publishDocumentToUser(entry, node, message, perm);
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToUserPlugin.NODE, node)
                    .append(ShareFileToUserPlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToUserPlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, documentService.getDocumentUrlInPersonalDocuments(node, entry))
                    .append(ShareFileToUserPlugin.RECEIVER, entry)
                    .append(ShareFileToUserPlugin.PERM, perm)
                    .append(ShareFileToUserPlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToUserPlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToUserPlugin.MESSAGE, message);

                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToUserPlugin.ID))).execute(ctx);
                isShared = true;
              }
            }
          }
        }
        if (isShared) {
          uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.success", null,
              ApplicationMessage.INFO));
        }
        uiform.getAncestorOfType(UIJCRExplorer.class).cancelAction();
      } else {
        uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoPermission", null,
            ApplicationMessage.WARNING));
      }
    }
  }

  public static String getPortalLoginRedirectURL() {
    String portal = PortalContainer.getCurrentPortalContainerName();
    return new StringBuffer(CommonsUtils.getCurrentDomain()).append("/").append(portal).append(LOGIN_INITIALURI).append(portal).append("/").toString();
  }

  public void addPermission() throws Exception {
    List<String> entries = this.entries;
    UIFormStringInput input = this.getUIStringInput(USER_SUGGESTER);
    String value = input.getValue();

    if (value != null && !value.trim().isEmpty()) {
      input.setValue(null);

      String[] selectedIdentities = value.split(",");
      String name = null;
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (this.hasPermissionDropDown() && (this.canEdit(identity) || this.isOwner(identity.getUserId()))) {
        String permission = this.getPermission();
        List<String> notFound = new LinkedList<String>();
        int i=0;
        if (selectedIdentities != null) {
          for (int idx = 0; idx < selectedIdentities.length; idx++) {
            name = selectedIdentities[idx].trim();
            if (name.length() > 0) {
              if (isExisting(name) && !this.isOwner(name)) {
                if (name.startsWith(SPACE_PREFIX1)) name = name.replace(SPACE_PREFIX1, SPACE_PREFIX2);
                if (!this.hasPermission(name, permission)) {
                  this.updatePermission(name, permission);
                  this.getChild(UIWhoHasAccess.class).update(name, permission);
                  if (!entries.contains(name)) entries.add(name);
                }
              } else if (this.isOwner(name)) {
                throw new PermissionException(PermissionException.Code.INVALID_OWNER);
              } else {
                notFound.add(name);
              }
            }
          }
        }
        WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
        requestContext.addUIComponentToUpdateByAjax(this);
        requestContext.getJavascriptManager()
                .require("SHARED/share-content", "shareContent")
                .addScripts("eXo.ecm.ShareContent.checkSelectedEntry('" + entries + "');");
        if (notFound.size() > 0) {
          throw new PermissionException(PermissionException.Code.NOT_FOUND, notFound.toString());
        }
      } else {
        throw new PermissionException(PermissionException.Code.NO_PERMISSION);
      }
    }
  }

  public static class PermissionException extends Exception {
    public enum Code {
      NOT_FOUND, NO_PERMISSION, INVALID_OWNER
    }

    private Code error;

    private String data;

    public PermissionException(Code error) {
      this(error, null);
    }

    public PermissionException(Code error, String data) {
      this.error = error;
      this.data = data;
    }

    public Code getError() {
      return error;
    }

    public String getData() {
      return data;
    }
  }

  public static class ChangePermissionActionListener extends EventListener<UIShareDocuments> {

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uicomponent = event.getSource();
      if (uicomponent.getPermission().equals(SHARE_PERMISSION_MODIFY)) uicomponent.setPermission(SHARE_PERMISSION_VIEW);
      else uicomponent.setPermission(SHARE_PERMISSION_MODIFY);
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomponent);

      event.getRequestContext().getJavascriptManager()
              .require("SHARED/share-content", "shareContent")
              .addScripts("eXo.ecm.ShareContent.checkSelectedEntry('" + uicomponent.entries + "');");
    }
  }

  private void setPermission(String permission) {
    this.permission = permission;
  }

  private String getPermission() {
    return permission;
  }

  private static boolean isExisting(String name) {
    if (name.contains("space::")) {
      SpaceService service = WCMCoreUtils.getService(SpaceService.class);
      return (service.getSpaceByPrettyName(name.split("::")[1]) != null);
    } else {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
      return (identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true) != null);
    }
  }

  private boolean hasPermission(String name, String permission) {
    if (permissions.containsKey(name)) {
      return permissions.get(name).equals(permission);
    }
    return false;
  }

  private String nodeTitle;
  List<String> entries = new ArrayList<String>();
  public String comment = "";
  private NodeLocation node;
  private static final String USER_SUGGESTER = "userSuggester";
  private Map<String, String> permissions;

  public UIShareDocuments(){ }

  public String getValue() {
    return getUIStringInput(USER_SUGGESTER).getValue();
  }

  public void init() {
    try {
      addChild(UIWhoHasAccess.class, null, null);
      getChild(UIWhoHasAccess.class).init();
      addChild(new UIFormTextAreaInput("textAreaInput", "textAreaInput", ""));
      Node currentNode = this.getNode();
      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(SHARECONTENT_BUNDLE_LOCATION, Util.getPortalRequestContext().getLocale());
      String canView = resourceBundle.getString(SHARE_OPTION_CANVEW);
      String canModify = resourceBundle.getString(SHARE_OPTION_CANMODIFY);

      List<SelectItemOption<String>> itemOptions = new ArrayList<SelectItemOption<String>>();

      if(PermissionUtil.canSetProperty(currentNode)) {
        setPermissionDropDown(true);
      }else{
        setPermissionDropDown(false);
      }
      addUIFormInput(new UIFormStringInput(USER_SUGGESTER, null, null));
      permissions = getAllPermissions();
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }


  public String getDocumentName() throws Exception {
    return nodeTitle;
  }

  public ExtendedNode getNode(){
    ExtendedNode node = (ExtendedNode)NodeLocation.getNodeByLocation(this.node);
    try {
      if (node.isNodeType("exo:symlink") && node.hasProperty("exo:uuid")) {
        LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
        return (ExtendedNode)linkManager.getTarget(node);
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
    return node;
  }

  public String getIconURL(){
    try {
      return Utils.getNodeTypeIcon(getNode(), "uiIcon24x24");
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
    return null;
  }
  public void setSelectedNode(NodeLocation node) throws Exception {
    this.node = node;
    this.nodeTitle = org.exoplatform.ecm.webui.utils.Utils.getTitle(getNode());
  }

  public Set<String> getWhoHasAccess() {
    Set<String> set = new HashSet<String>();
    try {
      for (AccessControlEntry t : getNode().getACL().getPermissionEntries()) {
        set.add(t.getIdentity());
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return null;
    }
    return set;
  }
  
  /**
   * Used to check edit permission on the current document node for users, spaces members
   * @param username
   * @return True if the given username has Edit permission on the current node.
   */
  public boolean canEdit(String username) {
    try {
      AccessControlList controlList = getNode().getACL();
      return controlList.getPermissions(username).contains(PermissionType.ADD_NODE)
              && controlList.getPermissions(username).contains(PermissionType.SET_PROPERTY);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }
  
  /**
   * Used to check edit permission on the current document node for the logged in user.
   * @param identity
   * @return True if the given identity has Edit permission on the current node.
   */
  public boolean canEdit(Identity identity) {
    try {
      AccessManager accessManager = ((SessionImpl)getNode().getSession()).getAccessManager();
      return accessManager.hasPermission(getNode().getACL(), new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY}, identity);
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }
  
  public String getPermission(String name) {
    return canEdit(name) ? SHARE_PERMISSION_MODIFY : SHARE_PERMISSION_VIEW;
  }

  public boolean isOwner(String username) {
    try {
      return username.equals(getNode().getACL().getOwner());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

  public String getOwner() {
    try {
      return getNode().getACL().getOwner();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return null;
  }

  public Map<String, String> getAllPermissions() {
    return getWhoHasAccess().stream()
            .filter(identity -> !IdentityConstants.ANY.equals(identity)
                    && !IdentityConstants.SYSTEM.equals(identity)
                    && !isOwner(identity)).filter(identity -> !isGroupType(identity))
            .collect(Collectors.toMap(Function.identity(), identity -> getPermission(identity)));
  }

  public String getRestURL() {
    StringBuilder builder = new StringBuilder();
    builder.append("/").append(PortalContainer.getCurrentRestContextName()).append("/social/people/suggest.json?");
    builder.append("currentUser=").append(RequestContext.getCurrentInstance().getRemoteUser());
    builder.append("&typeOfRelation=").append("share_document");
    return builder.toString();
  }

  public String getComment(){
    if(this.comment == null) return "";
    return this.comment;
  }

  private String getDefaultThumbnail(Node node) throws Exception {
    String baseURI = CommonsUtils.getCurrentDomain();
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(
        node.getName() , getMimeType(node), CssClassManager.ICON_SIZE.ICON_64);

    if (cssClass.indexOf(CssClassIconFile.DEFAULT_CSS) > 0) {
      return baseURI + EXO_RESOURCES_URI  + "uiIcon64x64Templatent_file.png";
    }
    return baseURI + EXO_RESOURCES_URI + cssClass.split(" ")[0] + ICON_FILE_EXTENSION;
  }

  private String getMimeType(Node node) throws Exception {
    return DMSMimeTypeResolver.getInstance().getMimeType(node.getName());
  }

  @Override
  public void activate() {  }
  @Override
  public void deActivate() {}


}