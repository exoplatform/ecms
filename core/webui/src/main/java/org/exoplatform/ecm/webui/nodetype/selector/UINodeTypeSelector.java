/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.nodetype.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Dec 22, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/ecm/webui/nodetype/selector/UINodeTypeSelector.gtmpl",
    events = {
              @EventConfig(listeners = UINodeTypeSelector.SearchNodeTypeActionListener.class),
              @EventConfig(listeners = UINodeTypeSelector.SaveActionListener.class, phase = Phase.DECODE),
              @EventConfig(listeners = UINodeTypeSelector.RefreshActionListener.class, phase = Phase.DECODE),
              @EventConfig(listeners = UINodeTypeSelector.ShowPageActionListener.class, phase = Phase.DECODE),
              @EventConfig(listeners = UINodeTypeSelector.OnChangeActionListener.class, phase = Phase.DECODE),
              @EventConfig(listeners = UINodeTypeSelector.CloseActionListener.class, phase = Phase.DECODE)
             }
)
public class UINodeTypeSelector extends UIForm implements ComponentSelector {

  private UIPageIterator uiPageIterator_;

  private UIComponent    sourceUIComponent;

  private String         returnFieldName = null;

  private String         repositoryName  = null;

  private List<String> selectedNodetypes = new ArrayList<String>();

  private List<String> documentNodetypes = new ArrayList<String>();

  private static final String ALL_DOCUMENT_TYPES = "ALL_DOCUMENT_TYPES";

  private List<NodeTypeBean> lstNodetype;

  private String[] actions_ = {"Save", "Refresh", "Close"};

  public String[] getActions() {
    return actions_;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getResource(String key) {
    try {
      return Utils.getResourceBundle(Utils.LOCALE_WEBUI_DMS, key, getClass().getClassLoader());
    } catch (Exception e) {
      return key;
    }
  }

  public List<NodeTypeBean> getLSTNodetype() {
    return lstNodetype;
  }

  public void setLSTNodetype(List<NodeTypeBean> lstNodetype) {
    this.lstNodetype = lstNodetype;
  }

  public List<String> getDocumentNodetypes() {
    return documentNodetypes;
  }

  public void setDocumentNodetypes(List<String> documentNodetypes) {
    this.documentNodetypes = documentNodetypes;
  }

  public UINodeTypeSelector() throws Exception {
    addChild(UINodeTypeSearch.class, null, "SearchNodeType");
    uiPageIterator_ = addChild(UIPageIterator.class, null, "UINodeTypeSelectorIterator");
  }

  public String getReturnFieldName() {
    return returnFieldName;
  }

  public void setReturnFieldName(String name) {
    this.returnFieldName = name;
  }

  public UIComponent getSourceComponent() {
    return sourceUIComponent;
  }

  public void setSourceComponent(UIComponent uicomponent, String[] initParams) {
    sourceUIComponent = uicomponent;
    if (initParams == null || initParams.length < 0)
      return;
    for (int i = 0; i < initParams.length; i++) {
      if (initParams[i].indexOf("returnField") > -1) {
        String[] array = initParams[i].split("=");
        returnFieldName = array[1];
        break;
      }
      returnFieldName = initParams[0];
    }
  }

  public UIPageIterator getUIPageIterator() {
    return uiPageIterator_;
  }

  public List<NodeTypeBean> getNodeTypeList() throws Exception {
    return uiPageIterator_.getCurrentPageData();
  }

  public List<NodeTypeBean> getAllNodeTypes() throws Exception{
    List<NodeTypeBean> nodeList = new ArrayList<NodeTypeBean>();
    ManageableRepository mRepository = getApplicationComponent(RepositoryService.class).getCurrentRepository() ;
    NodeTypeManager ntManager = mRepository.getNodeTypeManager() ;
    NodeTypeIterator nodeTypeIter = ntManager.getAllNodeTypes() ;
    while(nodeTypeIter.hasNext()) {
      nodeList.add(new NodeTypeBean(nodeTypeIter.nextNodeType())) ;
    }
    Collections.sort(nodeList, new NodeTypeNameComparator()) ;
    return nodeList ;
  }

  protected boolean getCheckedValue(List<String> values, String name) {
    if (values.contains(name))
      return true;
    return false;
  }

  public void init(int currentPage, List<String> values) throws Exception {
    lstNodetype = getAllNodeTypes();
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    documentNodetypes = templateService.getAllDocumentNodeTypes();
    getChild(UINodeTypeSearch.class).init();
    init(currentPage, values, lstNodetype);
  }

  protected void init(int currentPage, List<String> values, List<NodeTypeBean> lstNodetype) throws Exception {
    if (lstNodetype == null) return;
    ListAccess<NodeTypeBean> nodeTypeList = new ListAccessImpl<NodeTypeBean>(NodeTypeBean.class,
                                                                             lstNodetype);
    LazyPageList<NodeTypeBean> pageList = new LazyPageList<NodeTypeBean>(nodeTypeList, 5);
    uiPageIterator_.setPageList(pageList);
    if (currentPage > uiPageIterator_.getAvailablePage())
      uiPageIterator_.setCurrentPage(uiPageIterator_.getAvailablePage());
    else
      uiPageIterator_.setCurrentPage(currentPage);

    UIFormCheckBoxInput<String> uiCheckbox = new UIFormCheckBoxInput<String>(ALL_DOCUMENT_TYPES, null, ALL_DOCUMENT_TYPES);
    uiCheckbox.setOnChange("OnChange");

    if (values != null) {
      if (values.containsAll(getDocumentNodetypes()) && !values.contains(ALL_DOCUMENT_TYPES))
        values.add(ALL_DOCUMENT_TYPES);
      if (values.contains(uiCheckbox.getValue())) {
        uiCheckbox.setChecked(true);
        if (!getSelectedNodetypes().contains(uiCheckbox.getValue())) getSelectedNodetypes().add(uiCheckbox.getValue());
       }
    }

    addChild(uiCheckbox);
    for(NodeTypeBean nt : lstNodetype) {
      String ntName = nt.getName();
      uiCheckbox = new UIFormCheckBoxInput<String>(ntName, ntName, ntName);
      uiCheckbox.setOnChange("OnChange");
      if(values != null) {
        if(values.contains(ntName)) {
          uiCheckbox.setChecked(true);
          if (!getSelectedNodetypes().contains(ntName)) getSelectedNodetypes().add(ntName);
        }
      }
      removeChildById(ntName);
      addChild(uiCheckbox);
    }
    if (values == null) getSelectedNodetypes().clear();
  }

  public void setSelectedNodetypes(List<String> selectedNodetypes) {
    this.selectedNodetypes = selectedNodetypes;
  }
  public List<String> getSelectedNodetypes() {
    return selectedNodetypes;
  }

  public static class SearchNodeTypeActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelect = event.getSource();
      UIFormStringInput uiInputNodeType = (UIFormStringInput)uiNodeTypeSelect.findComponentById("NodeTypeText");
      String nodeTypeName = uiInputNodeType.getValue();
      if (nodeTypeName == null || nodeTypeName.length() == 0) return;
      if (nodeTypeName.contains("*") && !nodeTypeName.contains(".*")) {
        nodeTypeName = nodeTypeName.replace("*", ".*");
      }
      Pattern p = Pattern.compile(".*".concat(nodeTypeName.trim()).concat(".*"),
                                  Pattern.CASE_INSENSITIVE);
      if (uiNodeTypeSelect.lstNodetype == null) {
        uiNodeTypeSelect.lstNodetype = uiNodeTypeSelect.getAllNodeTypes();
      }
      List<NodeTypeBean> lstNodetype = new ArrayList<NodeTypeBean>();
      for (NodeTypeBean nodeType : uiNodeTypeSelect.lstNodetype) {
        if (p.matcher(nodeType.getName()).find()) {
          lstNodetype.add(nodeType);
        }
      }
      uiNodeTypeSelect.init(1, uiNodeTypeSelect.getSelectedNodetypes(), lstNodetype);
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShowMask(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }

  public static class SaveActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelect = event.getSource();
      List<String> selectedNodetypes = uiNodeTypeSelect.getSelectedNodetypes();
      String returnField = uiNodeTypeSelect.getReturnFieldName();
      if (selectedNodetypes.contains(uiNodeTypeSelect.ALL_DOCUMENT_TYPES)) {
        selectedNodetypes.remove(uiNodeTypeSelect.ALL_DOCUMENT_TYPES);
        for (String docNodeType : uiNodeTypeSelect.getDocumentNodetypes()) {
          if (!selectedNodetypes.contains(docNodeType)
              && ((UIFormCheckBoxInput) uiNodeTypeSelect.findComponentById(docNodeType)).isChecked()) {
            selectedNodetypes.add(docNodeType);
          }
        }
      }
      ((UISelectable)(uiNodeTypeSelect).getSourceComponent()).doSelect(returnField, selectedNodetypes);
      selectedNodetypes.clear();
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      if (uiPopup != null) {
        uiPopup.setShow(false);
        uiPopup.setRendered(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
      }
      UIComponent component = event.getSource().getSourceComponent().getParent();
      if (component != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(component);
      }
    }
  }

  public static class ShowPageActionListener extends EventListener<UIPageIterator> {
    public void execute(Event<UIPageIterator> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelect = event.getSource().getAncestorOfType(UINodeTypeSelector.class);
      List<String> selectedNodetypes = uiNodeTypeSelect.getSelectedNodetypes();
      List<UIFormCheckBoxInput> listCheckbox = new ArrayList<UIFormCheckBoxInput>();
      uiNodeTypeSelect.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      for (UIFormCheckBoxInput uiCheckBox : listCheckbox) {
        if (selectedNodetypes.contains(uiCheckBox.getValue().toString())) {
          uiCheckBox.setChecked(true);
        } else {
          uiCheckBox.setChecked(false);
        }
      }
    }
  }

  public static class OnChangeActionListener extends EventListener<UINodeTypeSelector> {

    private void updateCheckBox(List<String> selectedNodetypes, UIFormCheckBoxInput uiCheckBox) {
      if (uiCheckBox.isChecked()) {
        if (!selectedNodetypes.contains(uiCheckBox.getValue().toString()))
          selectedNodetypes.add(uiCheckBox.getValue().toString());
      } else {
        selectedNodetypes.remove(uiCheckBox.getValue().toString());
      }
    }

    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelect = event.getSource();
      List<String> selectedNodetypes = uiNodeTypeSelect.getSelectedNodetypes();
      List<String> preSelectedNodetypes = new ArrayList<String>();
      preSelectedNodetypes.addAll(selectedNodetypes);
      List<NodeTypeBean> lstNodeType = uiNodeTypeSelect.getNodeTypeList();
      UIFormCheckBoxInput uiCheckBox = (UIFormCheckBoxInput)uiNodeTypeSelect.getChildById(ALL_DOCUMENT_TYPES);
      updateCheckBox(selectedNodetypes, uiCheckBox);
      for (NodeTypeBean nodetype : lstNodeType) {
        uiCheckBox = (UIFormCheckBoxInput) uiNodeTypeSelect.getChildById(nodetype.getName());
        updateCheckBox(selectedNodetypes, uiCheckBox);
      }

      // if at this times, check box 'ALL_DOCUMENT_TYPES' change
      if (selectedNodetypes.contains(ALL_DOCUMENT_TYPES) && !preSelectedNodetypes.contains(ALL_DOCUMENT_TYPES)) {
        for(String nodeTypeName : uiNodeTypeSelect.getDocumentNodetypes()) {
          ((UIFormCheckBoxInput) uiNodeTypeSelect.getChildById(nodeTypeName)).setChecked(true);
          if (!selectedNodetypes.contains(nodeTypeName)) selectedNodetypes.add(nodeTypeName);
        }
      } else if (!selectedNodetypes.contains(ALL_DOCUMENT_TYPES) && preSelectedNodetypes.contains(ALL_DOCUMENT_TYPES)) {
        if (selectedNodetypes.containsAll(uiNodeTypeSelect.getDocumentNodetypes()))
          for (String nodeTypeName : uiNodeTypeSelect.getDocumentNodetypes()) {
            ((UIFormCheckBoxInput) uiNodeTypeSelect.getChildById(nodeTypeName)).setChecked(false);
            selectedNodetypes.remove(nodeTypeName);
          }
      }
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShowMask(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }

  public static class CloseActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      UIComponent component = event.getSource().getSourceComponent().getParent();
      if (component != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
        event.getRequestContext().addUIComponentToUpdateByAjax(component);
      }
    }
  }

  public static class RefreshActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelect = event.getSource();
      List<UIFormCheckBoxInput> listCheckbox = new ArrayList<UIFormCheckBoxInput>();
      uiNodeTypeSelect.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      for (int i = 0; i < listCheckbox.size(); i++) {
        listCheckbox.get(i).setChecked(false);
        uiNodeTypeSelect.getSelectedNodetypes().clear();
      }
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShowMask(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }
  
  public static class NodeTypeBean {
    private String nodeTypeName_;
    private boolean isMixin_;
    
    public NodeTypeBean(NodeType nodeType) {
      this.nodeTypeName_ = nodeType.getName();
      this.isMixin_ = nodeType.isMixin();
    }

    public String getName() {
      return nodeTypeName_;
    }

    public void setName(String nodeTypeName) {
      nodeTypeName_ = nodeTypeName;
    }

    public boolean isMixin() {
      return isMixin_;
    }

    public void setMixin(boolean isMixin) {
      isMixin_ = isMixin;
    }
  }
  
  static public class NodeTypeNameComparator implements Comparator<NodeTypeBean> {
    public int compare(NodeTypeBean n1, NodeTypeBean n2) throws ClassCastException {
      String name1 = n1.getName();
      String name2 = n2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }  
}
