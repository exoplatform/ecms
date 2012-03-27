package org.exoplatform.wcm.webui.selector.content;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Feb 2, 2009
 */

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIContentNodeTypeSelector.SaveActionListener.class),
      @EventConfig(listeners = UIContentNodeTypeSelector.CancelActionListener.class, phase=Phase.DECODE)
    }
)

public class UIContentNodeTypeSelector extends UIForm {

  public final static String WEB_CONTENT_NODETYPE_POPUP = "WebContentNodeTypePopup";

  /**
   * Instantiates a new uIWCM node type select form.
   *
   * @throws Exception the exception
   */
  public UIContentNodeTypeSelector() throws Exception {
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  public void init() throws Exception {
    getChildren().clear();
    TemplateService tempService = getApplicationComponent(TemplateService.class);
    List<String> nodeTypes = tempService.getAllDocumentNodeTypes();
    UIFormCheckBoxInput<String> uiCheckBox = null;
    for(String nodeType : nodeTypes) {
      uiCheckBox = new UIFormCheckBoxInput<String>(nodeType, nodeType, "");
      if(propertiesSelected(nodeType)) uiCheckBox.setChecked(true);
      else uiCheckBox.setChecked(false);
      addUIFormInput(uiCheckBox);
    }
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UIContentNodeTypeSelector.label." + id) ;
    } catch (MissingResourceException ex) {
      return id + " ";
    }
  }

  /**
   * Properties selected.
   *
   * @param name the name
   *
   * @return true, if successful
   */
  private boolean propertiesSelected(String name) {
    UIPopupWindow popupWindow = Utils.getPopupContainer(this)
                                     .getChildById(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
    UIContentSelector contentSelector = (UIContentSelector) popupWindow.getUIComponent();
    UIContentSearchForm contentSearchForm = contentSelector.getChild(UIContentSearchForm.class);
    String typeValues = contentSearchForm.getUIStringInput(UIContentSearchForm.DOC_TYPE).getValue() ;
    if(typeValues == null) return false ;
    if(typeValues.indexOf(",") > -1) {
      String[] values = typeValues.split(",") ;
      for(String value : values) {
        if(value.equals(name)) return true ;
      }
    } else if(typeValues.equals(name)) {
      return true ;
    }
    return false ;
  }

  /**
   * Sets the node types.
   *
   * @param selectedNodeTypes the selected node types
   * @param uiWCSearchForm the ui wc search form
   */
  private void setNodeTypes(List<String> selectedNodeTypes, UIContentSearchForm uiWCSearchForm) {
    StringBuffer strNodeTypes = new StringBuffer();
    for (int i = 0; i < selectedNodeTypes.size(); i++) {
      if (strNodeTypes.length() == 0)
        strNodeTypes = new StringBuffer(selectedNodeTypes.get(i));
      else
        strNodeTypes.append(",").append(selectedNodeTypes.get(i));
    }
    uiWCSearchForm.getUIStringInput(UIContentSearchForm.DOC_TYPE).setValue(strNodeTypes.toString());
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UIContentNodeTypeSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    @SuppressWarnings("unchecked")
    public void execute(Event<UIContentNodeTypeSelector> event) throws Exception {
      UIContentNodeTypeSelector contentNodetypeSelector = event.getSource();
      UIPopupWindow popupWindow = Utils.getPopupContainer(contentNodetypeSelector)
                                       .getChildById(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
      UIContentSelector contentSelector = (UIContentSelector) popupWindow.getUIComponent();
      List<String> selectedNodeTypes = new ArrayList<String>();
      List<UIFormCheckBoxInput> listCheckbox =  new ArrayList<UIFormCheckBoxInput>();
      contentNodetypeSelector.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      UIContentSearchForm contentSearchForm = contentSelector.getChild(UIContentSearchForm.class);
      String nodeTypesValue = contentSearchForm.getUIStringInput(UIContentSearchForm.DOC_TYPE).getValue();
      contentNodetypeSelector.makeSelectedNode(nodeTypesValue, selectedNodeTypes, listCheckbox);
      contentNodetypeSelector.setNodeTypes(selectedNodeTypes, contentSearchForm);
      Utils.closePopupWindow(contentNodetypeSelector, WEB_CONTENT_NODETYPE_POPUP);
      contentSelector.setSelectedTab(contentSearchForm.getId());
    }
  }

  /**
   * Make selected node.
   *
   * @param nodeTypesValue the node types value
   * @param selectedNodeTypes the selected node types
   * @param listCheckbox the list checkbox
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  private void makeSelectedNode(String nodeTypesValue,
      List<String> selectedNodeTypes, List<UIFormCheckBoxInput> listCheckbox) throws Exception {
    if(nodeTypesValue != null && nodeTypesValue.length() > 0) {
      String[] array = nodeTypesValue.split(",");
      for(int i = 0; i < array.length; i ++) {
        selectedNodeTypes.add(array[i].trim());
      }
    }
    for(int i = 0; i < listCheckbox.size(); i ++) {
      if(listCheckbox.get(i).isChecked()) {
        if(!selectedNodeTypes.contains(listCheckbox.get(i).getName())) {
          selectedNodeTypes.add(listCheckbox.get(i).getName());
        }
      } else if(selectedNodeTypes.contains(listCheckbox.get(i))) {
        selectedNodeTypes.remove(listCheckbox.get(i).getName());
      } else {
        selectedNodeTypes.remove(listCheckbox.get(i).getName());
      }
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCancelActionListener<code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CancelActionEvent
   */
  public static class CancelActionListener extends EventListener<UIContentNodeTypeSelector> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentNodeTypeSelector> event) throws Exception {
      UIContentNodeTypeSelector contentNodetypeSelector = event.getSource();
      UIPopupWindow popupWindow = Utils.getPopupContainer(contentNodetypeSelector)
                                       .getChildById(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
      UIContentSelector contentSelector = (UIContentSelector) popupWindow.getUIComponent();
      UIContentSearchForm contentSearchForm = contentSelector.getChild(UIContentSearchForm.class);
      Utils.closePopupWindow(contentNodetypeSelector, WEB_CONTENT_NODETYPE_POPUP);
      contentSelector.setSelectedTab(contentSearchForm.getId());
    }
  }
}
