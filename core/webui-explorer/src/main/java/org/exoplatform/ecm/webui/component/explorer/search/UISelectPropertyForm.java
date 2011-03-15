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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trong.tran@exoplatform.com
 * May 6, 2007
 * 10:18:56 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(phase=Phase.DECODE, listeners = UISelectPropertyForm.CancelActionListener.class),
      @EventConfig(listeners = UISelectPropertyForm.AddActionListener.class),
      @EventConfig(listeners = UISelectPropertyForm.ChangeMetadataTypeActionListener.class)
    }
)
public class UISelectPropertyForm extends UIForm implements UIPopupComponent {

  final static public String METADATA_TYPE= "metadataType" ;
  final static public String PROPERTY = "property" ;

  private String fieldName_ = null ;

  private List<SelectItemOption<String>> properties_ = new ArrayList<SelectItemOption<String>>() ;

  public UISelectPropertyForm() throws Exception {
    setActions(new String[] {"Add", "Cancel"}) ;
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return super.getLabel(res, id) ;
    } catch (Exception ex) {
      return id ;
    }
  }

  public void activate() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class) ;
    UIFormSelectBox uiSelect = new UIFormSelectBox(METADATA_TYPE, METADATA_TYPE, options) ;
    uiSelect.setOnChange("ChangeMetadataType") ;
    addUIFormInput(uiSelect) ;
    String metadataPath = nodeHierarchyCreator.getJcrPath(BasePath.METADATA_PATH) ;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspaceName = dmsConfiguration.getConfig().getSystemWorkspace();
    Session session = uiExplorer.getSystemProvider().getSession(workspaceName, uiExplorer.getRepository());
    Node homeNode = (Node) session.getItem(metadataPath) ;
    NodeIterator nodeIter = homeNode.getNodes() ;
    Node meta = nodeIter.nextNode() ;
    renderProperties(meta.getName()) ;
    options.add(new SelectItemOption<String>(meta.getName(), meta.getName())) ;
    while(nodeIter.hasNext()) {
      meta = nodeIter.nextNode() ;
      options.add(new SelectItemOption<String>(meta.getName(), meta.getName())) ;
    }
    addUIFormInput(new UIFormRadioBoxInput(PROPERTY, null, properties_).
                       setAlign(UIFormRadioBoxInput.VERTICAL_ALIGN)) ;
  }

  public void deActivate() throws Exception {}

  public void setFieldName(String fieldName) { fieldName_ = fieldName ; }

  public void renderProperties(String metadata) throws Exception {
    properties_.clear() ;
    UIJCRExplorer uiExpolrer = getAncestorOfType(UIJCRExplorer.class) ;
    NodeTypeManager ntManager = uiExpolrer.getSession().getWorkspace().getNodeTypeManager() ;
    NodeType nt = ntManager.getNodeType(metadata) ;
    PropertyDefinition[] properties = nt.getPropertyDefinitions() ;
    for(PropertyDefinition property : properties) {
      String name = property.getName() ;
      if(!name.equals("exo:internalUse")) properties_.add(new SelectItemOption<String>(name, name)) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UISelectPropertyForm> {
    public void execute(Event<UISelectPropertyForm> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class) ;
      UIPopupContainer uiPopup = uiSearchContainer.getChild(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }

  static  public class AddActionListener extends EventListener<UISelectPropertyForm> {
    public void execute(Event<UISelectPropertyForm> event) throws Exception {
      UISelectPropertyForm uiForm = event.getSource() ;
      String property = uiForm.<UIFormRadioBoxInput>getUIInput(PROPERTY).getValue();
      UIPopupContainer UIPopupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      UISearchContainer uiSearchContainer = UIPopupContainer.getParent() ;
      UIConstraintsForm uiConstraintsForm =
        uiSearchContainer.findFirstComponentOfType(UIConstraintsForm.class) ;
      /* Set value for textbox */
      uiConstraintsForm.getUIStringInput(uiForm.fieldName_).setValue(property) ;
      /*  Set value of checkbox is checked when choose value of property */
      if (uiForm.fieldName_.equals(UIConstraintsForm.PROPERTY1)) {
        uiConstraintsForm.getUIFormCheckBoxInput(UIConstraintsForm.EXACTLY_PROPERTY).setChecked(true);
      } else if (uiForm.fieldName_.equals(UIConstraintsForm.PROPERTY2)) {
        uiConstraintsForm.getUIFormCheckBoxInput(UIConstraintsForm.CONTAIN_PROPERTY).setChecked(true);
      } else if (uiForm.fieldName_.equals(UIConstraintsForm.PROPERTY3)) {
        uiConstraintsForm.getUIFormCheckBoxInput(UIConstraintsForm.NOT_CONTAIN_PROPERTY).setChecked(true);
      }
      UIPopupContainer.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConstraintsForm) ;
    }
  }

  static  public class ChangeMetadataTypeActionListener extends EventListener<UISelectPropertyForm> {
    public void execute(Event<UISelectPropertyForm> event) throws Exception {
      UISelectPropertyForm uiForm = event.getSource() ;
      uiForm.renderProperties(uiForm.getUIFormSelectBox(METADATA_TYPE).getValue()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
    }
  }
}
