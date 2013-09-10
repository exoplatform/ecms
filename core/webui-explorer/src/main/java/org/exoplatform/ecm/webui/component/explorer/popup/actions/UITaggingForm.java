/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.*;

import javax.jcr.Node;
import java.util.*;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Jan 12, 2007 11:56:51 AM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl",
events = {
  @EventConfig(listeners = UITaggingForm.AddTagActionListener.class),
  @EventConfig(listeners = UITaggingForm.EditActionListener.class, phase = Phase.DECODE),
  @EventConfig(listeners = UITaggingForm.RemoveActionListener.class, phase = Phase.DECODE),
  @EventConfig(listeners = UITaggingForm.CancelActionListener.class, phase = Phase.DECODE),
  @EventConfig(listeners = UITaggingForm.ChangeActionListener.class, phase = Phase.DECODE) })
public class UITaggingForm extends UIForm {

  final static public String  TAG_NAME_LIST          = "tagNameList";

  final static public String  TAG_NAMES              = "names";

  final static public String  TAG_SCOPES             = "tagScopes";

  final static public String  LINKED_TAGS            = "linked";

  final static public String  LINKED_TAGS_SET        = "tagSet";

  private static final String USER_FOLKSONOMY_ALIAS  = "userPrivateFolksonomy";

  private static final String GROUP_FOLKSONOMY_ALIAS = "folksonomy";

  private static final String GROUPS_ALIAS           = "groupsPath";

  final static public String  PUBLIC_TAG_NODE_PATH   = "exoPublicTagNode";

  public UITaggingForm() throws Exception {
    UIFormInputSetWithActionForTaggingForm uiInputSet = new UIFormInputSetWithActionForTaggingForm(LINKED_TAGS_SET);
    uiInputSet.addUIFormInput(new UIFormStringInput(TAG_NAMES, TAG_NAMES, null));
    uiInputSet.addUIFormInput(new UIFormTextAreaInput(TAG_NAME_LIST, TAG_NAME_LIST, null));

    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    List<SelectItemOption<String>> tagScopes = new ArrayList<SelectItemOption<String>>();
    /* Only Public tag enabled in PLF 4
    tagScopes.add(new SelectItemOption<String>(res.getString("UITaggingForm.label." + Utils.PRIVATE),
                                            Utils.PRIVATE));
     */
    tagScopes.add(new SelectItemOption<String>(res.getString("UITaggingForm.label." + Utils.PUBLIC),
        Utils.PUBLIC));
    /*
     * Disable Group and Site tag tagScopes.add(new
     * SelectItemOption<String>(res.getString("UITaggingForm.label." +
     * Utils.GROUP), Utils.GROUP)); tagScopes.add(new
     * SelectItemOption<String>(res.getString("UITaggingForm.label." +
     * Utils.SITE), Utils.SITE));
     */
    UIFormSelectBox box = new UIFormSelectBox(TAG_SCOPES, TAG_SCOPES, tagScopes);
    box.setOnChange("Change");
    uiInputSet.addUIFormInput(box);
    box.setSelectedValues(new String[] { Utils.PUBLIC });
    box.setRendered(false);

    uiInputSet.addUIFormInput(new UIFormInputInfo(LINKED_TAGS, LINKED_TAGS, null));
    uiInputSet.setIntroduction(TAG_NAMES, "UITaggingForm.introduction.tagName");
    addUIComponentInput(uiInputSet);
    uiInputSet.setIsView(false);
    super.setActions(new String[] {"Cancel" });
  }

  public void activate() throws Exception {

    String workspace = WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName();
    NewFolksonomyService folksonomyService = WCMCoreUtils.getService(NewFolksonomyService.class);

    StringBuilder linkedTags = new StringBuilder();
    Set<String> linkedTagSet = new HashSet<String>();
    String tagScope = this.getUIFormSelectBox(TAG_SCOPES).getValue();

    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
    getAncestorOfType(UIJCRExplorer.class).setTagScope(getIntValue(tagScope));
    for (Node tag : folksonomyService.getLinkedTagsOfDocumentByScope(getIntValue(tagScope),
                                                                     getStrValue(tagScope,
                                                                                 currentNode),
                                                                                 currentNode,
                                                                                 workspace)) {
      linkedTagSet.add(tag.getName());
    }

    List<String> linkedTagList = new ArrayList<String>(linkedTagSet);
    Collections.sort(linkedTagList);

    for (String tagName : linkedTagList) {
      if (linkedTags.length() > 0)
        linkedTags = linkedTags.append(",");
      linkedTags.append(tagName);
    }

    UIFormInputSetWithAction uiLinkedInput = getChildById(LINKED_TAGS_SET);
    uiLinkedInput.setInfoField(LINKED_TAGS, linkedTags.toString());
    //check if current user can remove tag
    NewFolksonomyService newFolksonomyService = WCMCoreUtils.getService(NewFolksonomyService.class);
    List<String> memberships = Utils.getMemberships();
    String[] actionsForTags = newFolksonomyService.canEditTag(this.getIntValue(tagScope), memberships) ?
                                                                                                        new String[] {"Edit", "Remove"} : null;
                                                                                                        uiLinkedInput.setActionInfo(LINKED_TAGS, actionsForTags);
                                                                                                        uiLinkedInput.setIsShowOnly(true);
                                                                                                        uiLinkedInput.setIsDeleteOnly(false);
  }

  public void deActivate() throws Exception {
  }

  public int getIntValue(String scope) {
    if (Utils.PUBLIC.equals(scope))
      return NewFolksonomyService.PUBLIC;
    else if (Utils.GROUP.equals(scope))
      return NewFolksonomyService.GROUP;
    else if (Utils.PRIVATE.equals(scope))
      return NewFolksonomyService.PRIVATE;
    return NewFolksonomyService.SITE;
  }

  public List<String> getAllTagNames() throws Exception {
    String workspace = WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName();
    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class);

    String tagScope = this.getUIFormSelectBox(TAG_SCOPES).getValue();
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();

    return folksonomyService.getAllTagNames(workspace,
                                            getIntValue(tagScope),
                                            getStrValue(tagScope, currentNode));
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    //    context.getJavascriptManager().importJavascript("eXo.ecm.ECMUtils",
    //                                                    "/ecm-wcm-extension/javascript/");

    context.getJavascriptManager().require("SHARED/ecm-utils", "ecmutil").
    addScripts("ecmutil.ECMUtils.disableAutocomplete('UITaggingForm');");
    super.processRender(context);
  }

  private String getStrValue(String scope, Node node) throws Exception {
    StringBuilder ret = new StringBuilder();
    if (Utils.PRIVATE.equals(scope))
      ret.append(node.getSession().getUserID());
    else if (Utils.GROUP.equals(scope)) {
      for (String group : Utils.getGroups())
        ret.append(group).append(';');
      ret.deleteCharAt(ret.length() - 1);
    } else if (Utils.PUBLIC.equals(scope)) {
      NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
      ret.append(nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH));
    }
    return ret.toString();
  }

  static public class AddTagActionListener extends EventListener<UITaggingForm> {
    public void execute(Event<UITaggingForm> event) throws Exception {
      UITaggingForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      String workspace = uiForm.getAncestorOfType(UIJCRExplorer.class)
          .getRepository()
          .getConfiguration()
          .getDefaultWorkspaceName();
      String tagName = uiForm.getUIStringInput(TAG_NAMES).getValue();
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.addLockToken(currentNode);
      if (tagName == null || tagName.trim().length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tag-name-empty",
                                                null,
                                                ApplicationMessage.WARNING));
	      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        return;
      }
      String[] tagNames;
      tagNames = tagName.split(",");
      List<String> listTagNames = new ArrayList<String>(tagNames.length);
      List<String> listTagNamesClone = new ArrayList<String>(tagNames.length);

      if (tagName.contains(",")) {
        for (String tName : tagNames) {
          listTagNames.add(tName.trim());
          listTagNamesClone.add(tName.trim());
        }
        for (String tag : listTagNames) {
          listTagNamesClone.remove(tag);
          if (listTagNamesClone.contains(tag)) {
            continue;
          }
          listTagNamesClone.add(tag);
        }
      } else {
        listTagNames.add(tagName.trim());
        listTagNamesClone.add(tagName.trim());
      }
      for (String t : listTagNames) {
        if (t.trim().length() == 0) {
          listTagNamesClone.remove(t);
          continue;
        }
        if (t.trim().length() > 30) {
          listTagNamesClone.remove(t);
          continue;
        }
        String[] arrFilterChar = {"&", "'", "$", "@", ":", "]", "[", "*", "%", "!", "/", "\\"};
        for (String filterChar : arrFilterChar) {
          if (t.contains(filterChar)) {
            listTagNamesClone.remove(t);
          }
        }
      }
      if(listTagNamesClone.size() == 0) {
	      uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tagName-empty-or-invalid",
		      null,
		      ApplicationMessage.WARNING));
	      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
	      return;
      }
      String tagScope = uiForm.getUIFormSelectBox(TAG_SCOPES).getValue();
      List<Node> tagList = newFolksonomyService.getLinkedTagsOfDocumentByScope(uiForm.getIntValue(tagScope),
                                                                               uiForm.getStrValue(tagScope,
                                                                               currentNode),
                                                                               uiExplorer.getCurrentNode(),
                                                                               workspace);
      for (Node tag : tagList) {
        for (String t : listTagNames) {
          if (t.equals(tag.getName())) {
            listTagNamesClone.remove(t);
          }
        }
      }
      if(listTagNamesClone.size() > 0)
        addTagToNode(tagScope, currentNode, listTagNamesClone.toArray(new String[listTagNamesClone.size()]), uiForm);
      uiForm.activate();

      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      uiForm.getUIStringInput(TAG_NAMES).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }

    private void addTagToNode(String scope,
                              Node currentNode,
                              String[] tagNames,
                              UITaggingForm uiForm) throws Exception {

      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      ManageableRepository manageableRepo = WCMCoreUtils.getRepository();
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
      List<String> groups = Utils.getGroups();
      String[] roles = groups.toArray(new String[groups.size()]);
      String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);

      if (Utils.PUBLIC.equals(scope))
        newFolksonomyService.addPublicTag(publicTagNodePath,
                                          tagNames,
                                          currentNode,
                                          workspace);
      // else if (SITE.equals(scope))
      // newFolksonomyService.addSiteTag(siteName, treePath, tagNames,
      // currentNode, repository, workspace);
      else if (Utils.GROUP.equals(scope))
        newFolksonomyService.addGroupsTag(tagNames, currentNode, workspace, roles);
      else if (Utils.PRIVATE.equals(scope)) {
        String userName = currentNode.getSession().getUserID();
        newFolksonomyService.addPrivateTag(tagNames, currentNode, workspace, userName);
      }
    }
  }

  static public class CancelActionListener extends EventListener<UITaggingForm> {
    public void execute(Event<UITaggingForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  static public class RemoveActionListener extends EventListener<UITaggingForm> {
    public void execute(Event<UITaggingForm> event) throws Exception {
      UITaggingForm uiForm = event.getSource();
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      String tagScope = uiForm.getUIFormSelectBox(TAG_SCOPES).getValue();
      List<String> memberships = Utils.getMemberships();
      if (!newFolksonomyService.canEditTag(uiForm.getIntValue(tagScope), memberships)) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.editTagAccessDenied",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }

      Node currentNode = uiExplorer.getCurrentNode();
      String tagName = event.getRequestContext().getRequestParameter(OBJECTID);
      String[] arrFilterChar = { "&", "'", "$", "@", ":", "]", "[", "*", "%", "!", "/", "\\" };
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if (!Utils.isNameValid(tagName, arrFilterChar)) {
        uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tagName-invalid",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
      removeTagFromNode(tagScope, currentNode, tagName, uiForm);
      uiForm.activate();

      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }

    public void removeTagFromNode(String scope,
                                  Node currentNode,
                                  String tagName,
                                  UITaggingForm uiForm) throws Exception {

      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String userName = currentNode.getSession().getUserID();
      String workspace = WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName();

      String tagPath;
      if (Utils.PUBLIC.equals(scope)) {
        tagPath = newFolksonomyService.getDataDistributionType().getDataNode(
                                                                             (Node)(WCMCoreUtils.getUserSessionProvider().getSession(workspace, WCMCoreUtils.getRepository()).getItem(
                                                                                                                                                                                      nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH))),
                                                                                                                                                                                      tagName).getPath();
        newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);
      } else if (Utils.PRIVATE.equals(scope)) {
        Node userFolksonomyNode = getUserFolksonomyFolder(userName, uiForm);
        tagPath = newFolksonomyService.getDataDistributionType().getDataNode(userFolksonomyNode, tagName).getPath();
        newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);
      } else if (Utils.GROUP.equals(scope)) {
        String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_ALIAS);
        String folksonomyPath = nodeHierarchyCreator.getJcrPath(GROUP_FOLKSONOMY_ALIAS);
        Node groupsNode = getNode(workspace, groupsPath);
        for (String role : Utils.getGroups()) {
          tagPath = newFolksonomyService.getDataDistributionType().getDataNode(groupsNode.getNode(role).getNode(folksonomyPath), 
                                                                               tagName).getPath();
          newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);
        }
      }
    }

    private Node getNode(String workspace, String path) throws Exception {
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      return (Node) sessionProvider.getSession(workspace, WCMCoreUtils.getRepository()).getItem(path);
    }

    private Node getUserFolksonomyFolder(String userName, UITaggingForm uiForm) throws Exception {
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      Node userNode = nodeHierarchyCreator.getUserNode(WCMCoreUtils.getUserSessionProvider(), userName);
      String folksonomyPath = nodeHierarchyCreator.getJcrPath(USER_FOLKSONOMY_ALIAS);
      return userNode.getNode(folksonomyPath);
    }
  }

  static public class ChangeActionListener extends EventListener<UITaggingForm> {
    public void execute(Event<UITaggingForm> event) throws Exception {
      UITaggingForm uiForm = event.getSource();
      uiForm.activate();
    }
  }

  static public class EditActionListener extends EventListener<UITaggingForm> {
    public void execute(Event<UITaggingForm> event) throws Exception {
      UITaggingForm uiForm = event.getSource();
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      String tagScope = uiForm.getUIFormSelectBox(TAG_SCOPES).getValue();
      List<String> memberships = Utils.getMemberships();
      if (!newFolksonomyService.canEditTag(uiForm.getIntValue(tagScope), memberships)) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.editTagAccessDenied",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
      ((UITaggingFormContainer) uiForm.getParent()).edit(event);
    }
  }

}
