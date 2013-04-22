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

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.core.ManageableRepository;
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

  final static public String  LINKED_TAGS            = "linked";

  final static public String  LINKED_TAGS_SET        = "tagSet";

  final static public String  TAG_STATUS_PROP        = "exo:tagStatus";

  final static public String  TAG_NAME_ACTION        = "tagNameAct";

  final static public String  ASCENDING_ORDER        = "Ascending";

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
    tagScopes.add(new SelectItemOption<String>(res.getString("UITaggingForm.label." + Utils.PUBLIC),
                                               Utils.PUBLIC));
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
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
    getAncestorOfType(UIJCRExplorer.class).setTagScope(getIntValue());
    for (Node tag : folksonomyService.getLinkedTagsOfDocumentByScope(getIntValue(),
                                                                     getStrValue(),
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
    //check if current user can remove tag
    NewFolksonomyService newFolksonomyService = WCMCoreUtils.getService(NewFolksonomyService.class);
    List<String> memberships = Utils.getMemberships();
    String[] actionsForTags = newFolksonomyService.canEditTag(this.getIntValue(), memberships) ?
                              new String[] {"Edit", "Remove"} : null;
    uiLinkedInput.setActionInfo(LINKED_TAGS, actionsForTags);
    uiLinkedInput.setIsShowOnly(true);
    uiLinkedInput.setIsDeleteOnly(false);
  }

  public void deActivate() throws Exception {
  }

  public int getIntValue() {
    return NewFolksonomyService.PUBLIC;
  }

  public List<String> getAllTagNames() throws Exception {
    String workspace = WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName();
    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class);
    return folksonomyService.getAllTagNames(workspace, getIntValue(), getStrValue());
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getJavascriptManager().require("SHARED/ecm-utils", "ecmutil").
    addScripts("ecmutil.ECMUtils.disableAutocomplete('UITaggingForm');");
    super.processRender(context);
  }

  private String getStrValue() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    return nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
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
      List<Node> tagList = newFolksonomyService.getLinkedTagsOfDocumentByScope(uiForm.getIntValue(),
                                                                               uiForm.getStrValue(),
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
      addTagToNode(currentNode, fitlerTagNames, uiForm);
      uiForm.activate();

      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      uiForm.getUIStringInput(TAG_NAMES).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }

    private void addTagToNode(Node currentNode, String[] tagNames, UITaggingForm uiForm) throws Exception {

      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      ManageableRepository manageableRepo = WCMCoreUtils.getRepository();
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String workspace = manageableRepo.getConfiguration().getDefaultWorkspaceName();
      String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
      newFolksonomyService.addPublicTag(publicTagNodePath, tagNames, currentNode, workspace);
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
      List<String> memberships = Utils.getMemberships();
      if (!newFolksonomyService.canEditTag(uiForm.getIntValue(), memberships)) {
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
      removeTagFromNode(currentNode, tagName, uiForm);
      uiForm.activate();

      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }

    public void removeTagFromNode(Node currentNode, String tagName, UITaggingForm uiForm) throws Exception {

      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String workspace = WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName();

      String tagPath = newFolksonomyService.getDataDistributionType().getDataNode(
              (Node)(WCMCoreUtils.getUserSessionProvider().getSession(workspace, WCMCoreUtils.getRepository()).getItem(
                      nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH))),
                      tagName).getPath();
      newFolksonomyService.removeTagOfDocument(tagPath, currentNode, workspace);

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
      List<String> memberships = Utils.getMemberships();
      if (!newFolksonomyService.canEditTag(uiForm.getIntValue(), memberships)) {
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
