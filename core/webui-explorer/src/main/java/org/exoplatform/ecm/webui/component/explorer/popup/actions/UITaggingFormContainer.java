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

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UISideBar;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Dec 10, 2009
 * 4:56:12 PM
 */

@ComponentConfig(
    lifecycle = UIContainerLifecycle.class,
    template = "system:/groovy/portal/webui/container/UIContainer.gtmpl"
//    events = {
//      @EventConfig(listeners = UITaggingFormContainer.EditActionListener.class, phase = Phase.DECODE)
//    }
)
public class UITaggingFormContainer extends UIContainer implements UIPopupComponent {

  public void activate() throws Exception {
    UITaggingForm uiForm = addChild(UITaggingForm.class, null, null);
    uiForm.activate();
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    getChild(UITaggingForm.class).activate();
    super.processRender(context);
  }

  public void deActivate() throws Exception {
  }

  private void initTaggingFormPopup(Node selectedTag) throws Exception {
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
    UITaggingForm uiTaggingForm = getChild(UITaggingForm.class);

    String workspace = uiExplorer.getRepository().getConfiguration().getDefaultWorkspaceName();
    String userName = uiExplorer.getSession().getUserID();
    String tagScope = uiTaggingForm.getUIFormSelectBox(UITaggingForm.TAG_SCOPES).getValue();
    int scope = uiTaggingForm.getIntValue(tagScope);
    uiExplorer.setTagScope(scope);

    String publicTagNodePath = nodeHierarchyCreator.getJcrPath(UITaggingForm.PUBLIC_TAG_NODE_PATH);

    List<Node> tagList = (scope == NewFolksonomyService.PUBLIC) ?
            newFolksonomyService.getAllPublicTags(publicTagNodePath, workspace) :
            newFolksonomyService.getAllPrivateTags(userName);

    for (Node tag : tagList)
      if (tag.getName().equals(tagName)) return tag;
    return null;
  }

  public void edit(Event<? extends UIComponent> event) throws Exception {
    UITaggingFormContainer uiTaggingFormContainer = this;
    String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
    Node selectedTag = uiTaggingFormContainer.getSelectedTag(selectedName);
    uiTaggingFormContainer.initTaggingFormPopup(selectedTag);

    UIJCRExplorer uiExplorer = uiTaggingFormContainer.getAncestorOfType(UIJCRExplorer.class);
    Preference preferences = uiExplorer.getPreference();
    if (preferences.isShowSideBar()) {
      UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
    }
    event.getRequestContext().addUIComponentToUpdateByAjax(uiTaggingFormContainer);
  }

  static public class EditActionListener extends EventListener<UITaggingFormContainer> {
    public void execute(Event<UITaggingFormContainer> event) throws Exception {
      UITaggingFormContainer uiTaggingFormContainer = event.getSource();
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
      Node selectedTag = uiTaggingFormContainer.getSelectedTag(selectedName);
      uiTaggingFormContainer.initTaggingFormPopup(selectedTag);

      UIJCRExplorer uiExplorer = uiTaggingFormContainer.getAncestorOfType(UIJCRExplorer.class);
      Preference preferences = uiExplorer.getPreference();
      if (preferences.isShowSideBar()) {
        UISideBar uiSideBar = uiExplorer.findFirstComponentOfType(UISideBar.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSideBar);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaggingFormContainer);
    }
  }


}
