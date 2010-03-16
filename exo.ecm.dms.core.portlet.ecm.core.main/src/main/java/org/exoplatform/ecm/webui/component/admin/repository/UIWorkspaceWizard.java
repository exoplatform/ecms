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
package org.exoplatform.ecm.webui.component.admin.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.repository.UIRepositoryValueSelect.ClassData;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.LockPersisterEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.util.StringNumberParser;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * May 11, 2007  
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/admin/UIWorkspaceWizard.gtmpl",    
    events = {
      @EventConfig(listeners = UIWorkspaceWizard.AddPermissionActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.SelectContainerActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.SelectStoreActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.SelectQueryHandlerActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.FinishActionListener.class),
      @EventConfig(listeners = UIWorkspaceWizard.NextActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.BackActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.ViewStep1ActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.ViewStep2ActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.ViewStep3ActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.CancelActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.EditPermissionActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.RemovePermissionActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIWorkspaceWizard.ChangeTypeStoreActionListener.class, phase=Phase.DECODE)
    }

)
public class UIWorkspaceWizard extends UIFormTabPane implements UISelectable {
  private int wizardMaxStep_ = 3 ;
  private int selectedStep_ = 1 ;
  private int currentStep_ = 0 ;
  private String selectedWsName_ = null ;
  private Map<Integer, String> chidrenMap_ = new HashMap<Integer, String>() ; 
  public boolean isNewWizard_ = true ;
  public boolean isNewRepo_ = true ;
  public boolean isCheckValid_ = true ;
  public boolean hasCheckDefault_ = false ;
  private Map<Integer, String[]> actionMap_ = new HashMap<Integer, String[]>() ;
  final static public String POPUPID = "UIPopupWindowInWizard" ;
  final static public String FIELD_STEP1 = "step1" ;
  final static public String FIELD_STEP2 = "step2" ;
  final static public String FIELD_STEP3 = "step3" ;
  final static public String KEY_SWAPDIRECTORY = "swap-directory" ;
  final static public String KEY_SOURCENAME = "source-name" ;
  final static public String KEY_DIALECT = "dialect" ;
  final static public String KEY_MULTIDB = "multi-db" ;
  final static public String KEY_MAXBUFFER = "max-buffer-size" ;
  final static public String KEY_PATH = "path"  ;
  final static public String KEY_INDEXDIR = "index-dir" ;
  final static public String KEY_MAXSIZE = "max-size" ;
  final static public String KEY_LIVETIME = "live-time" ;
  final static public String KEY_UPDATESTORE = "update-storage";
  final static public String KEY_CONTAINERTYPE = "org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer" ;
  final static public String KEY_STORETYPE = "org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage" ;
  final static public String KEY_TREE_STORETYPE = "org.exoplatform.services.jcr.impl.storage.value.fs.TreeFileValueStorage" ;
  final static public String KEY_QUERYHANDLER = "org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex" ;
  final static public String KEY_LOCKMANAGER = "org.exoplatform.services.jcr.impl.core.lock.FileSystemLockPersister";
  private static final Log LOG  = ExoLogger.getLogger("admin.UIWorkspaceWizard");
  public UIWorkspaceWizard() throws Exception {
    super("UIWorkspaceWizard");
    chidrenMap_.put(1, FIELD_STEP1) ;
    chidrenMap_.put(2, FIELD_STEP2) ;
    chidrenMap_.put(3, FIELD_STEP3) ;
    actionMap_.put(1, new String[]{"Next", "Cancel"}) ;
    actionMap_.put(2, new String[]{"Back", "Next", "Cancel"}) ;
    actionMap_.put(3, new String[]{"Back", "Finish", "Cancel"}) ;
    UIWizardStep1 step1 = new UIWizardStep1(FIELD_STEP1) ;
    UIFormInputSetWithAction step2 = new UIWizardStep2(FIELD_STEP2) ;
    UIFormInputSetWithAction step3 = new UIWizardStep3(FIELD_STEP3) ;
    addUIComponentInput(step1) ;
    addUIComponentInput(step2) ;
    addUIComponentInput(step3) ;
    setRenderedChild(getCurrentChild()) ;
  }
  protected void removePopup(String id) {
    getAncestorOfType(UIWorkspaceWizardContainer.class).removePopup(id) ;
  }
  protected void lockForm(boolean isLock) {
    UIWizardStep1 wsStep1 = getChildById(UIWorkspaceWizard.FIELD_STEP1) ;
    wsStep1.lockFields(isLock) ;
    UIWizardStep2 wsStep2 = getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
    wsStep2.lockFields(isLock) ;
    UIWizardStep3 wsStep3 = getChildById(UIWorkspaceWizard.FIELD_STEP3) ;
    wsStep3.lockFields(isLock) ;
  }
  public void setCurrentSep(int step){ currentStep_ = step ;}
  public int getCurrentStep() { return currentStep_; }
  public void setSelectedStep(int step){ selectedStep_ = step ;}
  public int getSelectedStep() { return selectedStep_; }
  public int getMaxStep(){return wizardMaxStep_ ;}
  public String[] getActions(){return actionMap_.get(selectedStep_) ;}
  public String getCurrentChild() {return chidrenMap_.get(selectedStep_) ;}
  public String[] getCurrentAction() {return actionMap_.get(selectedStep_) ;}

  @SuppressWarnings("unchecked")
  protected void refresh(WorkspaceEntry workSpace, boolean isAddNewWs) throws Exception{
    reset() ;
    UIWizardStep1 uiWSFormStep1 = getChildById(FIELD_STEP1) ;
    UIWizardStep2 uiWSFormStep2 = getChildById(FIELD_STEP2) ;
    UIWizardStep3 uiWSFormStep3 = getChildById(FIELD_STEP3) ;
    uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).setChecked(false) ;
    uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).setChecked(false) ;
    UIRepositoryForm uiRepoForm = getAncestorOfType(UIECMAdminPortlet.class).findFirstComponentOfType(UIRepositoryForm.class) ;
    String repoName = uiRepoForm.getUIStringInput(UIRepositoryForm.FIELD_NAME).getValue() ;
    String name = "" ;
    boolean isDefaultWS  = false;
    boolean isDMSSystem  = false;
    String  lockTime = "0";
    String dbType = null ;
    String selectedNodeType = null ;
    String permission = null ;
    String containerType = "" ;
    String swapPath = "" ;
    String sourceName = "" ;
    boolean isMutil = false ;
    String storeType = "" ;    
    String storePath = "" ;
    String filterType = "" ;
    String queryHandlerType = ""  ;
    String indexDir ="" ;
    String maxBuffer = "204800" ;
    boolean isEnableCache = true ;
    String maxCache ="5000" ;
    String liveTime ="30000" ;
    
    if (workSpace != null) {
      RepositoryService rService = (RepositoryService)getApplicationComponent(ExoContainer.class).
      getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageRepository;
      if (isAddNewWs) {
        manageRepository = rService.getDefaultRepository();
      } else {
        try {
          manageRepository = rService.getRepository(repoName);
        } catch (RepositoryException e) {
          manageRepository = rService.getDefaultRepository();
        }
      }
      if (!isNewWizard_) {
        name = workSpace.getName();
        isDefaultWS = uiRepoForm.isDefaultWorkspace(name);
        isDMSSystem = uiRepoForm.isDmsSystemWorkspace(name);
        try {
          Session workspaceSession = manageRepository.getSystemSession(workSpace.getName());
          selectedNodeType = workspaceSession.getRootNode().getPrimaryNodeType().getName();
          List<AccessControlEntry> listEntry = ((ExtendedNode)workspaceSession.getRootNode()).getACL().getPermissionEntries();
          workspaceSession.logout();
          Iterator perIter = listEntry.iterator() ;
          StringBuilder userPermission = new StringBuilder();
          while (perIter.hasNext()) {
            AccessControlEntry accessControlEntry = (AccessControlEntry)perIter.next();
            userPermission.append(accessControlEntry.getIdentity());
            userPermission.append(" ");
            userPermission.append(accessControlEntry.getPermission() + ";");
          }
          if (!isAddNewWs) { permission = userPermission.toString(); } 
        } catch (RepositoryException e) {
          selectedNodeType = uiRepoForm.getWorkspaceMapNodeType(repoName);
          if (!isAddNewWs) { permission = uiRepoForm.getWorkspaceMapPermission(repoName); } 
          //manageRepository = rService.getRepository(repoName);
          //selectedNodeType = manageRepository.getSystemSession(workSpace.getName()).getRootNode().getPrimaryNodeType().getName();
        }
      }
      if (permission == null) permission = uiRepoForm.getWorkspaceMapPermission(name);
      if(workSpace.getLockManager() != null) {
        lockTime  = String.valueOf(workSpace.getLockManager().getTimeout()) + "ms"; 
      }
      ContainerEntry container = workSpace.getContainer() ;
      if(container != null) {
        containerType = container.getType() ;
        swapPath = container.getParameterValue(KEY_SWAPDIRECTORY) ;
        sourceName = container.getParameterValue(KEY_SOURCENAME) ;
        dbType = container.getParameterValue(KEY_DIALECT) ;
        isMutil = Boolean.parseBoolean(container.getParameterValue(KEY_MULTIDB)) ;
        maxBuffer = container.getParameterValue(KEY_MAXBUFFER) ;
      }
      ArrayList<ValueStorageEntry> valueStore = container.getValueStorages() ;
      if(valueStore != null && valueStore.size() > 0) {
        storeType = valueStore.get(0).getType() ;    
        storePath = valueStore.get(0).getParameterValue(KEY_PATH) ;
        filterType = valueStore.get(0).getFilters().get(0).getPropertyType() ;
      }
      QueryHandlerEntry queryHandler = workSpace.getQueryHandler() ;
      if(queryHandler != null) {
        queryHandlerType = queryHandler.getType() ;
        indexDir = queryHandler.getParameterValue(KEY_INDEXDIR) ;
      }
      CacheEntry cache  = workSpace.getCache() ;
      if(cache != null) {
        isEnableCache = cache.isEnabled() ;
        maxCache =  cache.getParameterValue(KEY_MAXSIZE)  ;
        liveTime = cache.getParameterValue(KEY_LIVETIME) ;
      }
      uiWSFormStep1.fillFields(name, selectedNodeType, isDefaultWS, isDMSSystem, permission, lockTime);
      uiWSFormStep2.fillFields(containerType, sourceName, dbType, isMutil, storeType, storePath, 
          filterType, maxBuffer, swapPath) ;
      uiWSFormStep3.fillFields(queryHandlerType, indexDir, isEnableCache, maxCache, liveTime) ;

      if(isNewWizard_) { 
        StringBuilder sb1 = new StringBuilder() ;
        StringBuilder sb2 = new StringBuilder() ;
        if(isNewRepo_) {
          uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).setEditable(true) ;
          uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).setEditable(true) ;
          sb1.append(swapPath.substring(0, swapPath.lastIndexOf("/")+1)).append(repoName).append("/") ;
          sb2.append(storePath.substring(0, storePath.lastIndexOf("/")+1)).append(repoName).append("/") ;
        } else {
          sb1.append(swapPath.substring(0, swapPath.lastIndexOf("/")+1));
          sb2.append(storePath.substring(0, storePath.lastIndexOf("/")+1));
        }
        uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).setValue(sb1.toString()) ;
        uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).setValue(sb2.toString()) ;
        uiWSFormStep2.getUIFormSelectBox(UIWizardStep2.FIELD_FILTER).setValue(filterType) ;
      } 
    }
    if(isNewRepo_) {
      lockForm(false);
      if (isAddNewWs) {
        if (uiRepoForm.defaulWorkspace_ == null) 
          uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).setEnable(true);
        else
          uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).setEnable(false);
        if (uiRepoForm.dmsSystemWorkspace_ == null)
          uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).setEnable(true);
        else 
          uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).setEnable(false);
      }
    } else {
      if(isNewWizard_) {
        lockForm(false) ;
        uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).setEnable(false) ;
        uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).setEnable(false) ;
      } else {
        lockForm(true) ;
      }
    } 
    if( isNewWizard_) {
      isCheckValid_ = true ;
    } else {
      if( isNewRepo_) {
        isCheckValid_ = true ;
        if(workSpace != null) selectedWsName_ = workSpace.getName() ;
      } else {
        isCheckValid_ = false ;
      }
    }
  }

  private void setPermissionToRoot(ExtendedNode rootNode, String stringPermission) throws Exception {
    HashMap<String, List<String>> permission = new HashMap<String, List<String>>();
    String [] items = stringPermission.split(";");
    for (String item : items) {
      String[] permissionType = item.split(" ");
      if (permission.containsKey(permissionType[0])) {
        List<String> type = permission.get(permissionType[0]);
        if (!type.contains(permissionType[1])) {
          type.add(permissionType[1]);
          permission.put(permissionType[0], type);
        }
      } else {
        List<String> type = new ArrayList<String>();
        type.add(permissionType[1]);
        permission.put(permissionType[0], type);
      }
    }
    
    Iterator iter = permission.keySet().iterator();
    List<String> listKey = new ArrayList<String>();
    List<String> listType = new ArrayList<String>();
    while (iter.hasNext()) {
      String key = (String)iter.next();
      listKey.add(key);
      List<String> types = permission.get(key);
      List<String> listPermission = new ArrayList<String>();      
      if (key.equals("*")) key = "any";
      for (String type : types) {
        if ((key.equals("any") && !listType.contains(type))) listType.add(type);  
        if (type.equals("read")) listPermission.add(PermissionType.READ);
        else if (type.equals("add_node")) listPermission.add(PermissionType.ADD_NODE);
        else if (type.equals("set_property")) listPermission.add(PermissionType.SET_PROPERTY);
        else listPermission.add(PermissionType.REMOVE);
      }
      String[] criteria = new String[listPermission.size()];
      rootNode.setPermission(key, listPermission.toArray(criteria));      
    }
    if (!listKey.contains("any")) {
      if (!listType.contains(PermissionType.ADD_NODE)) rootNode.removePermission("any", PermissionType.ADD_NODE);
      if (!listType.contains(PermissionType.SET_PROPERTY)) rootNode.removePermission("any", PermissionType.SET_PROPERTY);
      if (!listType.contains(PermissionType.REMOVE)) rootNode.removePermission("any", PermissionType.REMOVE);
    }
    rootNode.save();
  }
  
  public String url(String name) throws Exception {
    UIComponent renderedChild = getChild(currentStep_);
    if(!(renderedChild instanceof UIForm)) return super.event(name);
    org.exoplatform.webui.config.Event event = config.getUIComponentEventConfig(name) ;
    if(event == null) return "??config??" ;
    UIForm uiForm = (UIForm) renderedChild;
    return uiForm.event(name);
  }

  public int getNumberSteps() {return wizardMaxStep_ ;}

  public void viewStep(int step) {   
    selectedStep_ = step ;
    currentStep_ = step - 1 ;    
    List<UIComponent> children = getChildren(); 
    for(int i=0; i<children.size(); i++){
      if(i == getCurrentStep()) {
        children.get(i).setRendered(true);
      } else {
        children.get(i).setRendered(false);
      }
    }
  }
  protected boolean isEmpty(String value) {
    return (value == null) || (value.trim().length() == 0) ;
  }

  protected String autoInitStorePath(String storePath, String repoName, String wsName) {
    StringBuilder sb  = new StringBuilder() ;
    sb.append(storePath.substring(0, storePath.lastIndexOf("/")+1)).append(repoName).append("/").append(wsName) ;
    return sb.toString() ;
  }
  protected String autoInitSwapPath(String swapPath, String repoName, String wsName) {
    StringBuilder sb  = new StringBuilder() ;
    sb.append(swapPath.substring(0, swapPath.lastIndexOf("/")+1)).append(repoName).append("/").append(wsName) ;
    return sb.toString() ;
  }
  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) {
    UIFormInputSetWithAction uiFormAction = getChildById(FIELD_STEP1) ;
    UIFormStringInput permissionField = uiFormAction.getUIStringInput(UIWizardStep1.FIELD_PERMISSION) ;
    permissionField.setValue(value.toString()) ;
  }
  
  protected void showHidden(boolean isChecked) {
    getUIStringInput(UIWizardStep2.FIELD_STOREPATH).setRendered(isChecked) ;
    getUIStringInput(UIWizardStep2.FIELD_FILTER).setRendered(isChecked) ;
    getUIStringInput(UIWizardStep2.FIELD_STORETYPE).setRendered(isChecked) ;
    if(isChecked) {
      
    }
  }
  
  private boolean checkLockTimeOut(String lockTimeOut) {
    if (lockTimeOut.matches("\\d+") || lockTimeOut.matches("\\d+[smhdw]") || lockTimeOut.matches("\\d+ms")) {
      return true;
    }
    return false;
  }
  
  public static class ViewStep1ActionListener extends EventListener<UIWorkspaceWizard>{
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      uiFormWizard.viewStep(1) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormWizard.getAncestorOfType(UIPopupContainer.class)) ; 
    }
  }

  public static class ViewStep2ActionListener extends EventListener<UIWorkspaceWizard>{
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      UIWizardStep1 uiWSFormStep1 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP1) ;
      String wsName = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_NAME).getValue() ;
      boolean isDefault = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).isChecked() ;
      boolean isDMSSystemWs = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).isChecked() ;
      String nodeType = uiWSFormStep1.getUIFormSelectBox(UIWizardStep1.FIELD_NODETYPE).getValue() ;
      String lockTimeOut = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_TIMEOUT).getValue() ;
      UIFormInputSet uiWSFormStep2 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
      boolean isExternalStoreage = uiWSFormStep2.getUIFormCheckBoxInput(UIWizardStep2.FIELD_EXTERNAL_STORE).isChecked() ;
      String storePath = null;
      if(isExternalStoreage) storePath = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).getValue() ;
      String swapPath =  uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).getValue() ;
      UIApplication uiApp = uiFormWizard.getAncestorOfType(UIApplication.class) ;
      if(uiFormWizard.isCheckValid_) {
        if(uiWSFormStep1.isRendered()) {
          if(uiFormWizard.isEmpty(wsName)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", 
              "'", "#", ";", "}", "{", "/", "|", "\"", " "};
          if (!Utils.isNameValid(wsName, arrFilterChar)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-not-allowed", null, 
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
            return;
          }
          if(uiFormWizard.isNewWizard_) {
            UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
            UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
            if(uiRepoForm.isExistWorkspace(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }          
          } else if(uiFormWizard.isNewRepo_ ) {
            UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
            UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
            if(uiFormWizard.selectedWsName_ == null && uiRepoForm.getWorkspaceMap().containsKey(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }  
          }
          if (isDefault && !Utils.NT_UNSTRUCTURED.equals(nodeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.nodeType-invalid",null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if (isDMSSystemWs && !Utils.NT_UNSTRUCTURED.equals(nodeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.nodeType-invalidSystemWs",null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(uiFormWizard.isNewWizard_ && uiWSFormStep1.isPermissionEmpty()) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.permission-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if(Utils.isNameEmpty(lockTimeOut)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.lockTimeOut-required", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if (!uiFormWizard.checkLockTimeOut(lockTimeOut)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.lockTimeOut-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return; 
          }
          if(!Utils.isNameEmpty(swapPath)) {
            if(!swapPath.contains(wsName))  swapPath = swapPath + wsName ;
          }
          if(isExternalStoreage && !Utils.isNameEmpty(storePath)) {
            if(!storePath.contains(wsName))  storePath = storePath + wsName ;
            uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).setValue(storePath) ;
          }
          uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).setValue(swapPath) ;
        }
      }
      if(uiFormWizard.isNewWizard_){
        String swapPathAuto = swapPath ;
        swapPathAuto = swapPath.substring(0,swapPath.lastIndexOf("/")+1) + wsName ;
        uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).setValue(swapPathAuto) ;
        if(isExternalStoreage) {
          String storePathAuto = storePath ;
          storePathAuto = storePath.substring(0,storePath.lastIndexOf("/")+1) + wsName ;
          uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).setValue(storePathAuto) ;
        }
      }
      uiFormWizard.viewStep(2) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormWizard.getAncestorOfType(UIPopupContainer.class)) ;
    }
  }

  public static class ViewStep3ActionListener extends EventListener<UIWorkspaceWizard>{
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      UIApplication uiApp = uiFormWizard.getAncestorOfType(UIApplication.class) ;
      UIWizardStep1 uiWSFormStep1 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP1) ;
      String wsName = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_NAME).getValue() ;
      boolean isDefault = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).isChecked() ;
      boolean isDMSSystemWs = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).isChecked() ;
      String nodeType = uiWSFormStep1.getUIFormSelectBox(UIWizardStep1.FIELD_NODETYPE).getValue() ;
      String lockTimeOut = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_TIMEOUT).getValue() ;
      UIFormInputSet uiWSFormStep2 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
      UIWizardStep3 uiWSFormStep3 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP3) ;
      String sourceName = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SOURCENAME).getValue() ;
      String containerType = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_CONTAINER).getValue() ;
      String storeType = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STORETYPE).getValue() ;
      String storePath = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).getValue() ;
      String swapPath =  uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).getValue() ;
      String maxBuffer = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_MAXBUFFER).getValue() ;
      String indexPath = uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_INDEXPATH).getValue();
      if(uiFormWizard.isCheckValid_) {
        if(uiWSFormStep1.isRendered()) {
          if(uiFormWizard.isEmpty(wsName)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", 
              "'", "#", ";", "}", "{", "/", "|", "\"", " "};
          if (!Utils.isNameValid(wsName, arrFilterChar)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-not-allowed", null, 
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
            return;
          }
          if(isDefault && !Utils.NT_UNSTRUCTURED.equals(nodeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.nodeType-invalid",null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(isDMSSystemWs && !Utils.NT_UNSTRUCTURED.equals(nodeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.nodeType-invalidSystemWs",null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(uiFormWizard.isNewWizard_) {
            UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
            UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
            if(uiRepoForm.isExistWorkspace(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }          
          } else if(uiFormWizard.isNewRepo_ ) {
            UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
            UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
            if (uiFormWizard.selectedWsName_ == null && uiRepoForm.getWorkspaceMap().containsKey(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }  
          }
          if (uiFormWizard.isNewWizard_ && uiWSFormStep1.isPermissionEmpty()) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.permission-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if (Utils.isNameEmpty(lockTimeOut)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.lockTimeOut-required", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if (!uiFormWizard.checkLockTimeOut(lockTimeOut)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.lockTimeOut-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return; 
          }
        }
        if (uiWSFormStep2.isRendered()) {
          boolean isExternalStoreage = uiWSFormStep2.getUIFormCheckBoxInput(UIWizardStep2.FIELD_EXTERNAL_STORE).isChecked() ;
          if (uiFormWizard.isEmpty(containerType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.containerName-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
          if(uiFormWizard.isEmpty(sourceName)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.sourceName-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
          if(isExternalStoreage && uiFormWizard.isEmpty(storeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.storeType-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if(uiFormWizard.isEmpty(maxBuffer)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.buffer-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(Utils.isNameEmpty(swapPath)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.swapPath-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          } 
          if(isExternalStoreage && uiFormWizard.isEmpty(storePath)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.storePath-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
        }
      }
      if(uiFormWizard.isNewWizard_){
        boolean isExternalStoreage = uiWSFormStep2.getUIFormCheckBoxInput(UIWizardStep2.FIELD_EXTERNAL_STORE).isChecked() ;
        String swapPathAuto = swapPath ;
        swapPathAuto = swapPath.substring(0,swapPath.lastIndexOf("/")+1) + wsName ;
        String indexPathAuto = indexPath.substring(0,indexPath.lastIndexOf("/")+1) + wsName ;
        uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_INDEXPATH).setValue(indexPathAuto);
        if(isExternalStoreage) {
          String storePathAuto = storePath ;
          storePathAuto = storePath.substring(0,storePath.lastIndexOf("/")+1) + wsName ;
          uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).setValue(storePathAuto) ;
        }
        uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).setValue(swapPathAuto) ;
      }
      uiFormWizard.viewStep(3) ; 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormWizard.getAncestorOfType(UIPopupContainer.class)) ;
    }
  }
  
  public static class FinishActionListener extends EventListener<UIWorkspaceWizard>{
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      uiFormWizard.removePopup(UIWorkspaceWizard.POPUPID) ;
      long lockTimeOutValue = 0 ;

      UIWizardStep1 uiWSFormStep1 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP1) ;
      String name = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_NAME).getValue().trim();
      String initNodeType = uiWSFormStep1.getUIFormSelectBox(UIWizardStep1.FIELD_NODETYPE).getValue() ;
      boolean isDefault = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).isChecked() ;
      boolean isDMSSystemWs = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).isChecked() ;
      String lockTimeOut = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_TIMEOUT).getValue() ;

      UIWizardStep2 uiWSFormStep2 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
      String containerType = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_CONTAINER).getValue() ;
      String sourceName = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SOURCENAME).getValue() ;
      String dbType =  uiWSFormStep2.getUIFormSelectBox(UIWizardStep2.FIELD_DBTYPE).getValue() ;
      boolean isMulti = uiWSFormStep2.getUIFormCheckBoxInput(UIWizardStep2.FIELD_ISMULTI).isChecked() ;
      String storeType = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STORETYPE).getValue() ;
      String storePath = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).getValue() ;
      String filterType = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_FILTER).getValue() ;
      String maxBuffer =  uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_MAXBUFFER).getValue() ;
      String swapPath =  uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).getValue() ;

      UIWizardStep3 uiWSFormStep3 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP3) ;
      String queryHandlerType = uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_QUERYHANDLER).getValue() ;
      String indexPath = uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_INDEXPATH).getValue() ;
      boolean isCache = uiWSFormStep3.getUIFormCheckBoxInput(UIWizardStep3.FIELD_ISCACHE).isChecked() ;
      String maxSize = uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_MAXSIZE).getValue() ;
      String liveTime = uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_LIVETIME).getValue() ;
      UIApplication uiApp = uiFormWizard.getAncestorOfType(UIApplication.class) ;
      UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
      UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
      String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", 
          "'", "#", ";", "}", "{", "/", "|", "\"", " "};
      if (!Utils.isNameValid(name, arrFilterChar)) {
        uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-not-allowed", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      if(uiFormWizard.isCheckValid_){
        if(uiFormWizard.isEmpty(queryHandlerType)) {
          uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.queryHandlerType-invalid", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }  
        if(uiFormWizard.isEmpty(indexPath)) {
          uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.indexPath-invalid", null)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
        }
        if(isCache){
          if(uiFormWizard.isEmpty(maxSize)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.maxSize-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(uiFormWizard.isEmpty(liveTime)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.liveTime-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
        }
        if (lockTimeOut.endsWith("s") && !lockTimeOut.endsWith("ms")) {
          lockTimeOut = lockTimeOut.substring(0, lockTimeOut.length() - 1);
        }
        lockTimeOutValue = StringNumberParser.parseTime(lockTimeOut);
        if(uiFormWizard.isNewWizard_) {
          if(uiRepoForm.isExistWorkspace(name)){
            Object[] args = new Object[]{name}  ;        
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }          
        } 
      }
      WorkspaceEntry workspaceEntry = new WorkspaceEntry(name, initNodeType);
      StringBuilder permSb = new StringBuilder() ;
      for(String s : uiWSFormStep1.getPermissions().values()) {
        if(!s.endsWith(";")) s = s + ";" ;
        permSb.append(s);
      }
      LockManagerEntry lockEntry = new LockManagerEntry() ;
      lockEntry.setTimeout(lockTimeOutValue) ;
      LockPersisterEntry persisterEntry = new LockPersisterEntry();
      String lockPath = "../temp/lock/" + name; 
      persisterEntry.setType(KEY_LOCKMANAGER);
      ArrayList<SimpleParameterEntry> lpParams = new ArrayList<SimpleParameterEntry>();
      lpParams.add(new SimpleParameterEntry("path", lockPath));
      persisterEntry.setParameters(lpParams);
      lockEntry.setPersister(persisterEntry);
      workspaceEntry.setLockManager(lockEntry) ;
      workspaceEntry.setContainer(newContainerEntry(containerType, sourceName, dbType, isMulti,storeType, filterType, maxBuffer, swapPath, storePath, true,name));
      workspaceEntry.setCache(newCacheEntry(isCache, maxSize, liveTime)) ;
      workspaceEntry.setQueryHandler(newQueryHandlerEntry(queryHandlerType, indexPath)) ;

      if(uiRepoForm.isAddnew_) {
        if (isDefault) uiRepoForm.defaulWorkspace_ = name;
        if (isDMSSystemWs) uiRepoForm.dmsSystemWorkspace_ = name;
        if(uiFormWizard.isNewWizard_) {
          uiRepoForm.getWorkspaceMap().put(name, workspaceEntry) ;
          uiRepoForm.getWorkspaceMapNodeType().put(name, initNodeType) ;
          uiRepoForm.getWorkspaceMapPermission().put(name, permSb.toString());
        } else {
          uiRepoForm.getWorkspaceMap().remove(uiFormWizard.selectedWsName_) ;
          uiRepoForm.getWorkspaceMap().put(name, workspaceEntry) ;
          
          uiRepoForm.getWorkspaceMapNodeType().remove(uiFormWizard.selectedWsName_) ;
          uiRepoForm.getWorkspaceMapNodeType().put(name, initNodeType) ;
          
          uiRepoForm.getWorkspaceMapPermission().remove(uiFormWizard.selectedWsName_) ;
          uiRepoForm.getWorkspaceMapPermission().put(name, permSb.toString());
        }
        uiRepoForm.refreshWorkspaceList() ;  
      }

      if(!uiRepoForm.isAddnew_ && uiFormWizard.isNewWizard_) {
        InitialContextInitializer ic = (InitialContextInitializer)uiFormWizard.getApplicationComponent(ExoContainer.class).
        getComponentInstanceOfType(InitialContextInitializer.class) ;
        if(ic != null) ic.recall() ;
        RepositoryService rService = (RepositoryService)uiFormWizard.getApplicationComponent(ExoContainer.class).
        getComponentInstanceOfType(RepositoryService.class);
        ManageableRepository manageRepository = rService.getRepository(uiRepoForm.repoName_);
        try {
          manageRepository.configWorkspace(workspaceEntry) ;
          manageRepository.createWorkspace(workspaceEntry.getName()) ;
          if(rService.getConfig().isRetainable()) {
            rService.getConfig().retain() ;
          }
//          ((ExtendedNode)manageRepository.getSystemSession(name).getRootNode()).setPermission("any", PermissionType.ALL);
          Session systemSession = manageRepository.getSystemSession(name); 
          uiFormWizard.setPermissionToRoot((ExtendedNode)systemSession.getRootNode(), permSb.toString());
          uiRepoForm.workspaceMap_.clear() ;
          for(WorkspaceEntry ws : manageRepository.getConfiguration().getWorkspaceEntries()) {
            uiRepoForm.workspaceMap_.put(ws.getName(), ws) ;
          }
          uiRepoForm.refreshWorkspaceList() ;   
          systemSession.logout();
        }
        catch (Exception e) {
          LOG.error("Unexpected error", e);
          return;
        }
      }
      UIPopupContainer UIPopupContainer = uiFormWizard.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupContainer.deActivate() ;      
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiRepoForm) ;
    }

    @SuppressWarnings("unchecked")
    private ContainerEntry newContainerEntry(String containerType, String sourceName, String dbType, boolean  isMulti,
        String storeType, String filterType, String bufferValue, String swapPath, String storePath, boolean isUpdateStore,String valueStorageId) {
      List containerParams = new ArrayList();
      containerParams.add(new SimpleParameterEntry(KEY_SOURCENAME, sourceName)) ;
      containerParams.add(new SimpleParameterEntry(KEY_DIALECT, dbType)) ;
      containerParams.add(new SimpleParameterEntry(KEY_MULTIDB, String.valueOf(isMulti))) ;
      containerParams.add(new SimpleParameterEntry(KEY_UPDATESTORE, String.valueOf(isUpdateStore))) ;
      containerParams.add(new SimpleParameterEntry(KEY_MAXBUFFER, bufferValue)) ;
      containerParams.add(new SimpleParameterEntry(KEY_SWAPDIRECTORY, swapPath)) ;
      ContainerEntry containerEntry = new ContainerEntry(containerType, (ArrayList) containerParams) ;      
      containerEntry.setParameters(containerParams);
      
      if(storeType != null) {
        ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
        ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
        filterEntry.setPropertyType(filterType);
        vsparams.add(filterEntry);
        
        ValueStorageEntry valueStorageEntry = new ValueStorageEntry(storeType,
            vsparams);
        ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
        spe.add(new SimpleParameterEntry(KEY_PATH, storePath));
        valueStorageEntry.setId(valueStorageId);
        valueStorageEntry.setParameters(spe);
        valueStorageEntry.setFilters(vsparams);
        ArrayList list = new ArrayList(1);
        list.add(valueStorageEntry);
        containerEntry.setValueStorages(list);
      } else {
        containerEntry.setValueStorages(new ArrayList());
      }
      return containerEntry ;
    }

    @SuppressWarnings("unused")
    private ValueStorageEntry newValueStorageEntry(String storeType, String value, String filter) {
      ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
      ValueStorageEntry valueStorageEntry = new ValueStorageEntry(storeType, vsparams)  ;
      return valueStorageEntry ;
    }
    private CacheEntry newCacheEntry(boolean isCache, String maxSizeValue, String liveTimeValue) {
      CacheEntry cache = new CacheEntry() ;
      cache.setEnabled(isCache) ;      
      ArrayList<SimpleParameterEntry> cacheParams = new ArrayList<SimpleParameterEntry>() ;
      cacheParams.add(new SimpleParameterEntry(KEY_MAXSIZE, maxSizeValue)) ;
      cacheParams.add(new SimpleParameterEntry(KEY_LIVETIME, liveTimeValue)) ;
      cache.setParameters(cacheParams) ;
      return cache ;
    }
    private QueryHandlerEntry newQueryHandlerEntry(String queryHandlerType, String indexPath) {
      List<SimpleParameterEntry> queryParams = new ArrayList<SimpleParameterEntry>() ;
      queryParams.add(new SimpleParameterEntry(KEY_INDEXDIR, indexPath)) ;
      QueryHandlerEntry queryHandler = new QueryHandlerEntry(queryHandlerType, queryParams) ;
      queryHandler.setType(queryHandlerType) ;
      queryHandler.setParameters(queryParams) ;
      return queryHandler ;
    }
  }

  public static class NextActionListener extends EventListener<UIWorkspaceWizard>{
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      uiFormWizard.removePopup(POPUPID) ;
      UIWizardStep1 uiWSFormStep1 = uiFormWizard.getChildById(FIELD_STEP1) ;
      String wsName = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_NAME).getValue() ;
      boolean isDefault = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).isChecked() ;
      boolean isDMSSytemWs = uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).isChecked() ;
      String nodeType = uiWSFormStep1.getUIFormSelectBox(UIWizardStep1.FIELD_NODETYPE).getValue() ;
      String lockTimeOut = uiWSFormStep1.getUIStringInput(UIWizardStep1.FIELD_TIMEOUT).getValue() ;
      UIWizardStep2 uiWSFormStep2 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
      String containerName = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_CONTAINER).getValue() ;
      boolean isExternalStoreage = uiWSFormStep2.getUIFormCheckBoxInput(UIWizardStep2.FIELD_EXTERNAL_STORE).isChecked() ;
      String storeType = null ; 
      String storePath = null ;
      if(isExternalStoreage) {
        storeType = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STORETYPE).getValue() ;
        storePath = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).getValue() ;
      }
      String sourceName = uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SOURCENAME).getValue() ;
      String swapPath =  uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).getValue() ;
      String maxBuffer =  uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_MAXBUFFER).getValue() ;
      UIWizardStep3 uiWSFormStep3 = uiFormWizard.getChildById(UIWorkspaceWizard.FIELD_STEP3) ;
      String indexPath = uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_INDEXPATH).getValue() ;
      String maxCacheSize =  uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_MAXSIZE).getValue() ;
      UIApplication uiApp = uiFormWizard.getAncestorOfType(UIApplication.class) ;
      if(uiFormWizard.isCheckValid_) {
        if(uiWSFormStep1.isRendered()) {
          if(uiFormWizard.isEmpty(wsName)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")", 
              "'", "#", ";", "}", "{", "/", "|", "\"", " "};
          if (!Utils.isNameValid(wsName, arrFilterChar)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.name-not-allowed", null, 
                ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
            return;
          }
          if(uiFormWizard.isNewWizard_) {
            UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
            UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
            if(uiRepoForm.isExistWorkspace(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }
            if(uiFormWizard.selectedWsName_ == null && uiRepoForm.getWorkspaceMap().containsKey(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }
            if (!isDefault && (uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).isEnable())) 
              uiRepoForm.defaulWorkspace_ = null;
            if (!isDMSSytemWs && (uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).isEnable())) 
              uiRepoForm.dmsSystemWorkspace_ = null;
          } else if(uiFormWizard.isNewRepo_ ) {
            UIRepositoryFormContainer formContainer = uiFormWizard.getAncestorOfType(UIRepositoryFormContainer.class) ;
            UIRepositoryForm uiRepoForm = formContainer.findFirstComponentOfType(UIRepositoryForm.class) ;
            if(uiRepoForm.isExistWorkspace(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }
            if(uiFormWizard.selectedWsName_ == null && uiRepoForm.getWorkspaceMap().containsKey(wsName)){
              Object[] args = new Object[]{wsName}  ;        
              uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.wsname-exist", args)) ;
              event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
              return ;
            }
            if (!isDefault && (uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDEFAULT).isEnable())) 
              uiRepoForm.defaulWorkspace_ = null;
            if (!isDMSSytemWs && (uiWSFormStep1.getUIFormCheckBoxInput(UIWizardStep1.FIELD_ISDMS_SYSTEM_WS).isEnable())) 
              uiRepoForm.dmsSystemWorkspace_ = null;
          }
          if(isDefault && !Utils.NT_UNSTRUCTURED.equals(nodeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.nodeType-invalid",null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if (isDMSSytemWs && !Utils.NT_UNSTRUCTURED.equals(nodeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.nodeType-invalidSystemWs",null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(uiFormWizard.isNewWizard_ && uiWSFormStep1.isPermissionEmpty()) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.permission-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if (Utils.isNameEmpty(lockTimeOut)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.lockTimeOut-required", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
          if (!uiFormWizard.checkLockTimeOut(lockTimeOut)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.lockTimeOut-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return; 
          }
        }
        if(uiWSFormStep2.isRendered()) {
          if(uiFormWizard.isEmpty(containerName)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.containerName-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
          if(uiFormWizard.isEmpty(sourceName)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.sourceName-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
          if(isExternalStoreage && uiFormWizard.isEmpty(storeType)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.storeType-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
          if(uiFormWizard.isEmpty(maxBuffer)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.buffer-require", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;  
            return ;
          }
          if(uiFormWizard.isEmpty(swapPath)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.swapPath-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          } 
          if(isExternalStoreage && uiFormWizard.isEmpty(storePath)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.storePath-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }  
        }
        if(uiWSFormStep3.isRendered()) {
          if(Utils.isNameEmpty(indexPath)) {
            uiApp.addMessage(new ApplicationMessage("UIWorkspaceWizard.msg.indexPath-invalid", null)) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
            return ;
          }
        }
      }
      if(uiFormWizard.isNewWizard_){
        String swapPathAuto = swapPath ;
        swapPathAuto = swapPath.substring(0,swapPath.lastIndexOf("/")+1) + wsName ;
        uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_SWAPPATH).setValue(swapPathAuto) ;
        String indexPathAuto = indexPath.substring(0,indexPath.lastIndexOf("/")+1) + wsName ;
        uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_INDEXPATH).setValue(indexPathAuto);
        if(isExternalStoreage) {
          String storePathAuto = storePath ;
          storePathAuto = storePath.substring(0,storePath.lastIndexOf("/")+1) + wsName ;
          uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STOREPATH).setValue(storePathAuto) ;
        }
      }

      int step = uiFormWizard.getCurrentStep() ;
      List<UIComponent> children = uiFormWizard.getChildren() ;
      if(step < uiFormWizard.getMaxStep()) {
        step++ ;
        uiFormWizard.setCurrentSep(step) ;
        for(int i = 0 ; i< children.size(); i++) {
          if(i == step) {
            children.get(i).setRendered(true);
            uiFormWizard.setSelectedStep(step+1) ;
          } else {
            children.get(i).setRendered(false);
          } 
        }
      }     
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormWizard.getAncestorOfType(UIWorkspaceWizardContainer.class)) ; 
    }
  }

  public static class BackActionListener extends EventListener<UIWorkspaceWizard>{
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      uiFormWizard.removePopup(UIWorkspaceWizard.POPUPID) ;
      int step = uiFormWizard.getCurrentStep() ;
      List<UIComponent> children = uiFormWizard.getChildren() ;
      if(step > 0) {
        step-- ;
        uiFormWizard.setCurrentSep(step) ;
        for(int i = 0 ; i< children.size(); i++) {
          if(i == step) {
            children.get(i).setRendered(true);
            uiFormWizard.setSelectedStep(step+1) ;
          } else {
            children.get(i).setRendered(false);
          } 
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiFormWizard.getAncestorOfType(UIWorkspaceWizardContainer.class)) ;
    }
  }
  public static class AddPermissionActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiWizardForm = event.getSource() ;
      UIPopupContainer UIPopupContainer = uiWizardForm.getAncestorOfType(UIWorkspaceWizardContainer.class).
      getChild(UIPopupContainer.class) ;
      UIPopupContainer.activate(UIPermissionContainer.class, 600) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer) ;
    }

  }
  public static class EditPermissionActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiForm = event.getSource() ;
      String permName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UIPopupContainer UIPopupContainer = uiForm.getAncestorOfType(UIWorkspaceWizardContainer.class).
      getChild(UIPopupContainer.class) ;
      UIWizardStep1 ws1 = uiForm.getChildById(FIELD_STEP1) ;
      UIPermissionContainer uiContainer = UIPopupContainer.activate(UIPermissionContainer.class, 600) ;
      uiContainer.setValues(permName, ws1.getPermissions().get(permName)) ;
      uiContainer.lockForm(!uiForm.isNewRepo_ && !uiForm.isNewWizard_) ;
      ws1.refreshPermissionList() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIWorkspaceWizardContainer.class)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer) ;
    }
  }

  public static class RemovePermissionActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiForm = event.getSource() ;
      UIWizardStep1 ws1 = uiForm.getChildById(FIELD_STEP1) ;
      String permName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      ws1.removePermission(permName) ;
      ws1.refreshPermissionList() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIWorkspaceWizardContainer.class)) ;
    }
  }

  public static class CancelActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiFormWizard = event.getSource() ;
      uiFormWizard.refresh(null, false) ;
      UIPopupContainer UIPopupContainer = uiFormWizard.getAncestorOfType(UIPopupContainer.class) ;
      UIPopupContainer.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer) ;
    }
  }

  public void setContainerName(String value) {
    UIFormInputSet uiWSFormStep2 =  getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
    uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_CONTAINER).setValue(value) ;
  }
  public void setStoreTypeName(String value) {
    UIFormInputSet uiWSFormStep2 =  getChildById(UIWorkspaceWizard.FIELD_STEP2) ;
    uiWSFormStep2.getUIStringInput(UIWizardStep2.FIELD_STORETYPE).setValue(value) ;
  }
  public void setQueryHandlerName(String value) {
    UIFormInputSet uiWSFormStep3 =  getChildById(UIWorkspaceWizard.FIELD_STEP3) ;
    uiWSFormStep3.getUIStringInput(UIWizardStep3.FIELD_QUERYHANDLER).setValue(value) ;
  }

  public static class SelectContainerActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiWizard = event.getSource() ;
      UIPopupContainer uiPopup = uiWizard.getAncestorOfType(UIWorkspaceWizardContainer.class).
      getChild(UIPopupContainer.class);
      UIRepositoryValueSelect uiSelect = uiPopup.activate(UIRepositoryValueSelect.class, 500) ;
      uiSelect.isSetContainer_ = true ;
      List<ClassData> datas = new ArrayList<ClassData>() ;
      datas.add(new ClassData(UIWorkspaceWizard.KEY_CONTAINERTYPE)) ;
      uiSelect.updateGrid(datas) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    } 
  }
  
  public static class SelectStoreActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiWizard = event.getSource() ;
      UIPopupContainer uiPopup = uiWizard.getAncestorOfType(UIWorkspaceWizardContainer.class).
      getChild(UIPopupContainer.class);
      UIRepositoryValueSelect uiSelect = uiPopup.activate(UIRepositoryValueSelect.class, 500) ;
      uiSelect.isSetStoreType_ = true ;
      List<ClassData> datas = new ArrayList<ClassData>() ;
      datas.add(new ClassData(UIWorkspaceWizard.KEY_STORETYPE)) ;
      datas.add(new ClassData(UIWorkspaceWizard.KEY_TREE_STORETYPE)) ;
      uiSelect.updateGrid(datas) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    } 
  }
  public static class SelectQueryHandlerActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiWizard = event.getSource() ;
      UIPopupContainer uiPopup = uiWizard.getAncestorOfType(UIWorkspaceWizardContainer.class).
      getChild(UIPopupContainer.class);
      UIRepositoryValueSelect uiSelect = uiPopup.activate(UIRepositoryValueSelect.class, 500) ;
      uiSelect.isSetQueryHandler_ = true ;
      List<ClassData> datas = new ArrayList<ClassData>() ;
      datas.add(new ClassData(UIWorkspaceWizard.KEY_QUERYHANDLER)) ;
      uiSelect.updateGrid(datas) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    } 
  }
  
  public static class ChangeTypeStoreActionListener extends EventListener<UIWorkspaceWizard> {
    public void execute(Event<UIWorkspaceWizard> event) throws Exception {
      UIWorkspaceWizard uiWizard = event.getSource() ;
      UIWizardStep2 ws2 = uiWizard.getChildById(FIELD_STEP2) ;
      boolean isChecked = ws2.getUIFormCheckBoxInput(UIWizardStep2.FIELD_EXTERNAL_STORE).isChecked() ;
      uiWizard.showHidden(isChecked) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWizard) ;
    } 
  }
}