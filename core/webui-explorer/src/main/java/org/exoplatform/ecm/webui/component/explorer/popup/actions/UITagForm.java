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
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITagExplorer;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

import javax.jcr.Node;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 27, 2009
 * 5:03:28 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UITagForm.UpdateTagActionListener.class),
      @EventConfig(listeners = UITagForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UITagForm extends UIForm {

  final static public String TAG_NAME = "tagName" ;
  final static public String PUBLIC_TAG_NODE_PATH = "exoPublicTagNode";

  private NodeLocation selectedTag_ ;
  private String oldTagPath_;
  private String oldName_;

  public UITagForm() throws Exception {
    addUIFormInput(new UIFormStringInput(TAG_NAME, TAG_NAME, null).addValidator(MandatoryValidator.class)
                   .addValidator(ECMNameValidator.class)) ;
  }

  public Node getTag() { 
    return NodeLocation.getNodeByLocation(selectedTag_); 
  }

  public void setTag(Node selectedTag) throws Exception {
    selectedTag_ = NodeLocation.getNodeLocationByNode(selectedTag);
    if (selectedTag != null) {
      oldTagPath_ = selectedTag_.getPath();
      oldName_ = NodeLocation.getNodeByLocation(selectedTag_).getName();
      getUIStringInput(TAG_NAME).setValue(oldName_);
    }
  }

  static public class UpdateTagActionListener extends EventListener<UITagForm> {
    public void execute(Event<UITagForm> event) throws Exception {
      UITagForm uiForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;

      String workspace = uiForm.getAncestorOfType(UIJCRExplorer.class)
                               .getRepository()
                               .getConfiguration()
                               .getDefaultWorkspaceName();
      String userName = WCMCoreUtils.getRemoteUser();
      int scope = uiExplorer.getTagScope();

      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class) ;
      String tagName = uiForm.getUIStringInput(TAG_NAME).getValue().trim();
      if(tagName.trim().length() > 20) {
        uiApp.addMessage(new ApplicationMessage("UITaggingForm.msg.tagName-too-long", null,
                                                ApplicationMessage.WARNING));
        
        return;
      }      
      try {
        // add new tag
        if (uiForm.getTag() == null) {
          if (scope == NewFolksonomyService.PRIVATE) {
            newFolksonomyService.addPrivateTag(new String[] { tagName },
                                               null,
                                               workspace,
                                               userName);
          }
          if (scope == NewFolksonomyService.PUBLIC) {
            NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
            String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
            newFolksonomyService.addPublicTag(publicTagNodePath,
                                              new String[] { tagName },
                                              null,
                                              workspace);
          }
        }
        // rename tag
        else {
          if (!existTag(tagName, workspace, scope, uiForm, userName)) {
            NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
//            if (scope == NewFolksonomyService.PRIVATE) { 
//              Node newTagNode = newFolksonomyService.modifyPrivateTagName(uiForm.oldTagPath_, tagName, workspace, userName);
//              uiExplorer.setTagPath(newTagNode.getPath());
//            } else {
            //always public tags
            String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
            Node newTagNode = newFolksonomyService.modifyPublicTagName(uiForm.oldTagPath_, tagName, workspace, publicTagNodePath);
            if (uiExplorer.getTagPaths().contains(uiForm.oldTagPath_)) {
              uiExplorer.removeTagPath(uiForm.oldTagPath_);
              uiExplorer.setTagPath(newTagNode.getPath());
            }
//            }            
          } else if (!tagName.equals(uiForm.oldName_)) {
            uiApp.addMessage(new ApplicationMessage("UITagForm.msg.NameAlreadyExist", null,
                          ApplicationMessage.WARNING));
          }
        }

        UIEditingTagsForm uiEdit = uiForm.getAncestorOfType(UIEditingTagsForm.class) ;
        if (uiEdit != null) {
          uiEdit.getChild(UIEditingTagList.class).updateGrid();
        }
      } catch(Exception e) {
        String key = "UITagStyleForm.msg.error-update" ;
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        return ;
      }
      UIPopupWindow uiPopup = uiForm.getAncestorOfType(UIPopupWindow.class) ;
      uiPopup.setShow(false) ;

      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        uiSideBar.getChild(UITagExplorer.class).updateTagList();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup.getParent()) ;
    }

    private boolean existTag(String tagName, String workspace, int scope,
                             UITagForm uiForm, String userName) throws Exception {
      NewFolksonomyService newFolksonomyService = uiForm.getApplicationComponent(NewFolksonomyService.class) ;
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);
      List<Node> tagList = (scope == NewFolksonomyService.PUBLIC) ?
                            newFolksonomyService.getAllPublicTags(publicTagNodePath, workspace) :
                            newFolksonomyService.getAllPrivateTags(userName);
      for (Node tag : tagList)
        if (tag.getName().equals(tagName))
          return true;
      return false;
    }
  }

  static public class CancelActionListener extends EventListener<UITagForm> {
    public void execute(Event<UITagForm> event) throws Exception {
      UITagForm uiForm = event.getSource();
      UIPopupWindow uiPopup = uiForm.getAncestorOfType(UIPopupWindow.class) ;
      uiPopup.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup.getParent());
    }
  }

}
