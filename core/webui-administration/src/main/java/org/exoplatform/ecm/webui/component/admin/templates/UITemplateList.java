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
package org.exoplatform.ecm.webui.component.admin.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * Oct 03, 2006
 * 9:43:23 AM
 */
@ComponentConfig(
                 template = "app:/groovy/webui/component/admin/template/UITemplateList.gtmpl",
                 events = {
                     @EventConfig(listeners = UITemplateList.EditActionListener.class),
                     @EventConfig(listeners = UITemplateList.DeleteActionListener.class, confirm = "UITemplateList.msg.confirm-delete"),
                     @EventConfig(listeners = UITemplateList.AddNewActionListener.class)
                 }
    )

public class UITemplateList extends UIPagingGrid {

  private static String[] NODETYPE_BEAN_FIELD = {"icon", "label", "name"} ;
  private static String[] NODETYPE_ACTION = {"Edit", "Delete"} ;
  public static final String DOCUMENTS_TEMPLATE_TYPE = "templates";
  public static final String ACTIONS_TEMPLATE_TYPE = "actions";
  public static final String OTHERS_TEMPLATE_TYPE = "others";
  public static final String LABEL_PROPERTY = "label";
  public static final String ICON_FIELD = "icon";
  public static final String LABEL_FIELD = "label";

  private String filter = DOCUMENTS_TEMPLATE_TYPE;

  public void setTemplateFilter(String filter) {
    this.filter = filter;
  }  
  public String getTemplateFilter() {
    return this.filter;
  }

  public UITemplateList() throws Exception {
    configure("name", NODETYPE_BEAN_FIELD, NODETYPE_ACTION) ;
  }

  public String[] getActions() {
    return new String[] {"AddNew"} ;
  }

  static public class TemplateComparator implements Comparator<TemplateData> {
    public int compare(TemplateData t1, TemplateData t2) throws ClassCastException {
      String name1 = t1.getName();
      String name2 = t2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  static public class EditActionListener extends EventListener<UITemplateList> {
    public void execute(Event<UITemplateList> event) throws Exception {
      UITemplateList nodeTemplateList = event.getSource() ;

      UITemplatesManager uiManager = nodeTemplateList.getAncestorOfType(UITemplatesManager.class);
      UITemplateContainer uiTemplateContainer = uiManager.getChildById(uiManager.getSelectedTabId()) ;

      String nodeType = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIViewTemplate uiViewTemplate = uiTemplateContainer.createUIComponent(UIViewTemplate.class, null, 
                                                                            "UIViewTemplate" + "_" + uiManager.getSelectedTabId()) ;
      uiViewTemplate.getChild(UITemplateEditForm.class).update(nodeType) ;
      uiViewTemplate.setNodeTypeName(nodeType) ;
      UIDialogTab uiDialogTab = uiViewTemplate.findFirstComponentOfType(UIDialogTab.class) ;
      uiDialogTab.updateGrid(nodeType) ;
      UITemplateContent uiDialogTabForm = uiViewTemplate.findComponentById(UIDialogTab.DIALOG_FORM_NAME) ;
      uiDialogTabForm.setNodeTypeName(nodeType) ;
      uiDialogTabForm.update(null) ;
      UIViewTab uiViewTab = uiViewTemplate.findFirstComponentOfType(UIViewTab.class) ;
      uiViewTab.updateGrid(nodeType) ;
      UITemplateContent uiViewTabForm = uiViewTemplate.findComponentById(UIViewTab.VIEW_FORM_NAME) ;
      uiViewTabForm.setNodeTypeName(nodeType) ;
      uiViewTabForm.update(null) ;
      UISkinTab uiSkinTab = uiViewTemplate.findFirstComponentOfType(UISkinTab.class) ;
      uiSkinTab.updateGrid(nodeType) ;
      UITemplateContent uiSkinTabForm = uiViewTemplate.findComponentById(UISkinTab.SKIN_FORM_NAME) ;
      uiSkinTabForm.setNodeTypeName(nodeType) ;
      uiSkinTabForm.update(null);      
      uiManager.initPopup(uiViewTemplate);
      UIPopupWindow uiPopup = uiManager.getChildById(UITemplatesManager.POPUP_TEMPLATE_ID);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }

  }

  static public class DeleteActionListener extends EventListener<UITemplateList> {
    public void execute(Event<UITemplateList> event) throws Exception {
      UITemplatesManager uiTemplatesManager = event.getSource().getAncestorOfType(UITemplatesManager.class);
      UITemplateContainer uiTemplateContainer = uiTemplatesManager.getChildById(uiTemplatesManager.getSelectedTabId());
      UITemplateList uiTemplateList = uiTemplateContainer.getChild(UITemplateList.class);

      if (uiTemplatesManager.isEditingTemplate()) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UITemplateList.msg.editing-template", null, ApplicationMessage.WARNING)) ;
        return;
      }

      String nodeType = event.getRequestContext().getRequestParameter(OBJECTID);
      TemplateService templateService = uiTemplateList.getApplicationComponent(TemplateService.class);
      try {
        DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, true);
        templateService.removeManagedNodeType(nodeType);
      } catch (PathNotFoundException ex) {
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UITemplateList.msg.template-not-exist", null, ApplicationMessage.WARNING)) ;
        return;
      }
      uiTemplateList.refresh(uiTemplateList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTemplatesManager);
    }
  }

  static public class AddNewActionListener extends EventListener<UITemplateList> {
    public void execute(Event<UITemplateList> event) throws Exception {
      UITemplatesManager uiTemplatesManager = event.getSource().getAncestorOfType(UITemplatesManager.class) ;      
      UITemplateContainer uiTemplateContainer = uiTemplatesManager.getChildById(uiTemplatesManager.getSelectedTabId());
      UITemplateList uiList = uiTemplateContainer.getChild(UITemplateList.class);
      UITemplateForm uiTemplateForm = uiTemplateContainer.createUIComponent(UITemplateForm.class, null, null) ;
      uiTemplateForm.setFilter(uiList.getTemplateFilter());         
      uiTemplateForm.refresh();
      uiTemplatesManager.initPopup(uiTemplateForm);
      UIPopupWindow uiPopup = uiTemplatesManager.getChildById(UITemplatesManager.POPUP_TEMPLATE_ID);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }

  static public class TemplateData {
    private String name ;
    private String label;
    private String icon;

    public TemplateData(String dataName, String dataLabel, String dataIcon) { 
      name = dataName ;
      label = dataLabel;
      icon = dataIcon;
    }
    public String getName() { return name ; }
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
  }

  @Override
  public void refresh(int currentPage) throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    List<String> documentNodeTypes = templateService.getAllDocumentNodeTypes();
    Node templatesHome = templateService.getTemplatesHome(WCMCoreUtils.getSystemSessionProvider());
    List<TemplateData> templateData = new ArrayList<TemplateData>();
    if (templatesHome != null) {
      NodeTypeManager ntManager = templatesHome.getSession().getWorkspace().getNodeTypeManager();      
      NodeTypeIterator nodetypeIter = ntManager.getAllNodeTypes();      
      List<String> listNodeTypeName = new ArrayList<String>();
      while (nodetypeIter.hasNext()) {
        NodeType n1 = nodetypeIter.nextNodeType();
        listNodeTypeName.add(n1.getName());
      }
      NodeIterator nodes = templatesHome.getNodes();
      String label = "";
      String icon = "";
      while (nodes.hasNext()) {
        Node node = nodes.nextNode();        
        if (listNodeTypeName.contains(node.getName())) {
          label = node.hasProperty(LABEL_PROPERTY) ? node.getProperty(LABEL_PROPERTY).getString() : node.getName();
          icon = "uiIcon16x16Template" + label.replaceAll(" ", "");
          icon = icon + " uiIconFile";
          if(filter.equals(DOCUMENTS_TEMPLATE_TYPE)) {        		
            if(documentNodeTypes.contains(node.getName()))
              templateData.add(new TemplateData(node.getName(), label, icon));
          } else if(filter.equals(ACTIONS_TEMPLATE_TYPE)) {
            if(ntManager.getNodeType(node.getName()).isNodeType("exo:action")) templateData.add(new TemplateData(node.getName(), 
                                                                                                                 label, icon ));
          } else {
            if(!ntManager.getNodeType(node.getName()).isNodeType("exo:action") && !documentNodeTypes.contains(node.getName())) 
              templateData.add(new TemplateData(node.getName(), label, icon ));
          }          
        }
      }
      Collections.sort(templateData, new TemplateComparator());
    }
    ListAccess<TemplateData> dataList = new ListAccessImpl<TemplateData>(TemplateData.class,
        templateData);
    LazyPageList<TemplateData> pageList = new LazyPageList<TemplateData>(dataList,
        getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setTotalItems(templateData.size());
    getUIPageIterator().setPageList(pageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);

  }
}
