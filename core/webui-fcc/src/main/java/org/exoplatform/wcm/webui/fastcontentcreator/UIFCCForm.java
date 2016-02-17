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
package org.exoplatform.wcm.webui.fastcontentcreator;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.selector.ComponentSelector;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UIUploadInput;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.portlet.PortletPreferences;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 25, 2009
 */
@ComponentConfigs( {
  @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "WYSIWYGRichTextMultipleInputset", events = {
    @EventConfig(listeners = UIDialogForm.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, events = {
      @EventConfig(listeners = UIFCCForm.SaveActionListener.class),
      @EventConfig(listeners = UIFCCForm.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIFCCForm.RemoveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIFCCForm.ShowComponentActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIFCCForm.RemoveReferenceActionListener.class,
      confirm = "DialogFormField.msg.confirm-delete",
      phase = Phase.DECODE) }) })
public class UIFCCForm extends UIDialogForm implements UISelectable {

  /** The Constant FIELD_TAXONOMY. */
  final static public String FIELD_TAXONOMY = "categories";

  /** The Constant POPUP_TAXONOMY. */
  final static public String POPUP_TAXONOMY = "UIPopupTaxonomy";

  /** The list taxonomy. */
  private List<String> listTaxonomy = new ArrayList<String>();

  /** The list taxonomy name. */
  private List<String> listTaxonomyName = new ArrayList<String>();

  /** The document type_. */
  private String documentType_ ;

  /** The jcr template resource resolver_. */
  private JCRResourceResolver jcrTemplateResourceResolver_ ;

  /**
   * Instantiates a new uIFCC form.
   *
   * @throws Exception the exception
   */
  public UIFCCForm() throws Exception {
    PortletPreferences preferences = UIFCCUtils.getPortletPreferences() ;
    String custom_save_button = preferences.getValue(UIFCCConstant.PREFERENCE_SAVE_BUTTON, "");
    setActions(new String[]{custom_save_button}) ;

  }
  public String event(String name) throws Exception
  {

    StringBuilder b = new StringBuilder();
    b.append("javascript:eXo.webui.UIForm.submitForm('").append(getFormId()).append("','");
    b.append("Save").append("',true)");
    return b.toString();
  }
  private String getFormId()
  {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    if (context instanceof PortletRequestContext)
    {
      return ((PortletRequestContext)context).getWindowId() + "#" + getId();
    }
    return getId();
  }
  /**
   * Gets the list taxonomy.
   *
   * @return the list taxonomy
   */
  public List<String> getListTaxonomy() {
    return listTaxonomy;
  }

  /**
   * Gets the list taxonomy name.
   *
   * @return the list taxonomy name
   */
  public List<String> getlistTaxonomyName() {
    return listTaxonomyName;
  }

  /**
   * Sets the list taxonomy.
   *
   * @param listTaxonomyNew the new list taxonomy
   */
  public void setListTaxonomy(List<String> listTaxonomyNew) {
    listTaxonomy = listTaxonomyNew;
  }

  /**
   * Sets the list taxonomy name.
   *
   * @param listTaxonomyNameNew the new list taxonomy name
   */
  public void setListTaxonomyName(List<String> listTaxonomyNameNew) {
    listTaxonomyName = listTaxonomyNameNew;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#getTemplate()
   */
  public String getTemplate() {
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String userName = Util.getPortalRequestContext().getRemoteUser() ;
    try {
      if(userName == null) {
        return templateService.getTemplatePathByAnonymous(true, documentType_);
      }
      return templateService.getTemplatePathByUser(true, documentType_, userName) ;
    } catch (Exception e) {
      UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
      Object[] arg = { documentType_ } ;
      uiApp.addMessage(new ApplicationMessage("UIFCCForm.msg.not-support", arg, ApplicationMessage.ERROR)) ;
      return null ;
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public void doSelect(String selectField, Object value) throws Exception {
    this.isUpdateSelect = true;
    UIFormInput formInput = getUIInput(selectField);
    if(formInput instanceof UIFormInputBase) {
      ((UIFormInputBase)formInput).setValue(value.toString());
    }else if(formInput instanceof UIFormMultiValueInputSet) {
      UIFormMultiValueInputSet  inputSet = (UIFormMultiValueInputSet) formInput;
      String valueTaxonomy = String.valueOf(value).trim();
      List taxonomylist = inputSet.getValue();
      if (!taxonomylist.contains(valueTaxonomy)) {
        listTaxonomy.add(valueTaxonomy);
        listTaxonomyName.add(valueTaxonomy);
        taxonomylist.add(valueTaxonomy);
      }
      inputSet.setValue(taxonomylist);
    }
    UIFCCPortlet uiContainer = getParent();
    uiContainer.removeChildById("PopupComponent");
  }

  /**
   * Gets the current node.
   *
   * @return the current node
   *
   * @throws Exception the exception
   */
  public Node getCurrentNode() throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    PortletPreferences preferences = UIFCCUtils.getPortletPreferences() ;
    Session session = WCMCoreUtils.getUserSessionProvider()
        .getSession(preferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE,
                                         ""),
                                         repositoryService.getCurrentRepository());
    return (Node) session.getItem(preferences.getValue("path", ""));
  }

  /**
   * Sets the template node.
   *
   * @param type the new template node
   */
  public void setTemplateNode(String type) { documentType_ = type ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#isEditing()
   */
  public boolean isEditing() { return false ; }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    if(jcrTemplateResourceResolver_ == null) newJCRTemplateResourceResolver() ;
    return jcrTemplateResourceResolver_;
  }

  /**
   * New jcr template resource resolver.
   */
  public void newJCRTemplateResourceResolver() {
    try {
      jcrTemplateResourceResolver_ = new JCRResourceResolver(getDMSWorkspace()) ;
    } catch(Exception e) {
      Utils.createPopupMessage(this, "UIFCCForm.msg.new-jcr-template", null, ApplicationMessage.ERROR);
    }
  }

  /**
   * Gets the dMS workspace.
   *
   * @return the dMS workspace
   */
  private String getDMSWorkspace() {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    return dmsConfiguration.getConfig().getSystemWorkspace();
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getJavascriptManager().loadScriptResource("wcm-webui-ext");
    context.getJavascriptManager().addCustomizedOnLoadScript("changeWarning();");
    super.processRender(context);
  }  

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener</code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class SaveActionListener extends EventListener<UIFCCForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCForm> event) throws Exception {
      UIFCCForm fastContentCreatorForm = event.getSource() ;
      UIApplication uiApp = fastContentCreatorForm.getAncestorOfType(UIApplication.class);
      PortletPreferences preferences = UIFCCUtils.getPortletPreferences();
      String preferencePath = preferences.getValue(UIFCCConstant.PREFERENCE_PATH, "") ;
      String preferenceType = preferences.getValue(UIFCCConstant.PREFERENCE_TYPE, "") ;
      String preferenceWorkspace = preferences.getValue(UIFCCConstant.PREFERENCE_WORKSPACE, "") ;

      RepositoryService repositoryService  = fastContentCreatorForm.getApplicationComponent(RepositoryService.class);
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      Session session = sessionProvider.getSession(preferenceWorkspace, repositoryService.getCurrentRepository());
      CmsService cmsService = fastContentCreatorForm.getApplicationComponent(CmsService.class) ;
      TaxonomyService taxonomyService = fastContentCreatorForm.getApplicationComponent(TaxonomyService.class);
      boolean hasCategories = false;
      StringBuffer sb = new StringBuffer();
      String categoriesPath = "";
      String[] categoriesPathList = null;
      List inputs = fastContentCreatorForm.getChildren();
      for (int i = 0; i < inputs.size(); i++) {
        UIFormInput input = (UIFormInput) inputs.get(i);
        if((input.getName() != null) && input.getName().equals("name")) {          
          String valueName = input.getValue().toString().trim();
          if (!org.exoplatform.ecm.webui.utils.Utils.isNameValid(valueName, 
                                                                 org.exoplatform.ecm.webui.utils.Utils.SPECIALCHARACTER)) {
            uiApp.addMessage(new ApplicationMessage("UIFCCForm.msg.name-not-allowed", null, ApplicationMessage.WARNING));

            return;
          }
        }
      }
      if(fastContentCreatorForm.isReference) {
        UIFormMultiValueInputSet uiSet = fastContentCreatorForm.getChild(UIFormMultiValueInputSet.class);
        if((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals("categories")) {
          hasCategories = true;
          List<UIComponent> listChildren = uiSet.getChildren();
          for (UIComponent component : listChildren) {
            UIFormStringInput uiStringInput = (UIFormStringInput)component;
            if(uiStringInput.getValue() != null) {
              String value = uiStringInput.getValue().trim();
              sb.append(value).append(",");
            }
          }
          categoriesPath = sb.toString();
          if (categoriesPath.endsWith(",")) categoriesPath = categoriesPath.substring(0, categoriesPath.length()-1).trim();
          categoriesPathList = categoriesPath.split(",");
          if ((categoriesPathList == null) || (categoriesPathList.length == 0)) {
            uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null,
                                                    ApplicationMessage.WARNING));

            return;
          }

          for(String categoryPath : categoriesPathList) {
            if((categoryPath != null) && (categoryPath.trim().length() > 0)){
              if (categoryPath.indexOf("/") == -1) {
                uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null,
                                                        ApplicationMessage.WARNING));

                return;
              }
            }
          }
        }
      }
      Map inputProperties = DialogFormUtil.prepareMap(fastContentCreatorForm.getChildren(),
                                                      fastContentCreatorForm.getInputProperties(),
                                                      fastContentCreatorForm.getInputOptions());
      Node homeNode = null;
      Node newNode = null ;
      try {
        homeNode = (Node) session.getItem(preferencePath);
      } catch (AccessDeniedException ade){
        Object[] args = { preferencePath } ;
        uiApp.addMessage(new ApplicationMessage("UIFCCForm.msg.access-denied", args,
                                                ApplicationMessage.WARNING)) ;

        return;
      } catch(PathNotFoundException pnfe) {
        Object[] args = { preferencePath } ;
        uiApp.addMessage(new ApplicationMessage("UIFCCForm.msg.path-not-found", args,
                                                ApplicationMessage.WARNING)) ;

        return;
      }
      try {
        String addedPath = cmsService.storeNode(preferenceType, homeNode, inputProperties, true);
        homeNode.getSession().save() ;
        int index = 0;
        if(homeNode.hasNode(addedPath.substring(addedPath.lastIndexOf("/") + 1))) {
          newNode = homeNode.getNode(addedPath.substring(addedPath.lastIndexOf("/") + 1));
          if (hasCategories && (newNode != null) && ((categoriesPath != null) && (categoriesPath.length() > 0))){
            for(String categoryPath : categoriesPathList) {
              index = categoryPath.indexOf("/");
              taxonomyService.addCategory(newNode, categoryPath.substring(0, index), categoryPath.substring(index + 1));
            }
          }
          event.getRequestContext().setAttribute("nodePath", newNode.getPath());
        }
        fastContentCreatorForm.reset() ;
        fastContentCreatorForm.setIsResetForm(true) ;
        for(UIComponent uiChild : fastContentCreatorForm.getChildren()) {
          if(uiChild instanceof UIFormMultiValueInputSet) {
            ((UIFormMultiValueInputSet)uiChild).setValue(new ArrayList<Value>()) ;
          } else if(uiChild instanceof UIUploadInput) {
            UploadService uploadService = fastContentCreatorForm.getApplicationComponent(UploadService.class) ;
            uploadService.removeUploadResource(((UIUploadInput)uiChild).getUploadIds()[0]) ;
          }
        }
        session.save() ;
        session.refresh(false) ;
        homeNode.getSession().refresh(false) ;

        boolean preferenceIsRedirect = Boolean.parseBoolean(preferences.getValue(UIFCCConstant.PREFERENCE_IS_REDIRECT, "")) ;
        String preferenceRedirectPath = preferences.getValue(UIFCCConstant.PREFERENCE_REDIRECT_PATH, "") ;
        if (preferenceIsRedirect && preferenceRedirectPath != null) {
          RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
          requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + preferenceRedirectPath
          + "');");
        } else {
          String saveMessage = preferences.getValue(UIFCCConstant.PREFERENCE_SAVE_MESSAGE, "") ;
          if (saveMessage == null) saveMessage = "saved-successfully";
          Object[] args = { saveMessage } ;
          ApplicationMessage appMessage = new ApplicationMessage("UIFCCForm.msg.saved-successfully", args);
          appMessage.setArgsLocalized(false);
          uiApp.addMessage(appMessage) ;

        }
        event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorForm.getParent()) ;
      } catch (AccessControlException ace) {
        throw new AccessDeniedException(ace.getMessage());
      } catch(VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UIFCCForm.msg.in-versioning", null,
                                                ApplicationMessage.WARNING)) ;

        return;
      } catch(AccessDeniedException e) {
        Object[] args = { preferencePath } ;
        String key = "UIFCCForm.msg.access-denied" ;
        uiApp.addMessage(new ApplicationMessage(key, args, ApplicationMessage.WARNING)) ;

        return;
      } catch(LockException lock) {
        Object[] args = { preferencePath } ;
        String key = "UIFCCForm.msg.node-locked" ;
        uiApp.addMessage(new ApplicationMessage(key, args, ApplicationMessage.WARNING)) ;

        return;
      } catch(ItemExistsException item) {
        Object[] args = { preferencePath } ;
        String key = "UIFCCForm.msg.node-isExist" ;
        uiApp.addMessage(new ApplicationMessage(key, args, ApplicationMessage.WARNING)) ;

      }
    }
  }

  /**
   * The listener interface for receiving showComponentAction events.
   * The class that is interested in processing a showComponentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addShowComponentActionListener</code> method. When
   * the showComponentAction event occurs, that object's appropriate
   * method is invoked.
   */
  @SuppressWarnings("unchecked")
  static public class ShowComponentActionListener extends EventListener<UIFCCForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCForm> event) throws Exception {
      UIFCCForm fastContentCreatorForm = event.getSource() ;
      UIFCCPortlet fastContentCreatorPortlet = fastContentCreatorForm.getParent() ;
      fastContentCreatorForm.isShowingComponent = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      Map fieldPropertiesMap = fastContentCreatorForm.componentSelectors.get(fieldName) ;
      String classPath = (String)fieldPropertiesMap.get("selectorClass") ;
      String rootPath = (String)fieldPropertiesMap.get("rootPath") ;
      ClassLoader cl = Thread.currentThread().getContextClassLoader() ;
      Class clazz = Class.forName(classPath, true, cl) ;
      UIComponent component = fastContentCreatorPortlet.createUIComponent(clazz, null, null);
      NodeHierarchyCreator nodeHierarchyCreator = fastContentCreatorForm.getApplicationComponent(NodeHierarchyCreator.class);
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      String selectorParams = (String)fieldPropertiesMap.get("selectorParams") ;
      if(component instanceof UIOneNodePathSelector) {
        String repositoryName = UIFCCUtils.getPreferenceRepository() ;
        String wsFieldName = (String)fieldPropertiesMap.get("workspaceField") ;
        String wsName = "";
        if(wsFieldName != null && wsFieldName.length() > 0) {
          wsName = (String)fastContentCreatorForm.<UIFormInputBase>getUIInput(wsFieldName).getValue() ;
          ((UIOneNodePathSelector)component).setIsDisable(wsName, true) ;
        }
        if(selectorParams != null) {
          String[] arrParams = selectorParams.split(",") ;
          if(arrParams.length == 4) {
            ((UIOneNodePathSelector)component).setAcceptedNodeTypesInPathPanel(new String[] {"nt:file"}) ;
            wsName = arrParams[1];
            rootPath = arrParams[2];
            ((UIOneNodePathSelector)component).setIsDisable(wsName, true) ;
            if(arrParams[3].indexOf(";") > -1) {
              ((UIOneNodePathSelector)component).setAcceptedMimeTypes(arrParams[3].split(";")) ;
            } else {
              ((UIOneNodePathSelector)component).setAcceptedMimeTypes(new String[] {arrParams[3]}) ;
            }
          }
        }
        if(rootPath == null) rootPath = "/";
        ((UIOneNodePathSelector)component).setRootNodeLocation(repositoryName, wsName, rootPath) ;
        ((UIOneNodePathSelector)component).setShowRootPathSelect(true);
        ((UIOneNodePathSelector)component).init(sessionProvider);
      } else if (component instanceof UIOneTaxonomySelector) {
        String workspaceName = fastContentCreatorForm.getDMSWorkspace();
        ((UIOneTaxonomySelector)component).setIsDisable(workspaceName, false);
        String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
        RepositoryService repositoryService  = fastContentCreatorForm.getApplicationComponent(RepositoryService.class);
        Session session = sessionProvider.getSession(workspaceName,
                                                     repositoryService.getCurrentRepository());
        Node rootTree = (Node) session.getItem(rootTreePath);
        NodeIterator childrenIterator = rootTree.getNodes();
        while (childrenIterator.hasNext()) {
          Node childNode = childrenIterator.nextNode();
          rootTreePath = childNode.getPath();
          break;
        }

        ((UIOneTaxonomySelector) component).setRootNodeLocation(fastContentCreatorForm.repositoryName,
                                                                workspaceName,
                                                                rootTreePath);
        ((UIOneTaxonomySelector)component).init(sessionProvider);
      }
      Utils.createPopupWindow(fastContentCreatorForm, component, UIFCCConstant.TAXONOMY_POPUP_WINDOW, 640);
      String param = "returnField=" + fieldName ;
      String[] params = selectorParams == null ? new String[] { param } : new String[] { param,
          "selectorParams=" + selectorParams };
      ((ComponentSelector)component).setSourceComponent(fastContentCreatorForm, params) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorPortlet) ;
    }
  }

  /**
   * The listener interface for receiving removeReferenceAction events.
   * The class that is interested in processing a removeReferenceAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRemoveReferenceActionListener</code> method. When
   * the removeReferenceAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class RemoveReferenceActionListener extends EventListener<UIFCCForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCForm> event) throws Exception {
      UIFCCForm fastContentCreatorForm = event.getSource() ;
      fastContentCreatorForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      fastContentCreatorForm.getUIStringInput(fieldName).setValue(null) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorForm.getParent()) ;
    }
  }

  /**
   * The listener interface for receiving addAction events.
   * The class that is interested in processing a addAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddActionListener</code> method. When
   * the addAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class AddActionListener extends EventListener<UIFCCForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCForm> event) throws Exception {
      UIFCCForm uiFCCForm = event.getSource();
      UIFCCPortlet fastContentCreatorPortlet = uiFCCForm.getParent();
      String clickedField = event.getRequestContext().getRequestParameter(OBJECTID);
      if (uiFCCForm.isReference) {
        UIFormMultiValueInputSet uiSet = uiFCCForm.getChildById(FIELD_TAXONOMY);
        if((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals(FIELD_TAXONOMY)) {
          if ((clickedField != null) && (clickedField.equals(FIELD_TAXONOMY))){
            NodeHierarchyCreator nodeHierarchyCreator = uiFCCForm.getApplicationComponent(NodeHierarchyCreator.class);
            if(uiSet.getValue().size() == 0) uiSet.setValue(new ArrayList<Value>());
            String workspaceName = uiFCCForm.getDMSWorkspace();
            UIOneTaxonomySelector uiOneTaxonomySelector = uiFCCForm.createUIComponent(UIOneTaxonomySelector.class,
                                                                                      null,
                                                                                      null);
            uiOneTaxonomySelector.setIsDisable(workspaceName, false);
            String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
            RepositoryService repositoryService  = uiFCCForm.getApplicationComponent(RepositoryService.class);
            SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
            Session session = sessionProvider.getSession(workspaceName,
                                                         repositoryService.getCurrentRepository());
            Node rootTree = (Node) session.getItem(rootTreePath);
            NodeIterator childrenIterator = rootTree.getNodes();
            while (childrenIterator.hasNext()) {
              Node childNode = childrenIterator.nextNode();
              rootTreePath = childNode.getPath();
              break;
            }
            uiOneTaxonomySelector.setRootNodeLocation(uiFCCForm.repositoryName, workspaceName, rootTreePath);
            uiOneTaxonomySelector.init(WCMCoreUtils.getUserSessionProvider());
            String param = "returnField=" + FIELD_TAXONOMY;
            uiOneTaxonomySelector.setSourceComponent(uiFCCForm, new String[]{param});
            Utils.createPopupWindow(uiFCCForm, uiOneTaxonomySelector, UIFCCConstant.TAXONOMY_POPUP_WINDOW, 640);
          }
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(fastContentCreatorPortlet);
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiFCCForm.getParent());
      }
    }
  }

  /**
   * The listener interface for receiving removeAction events.
   * The class that is interested in processing a removeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRemoveActionListener</code> method. When
   * the removeAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class RemoveActionListener extends EventListener<UIFCCForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIFCCForm> event) throws Exception {
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getParent()) ;
    }
  }
}
