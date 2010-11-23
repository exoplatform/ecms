/*
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
 */
package org.exoplatform.contentvalidation.webui;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.form.DialogFormActionListeners;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.core.ManageableRepository;
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
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.workflow.webui.component.controller.UITask;
import org.exoplatform.workflow.webui.component.controller.UITaskManager;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 16, 2009  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    events = {
      @EventConfig(listeners = UIDocumentForm.SaveActionListener.class),
      @EventConfig(listeners = UIDocumentForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.RemoveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = DialogFormActionListeners.RemoveDataActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = DialogFormActionListeners.ChangeTabActionListener.class, phase = Phase.DECODE)
    }
)

public class UIDocumentForm extends UIDialogForm implements UIPopupComponent, UISelectable {
  final static public String FIELD_TAXONOMY = "categories";
  final static public String POPUP_TAXONOMY = "PopupComponent";
  final static public String PATH_TAXONOMY = "exoTaxonomiesPath";
  
  private List<String> listTaxonomyName = new ArrayList<String>();
  private String documentType_ ;
  private static final Log LOG  = ExoLogger.getLogger(UIDocumentForm.class);
  
  public UIDocumentForm() throws Exception {
    setActions(new String[]{"Save", "Cancel"}) ;
  }
  
  public List<String> getlistTaxonomyName() {
    return listTaxonomyName;
  }
  
  public void setListTaxonomyName(List<String> listTaxonomyNameNew) {
    listTaxonomyName = listTaxonomyNameNew;
  }

  public void setNodePath(String nodePath) { this.nodePath = nodePath; }
  
  public void setTemplateNode(String type) { documentType_ = type ; }
  
  public void setRepositoryName(String repositoryName){ this.repositoryName = repositoryName; }     
  
  public void setWorkspace(String workspace) { workspaceName = workspace; }  
  
  private String getRepository() throws Exception {
    ManageableRepository manaRepo = (ManageableRepository)getCurrentNode().getSession().getRepository() ;
    return manaRepo.getConfiguration().getName() ;
  }
  
  public String getTemplate() {
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    try {
      return templateService.getTemplatePathByUser(true, documentType_, userName, getRepository()) ;
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
      return null ;
    } 
  }
  
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    try {
      DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
      String wsName = dmsConfiguration.getConfig(getRepository()).getSystemWorkspace();
      return new JCRResourceResolver(getRepository(), wsName, Utils.EXO_TEMPLATEFILE);
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    }
    return super.getTemplateResourceResolver(context, template);
  }
  
  public Node getCurrentNode() throws Exception { return getNode() ; }
  
  public boolean isEditing() { return true ; }
  
  public void activate() throws Exception {
  }
  public void deActivate() throws Exception {
  }
  
  public Node getRootPathTaxonomy(Node node) throws Exception {
    try {
      TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
      List<Node> allTaxonomyTrees = taxonomyService.getAllTaxonomyTrees(this.repositoryName);
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
  
  public void renderField(String name) throws Exception {    
    if (name.equals(FIELD_TAXONOMY)) {
      if (!isUpdateSelect) {      
        UIComponent uiInput = findComponentById(name);
        TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
        List<Node> listCategories = taxonomyService.getAllCategories(getCurrentNode());
        Node taxonomyTree;
        for (Node itemNode : listCategories) {
          taxonomyTree = getRootPathTaxonomy(itemNode);
          if (taxonomyTree == null) continue;
          String categoryPath = itemNode.getPath().replaceAll(taxonomyTree.getPath(), "");
          if (!getListTaxonomy().contains(taxonomyTree.getName() + categoryPath)) {
            listTaxonomyName.add(getCategoryLabel(taxonomyTree.getName() + categoryPath));
            getListTaxonomy().add(taxonomyTree.getName() + categoryPath);
          }
        }
        ((UIFormMultiValueInputSet) uiInput).setValue(listTaxonomyName);
      }
    }
    super.renderField(name);
  }
  
  public void initFieldInput() throws Exception {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    List<Node> listCategories = taxonomyService.getAllCategories(getCurrentNode());
    Node taxonomyTree;
    for (Node itemNode : listCategories) {
      taxonomyTree = getRootPathTaxonomy(itemNode);
      if (taxonomyTree == null) continue;
      String categoryPath = itemNode.getPath().replaceAll(taxonomyTree.getPath(), "");
      if (!getListTaxonomy().contains(taxonomyTree.getName() + categoryPath)) {
        listTaxonomyName.add(getCategoryLabel(taxonomyTree.getName() + categoryPath));
        getListTaxonomy().add(taxonomyTree.getName() + categoryPath);
      }
    }
    UIFormMultiValueInputSet uiFormMultiValue = createUIComponent(UIFormMultiValueInputSet.class, null, null);
    uiFormMultiValue.setId(FIELD_TAXONOMY);
    uiFormMultiValue.setName(FIELD_TAXONOMY);
    uiFormMultiValue.setType(UIFormStringInput.class);
    uiFormMultiValue.setValue(listTaxonomyName);
    uiFormMultiValue.setEditable(false);
    addUIFormInput(uiFormMultiValue);
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
      listTaxonomyName = values;
      inputSet.setValue(values);
    }

    UITaskManager uiContainer = getParent();
    uiContainer.removeChildById(POPUP_TAXONOMY);
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
      UIDocumentForm uiForm = event.getSource();
      List inputs = uiForm.getChildren();
      String repository = uiForm.getRepository();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      int index = 0;
      boolean hasCategories = false;
      String categoriesPath = "";
      TaxonomyService taxonomyService = uiForm.getApplicationComponent(TaxonomyService.class);
      List<String> listTaxonomy = uiForm.getListTaxonomy();
      if (uiForm.isReference) {
        UIFormMultiValueInputSet uiSet = uiForm.getChildById(FIELD_TAXONOMY);
        if((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals(FIELD_TAXONOMY)) {
          hasCategories = true;
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
      Map inputProperties = DialogFormUtil.prepareMap(inputs, uiForm.getInputProperties()) ;
      Node homeNode = uiForm.getNode().getParent() ;      
      try {
        CmsService cmsService = uiForm.getApplicationComponent(CmsService.class) ;
        String addedPath = cmsService.storeNode(uiForm.documentType_, homeNode, inputProperties, false,repository);
        homeNode.getSession().save() ;
        homeNode.save() ;
        Node newNode = (Node)homeNode.getSession().getItem(addedPath);
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
          for(String removedCate : uiForm.getRemovedListCategory(listTaxonomy, listExistingTaxonomy)) {
            index = removedCate.indexOf("/");
            if (index != -1) {
              taxonomyService.removeCategory(newNode, removedCate.substring(0, index), removedCate.substring(index + 1));
            } else {
              taxonomyService.removeCategory(newNode, removedCate, "");
            }
          }
        }
        
        if (hasCategories && (newNode != null) && ((listTaxonomy != null) && (listTaxonomy.size() > 0))){
          uiForm.releaseLock();
          for(String categoryPath : uiForm.getAddedListCategory(listTaxonomy, listExistingTaxonomy)) {
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
        
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent()) ;
      } catch (AccessControlException ace) {
    	  LOG.error("Unexpected error", ace);
    	  throw new AccessDeniedException(ace.getMessage());
      } catch(VersionException ve) {
    	  LOG.error("Unexpected error", ve);
    	  uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.in-versioning", null, 
                                                ApplicationMessage.WARNING)) ;
    	  event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
    	  return;
      } catch(ConstraintViolationException constraintViolationException) {
    	  LOG.error("Unexpected error occurrs", constraintViolationException);
          uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.constraintviolation-exception", null, ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          return;
      } catch(Exception e) {
    	  LOG.error("Unexpected error", e);
    	  String key = "UIDocumentForm.msg.cannot-save" ;
    	  uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING)) ;
    	  event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
    	  return;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UITaskManager uiTaskManager = event.getSource().getParent() ;
      uiTaskManager.setRenderedChild(UITask.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaskManager) ;
    }
  }
  
  static public class AddActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm uiDocumentForm = event.getSource();
      UITaskManager uiTaskManager = uiDocumentForm.getParent();
      String clickedField = event.getRequestContext().getRequestParameter(OBJECTID);
      if (uiDocumentForm.isReference) {
        UIApplication uiApp = uiDocumentForm.getAncestorOfType(UIApplication.class);
        try {        
          UIFormMultiValueInputSet uiSet = uiDocumentForm.getChildById(FIELD_TAXONOMY);
          if((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals(FIELD_TAXONOMY)) {
            if ((clickedField != null) && (clickedField.equals(FIELD_TAXONOMY))) {
              String repository = uiDocumentForm.getRepository();              
              DMSConfiguration dmsConfig = uiDocumentForm.getApplicationComponent(DMSConfiguration.class);
              DMSRepositoryConfiguration dmsRepoConfig = dmsConfig.getConfig(repository);
              String workspaceName = dmsRepoConfig.getSystemWorkspace();            
              if(uiSet.getValue().size() == 0) uiSet.setValue(new ArrayList<Value>());            
              UIOneTaxonomySelector uiOneTaxonomySelector = 
                uiTaskManager.createUIComponent(UIOneTaxonomySelector.class, null, null);
              uiOneTaxonomySelector.setIsDisable(workspaceName, false);
              TaxonomyService taxonomyService = uiDocumentForm.getApplicationComponent(TaxonomyService.class);
              List<Node> lstTaxonomyTree = taxonomyService.getAllTaxonomyTrees(repository);
              if (lstTaxonomyTree.size() == 0) throw new AccessDeniedException();
              uiOneTaxonomySelector.setRootNodeLocation(repository, workspaceName, lstTaxonomyTree.get(0).getPath());
              uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
              uiOneTaxonomySelector.init(SessionProviderFactory.createSystemProvider());
              String param = "returnField=" + FIELD_TAXONOMY;
              uiOneTaxonomySelector.setSourceComponent(uiDocumentForm, new String[]{param});
              UIPopupWindow uiPopupWindow = uiTaskManager.getChildById(POPUP_TAXONOMY);
              if (uiPopupWindow == null) {
                uiPopupWindow = uiTaskManager.addChild(UIPopupWindow.class, null, POPUP_TAXONOMY);
              }
              uiPopupWindow.setWindowSize(700, 450);
              uiPopupWindow.setUIComponent(uiOneTaxonomySelector);
              uiPopupWindow.setRendered(true);
              uiPopupWindow.setShow(true);
            }
          } 
          event.getRequestContext().addUIComponentToUpdateByAjax(uiTaskManager);
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
