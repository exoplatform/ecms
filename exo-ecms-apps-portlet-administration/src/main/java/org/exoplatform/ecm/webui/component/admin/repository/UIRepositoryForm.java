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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Session;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.monitor.jvm.J2EEServerInfo;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.repository.UIRepositoryValueSelect.ClassData;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.picocontainer.PicoIntrospectionException;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * May 9, 2007  
 */

@ComponentConfig(  
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIForm.gtmpl",   
    events = {
      @EventConfig(listeners = UIRepositoryForm.SaveActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIRepositoryForm.SelectActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIRepositoryForm.ResetActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIRepositoryForm.CloseActionListener.class),
      @EventConfig(listeners = UIRepositoryForm.AddWorkspaceActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIRepositoryForm.ShowHiddenActionListener.class),
      @EventConfig(listeners = UIRepositoryForm.RemoveWorkspaceActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UIRepositoryForm.EditWorkspaceActionListener.class)
    }  
)
public class UIRepositoryForm extends UIForm implements UIPopupComponent {  
  private ConfigurationManager configurationManager;
  private static final Log LOG  = ExoLogger.getLogger("admin.UIRepositoryForm");
  final  static public String ST_ADD = "AddRepoPopup";
  final  static public String ST_EDIT = "EditRepoPopup";
  final static public String POPUP_WORKSPACE = "PopupWorkspace";
  final static public String FIELD_NAME = "name";  
  final static public String FIELD_WSINPUTSET = "wsInputSet"; 
  final static public String FIELD_WORKSPACE = "workspace";
  final static public String FIELD_ISDEFAULT = "isDefault";
  final static public String FIELD_ACCESSCONTROL = "accessControl";
  final static public String FIELD_AUTHINPUTSET = "authInputSet";
  final static public String FIELD_AUTHENTICATION = "authenticationPolicy";

  final static public String FIELD_SCURITY  = "securityDomain";
  final static public String FIELD_SESSIONTIME = "sessionTime";

  final static public String FIELD_REPCHANNEL = "channelConfig";
  final static public String FIELD_REPENABLE = "enableReplication";
  final static public String FIELD_REPMODE = "repMode";
  final static public String FIELD_REPTESTMODE = "repTestMode";

  final static public String FIELD_BSEPATH = "directoryPath";
  final static public String FIELD_BSEMAXBUFFER = "maxBufferSize";
  final static public String KEY_AUTHENTICATIONPOLICY = "org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator";
  protected boolean isAddnew_ = true;  
  protected String defaulWorkspace_ = null;
  protected String dmsSystemWorkspace_ = null;
  protected String repoName_ = null;
  protected Map<String, WorkspaceEntry> workspaceMap_ = new HashMap<String, WorkspaceEntry>(); 
  protected Map<String, String> workspaceMapNodeType_ = new HashMap<String, String>();
  protected Map<String, String> workspaceMapPermission_ = new HashMap<String, String>();
  
  protected Map<String, String> defaulWorkspaceMap = new HashMap<String, String>();
  
  public UIRepositoryForm() throws Exception { 
    configurationManager = getApplicationComponent(ConfigurationManager.class);
    addChild(new UIFormStringInput(FIELD_NAME,FIELD_NAME, null).addValidator(MandatoryValidator.class)); 
    UIFormInputSetWithAction workspaceField = new UIFormInputSetWithAction(FIELD_WSINPUTSET);
    workspaceField.addUIFormInput(new UIFormInputInfo(FIELD_WORKSPACE, FIELD_WORKSPACE, null));
    workspaceField.setActionInfo(FIELD_WORKSPACE, new String[]{"EditWorkspace", "RemoveWorkspace"});
    addUIComponentInput(workspaceField);
    addChild(new UIFormCheckBoxInput<String>(FIELD_ISDEFAULT,FIELD_ISDEFAULT, null).setEditable(false));
    addChild(new UIFormStringInput(FIELD_ACCESSCONTROL,FIELD_ACCESSCONTROL, null).addValidator(MandatoryValidator.class));  
    UIFormInputSetWithAction autField = new UIFormInputSetWithAction(FIELD_AUTHINPUTSET);
    autField.addChild(new UIFormStringInput(FIELD_AUTHENTICATION, FIELD_AUTHENTICATION, null).addValidator(MandatoryValidator.class));
    autField.setActionInfo(FIELD_AUTHENTICATION, new String[]{"Select"});
    addChild(autField);
    addChild(new UIFormStringInput(FIELD_SCURITY,FIELD_SCURITY, null).addValidator(MandatoryValidator.class));    
    addChild(new UIFormStringInput(FIELD_SESSIONTIME,FIELD_SESSIONTIME, null));
  }  

  public void refresh(RepositoryEntry repo) throws Exception{
    reset();
    getUIFormCheckBoxInput(FIELD_ISDEFAULT).setChecked(false);
    UIFormInputSetWithAction autField = getChildById(FIELD_AUTHINPUTSET);
    workspaceMap_.clear();
    if(repo != null) {
      if(isAddnew_) {      
        repoName_ = null;
        defaulWorkspace_ = null;
        dmsSystemWorkspace_ = null;
        refreshWorkspaceList();
        getUIStringInput(FIELD_NAME).setEditable(true);
        getUIFormCheckBoxInput(FIELD_ISDEFAULT).setChecked(false);
        getUIFormCheckBoxInput(FIELD_ISDEFAULT).setEnable(false);
        setActions(new String[] {"Save","AddWorkspace", "Reset", "Close"});
      } else {
        repoName_ = repo.getName();
        defaulWorkspace_ = repo.getDefaultWorkspaceName();
        dmsSystemWorkspace_ = repo.getSystemWorkspaceName();
        for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
          workspaceMap_.put(ws.getName(), ws);
        }
        getUIStringInput(FIELD_NAME).setEditable(false);
        getUIStringInput(FIELD_NAME).setValue(repo.getName());
        refreshWorkspaceList();
        getUIFormCheckBoxInput(FIELD_ISDEFAULT).setChecked(isDefaultRepo(repo.getName()));
        getUIFormCheckBoxInput(FIELD_ISDEFAULT).setEnable(false);
        autField.setActionInfo(FIELD_AUTHENTICATION, null);
        setActions(new String[] {"AddWorkspace", "Close"});
      }
      getUIStringInput(UIRepositoryForm.FIELD_ACCESSCONTROL).setValue(repo.getAccessControl());      
      autField.getUIStringInput(UIRepositoryForm.FIELD_AUTHENTICATION).setValue(repo.getAuthenticationPolicy());      
      getUIStringInput(UIRepositoryForm.FIELD_SCURITY).setValue(repo.getSecurityDomain());
      getUIStringInput(UIRepositoryForm.FIELD_SESSIONTIME).setValue(String.valueOf(repo.getSessionTimeOut()));
    }
  }
  protected void lockForm(boolean isLock) throws Exception {
    boolean editable = !isLock;
    UIFormInputSetWithAction autField = getChildById(FIELD_AUTHINPUTSET);
    if(isLock) {
      autField.setActionInfo(FIELD_AUTHENTICATION, null);
    } else {
      autField.setActionInfo(FIELD_AUTHENTICATION, new String[]{"Select"});
    }
    getUIStringInput(UIRepositoryForm.FIELD_ACCESSCONTROL).setEditable(editable); 
    autField.getUIStringInput(UIRepositoryForm.FIELD_AUTHENTICATION).setEditable(editable);     
    getUIStringInput(UIRepositoryForm.FIELD_SCURITY).setEditable(editable); 
    getUIStringInput(UIRepositoryForm.FIELD_SESSIONTIME).setEditable(editable);
  }
  
  protected boolean isDefaultWorkspace(String workspaceName) {
    return workspaceName.equals(defaulWorkspace_);
  }
  
  protected boolean isDmsSystemWorkspace(String workspaceName) {
    return workspaceName.equals(dmsSystemWorkspace_);
  }
  
  protected boolean isExistWorkspace(String workspaceName){
    RepositoryService rservice = getApplicationComponent(RepositoryService.class);
    for(RepositoryEntry repo : rservice.getConfig().getRepositoryConfigurations() ) {
      for(WorkspaceEntry ws : repo.getWorkspaceEntries()) {
        if( ws.getName().equals(workspaceName)) return true;
      }
    }
    return false; 
  }

  protected WorkspaceEntry getWorkspace(String workspaceName) {
    return workspaceMap_.get(workspaceName);
  }

  protected Map<String, WorkspaceEntry> getWorkspaceMap() {
    return workspaceMap_;
  }
  
  protected String getWorkspaceMapNodeType(String workspaceName) {
    return workspaceMapNodeType_.get(workspaceName);
  }
  
  protected Map<String, String> getWorkspaceMapNodeType() {
    return workspaceMapNodeType_;
  }
  
  protected String getWorkspaceMapPermission(String workspaceName) {
    return workspaceMapPermission_.get(workspaceName);
  }
  
  protected Map<String, String> getWorkspaceMapPermission() {
    return workspaceMapPermission_;
  }

  protected void refreshWorkspaceList() {
    StringBuilder labels = new StringBuilder();
    for(String wsName : workspaceMap_.keySet()){
      if(labels.length() > 0) labels.append(",");
      labels.append(wsName);
    }
    UIFormInputSetWithAction workspaceField = getChildById(UIRepositoryForm.FIELD_WSINPUTSET);
    workspaceField.setInfoField(UIRepositoryForm.FIELD_WORKSPACE, labels.toString());
  }

  protected boolean isDefaultRepo(String repoName) {
    RepositoryService rservice = getApplicationComponent(RepositoryService.class);    
    return rservice.getConfig().getDefaultRepositoryName().equals(repoName);
  }

  protected void saveRepo(RepositoryEntry repositoryEntry) throws Exception {    
    InitialContextInitializer ic = (InitialContextInitializer)getApplicationComponent(ExoContainer.class).
    getComponentInstanceOfType(InitialContextInitializer.class);
    RegistryService registryService = getApplicationComponent(RegistryService.class);
    if(ic != null) ic.recall();
    RepositoryService rService = (RepositoryService)getApplicationComponent(ExoContainer.class).
    getComponentInstanceOfType(RepositoryService.class);
    if(isAddnew_){
      try { 
        rService.createRepository(repositoryEntry);
        for(WorkspaceEntry ws : getWorkspaceMap().values()) {
          if(ws.getName().equals(repositoryEntry.getSystemWorkspaceName())) {
            registryService.addRegistryLocation(repositoryEntry.getName(), ws.getName());
          }
        }
        for(WorkspaceEntry ws : getWorkspaceMap().values()) {
          if(!rService.getRepository(repositoryEntry.getName()).isWorkspaceInitialized(ws.getName())) {
            rService.getRepository(repositoryEntry.getName()).configWorkspace(ws);
            rService.getRepository(repositoryEntry.getName()).createWorkspace(ws.getName());
          }
        }
      } catch (RepositoryConfigurationException repositoryConfigurationException) {
        return;
      } catch (PicoIntrospectionException picoIntrospectionException) {
        return;
      } catch (Exception e) {
        LOG.error("Unexpected error", e);
        return;
      }
      initServices(repositoryEntry.getName());        
      if(rService.getConfig().isRetainable()) {
        rService.getConfig().retain();
      }
    } 
  }

  private void initServices(String repository) throws Exception{
    try {
      RepositoryService rService = getApplicationComponent(RepositoryService.class);
      InputStream xml = configurationManager.getURL("classpath:/conf/portal/registry-nodetypes.xml").openStream();
      rService.getRepository(repository).getNodeTypeManager().registerNodeTypes(xml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
      xml.close();
      getApplicationComponent(RegistryService.class).initStorage(false);
      getApplicationComponent(NodeHierarchyCreator.class).init(repository);
      getApplicationComponent(TaxonomyService.class).init(repository);
      getApplicationComponent(ManageDriveService.class).init(repository);
      getApplicationComponent(NewFolksonomyService.class).init(repository);
      getApplicationComponent(MetadataService.class).init(repository);
      getApplicationComponent(QueryService.class).init(repository);
      getApplicationComponent(RelationsService.class).init(repository);
      getApplicationComponent(ScriptService.class).initRepo(repository);
      getApplicationComponent(TemplateService.class).init(repository);
      getApplicationComponent(ManageViewService.class).init(repository);
      getApplicationComponent(ActionServiceContainer.class).init(repository);
    } catch(NullPointerException nullPointerException) {
      return;
    } catch(ItemExistsException itemExistsException) {
      return;
    } catch(IllegalArgumentException illegalArgumentException) {
      return;
    } catch(Exception e) {
      LOG.error("Unexpected error", e);
      return;
    }
  }
  
  protected void ShowHidden() {
  }
  
  public void activate() throws Exception {}

  public void deActivate() throws Exception { repoName_ = null;}
  
  private void addConfiguration(String fileConfiguration) throws Exception {
    SAXBuilder builder1 = new SAXBuilder();
    Document docConfiguration = null;
    Element rootConfiguration = null;
    boolean isExist = false;
    try {
      docConfiguration = builder1.build(fileConfiguration);
      rootConfiguration = docConfiguration.getRootElement();
      List listImportElement = rootConfiguration.getChildren("import");
      for (int i = 0; i < listImportElement.size(); i++) {
        Element element = (Element)listImportElement.get(i);
        if (element.getValue().trim().equals("dms-common-extend-configuration.xml")) {
          isExist = true;
          break;
        }
      }
      if (!isExist) {
        Element importElement = new Element("import");
        importElement.addContent("dms-common-extend-configuration.xml");
        rootConfiguration.addContent(importElement);
        XMLOutputter xmlOutputter = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
        xmlOutputter.output(docConfiguration, new FileWriter(fileConfiguration));
      }
    } catch (FileNotFoundException e) {
      StringBuilder builder = new StringBuilder();
      FileWriter outputFileReader;
      PrintWriter outputStream = null;
      builder.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
      builder.append("<configuration>\n");
      builder.append("  <import>dms-common-extend-configuration.xml</import>\n"); 
      builder.append("</configuration>\n");
      outputFileReader  = new FileWriter(fileConfiguration);
      outputStream = new PrintWriter(outputFileReader);
      outputStream.println(builder);
      if (outputStream != null) outputStream.close();
    }
  }
  
  private void addElement(String fileDmsCommon, String repoName, String systemWs) throws Exception {
    SAXBuilder builder = new SAXBuilder();
    Document doc = null;
    Element root = null;
    int count = 0;
    FileWriter outputFileReader;
    PrintWriter outputStream = null;
    try {
      doc = builder.build(fileDmsCommon);
    } catch (FileNotFoundException e) {
      StringBuilder builderString = new StringBuilder();
      builderString.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
      builderString.append("<configuration>\n");
      builderString.append("  <component>\n");
      builderString.append("    <key>org.exoplatform.services.cms.impl.DMSConfiguration</key>\n");
      builderString.append("    <type>org.exoplatform.services.cms.impl.DMSConfiguration</type>\n"); 
      builderString.append("  </component>\n");
      builderString.append("</configuration>\n");
      outputFileReader  = new FileWriter(fileDmsCommon);
      outputStream = new PrintWriter(outputFileReader);
      outputStream.println(builderString);
      outputStream.close();
      doc = builder.build(fileDmsCommon);
    } 
    
    root = doc.getRootElement();
    count = root.getChildren("external-component-plugins").size();
    
    Element externalElement = new Element("external-component-plugins");
    Element targetComponent = new Element("target-component");
    targetComponent.addContent("org.exoplatform.services.cms.impl.DMSConfiguration");
    
    Element componentPlugin = new Element("component-plugin");
    Element name = new Element("name");
    name.addContent("dmsconfiguration.plugin" + count);
    Element setMethod = new Element("set-method");
    setMethod.addContent("addPlugin");
    Element type = new Element("type");
    type.addContent("org.exoplatform.services.cms.impl.DMSRepositoryConfiguration");
    
    Element initParams = new Element("init-params");
    Element valueParam1 = new Element("value-param");
    Element nameRepoParam = new Element("name");
    nameRepoParam.addContent("repository");
    Element valueRepoParam = new Element("value");
    valueRepoParam.addContent(repoName);
    
    valueParam1.addContent(nameRepoParam);
    valueParam1.addContent(valueRepoParam);
    
    Element valueParam2 = new Element("value-param");
    Element nameWsParam = new Element("name");
    nameWsParam.addContent("systemWorkspace");
    Element valueWsParam = new Element("value");
    valueWsParam.addContent(systemWs);
    
    valueParam2.addContent(nameWsParam);
    valueParam2.addContent(valueWsParam);
    
    initParams.addContent(valueParam1);
    initParams.addContent(valueParam2);
    
    componentPlugin.addContent(name);
    componentPlugin.addContent(setMethod);
    componentPlugin.addContent(type);
    componentPlugin.addContent(initParams);
    
    externalElement.addContent(targetComponent);
    externalElement.addContent(componentPlugin);
    
    root.addContent(externalElement);
    
    XMLOutputter xmlOutputter = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
    xmlOutputter.output(doc, new FileWriter(fileDmsCommon));
  }
  
  private void addNewElement(String repoName, String wsName) throws Exception {
    J2EEServerInfo jServerInfo = new J2EEServerInfo();
    String configDir = jServerInfo.getExoConfigurationDirectory();
    String commonExtPath = configDir + "/dms-common-extend-configuration.xml";
    File configDirFile;
    configDirFile = new File(configDir);
    if (!configDirFile.exists()) {
      configDirFile.mkdir();
      File gadgets = new File(configDir + "/gadgets");
      gadgets.mkdir();
      new FileWriter(configDir + "/gadgets/key.txt");
    }
    addConfiguration(configDir + "/configuration.xml");
    addElement(commonExtPath, repoName, wsName);
  }
  
  public static class SelectActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryFormContainer uiRepoContainer = 
        event.getSource().getAncestorOfType(UIRepositoryFormContainer.class);
      UIRepositoryValueSelect uiSelect = 
        uiRepoContainer.getChild(UIPopupContainer.class).activate(UIRepositoryValueSelect.class, 500);
      uiSelect.isSetAuthentication_  = true;
      List<ClassData> datas = new ArrayList<ClassData>();
      datas.add(new ClassData(UIRepositoryForm.KEY_AUTHENTICATIONPOLICY));
      uiSelect.updateGrid(datas);
    }
  }
  public static class ShowHiddenActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm =  event.getSource();
      uiForm.ShowHidden();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupContainer.class));
    }
  }
  public static class SaveActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm = event.getSource();
      UIRepositoryFormContainer uiControl = uiForm.getAncestorOfType(UIRepositoryFormContainer.class);
      UIPopupContainer uiWizardPopup = uiControl.getChild(UIPopupContainer.class);
      uiWizardPopup.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWizardPopup);
      RepositoryEntry re = new RepositoryEntry();
      String repoName = uiForm.getUIStringInput(UIRepositoryForm.FIELD_NAME).getValue();
      RepositoryService rService = uiForm.getApplicationComponent(RepositoryService.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      for(RepositoryEntry repo : rService.getConfig().getRepositoryConfigurations()) { 
        if(repo.getName().equals(repoName) && uiForm.isAddnew_) {
          Object[] args = new Object[]{repo.getName()} ;    
          uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.repoName-exist", args));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
          return;
        }
      }
      if(!Utils.isNameValid(repoName, Utils.SPECIALCHARACTER)) {        
        Object[] args = new Object[]{repoName} ;    
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.repoName-not-alow", args));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      if (uiForm.getWorkspaceMap().isEmpty()) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.workspace-isrequire", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return; 
      }
      if (uiForm.defaulWorkspace_ == null) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.workspace-setdefault", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return; 
      }
      if (uiForm.dmsSystemWorkspace_ == null) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.workspace-setDMSsystem", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return; 
      }
      String acess = uiForm.getUIStringInput(UIRepositoryForm.FIELD_ACCESSCONTROL).getValue();
      UIFormInputSetWithAction autField = uiForm.getChildById(UIRepositoryForm.FIELD_AUTHINPUTSET);
      String authen = autField.getUIStringInput(UIRepositoryForm.FIELD_AUTHENTICATION).getValue();
      if(Utils.isNameEmpty(authen)) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.authen-isrequire", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      String security = uiForm.getUIStringInput(UIRepositoryForm.FIELD_SCURITY).getValue();
      String sessionTimeOut = uiForm.getUIStringInput(UIRepositoryForm.FIELD_SESSIONTIME).getValue();
      if(Utils.isNameEmpty(sessionTimeOut)) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.sessionTime-required", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      try {
        Long.parseLong(sessionTimeOut);
      } catch (NumberFormatException nfe) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.sessionTime-invalid", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      re.setName(repoName);
      re.setAccessControl(acess);
      re.setAuthenticationPolicy(authen);
      re.setSecurityDomain(security);
      re.setSessionTimeOut(Long.parseLong(sessionTimeOut));
      re.setDefaultWorkspaceName(uiForm.defaulWorkspace_);
      re.setSystemWorkspaceName(uiForm.defaulWorkspace_);
      re.addWorkspace(uiForm.getWorkspace(uiForm.defaulWorkspace_));
      
      DMSRepositoryConfiguration newDConfiguration = new DMSRepositoryConfiguration();
      newDConfiguration.setRepositoryName(repoName);
      newDConfiguration.setSystemWorkspace(uiForm.dmsSystemWorkspace_);
      DMSConfiguration dmsConfiguration = uiForm.getApplicationComponent(DMSConfiguration.class);
      dmsConfiguration.initNewRepo(repoName, newDConfiguration);
      
      uiForm.addNewElement(repoName, uiForm.dmsSystemWorkspace_);
      uiForm.saveRepo(re);
      
      Iterator wsPermission = uiForm.workspaceMapPermission_.keySet().iterator();
      while (wsPermission.hasNext()) {
        String workSpaceName = (String)wsPermission.next();        
        ManageableRepository manageRepository = rService.getRepository(repoName);
        Session systemSession = manageRepository.getSystemSession(workSpaceName);        
        String stringPermission = uiForm.workspaceMapPermission_.get(workSpaceName);
        ExtendedNode rootNode = (ExtendedNode)systemSession.getRootNode();
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
        try {
          rootNode.save();
        } catch (NullPointerException nullPointerException) {
          uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.not-complete-repository", null, ApplicationMessage.WARNING));
          uiWizardPopup.deActivate();
          event.getRequestContext().addUIComponentToUpdateByAjax(uiWizardPopup);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
          return;
        } catch (Exception e) {
          LOG.error("Unexpected error", e);
          return;
        }
        systemSession.save();        
        systemSession.logout();
      }      
      
      UIRepositoryControl uiRepoControl = uiForm.getAncestorOfType(UIECMAdminPortlet.class).
      findFirstComponentOfType(UIRepositoryControl.class);
      uiRepoControl.reloadValue(true, rService);
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class);    
      uiPopupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction); 
      event.getRequestContext().addUIComponentToUpdateByAjax(uiRepoControl);
    }
  }
  public static class ResetActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm = event.getSource();
      UIRepositoryFormContainer uiControl = uiForm.getAncestorOfType(UIRepositoryFormContainer.class);
      UIPopupContainer uiWizardPopup = uiControl.getChild(UIPopupContainer.class);
      uiWizardPopup.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWizardPopup); 
      RepositoryService rService = uiForm.getApplicationComponent(RepositoryService.class);
      if(uiForm.isAddnew_) uiForm.refresh(rService.getDefaultRepository().getConfiguration());
      else uiForm.refresh(rService.getRepository(uiForm.repoName_).getConfiguration());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupContainer.class));
    }
  }

  public static class AddWorkspaceActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm = event.getSource();  
      String repoName = uiForm.getUIStringInput(UIRepositoryForm.FIELD_NAME).getValue();
      String sessionTime = uiForm.getUIStringInput(UIRepositoryForm.FIELD_SESSIONTIME).getValue();
      RepositoryService rService = uiForm.getApplicationComponent(RepositoryService.class);
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      for(RepositoryEntry repo : rService.getConfig().getRepositoryConfigurations()) { 
        if(repo.getName().equals(repoName) && uiForm.isAddnew_) {
          Object[] args = new Object[]{repo.getName()} ;    
          uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.repoName-exist", args));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
          return;
        }
      }
      if(!Utils.isNameValid(repoName, Utils.SPECIALCHARACTER)) {      
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.repoName-not-alow", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      if(Utils.isNameEmpty(sessionTime)) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.sessionTime-required", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      try {
        Long.parseLong(sessionTime.trim());
      } catch (NumberFormatException nfe) {
        uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.sessionTime-invalid", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());  
        return;
      }
      UIRepositoryFormContainer uiControl = uiForm.getAncestorOfType(UIRepositoryFormContainer.class);
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIECMAdminPortlet.class).findFirstComponentOfType(UIPopupContainer.class);
      UIPopupContainer uiWorkspaceAction = uiControl.getChild(UIPopupContainer.class);
      UIWorkspaceWizardContainer uiWsContainer = uiWorkspaceAction.activate(UIWorkspaceWizardContainer.class, 700); 
      WorkspaceEntry wsdf = null;
      RepositoryEntry  repoEntry = rService.getDefaultRepository().getConfiguration();
      for(WorkspaceEntry ws : repoEntry.getWorkspaceEntries()) {
        if(ws.getName().equals(repoEntry.getDefaultWorkspaceName())) {
          wsdf = ws;
          break;
        }
      }
      uiWsContainer.initWizard(uiForm.isAddnew_, true, wsdf, true);      
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
    }
  }
  public static class CloseActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm = event.getSource();
      UIRepositoryFormContainer uiControl = uiForm.getAncestorOfType(UIRepositoryFormContainer.class);
      UIPopupContainer uiWizardPopup = uiControl.getChild(UIPopupContainer.class);
      uiWizardPopup.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWizardPopup); 
      uiForm.refresh(null);
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class);
      uiPopupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
    }
  }
  public static class EditWorkspaceActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm = event.getSource();
      String workspaceName = event.getRequestContext().getRequestParameter(OBJECTID);
      UIRepositoryFormContainer uiControl = uiForm.getAncestorOfType(UIRepositoryFormContainer.class);
      UIPopupContainer uiPopupAction = uiControl.getChild(UIPopupContainer.class);
      uiPopupAction.deActivate();
      UIWorkspaceWizardContainer uiWsContainer = uiPopupAction.activate(UIWorkspaceWizardContainer.class, 600); 
      uiWsContainer.initWizard(uiForm.isAddnew_, false, uiForm.getWorkspace(workspaceName), false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupContainer.class));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction);
    }
  }
  public static class RemoveWorkspaceActionListener extends EventListener<UIRepositoryForm>{
    public void execute(Event<UIRepositoryForm> event) throws Exception{
      UIRepositoryForm uiForm = event.getSource();
      UIRepositoryFormContainer uiControl = uiForm.getAncestorOfType(UIRepositoryFormContainer.class); 
      UIPopupContainer uiWizardPopup = uiControl.getChild(UIPopupContainer.class);
      uiWizardPopup.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWizardPopup); 
      String workspaceName = event.getRequestContext().getRequestParameter(OBJECTID);   
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      if(!uiForm.isAddnew_) {
        if(uiForm.isDefaultWorkspace(workspaceName)) {
          Object[] args = {workspaceName} ;    
          uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.cannot-delete-default-workspace", 
              args, ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          return;
        }
        RepositoryService rService = uiForm.getApplicationComponent(RepositoryService.class);
        ManageableRepository manaRepo = rService.getRepository(uiForm.repoName_);
        if(manaRepo.canRemoveWorkspace(workspaceName)) {
          manaRepo.removeWorkspace(workspaceName);
          InitialContextInitializer ic = (InitialContextInitializer)uiForm.getApplicationComponent(ExoContainer.class).
          getComponentInstanceOfType(InitialContextInitializer.class);
          if(ic != null) ic.recall();
          if(rService.getConfig().isRetainable()) {
            rService.getConfig().retain();
          }
          uiForm.workspaceMap_.clear();
          for(WorkspaceEntry ws : manaRepo.getConfiguration().getWorkspaceEntries()) {
            uiForm.workspaceMap_.put(ws.getName(), ws);
          } 
          uiForm.refreshWorkspaceList();
        }else {
          Object[] args = {workspaceName} ;    
          uiApp.addMessage(new ApplicationMessage("UIRepositoryForm.msg.cannot-delete-workspace", 
              args, ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        }
      } else {
        uiForm.workspaceMap_.remove(workspaceName);
        uiForm.getWorkspaceMapPermission().remove(workspaceName);
        if (uiForm.isDefaultWorkspace(workspaceName)) uiForm.defaulWorkspace_ = null;
        if (uiForm.isDmsSystemWorkspace(workspaceName)) uiForm.dmsSystemWorkspace_ = null;
        uiForm.refreshWorkspaceList();      
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIRepositoryFormContainer.class));  
    }
  }
  public void setAuthentication(String value) {
    UIFormInputSetWithAction autField =  getChildById(FIELD_AUTHINPUTSET);
    autField.getUIStringInput(FIELD_AUTHENTICATION).setValue(value);
  }
}
