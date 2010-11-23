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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.action.EditDocumentActionComponent;
import org.exoplatform.ecm.webui.form.DialogFormActionListeners;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 * Editor : Pham Tuan
 *        phamtuanchip@yahoo.de
 * Nov 08, 2006  
 */

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  events = {
    @EventConfig(listeners = UIDocumentForm.SaveActionListener.class),
    @EventConfig(listeners = UIDocumentForm.CloseActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIDocumentForm.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIDocumentForm.RemoveActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIDocumentForm.ShowComponentActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIDocumentForm.RemoveReferenceActionListener.class, confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE),
    @EventConfig(listeners = DialogFormActionListeners.RemoveDataActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = DialogFormActionListeners.ChangeTabActionListener.class, phase = Phase.DECODE)
  }
)

public class UIDocumentForm extends UIDialogForm implements UIPopupComponent, UISelectable {   
  
  final static public String FIELD_TAXONOMY = "categories";
  final static public String POPUP_TAXONOMY = "PopupComponent";
  final static public String PATH_TAXONOMY = "exoTaxonomiesPath";
  
  private List<String> listTaxonomyName = new ArrayList<String>();
  
  private static final Log LOG  = ExoLogger.getLogger(UIDocumentForm.class);
  
  public UIDocumentForm() throws Exception {
    setActions(new String[]{"Save", "Close"});  
  }
  
  public List<String> getlistTaxonomyName() {
    return listTaxonomyName;
  }
  
  public void setListTaxonomyName(List<String> listTaxonomyNameNew) {
    listTaxonomyName = listTaxonomyNameNew;
  }
  
  public void releaseLock() throws Exception {
    if (isEditing()) {
      super.releaseLock();
    }
  }
  
  public String getDMSWorkspace() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    String repository = uiExplorer.getRepositoryName();
    DMSConfiguration dmsConfig = getApplicationComponent(DMSConfiguration.class);
    return dmsConfig.getConfig(repository).getSystemWorkspace();    
  }
  
  public Node getRootPathTaxonomy(Node node) throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    try {
      TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
      List<Node> allTaxonomyTrees = taxonomyService.getAllTaxonomyTrees(uiExplorer.getRepositoryName());
      for (Node taxonomyTree : allTaxonomyTrees) {
        if (node.getPath().startsWith(taxonomyTree.getPath())) return taxonomyTree;
      }
      return null;
    } catch (AccessDeniedException accessDeniedException) {
      return null;
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { contentType };
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-support", arg, 
          ApplicationMessage.ERROR));
      return null;
    }
  }
    
  @SuppressWarnings("unchecked")
  public void doSelect(String selectField, Object value) throws Exception {
    isUpdateSelect = true;    
    UIFormInput formInput = getUIInput(selectField);
    if(formInput instanceof UIFormInputBase) {
      ((UIFormInputBase)formInput).setValue(value.toString());
    }else if(formInput instanceof UIFormMultiValueInputSet) {
      UIFormMultiValueInputSet  inputSet = (UIFormMultiValueInputSet) formInput;            
      String valueTaxonomy = String.valueOf(value).trim();
      List<String> values = (List<String>) inputSet.getValue();
      if (!getListTaxonomy().contains(valueTaxonomy)) {
        getListTaxonomy().add(valueTaxonomy);
        values.add(getCategoryLabel(valueTaxonomy));
      }
      inputSet.setValue(values);
    }

    UIDocumentFormController uiContainer = getParent();
    uiContainer.removeChildById(POPUP_TAXONOMY);
  }
  
  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    String userName = Util.getPortalRequestContext().getRemoteUser();
    try {      
      return templateService.getTemplatePathByUser(true, contentType, userName, repositoryName);
    } catch (AccessControlException e) {
      LOG.error("Unexpected error", e);
      return null;
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { contentType };
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-support", arg, 
          ApplicationMessage.ERROR));
      return null;
    } 
  }

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public Node getCurrentNode() throws Exception { 
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode(); 
  }
  
  public String getLastModifiedDate() throws Exception {
    return getLastModifiedDate(getCurrentNode());
  }
  
  public void renderField(String name) throws Exception {
    if (FIELD_TAXONOMY.equals(name)) {
      if (!isAddNew && !isUpdateSelect) {    
        TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
        List<Node> listCategories = taxonomyService.getAllCategories(getNode());
        Node taxonomyTree;
        for (Node itemNode : listCategories) {
          taxonomyTree = getRootPathTaxonomy(itemNode);
          if (taxonomyTree == null) continue;
          String categoryPath = itemNode.getPath().replaceAll(taxonomyTree.getPath(), "");
          if (!getListTaxonomy().contains(taxonomyTree.getName() + categoryPath)) {
            if (!listTaxonomyName.contains(getCategoryLabel(taxonomyTree.getName() + categoryPath)))
              listTaxonomyName.add(getCategoryLabel(taxonomyTree.getName() + categoryPath));
            getListTaxonomy().add(taxonomyTree.getName() + categoryPath);
          }
        }
        UIFormMultiValueInputSet uiSet = getChildById(FIELD_TAXONOMY);
        if (uiSet != null) uiSet.setValue(listTaxonomyName);
      } 
    }    
    super.renderField(name);
  }
  
  private List<String> getAddedListCategory(List<String> taxonomyList, List<String> existingList) {
    List<String> addedList = new ArrayList<String>();
    for(String addedCategory : taxonomyList) {
      if(!existingList.contains(addedCategory)) addedList.add(addedCategory);
    }
    return addedList;
  }
  
  private List<String> getRemovedListCategory(List<String> taxonomyList, List<String> existingList) {
    List<String> removedList = new ArrayList<String>();
    for(String existedCategory : existingList) {
      if(!taxonomyList.contains(existedCategory)) removedList.add(existedCategory);
    }
    return removedList;
  }
  
  static  public class SaveActionListener extends EventListener<UIDocumentForm> {
    @SuppressWarnings("unchecked")
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm documentForm = event.getSource();
      UIJCRExplorer uiExplorer = documentForm.getAncestorOfType(UIJCRExplorer.class);
      List inputs = documentForm.getChildren();
      UIApplication uiApp = documentForm.getAncestorOfType(UIApplication.class);
      boolean hasCategories = false;
      String categoriesPath = "";
      String repository = uiExplorer.getRepositoryName();
      TaxonomyService taxonomyService = documentForm.getApplicationComponent(TaxonomyService.class);
      if (documentForm.isAddNew()) {
        for (int i = 0; i < inputs.size(); i++) {
          UIFormInput input = (UIFormInput) inputs.get(i);
          if ((input.getName() != null) && input.getName().equals("name")) {
            String[] arrFilterChar = {"]", "["};
            String valueName = input.getValue().toString();
            if (!Utils.isNameValid(valueName, arrFilterChar)) {
              uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-not-allowed", null,
                  ApplicationMessage.WARNING));
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
              return;
            }
          }
        }
      }
      
      int index = 0;
      List<String> listTaxonomy = documentForm.getListTaxonomy();
      if (documentForm.isReference) {
        UIFormMultiValueInputSet uiSet = documentForm.getChildById(FIELD_TAXONOMY);
        if((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals(FIELD_TAXONOMY)) {
          hasCategories = true;
          listTaxonomy = (List<String>) uiSet.getValue();
          for (String category : listTaxonomy) {
            categoriesPath.concat(category).concat(",");
          }
          
          if (listTaxonomy != null && listTaxonomy.size() > 0) {
            try {
              for (String categoryPath : listTaxonomy) {
                index = categoryPath.indexOf("/");
                if (index < 0) {
                  taxonomyService.getTaxonomyTree(repository, categoryPath);
                } else {
                  taxonomyService.getTaxonomyTree(repository, categoryPath.substring(0, index)).getNode(categoryPath.substring(index + 1));
                }
              }
            } catch (Exception e) {
              LOG.error("Unexpected error occurs", e);
              uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null, ApplicationMessage.WARNING));
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
              return;
            }
          }
        }
      }     
      Map inputProperties = DialogFormUtil.prepareMap(inputs, documentForm.getInputProperties());
      Node newNode = null;
      String nodeType;
      Node homeNode;
      Node currentNode = uiExplorer.getCurrentNode();
      if(documentForm.isAddNew()) {
        UIDocumentFormController uiDFController = documentForm.getParent();
        homeNode = currentNode;
        UISelectDocumentForm uiSelectDocumentForm = uiDFController.getChild(UISelectDocumentForm.class);
        if (uiSelectDocumentForm != null) {
          nodeType = uiSelectDocumentForm.getSelectValue();                           // Exist select box, get selected value
        } else {
          nodeType = uiDFController.getChild(UIDocumentForm.class).getContentType();  // Not exist select box, get default value
        }
        if(homeNode.isLocked()) {
          homeNode.getSession().addLockToken(LockUtil.getLockToken(homeNode));
        }
      } else { 
        Node documentNode = documentForm.getNode();
        homeNode = documentNode.getParent();
        nodeType = documentNode.getPrimaryNodeType().getName();
        if(documentNode.isLocked()) {
          documentNode.getSession().addLockToken(LockUtil.getLockToken(documentNode));
        }
      }       
      try {
        CmsService cmsService = documentForm.getApplicationComponent(CmsService.class);
        String addedPath = cmsService.storeNode(nodeType, homeNode, inputProperties, documentForm.isAddNew(),documentForm.repositoryName);
        try {
          newNode = (Node)homeNode.getSession().getItem(addedPath);
          if(newNode.isLocked()) {
            newNode.getSession().addLockToken(LockUtil.getLockToken(newNode));
          }
          List<Node> listTaxonomyTrees = taxonomyService.getAllTaxonomyTrees(repository);
          List<Node> listExistedTaxonomy = taxonomyService.getAllCategories(newNode);
          List<String> listExistingTaxonomy = new ArrayList<String>();
                    
          for (Node existedTaxonomy : listExistedTaxonomy) {
            for (Node taxonomyTrees : listTaxonomyTrees) {
              if(existedTaxonomy.getPath().contains(taxonomyTrees.getPath())) {
                listExistingTaxonomy.add(taxonomyTrees.getName() + existedTaxonomy.getPath().substring(taxonomyTrees.getPath().length()));
                break;
              }
            }
          }
          if (hasCategories && !homeNode.isNodeType("exo:taxonomy")) {
            for(String removedCate : documentForm.getRemovedListCategory(listTaxonomy, listExistingTaxonomy)) {
              index = removedCate.indexOf("/");
              if (index != -1) {
                taxonomyService.removeCategory(newNode, removedCate.substring(0, index), removedCate.substring(index + 1));
              } else {
                taxonomyService.removeCategory(newNode, removedCate, "");
              }
            }
          }
          if (hasCategories && (newNode != null) && ((listTaxonomy != null) && (listTaxonomy.size() > 0))){
            documentForm.releaseLock();
            for(String categoryPath : documentForm.getAddedListCategory(listTaxonomy, listExistingTaxonomy)) {
              index = categoryPath.indexOf("/");
              try {
                if (index != -1) {
                  taxonomyService.addCategory(newNode, categoryPath.substring(0, index), categoryPath.substring(index + 1));
                } else {
                  taxonomyService.addCategory(newNode, categoryPath, "");
                }
              } catch(AccessDeniedException accessDeniedException) {
                uiApp.addMessage(new ApplicationMessage("AccessControlException.msg", null, 
                    ApplicationMessage.WARNING));
                event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
              } catch (Exception e) {
                continue;
              }
            }
          } else {
            List<Value> vals = new ArrayList<Value>();
            if (newNode.hasProperty("exo:category")) newNode.setProperty("exo:category", vals.toArray(new Value[vals.size()]));
            newNode.save();
          }
        } catch(Exception e) {
          if(!uiExplorer.getPreference().isJcrEnable()) uiExplorer.getSession().save();
          uiExplorer.updateAjax(event);          
        }
        if(!uiExplorer.getPreference().isJcrEnable()) uiExplorer.getSession().save();
        uiExplorer.updateAjax(event);        
      } catch (AccessControlException ace) {
        throw new AccessDeniedException(ace.getMessage());
      } catch(VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.in-versioning", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(ItemNotFoundException item) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.item-not-found", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.repository-exception-permission", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(ItemExistsException existedex) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-allowed-same-name-sibling", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(ConstraintViolationException constraintViolationException) {
      LOG.error("Unexpected error occurrs", constraintViolationException);
        uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.constraintviolation-exception", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(RepositoryException repo) {
      LOG.error("Unexpected error occurrs", repo);
        uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.repository-exception", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(NumberFormatException nume) {
        String key = "UIDocumentForm.msg.numberformat-exception";
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(Exception e) {
        LOG.error("Unexpected error occurs", e);
        String key = "UIDocumentForm.msg.cannot-save";
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } finally {
         documentForm.releaseLock();
      }
      event.getRequestContext().setAttribute("nodePath",newNode.getPath());
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
//      uiExplorer.setCurrentPath(uiExplorer.getPathBeforeEditing());
      documentForm.setIsUpdateSelect(false);
      uiExplorer.setCurrentPath(newNode.getPath());      
      uiExplorer.refreshExplorer();
      uiExplorer.updateAjax(event);
      EditDocumentActionComponent.editDocument(event, null, uiExplorer, uiExplorer, uiExplorer.getCurrentNode(), uiApp);      
    }
  }
  
  @SuppressWarnings("unchecked")
  static public class ShowComponentActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm uiForm = event.getSource();
      UIDocumentFormController uiContainer = uiForm.getParent();
      uiForm.isShowingComponent = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      Map fieldPropertiesMap = uiForm.componentSelectors.get(fieldName);
      
      // get Param = fieldPropertiesMap.get("selectorParams");
      // Param = Param.split("'");
      String classPath = (String)fieldPropertiesMap.get("selectorClass");
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class clazz = Class.forName(classPath, true, cl);
      String rootPath = (String)fieldPropertiesMap.get("rootPath");
      UIComponent uiComp = uiContainer.createUIComponent(clazz, null, null);
      String selectorParams = (String)fieldPropertiesMap.get("selectorParams");
      UIJCRExplorer explorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      if(uiComp instanceof UIOneNodePathSelector) {
        String repositoryName = explorer.getRepositoryName();
        SessionProvider provider = explorer.getSessionProvider();                
        String wsFieldName = (String)fieldPropertiesMap.get("workspaceField");
        String wsName = "";
        if(wsFieldName != null && wsFieldName.length() > 0) {
          if (uiForm.<UIFormInputBase>getUIInput(wsFieldName) != null) {
            wsName = (String)uiForm.<UIFormInputBase>getUIInput(wsFieldName).getValue();
            ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, true);
          } else {
            wsName = explorer.getCurrentWorkspace();            
            ((UIOneNodePathSelector)uiComp).setIsDisable(wsName, false);
          }                
        }
        if(selectorParams != null) {
          String[] arrParams = selectorParams.split(",");
          if(arrParams.length == 4) {
            ((UIOneNodePathSelector)uiComp).setAcceptedNodeTypesInPathPanel(new String[] {Utils.NT_FILE, 
                Utils.NT_FOLDER, Utils.NT_UNSTRUCTURED, Utils.EXO_TAXANOMY});
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
        NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
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
        ((UIOneTaxonomySelector)uiComp).init(SessionProviderFactory.createSystemProvider());
      }
      uiContainer.initPopup(uiComp);
      String param = "returnField=" + fieldName;
      String[] params = selectorParams == null ? new String[]{param} : new String[]{param, "selectorParams=" + selectorParams};
      ((ComponentSelector)uiComp).setSourceComponent(uiForm, params);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }  

  static public class RemoveReferenceActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm uiForm = event.getSource();
      uiForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      uiForm.getUIStringInput(fieldName).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }

  static  public class CloseActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
    	event.getSource().releaseLock();      
//      uiExplorer.setCurrentPath(uiExplorer.getPathBeforeEditing());
      if (uiDocumentWorkspace.getChild(UIDocumentFormController.class) != null) {
        uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
      } else    
      uiExplorer.cancelAction();
      uiExplorer.updateAjax(event);
    }
  }
    
  static  public class AddActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {            
      UIDocumentForm uiDocumentForm = event.getSource();
      UIDocumentFormController uiFormController = uiDocumentForm.getParent();
      String clickedField = event.getRequestContext().getRequestParameter(OBJECTID);
      if (uiDocumentForm.isReference) {
        uiDocumentForm.setIsUpdateSelect(true);
        UIApplication uiApp = uiDocumentForm.getAncestorOfType(UIApplication.class);
        try {        
          UIFormMultiValueInputSet uiSet = uiDocumentForm.getChildById(FIELD_TAXONOMY);
          if((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals(FIELD_TAXONOMY)) {
            if ((clickedField != null) && (clickedField.equals(FIELD_TAXONOMY))){
              UIJCRExplorer uiExplorer = uiDocumentForm.getAncestorOfType(UIJCRExplorer.class);
              String repository = uiExplorer.getRepositoryName();
              DMSConfiguration dmsConfig = uiDocumentForm.getApplicationComponent(DMSConfiguration.class);
              DMSRepositoryConfiguration dmsRepoConfig = dmsConfig.getConfig(repository);
              String workspaceName = dmsRepoConfig.getSystemWorkspace();            
              if(uiSet.getValue().size() == 0) uiSet.setValue(new ArrayList<Value>());            
              UIOneTaxonomySelector uiOneTaxonomySelector = 
                uiFormController.createUIComponent(UIOneTaxonomySelector.class, null, null);
              uiOneTaxonomySelector.setIsDisable(workspaceName, false);
              TaxonomyService taxonomyService = uiDocumentForm.getApplicationComponent(TaxonomyService.class);
              List<Node> lstTaxonomyTree = taxonomyService.getAllTaxonomyTrees(repository);
              if (lstTaxonomyTree.size() == 0) throw new AccessDeniedException();
              uiOneTaxonomySelector.setRootNodeLocation(repository, workspaceName, lstTaxonomyTree.get(0).getPath());
              uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
              uiOneTaxonomySelector.init(uiExplorer.getSystemProvider());
              String param = "returnField=" + FIELD_TAXONOMY;
              uiOneTaxonomySelector.setSourceComponent(uiDocumentForm, new String[]{param});
              UIPopupWindow uiPopupWindow = uiFormController.getChildById(POPUP_TAXONOMY);
              if (uiPopupWindow == null) {
                uiPopupWindow = uiFormController.addChild(UIPopupWindow.class, null, POPUP_TAXONOMY);
              }
              uiPopupWindow.setWindowSize(700, 450);
              uiPopupWindow.setUIComponent(uiOneTaxonomySelector);
              uiPopupWindow.setRendered(true);
              uiPopupWindow.setShow(true);
            }
          } 
          event.getRequestContext().addUIComponentToUpdateByAjax(uiFormController);
        } catch (AccessDeniedException accessDeniedException) {
          uiApp.addMessage(new ApplicationMessage("Taxonomy.msg.AccessDeniedException", null, 
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          return;
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          return;
        }
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm.getParent());
      }
    }
  }
  
  static public class RemoveActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm uiDocumentForm = event.getSource();
      String objectid = event.getRequestContext().getRequestParameter(OBJECTID);
      String idx = objectid.replaceAll(FIELD_TAXONOMY,"");
      try {
        int idxInput = Integer.parseInt(idx);
        uiDocumentForm.getListTaxonomy().remove(idxInput);
        uiDocumentForm.getlistTaxonomyName().remove(idxInput);        
        uiDocumentForm.setIsUpdateSelect(true);        
      } catch (NumberFormatException ne) {
      } catch (Exception e) {
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
    }
  }  
}
