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
package org.exoplatform.ecm.webui.component.explorer.sidebar ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.ecm.webui.component.explorer.UIJcrExplorerContainer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.action.ExplorerActionComponent;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          nguyenkequanghung@yahoo.com
 * oct 5, 2006
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/sidebar/UISideBar.gtmpl",
    events = {
        @EventConfig(listeners = UISideBar.CloseActionListener.class)
    }
)
public class UISideBar extends UIContainer {
  public static final String UI_TAG_EXPLORER = "TagExplorer";

  private String                           currentComp;

  private static final Log                 LOG            = ExoLogger.getLogger(UISideBar.class.getName());

  public static final String               EXTENSION_TYPE = "org.exoplatform.ecm.dms.UISideBar";

  public static final int VISIBLE_COMPONENT_SIZE = 5;


  private List<UIAbstractManagerComponent> managers
      = Collections.synchronizedList(new ArrayList<UIAbstractManagerComponent>());

  private String selectedComp;

  private List<UIAbstractManagerComponent> lstVisibleComp;

  private List<UIAbstractManagerComponent> lstHiddenComp;


  public UISideBar() throws Exception {
    addChild(UITreeExplorer.class, null, null).setRendered(false);
    addChild(UIViewRelationList.class, null, null).setRendered(false);
    addChild(UITagExplorer.class, null, null).setRendered(false);
    addChild(UIClipboard.class, null, null).setRendered(false);
    addChild(UISavedSearches.class, null, null).setRendered(false);
    addChild(UIAllItems.class, null, null);
    addChild(UIAllItemsByType.class, null, null);
  }

  public List<UIAbstractManagerComponent> getLstVisibleComp() {
    return lstVisibleComp;
  }

  public void setLstVisibleComp(List<UIAbstractManagerComponent> lstVisibleComp) {
    this.lstVisibleComp = lstVisibleComp;
  }

  public List<UIAbstractManagerComponent> getLstHiddenComp() {
    return lstHiddenComp;
  }

  public void setLstHiddenComp(List<UIAbstractManagerComponent> lstHiddenComp) {
    this.lstHiddenComp = lstHiddenComp;
  }

  public void setSelectedComp(String componentName) {
    selectedComp = componentName;
  }

  public void initComponents() throws Exception {
    lstVisibleComp = new ArrayList<UIAbstractManagerComponent>(VISIBLE_COMPONENT_SIZE);
    lstHiddenComp = new ArrayList<UIAbstractManagerComponent>();
    List<UIAbstractManagerComponent> managers = getManagers();
    for (int i = 0; i < managers.size(); i++) {
      UIAbstractManagerComponent component = managers.get(i);

      if (lstVisibleComp.size() < VISIBLE_COMPONENT_SIZE) {
        if (!isHideExplorerPanel() || !(component instanceof ExplorerActionComponent)) {
          lstVisibleComp.add(component);
        }
      } else {
        lstHiddenComp.add(component);
      }
    }
  }

  public String getCurrentComp() throws Exception {
    if(currentComp == null || currentComp.length() == 0) {
      currentComp = getChild(UITreeExplorer.class).getId();
    }
    if (isHideExplorerPanel() && getChild(UITreeExplorer.class).getId().equals(currentComp)) {
      currentComp = getChild(UITagExplorer.class).getId();
      this.getAncestorOfType(UIJCRExplorer.class).setCurrentState();
      this.getChild(UITagExplorer.class).updateTagList();
    }
    return currentComp;
  }

  public String getSelectedComp() throws Exception {
    if(selectedComp == null || selectedComp.length() == 0) {
      selectedComp = "Explorer";
    }
    if (isHideExplorerPanel() && "Explorer".equals(selectedComp)) {
      selectedComp = UI_TAG_EXPLORER;
    }
    return selectedComp;
  }

  public void updateSideBarView() throws Exception {
    boolean showFilterBar = getAncestorOfType(UIJCRExplorerPortlet.class).isShowFilterBar();
    getChild(UIAllItems.class).setRendered(showFilterBar);
    getChild(UIAllItemsByType.class).setRendered(showFilterBar);
  }


  public void setCurrentComp(String currentComp) {
    this.currentComp = currentComp;
  }

  public void renderSideBarChild(String[] arrId) throws Exception {
    for(String id : arrId) {
      setRenderedChild(id); // Need to remove this because we've already called updateSideBarView() but need Checking
      renderChild(id);
    }
  }

  public String getRepository() {
    return getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
  }

  static public class CloseActionListener extends EventListener<UISideBar> {
    public void execute(Event<UISideBar> event) throws Exception {
      UIWorkingArea uiWorkingArea = event.getSource().getParent();
      uiWorkingArea.setShowSideBar(false);
      UIJCRExplorerPortlet explorerPorltet = uiWorkingArea.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = explorerPorltet.findFirstComponentOfType(UIJCRExplorer.class);
      UIJcrExplorerContainer uiJcrExplorerContainer= explorerPorltet.getChild(UIJcrExplorerContainer.class);
      uiExplorer.refreshExplorer();
      uiJcrExplorerContainer.setRenderedChild(UIJCRExplorer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
    }
  }

  private List<UIExtension> getUIExtensionList() {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    return manager.getUIExtensions(EXTENSION_TYPE);
  }

  public synchronized void initialize() throws Exception {
    List<UIExtension> extensions = getUIExtensionList();
    if (extensions == null) {
      return;
    }
    managers.clear();
    Map<String, Object> context = new HashMap<String, Object>();
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    context.put(UIJCRExplorer.class.getName(), uiExplorer);
    for (UIExtension extension : extensions) {
      UIComponent component = addUIExtension(extension, context);
      if (component != null && !managers.contains(component)) {
        managers.add((UIAbstractManagerComponent) component);
      }
    }
    initComponents();
  }

  private synchronized UIComponent addUIExtension(UIExtension extension, Map<String, Object> context) throws Exception {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    UIComponent component = manager.addUIExtension(extension, context, this);
    if (component instanceof UIAbstractManagerComponent) {
      // You can access to the given extension and the extension is valid
      UIAbstractManagerComponent uiAbstractManagerComponent = (UIAbstractManagerComponent) component;
      uiAbstractManagerComponent.setUIExtensionName(extension.getName());
      uiAbstractManagerComponent.setUIExtensionCategory(extension.getCategory());
      return component;
    } else if (component != null) {
      // You can access to the given extension but the extension is not valid
      if (LOG.isWarnEnabled()) {
        LOG.warn("All the extension '" + extension.getName() + "' of type '" + EXTENSION_TYPE
          + "' must be associated to a component of type " + UIAbstractManagerComponent.class);
      }
      removeChild(component.getClass());
    }
    return null;
  }

  public List<UIAbstractManagerComponent> getManagers() {
    List<UIAbstractManagerComponent> managers = new ArrayList<UIAbstractManagerComponent>();
    managers.addAll(this.managers);
    return managers;
  }

  public void unregister(UIAbstractManagerComponent component) {
    managers.remove(component);
  }
  
  public boolean isRenderComponent(String actionName) throws Exception {
    if ("Explorer".equals(actionName)) {
      return !isHideExplorerPanel();
    }
    return true;
  }
  
  private boolean isHideExplorerPanel() throws Exception {
    UIAddressBar uiAddress = this.getAncestorOfType(UIJCRExplorer.class).
    findFirstComponentOfType(UIAddressBar.class);
    String viewName = uiAddress.getSelectedViewName();
    Node viewNode = WCMCoreUtils.getService(ManageViewService.class).getViewByName(
                                 viewName, WCMCoreUtils.getSystemSessionProvider());
    return viewNode.getProperty(NodetypeConstant.EXO_HIDE_EXPLORER_PANEL).getBoolean();
  }

}
