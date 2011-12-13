/***************************************************************************
 * Copyright 2001-2010 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.drives;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 19, 2010
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/admin/drives/UINodeTypeSelector.gtmpl",
    events = {
      @EventConfig(listeners = UINodeTypeSelector.SearchNodeTypeActionListener.class),
      @EventConfig(listeners = UINodeTypeSelector.SaveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.RefreshActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.SelectedAllNodeTypesActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.ShowPageActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.OnChangeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINodeTypeSelector.CloseActionListener.class, phase = Phase.DECODE)
    }
)

public class UINodeTypeSelector extends
                               org.exoplatform.ecm.webui.nodetype.selector.UINodeTypeSelector
    implements ComponentSelector {

  private static final String ALL_DOCUMENT_TYPES = "ALL_DOCUMENT_TYPES";

  public UINodeTypeSelector() throws Exception {
  }

  public static class SearchNodeTypeActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelector = event.getSource();
      UIFormStringInput uiInputNodeType = (UIFormStringInput)uiNodeTypeSelector.findComponentById("NodeTypeText");
      String nodeTypeName = uiInputNodeType.getValue();
      if (nodeTypeName == null || nodeTypeName.length() == 0)
        return;
      if (nodeTypeName.contains("*") && !nodeTypeName.contains(".*")) {
        nodeTypeName = nodeTypeName.replace("*", ".*");
      }
      Pattern p = Pattern.compile(".*".concat(nodeTypeName.trim()).concat(".*"),
                                  Pattern.CASE_INSENSITIVE);
      if (uiNodeTypeSelector.getLSTNodetype() == null)
        uiNodeTypeSelector.setLSTNodetype(uiNodeTypeSelector.getAllNodeTypes());
      List<NodeTypeBean> lstNodetype = new ArrayList<NodeTypeBean>();
      for (NodeTypeBean nodeType : uiNodeTypeSelector.getLSTNodetype()) {
        if (p.matcher(nodeType.getName()).find()) {
          lstNodetype.add(nodeType);
        }
      }
      uiNodeTypeSelector.init(1, uiNodeTypeSelector.getSelectedNodetypes(), lstNodetype);
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShowMask(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }

  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
  public static class SaveActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelector = event.getSource();
      String returnField = uiNodeTypeSelector.getReturnFieldName();
      List<String> selectedNodetypes = uiNodeTypeSelector.getSelectedNodetypes();
      if (selectedNodetypes.contains(UINodeTypeSelector.ALL_DOCUMENT_TYPES)) {
        selectedNodetypes.remove(UINodeTypeSelector.ALL_DOCUMENT_TYPES);
        for (String docNodeType : uiNodeTypeSelector.getDocumentNodetypes()) {
          if (!selectedNodetypes.contains(docNodeType)
              && ((UIFormCheckBoxInput) uiNodeTypeSelector.findComponentById(docNodeType)).isChecked()) {
            selectedNodetypes.add(docNodeType);
          }
        }
      }
      StringBuffer sb = new StringBuffer("");
      int index=0;
      for (String strNodeType : selectedNodetypes) {
        if (index == 0) {
          sb.append(strNodeType);
        }
        else {
          sb.append(",").append(strNodeType);
        }
        index++;
      }
      String nodeTypeString = sb.toString();
      ((UISelectable)uiNodeTypeSelector.getSourceComponent()).doSelect(returnField, nodeTypeString);
      selectedNodetypes.clear();
      UIPopupWindow uiPopup = uiNodeTypeSelector.getParent();
      uiPopup.setShow(false);
      UIComponent component = uiNodeTypeSelector.getSourceComponent().getParent();
      if (component != null)
        event.getRequestContext().addUIComponentToUpdateByAjax(component);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }

  @SuppressWarnings("unchecked")
  public static class RefreshActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelector = event.getSource();
      List<UIFormCheckBoxInput> listCheckbox = new ArrayList<UIFormCheckBoxInput>();
      uiNodeTypeSelector.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      for (int i = 0; i < listCheckbox.size(); i++) {
        listCheckbox.get(i).setChecked(false);
        uiNodeTypeSelector.getSelectedNodetypes().clear();
      }
      UIPopupWindow uiPopup = event.getSource().getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShowMask(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }

  @SuppressWarnings("unchecked")
  public static class SelectedAllNodeTypesActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelector = event.getSource();
      String returnField = uiNodeTypeSelector.getReturnFieldName();
      String value = "*";
      ((UISelectable)uiNodeTypeSelector.getSourceComponent()).doSelect(returnField, value);
      UIPopupWindow uiPopup = uiNodeTypeSelector.getParent();
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiNodeTypeSelector.getSourceComponent().getParent());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }

  @SuppressWarnings("unchecked")
  public static class ShowPageActionListener extends EventListener<UIPageIterator> {
    public void execute(Event<UIPageIterator> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelector = event.getSource().getAncestorOfType(UINodeTypeSelector.class);
      List<String> selectedNodetypes = uiNodeTypeSelector.getSelectedNodetypes();
      List<UIFormCheckBoxInput> listCheckbox = new ArrayList<UIFormCheckBoxInput>();
      uiNodeTypeSelector.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      for (UIFormCheckBoxInput uiCheckBox : listCheckbox) {
        if (selectedNodetypes.contains(uiCheckBox.getValue().toString())) {
          uiCheckBox.setChecked(true);
        } else {
          uiCheckBox.setChecked(false);
        }
      }
    }
  }

  public static class CloseActionListener extends EventListener<UINodeTypeSelector> {
    public void execute(Event<UINodeTypeSelector> event) throws Exception {
      UINodeTypeSelector uiNodeTypeSelector = event.getSource();
      UIPopupWindow uiPopup = uiNodeTypeSelector.getAncestorOfType(UIPopupWindow.class);
      uiPopup.setShow(false);
      uiPopup.setRendered(false);
      UIComponent component = uiNodeTypeSelector.getSourceComponent().getParent();
      if (component != null) event.getRequestContext().addUIComponentToUpdateByAjax(component);
    }
  }
}
