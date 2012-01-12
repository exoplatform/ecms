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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.RepositoryService;
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
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;

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

  final static public String  SCOPE_VALUES           = "scopeValues";

  final static public String  LINKED_TAGS            = "linked";

  final static public String  LINKED_TAGS_SET        = "tagSet";

  final static public String  TAG_STATUS_PROP        = "exo:tagStatus";

  final static public String  TAG_NAME_ACTION        = "tagNameAct";

  final static public String  ASCENDING_ORDER        = "Ascending";

  private static final String USER_FOLKSONOMY_ALIAS  = "userPrivateFolksonomy";

  private static final String GROUP_FOLKSONOMY_ALIAS = "folksonomy";

  private static final String GROUPS_ALIAS           = "groupsPath";

  final static public String  PUBLIC_TAG_NODE_PATH   = "exoPublicTagNode";

  String[]                    groups;

  String[]                    users;

  public UITaggingForm() throws Exception {
    UIFormInputSetWithActionForTaggingForm uiInputSet = new UIFormInputSetWithActionForTaggingForm(LINKED_TAGS_SET);
    uiInputSet.addUIFormInput(new UIFormStringInput(TAG_NAMES, TAG_NAMES, null));
    uiInputSet.addUIFormInput(new UIFormTextAreaInput(TAG_NAME_LIST, TAG_NAME_LIST, null));

    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    List<SelectItemOption<String>> tagScopes = new ArrayList<SelectItemOption<String>>();
    tagScopes.add(new SelectItemOption<String>(res.getString("UITaggingForm.label." + Utils.PRIVATE),
                                               Utils.PRIVATE));
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
    box.setSelectedValues(new String[] { Utils.PRIVATE });

    uiInputSet.addUIFormInput(new UIFormInputInfo(LINKED_TAGS, LINKED_TAGS, null));
    uiInputSet.setIntroduction(TAG_NAMES, "UITaggingForm.introduction.tagName");
    addUIComponentInput(uiInputSet);
    uiInputSet.setIsView(false);
    super.setActions(new String[] { "AddTag", "Cancel" });
  }

  public void activate() throws Exception {

    ExoContainer container = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
    String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class);

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
    for (String tagName : linkedTagSet) {
      if (linkedTags.length() > 0)
        linkedTags = linkedTags.append(",");
      linkedTags.append(tagName);
    }

    UIFormInputSetWithAction uiLinkedInput = getChildById(LINKED_TAGS_SET);
    uiLinkedInput.setInfoField(LINKED_TAGS, linkedTags.toString());
    uiLinkedInput.setActionInfo(LINKED_TAGS, new String[] { "Edit", "Remove" });
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
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
    String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class);

    String tagScope = this.getUIFormSelectBox(TAG_SCOPES).getValue();
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();

    return folksonomyService.getAllTagNames(workspace,
                                            getIntValue(tagScope),
                                            getStrValue(tagScope, currentNode));
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getJavascriptManager().importJavascript("eXo.ecm.ECMUtils",
                                                    "/ecm-wcm-extension/javascript/");
    context.getJavascriptManager()
           .addJavascript("eXo.ecm.ECMUtils.disableAutocomplete('UITaggingForm');");
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
        
        return;
      }
      String[] tagNames = null;
      if (tagName.indexOf(",") > -1) {
        tagNames = tagName.split(",");
        List<String> listTagNames = new ArrayList<String>(tagNames.length);
        List<String> listTagNamesClone = new ArrayList<String>(tagNames.length);
        for (String tName : tagNames) {
          listTagNames.add(tName.trim());
          listTagNamesClone.add(tName.trim());
        }
        for (int i = 0; i < listTagNames.size(); i++) {
          String tag = listTagNames.get(i);
          listTagNamesClone.remove(tag);
          if (listTagNamesClone.contains(tag)) {
            uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tag-name-duplicate",
                                                    null,
                                                    ApplicationMessage.WARNING));
            
            return;
          }
          listTagNamesClone.add(tag);
        }
      } else
        tagNames = new String[] { tagName };
      String[] fitlerTagNames = new String[tagNames.length];
      int i = 0;
      for (String t : tagNames) {
        fitlerTagNames[i] = tagNames[i].trim();
        i++;
        if (t.trim().length() == 0) {
          uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tag-name-empty",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
        if (t.trim().length() > 30) {
          uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tagName-too-long",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
        String[] arrFilterChar = { "&", "'", "$", "@", ":", "]", "[", "*", "%", "!", "/", "\\" };
        for (String filterChar : arrFilterChar) {
          if (t.indexOf(filterChar) > -1) {
            uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tagName-invalid",
                                                    null,
                                                    ApplicationMessage.WARNING));
            
            return;
          }
        }
      }
      String tagScope = uiForm.getUIFormSelectBox(TAG_SCOPES).getValue();
      List<Node> tagList = newFolksonomyService.getLinkedTagsOfDocumentByScope(uiForm.getIntValue(tagScope),
                                                                               uiForm.getStrValue(tagScope,
                                                                                                  currentNode),
                                                                               uiExplorer.getCurrentNode(),
                                                                               workspace);
      for (Node tag : tagList) {
        for (String t : fitlerTagNames) {
          if (t.equals(tag.getName())) {
            Object[] args = { t };
            uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.name-exist",
                                                    args,
                                                    ApplicationMessage.WARNING));
            
            return;
          }
        }
      }
      addTagToNode(tagScope, currentNode, fitlerTagNames, uiForm);
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

      ExoContainer container = ExoContainerContext.getCurrentContainer();
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      RepositoryService repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
      String[] roles = Utils.getGroups().toArray(new String[] {});
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
      
      RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
      ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
      String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
      String tagName = event.getRequestContext().getRequestParameter(OBJECTID);
      if (!newFolksonomyService.canEditTag(workspace, tagName, uiForm.getIntValue(tagScope), memberships)) {
        UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.editTagAccessDenied",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }

      Node currentNode = uiExplorer.getCurrentNode();
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

      ExoContainer container = ExoContainerContext.getCurrentContainer();
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);

      String userName = currentNode.getSession().getUserID();

      RepositoryService repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
      String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();

      String tagPath = "";
      if (Utils.PUBLIC.equals(scope)) {
        tagPath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH) + '/' + tagName;
        newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);
      } else if (Utils.PRIVATE.equals(scope)) {
        Node userFolksonomyNode = getUserFolksonomyFolder(userName, uiForm);
        tagPath = userFolksonomyNode.getNode(tagName).getPath();
        newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);
      } else if (Utils.GROUP.equals(scope)) {
        String groupsPath = nodeHierarchyCreator.getJcrPath(GROUPS_ALIAS);
        String folksonomyPath = nodeHierarchyCreator.getJcrPath(GROUP_FOLKSONOMY_ALIAS);
        Node groupsNode = getNode(workspace, groupsPath);
        for (String role : Utils.getGroups()) {
          tagPath = groupsNode.getNode(role).getNode(folksonomyPath).getNode(tagName).getPath();
          newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);
        }
      }
    }

    private Node getNode(String workspace, String path) throws Exception {
      ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
      RepositoryService repositoryService = (RepositoryService) myContainer.getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      return (Node) sessionProvider.getSession(workspace, manageableRepository).getItem(path);
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
      
      RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
      ManageableRepository manageableRepo = repositoryService.getCurrentRepository();
      String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
      String tagName = event.getRequestContext().getRequestParameter(OBJECTID);
      
      String tagScope = uiForm.getUIFormSelectBox(TAG_SCOPES).getValue();
      List<String> memberships = Utils.getMemberships();
      if (!newFolksonomyService.canEditTag(workspace, tagName, uiForm.getIntValue(tagScope), memberships)) {
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
