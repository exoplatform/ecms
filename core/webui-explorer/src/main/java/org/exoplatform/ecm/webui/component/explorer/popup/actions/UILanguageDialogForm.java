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

import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.DialogFormActionListeners;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Apr 24, 2007 9:05:25 AM
 */
@ComponentConfigs( {
    @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "WYSIWYGRichTextMultipleInputset", events = {
        @EventConfig(listeners = UIDialogForm.AddActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, events = {
        @EventConfig(listeners = UILanguageDialogForm.SaveActionListener.class),
        @EventConfig(listeners = UILanguageDialogForm.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UILanguageDialogForm.AddActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UILanguageDialogForm.ShowComponentActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UILanguageDialogForm.RemoveReferenceActionListener.class,
                     confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE),
        @EventConfig(listeners = UILanguageDialogForm.RemoveActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = DialogFormActionListeners.RemoveDataActionListener.class,
                     confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE),
        @EventConfig(listeners = DialogFormActionListeners.ChangeTabActionListener.class, phase = Phase.DECODE) }) })
public class UILanguageDialogForm extends UIDialogForm implements UIPopupComponent, UISelectable {

  private boolean isAddNew_ = false;
  private String selectedLanguage_ = null;
  private boolean isDefault_ = false;
  private String documentType_;

  public UILanguageDialogForm() throws Exception {
    setActions(new String[]{"Save", "Cancel"});
  }

  public void doSelect(String selectField, Object value) {
    isUpdateSelect = true;
    UIFormInput formInput = getUIInput(selectField);
    if(formInput instanceof UIFormInputBase) {
      ((UIFormInputBase)formInput).setValue(value.toString());
    }else if(formInput instanceof UIFormMultiValueInputSet) {
      UIFormMultiValueInputSet  inputSet = (UIFormMultiValueInputSet) formInput;
      UIFormInputBase input = inputSet.getChild(inputSet.getChildren().size()-1);
      input.setValue(value.toString());
    }
    UIAddLanguageContainer uiContainer = getParent();
    uiContainer.removeChildById("PopupComponent");
  }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void setTemplateNode(String type) { documentType_ = type;}

  public String getTemplate() {
    repositoryName = getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser();
    try {
      return templateService.getTemplatePathByUser(true, documentType_, userName);
    } catch (Exception e) {
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { documentType_ };
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-support", arg,
                                              ApplicationMessage.ERROR));
      return null;
    }
  }

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public boolean isAddNew() {return isAddNew_;}
  public void addNew(boolean b) {isAddNew_ = b;}

  public void setIsAddNew(boolean isAddNew) { isAddNew_ = isAddNew; }

  public boolean isEditing() { return !isAddNew_; }

  public Node getCurrentNode() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
  }

  public void setSelectedLanguage(String selectedLanguage) { selectedLanguage_ = selectedLanguage; }
  public String getSelectedLanguage() { return selectedLanguage_; }

  public void setIsDefaultLanguage(boolean isDefault) { isDefault_ = isDefault; }
  public boolean isDefaultLanguage() { return isDefault_; }

  private boolean hasNodeTypeNTResource(Node node) throws Exception {
    if(node.hasNodes()) {
      NodeIterator nodeIter = node.getNodes();
      while(nodeIter.hasNext()) {
        Node childNode = nodeIter.nextNode();
        if(childNode.getPrimaryNodeType().getName().equals("nt:resource")) return true;
      }
    }
    return false;
  }

  private String getDMSWorkspace() {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    return dmsConfiguration.getConfig().getSystemWorkspace();
  }

  static  public class SaveActionListener extends EventListener<UILanguageDialogForm> {
    public void execute(Event<UILanguageDialogForm> event) throws Exception {
      UILanguageDialogForm uiForm = event.getSource();
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      Node node = uiExplorer.getCurrentNode();
      String nodeTypeName = node.getPrimaryNodeType().getName();
      uiExplorer.addLockToken(node);
      MultiLanguageService multiLanguageService = uiForm.getApplicationComponent(MultiLanguageService.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if (uiForm.selectedLanguage_ == null) {
        uiApp.addMessage(new ApplicationMessage("UILanguageDialogForm.msg.select-lang", null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      if (!uiExplorer.hasAddPermission()) {
        uiApp.addMessage(new ApplicationMessage("UILanguageDialogForm.msg.access-denied",
                                                null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      if (node.hasNode(Utils.EXO_IMAGE)) {
        Map inputProperties = DialogFormUtil.prepareMap(uiForm.getChildren(),
                                                        uiForm.getInputProperties(), uiForm.getInputOptions());
        try {
          multiLanguageService.addLanguage(node,
                                           inputProperties,
                                           uiForm.getSelectedLanguage(),
                                           uiForm.isDefaultLanguage(),
                                           Utils.EXO_IMAGE);
        } catch (AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UILanguageDialogForm.msg.access-denied",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
      } else if (uiForm.hasNodeTypeNTResource(node)) {
        Map inputProperties = DialogFormUtil.prepareMap(uiForm.getChildren(),
                                                        uiForm.getInputProperties(), uiForm.getInputOptions());
        try {
          multiLanguageService.addFileLanguage(node,
                                               uiForm.getSelectedLanguage(),
                                               inputProperties,
                                               uiForm.isDefaultLanguage());
        } catch (AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UILanguageDialogForm.msg.access-denied",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
      } else if (node.isNodeType(Utils.NT_FOLDER) || node.isNodeType(Utils.NT_UNSTRUCTURED)) {
        Map map = DialogFormUtil.prepareMap(uiForm.getChildren(), uiForm.properties, uiForm.options);
        try {
          multiLanguageService.addFolderLanguage(node,
                                                 map,
                                                 uiForm.getSelectedLanguage(),
                                                 uiForm.isDefaultLanguage(),
                                                 nodeTypeName,
                                                 uiExplorer.getRepositoryName());
        } catch (AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UILanguageDialogForm.msg.access-denied",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
      } else {
        Map map = DialogFormUtil.prepareMap(uiForm.getChildren(), uiForm.properties, uiForm.options);
        try {
          multiLanguageService.addLanguage(node,
                                           map,
                                           uiForm.getSelectedLanguage(),
                                           uiForm.isDefaultLanguage());
        } catch (AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UILanguageDialogForm.msg.access-denied",
                                                  null,
                                                  ApplicationMessage.WARNING));
          
          return;
        }
      }
      node.save();
      UIMultiLanguageManager uiManager = uiForm.getAncestorOfType(UIMultiLanguageManager.class);
      UIMultiLanguageForm uiMultiForm = uiManager.getChild(UIMultiLanguageForm.class);
      uiMultiForm.doSelect(node);
      if(uiForm.isDefaultLanguage()) uiExplorer.setLanguage(uiForm.getSelectedLanguage());
      uiManager.setRenderedChild(UIMultiLanguageForm.class);
      UIAddLanguageContainer uiAddContainer = uiManager.getChild(UIAddLanguageContainer.class);
      UILanguageTypeForm uiLanguageTypeForm = uiAddContainer.getChild(UILanguageTypeForm.class);
      uiLanguageTypeForm.resetLanguage();
      uiAddContainer.removeChild(UILanguageDialogForm.class);
      uiAddContainer.setComponentDisplay(uiForm.documentType_);
      node.getSession().save();
      uiExplorer.setIsHidePopup(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
      uiExplorer.updateAjax(event);
    }
  }

  @SuppressWarnings("unchecked")
  static public class ShowComponentActionListener extends EventListener<UILanguageDialogForm> {
    public void execute(Event<UILanguageDialogForm> event) throws Exception {
      UILanguageDialogForm uiForm = event.getSource();
      UIAddLanguageContainer uiContainer = uiForm.getAncestorOfType(UIAddLanguageContainer.class);
      UIJCRExplorer explorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
      uiForm.isShowingComponent = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      Map fieldPropertiesMap = uiForm.componentSelectors.get(fieldName);
      String classPath = (String)fieldPropertiesMap.get("selectorClass");
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class clazz = Class.forName(classPath, true, cl);
      UIComponent uiComp = uiContainer.createUIComponent(clazz, null, null);
      String rootPath = (String)fieldPropertiesMap.get("rootPath");
      String value = uiForm.getUIStringInput(fieldName).getValue();
      String[] arrayTaxonomy = new String[1];
      if (value != null && !value.equals("")) {
        arrayTaxonomy = value.split(",");
        if (arrayTaxonomy.length > 0) {
          if (arrayTaxonomy[0].startsWith("[")) {
            String taxo0 = arrayTaxonomy[0].substring(1, arrayTaxonomy[0].length());
            arrayTaxonomy[0] = taxo0;
          }
          int len = arrayTaxonomy.length - 1;
          if (arrayTaxonomy[len].endsWith("]")) {
            String taxon = arrayTaxonomy[len].substring(0, arrayTaxonomy[len].length() - 1);
            arrayTaxonomy[len] = taxon;
          }
        }
      }

      if(uiComp instanceof UIOneNodePathSelector) {
        String repositoryName = explorer.getRepositoryName();
        SessionProvider provider = explorer.getSessionProvider();
        String wsFieldName = (String)fieldPropertiesMap.get("workspaceField");
        String wsName = "";
        if(wsFieldName != null && wsFieldName.length() > 0) {
          wsName = (String)uiForm.<UIFormInputBase>getUIInput(wsFieldName).getValue();
          ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, true);
        }
        String selectorParams = (String)fieldPropertiesMap.get("selectorParams");
        if(selectorParams != null) {
          String[] arrParams = selectorParams.split(",");
          if(arrParams.length == 4) {
            ((UIOneNodePathSelector)uiComp).setAcceptedNodeTypesInPathPanel(new String[] {Utils.NT_FILE,
                Utils.NT_FOLDER, Utils.NT_UNSTRUCTURED, Utils.EXO_TAXONOMY});
            wsName = arrParams[1];
            rootPath = arrParams[2];
            ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, true);
            if(arrParams[3].indexOf(";") > -1) {
              ((UIOneNodePathSelector)uiComp).setAcceptedMimeTypes(arrParams[3].split(";"));
            } else {
              ((UIOneNodePathSelector)uiComp).setAcceptedMimeTypes(new String[] {arrParams[3]});
            }
          }
        }
        if(rootPath == null) rootPath = "/";
        ((UIOneNodePathSelector)uiComp).setRootNodeLocation(repositoryName, wsName, rootPath);
        ((UIOneNodePathSelector)uiComp).setShowRootPathSelect(true);
        ((UIOneNodePathSelector)uiComp).init(provider);
      } else if (uiComp instanceof UIOneTaxonomySelector) {
        String workspaceName = uiForm.getDMSWorkspace();
        ((UIOneTaxonomySelector)uiComp).setIsDisable(workspaceName, false);
        String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
        Session session = explorer.getSessionByWorkspace(workspaceName);
        Node rootTree = (Node) session.getItem(rootTreePath);
        NodeIterator childrenIterator = rootTree.getNodes();
        while (childrenIterator.hasNext()) {
          Node childNode = childrenIterator.nextNode();
          rootTreePath = childNode.getPath();
          break;
        }

        ((UIOneTaxonomySelector)uiComp).setRootNodeLocation(uiForm.repositoryName, workspaceName, rootTreePath);
        ((UIOneTaxonomySelector)uiComp).setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
        ((UIOneTaxonomySelector)uiComp).init(WCMCoreUtils.getSystemSessionProvider());

      }
      uiContainer.initPopup(uiComp);
      String param = "returnField=" + fieldName;
      ((ComponentSelector)uiComp).setSourceComponent(uiForm, new String[]{param});
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class RemoveReferenceActionListener extends EventListener<UILanguageDialogForm> {
    public void execute(Event<UILanguageDialogForm> event) throws Exception {
      UILanguageDialogForm uiForm = event.getSource() ;
      uiForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiForm.getUIStringInput(fieldName).setValue(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UILanguageDialogForm> {
    public void execute(Event<UILanguageDialogForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  static public class AddActionListener extends EventListener<UILanguageDialogForm> {
    public void execute(Event<UILanguageDialogForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent());
    }
  }

  static public class RemoveActionListener extends EventListener<UILanguageDialogForm> {
    public void execute(Event<UILanguageDialogForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent());
    }
  }
}
