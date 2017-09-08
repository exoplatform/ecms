/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITagExplorer;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 27, 2009
 * 11:13:55 AM
 */
@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    template = "system:/groovy/portal/webui/container/UIContainer.gtmpl",
    events = {
      @EventConfig(listeners = UIEditingTagsForm.EditTagActionListener.class),
      @EventConfig(listeners = UIEditingTagsForm.RemoveTagActionListener.class, confirm = "UIEditingTagsForm.msg.confirm-remove")
    }
)
public class UIEditingTagsForm extends UIContainer implements UIPopupComponent {
  private static final Log LOG = ExoLogger.getLogger(UIEditingTagsForm.class.getName());

  private static final String PUBLIC_TAG_NODE_PATH = "exoPublicTagNode";
  private static final String USER_FOLKSONOMY_ALIAS = "userPrivateFolksonomy";

  public void activate() {
    try {
      addChild(UIEditingTagList.class, null, null);
      getChild(UIEditingTagList.class).updateGrid();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error!", e.getMessage());
      }
    }
  }

  public void deActivate() {
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    getChild(UIEditingTagList.class).updateGrid();
    super.processRender(context);
  }

  public void initTaggingFormPopup(Node selectedTag) throws Exception {
    removeChildById("TagPopup") ;
    UIPopupWindow uiPopup = addChild(UIPopupWindow.class, null, "TagPopup") ;
    uiPopup.setShowMask(true);
    uiPopup.setWindowSize(600, 200) ;
    UITagForm uiForm = createUIComponent(UITagForm.class, null, null) ;
    uiForm.setTag(selectedTag) ;
    uiPopup.setUIComponent(uiForm) ;
    uiPopup.setRendered(true) ;
    uiPopup.setShow(true) ;
    uiPopup.setResizable(true) ;
  }

  public Node getSelectedTag(String tagName) throws Exception {
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);

    String workspace = uiExplorer.getRepository().getConfiguration().getDefaultWorkspaceName();
    String userName = WCMCoreUtils.getRemoteUser();
    int scope = uiExplorer.getTagScope();

    String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);

    List<Node> tagList = (scope == NewFolksonomyService.PUBLIC) ?
            newFolksonomyService.getAllPublicTags(publicTagNodePath, workspace) :
            newFolksonomyService.getAllPrivateTags(userName);

    for (Node tag : tagList)
      if (tag.getName().equals(tagName)) return tag;
    return null;
  }

  static public class EditTagActionListener extends EventListener<UIEditingTagsForm> {
    public void execute(Event<UIEditingTagsForm> event) throws Exception {
      UIEditingTagsForm uiEditingTagsForm = event.getSource() ;
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Node selectedTag = uiEditingTagsForm.getSelectedTag(selectedName) ;
      uiEditingTagsForm.initTaggingFormPopup(selectedTag) ;

      UIJCRExplorer uiExplorer = uiEditingTagsForm.getAncestorOfType(UIJCRExplorer.class);
      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiEditingTagsForm);
    }
  }

  static public class RemoveTagActionListener extends EventListener<UIEditingTagsForm> {
    public void execute(Event<UIEditingTagsForm> event) throws Exception {
      UIEditingTagsForm uiEdit = event.getSource();
      UIJCRExplorer uiExplorer = uiEdit.getAncestorOfType(UIJCRExplorer.class);
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
      removeTagFromNode(WCMCoreUtils.getRemoteUser(), uiExplorer.getTagScope(), selectedName, uiEdit);
      uiEdit.getChild(UIEditingTagList.class).updateGrid();
      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
        UIDocumentInfo uiDocumentInfo = uiExplorer.findFirstComponentOfType(UIDocumentInfo.class);
        if (uiDocumentInfo != null) {
          uiDocumentInfo.updatePageListData();
          uiExplorer.refreshExplorer();
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
    }

    public void removeTagFromNode(String userID, int scope, String tagName, UIEditingTagsForm uiForm) throws Exception {

      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class);
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String workspace = WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName();

      String tagPath = "";
      if (NewFolksonomyService.PUBLIC == scope) {
        tagPath = newFolksonomyService.getDataDistributionType().getDataNode(
                     (Node)(WCMCoreUtils.getUserSessionProvider().getSession(workspace, WCMCoreUtils.getRepository()).getItem(
                             nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH))),
                     tagName).getPath();
        newFolksonomyService.removeTag(tagPath, workspace);
      } else if (NewFolksonomyService.PRIVATE == scope) {
        Node userFolksonomyNode = getUserFolksonomyFolder(userID, uiForm);
        tagPath = newFolksonomyService.getDataDistributionType().getDataNode(userFolksonomyNode, tagName).getPath();
        newFolksonomyService.removeTag(tagPath, workspace);
      }
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.removeTagPath(tagPath);
      uiExplorer.findFirstComponentOfType(UITagExplorer.class).updateTagList();
    }

    private Node getUserFolksonomyFolder(String userName, UIEditingTagsForm uiForm) throws Exception {
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      Node userNode = nodeHierarchyCreator.getUserNode(WCMCoreUtils.getUserSessionProvider(), userName);
      String folksonomyPath = nodeHierarchyCreator.getJcrPath(USER_FOLKSONOMY_ALIAS);
      return userNode.getNode(folksonomyPath);
    }
  }

}
