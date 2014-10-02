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

import java.io.Writer;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
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

@ComponentConfigs( {
  @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "WYSIWYGRichTextMultipleInputset", events = {
    @EventConfig(listeners = UIDialogForm.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(lifecycle = UIFormLifecycle.class, events = {
      @EventConfig(listeners = UIDocumentForm.SaveActionListener.class),
      @EventConfig(listeners = UIDocumentForm.SaveAndCloseActionListener.class),
      @EventConfig(listeners = UIDocumentForm.CloseActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.ChangeTypeActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.RemoveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.ShowComponentActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIDocumentForm.RemoveReferenceActionListener.class,
      confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE),
      @EventConfig(listeners = DialogFormActionListeners.RemoveDataActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = DialogFormActionListeners.ChangeTabActionListener.class, phase = Phase.DECODE) }) })

public class UIDocumentForm extends UIDialogForm implements UIPopupComponent, UISelectable {

  final static public String FIELD_TAXONOMY = "categories";
  final static public String POPUP_TAXONOMY = "PopupComponent";
  private List<String> listTaxonomyName = new ArrayList<String>();
  private boolean canChangeType = true;
  private static final Log LOG  = ExoLogger.getLogger(UIDocumentForm.class.getName());

  public boolean isCanChangeType() {
    return canChangeType;
  }

  public void setCanChangeType(boolean canChangeType) {
    this.canChangeType = canChangeType;
  }

  public UIDocumentForm() throws Exception {
    setActions(new String[]{"Save", "SaveAndClose", "Close"});
  }

  private String getChangeTypeActionLink () throws Exception {
    if (!isAddNew || !canChangeType) return "";

    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();

    String action = "ChangeType";
    String strChangeTypeLabel = res.getString(getName() + ".action." + action);
    StringBuilder link = new StringBuilder();
    link.append("<a onclick=\"")
    .append(event(action))
    .append("\" class=\"changeTypeLink\">(")
    .append(strChangeTypeLabel)
    .append(")</a>");
    return link.toString();
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
    DMSConfiguration dmsConfig = getApplicationComponent(DMSConfiguration.class);
    return dmsConfig.getConfig().getSystemWorkspace();
  }

  public Node getRootPathTaxonomy(Node node) throws Exception {
    try {
      TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
      List<Node> allTaxonomyTrees = taxonomyService.getAllTaxonomyTrees();
      for (Node taxonomyTree : allTaxonomyTrees) {
        if (node.getPath().startsWith(taxonomyTree.getPath())) return taxonomyTree;
      }
      return null;
    } catch (AccessDeniedException accessDeniedException) {
      return null;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
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
      if (contentType != null && contentType.length() > 0 && userName != null & userName.length() > 0) {
        return templateService.getTemplatePathByUser(true, contentType, userName);
      }
      return null;
    } catch (AccessControlException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("AccessControlException: user [" + userName
                  + "] does not have access to the template for content type [" + contentType
                  + "] in repository + [" + repositoryName + "]");
      }
      return null;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      UIApplication uiApp = getAncestorOfType(UIApplication.class);
      Object[] arg = { contentType };
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-support", arg,
                                              ApplicationMessage.ERROR));
      return null;
    }
  }

  private String getTemplateLabel() throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    return templateService.getTemplateLabel(contentType);
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    context.getJavascriptManager().
    require("SHARED/uiDocumentForm", "uiDocumentForm").
    addScripts("uiDocumentForm.UIDocForm.UpdateGUI();").
    addScripts("uiDocumentForm.UIDocForm.AutoFocus();");

    context.getJavascriptManager().loadScriptResource("wcm-webui-ext");
    context.getJavascriptManager().addCustomizedOnLoadScript("changeWarning();");
    super.processRender(context);
  }

  public void processRenderAction() throws Exception {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    Writer writer = context.getWriter();
    writer.append("<h5 class=\"title uiDialogAction clearfix\" >");
    writer.append("<div class=\"dialogAction pull-right\">");
    String[] listAction = getActions();
    String contextID = "UIDocumentForm_" + System.currentTimeMillis();
    String actionLabel;
    String link;
    int count = 0;
    for (String action : listAction) {
      String btn = (count++ == 0) ? "btn btn-primary" : "btn";
      try {
        actionLabel = res.getString(getName() + ".action." + action);
      } catch (MissingResourceException e) {
        actionLabel = action;
      }
      link = event(action);
      writer.append("<button type=\"button\" ")
      .append("onclick=\"")
      .append(link)
      .append("\" class=\"" + btn  +"\">")
      .append(actionLabel)
      .append("</button>");
    }
    String fullscreen = res.getString(getName() + ".tooltip.FullScreen");
    writer.append("<a class=\"actionIcon\" onclick='eXo.webui.UIDocForm.FullScreenToggle(this); return false;'><i ")
    .append("title=\"").append(fullscreen)
    .append("\" id=\"")
    .append(contextID)
    .append("\" class=\"uiIconEcmsExpand uiIconEcmsLightGray\"></i></a>");

    writer.append("</div>");
    writer.append("<span class='uiDialogTitle'>" + ContentReader.getXSSCompatibilityContent(getTemplateLabel()) + " " + getChangeTypeActionLink () + "</span>");
    writer.append("</h5>");
    context.getJavascriptManager().loadScriptResource("uiDocumentForm");
    context.getJavascriptManager().addCustomizedOnLoadScript("eXo.webui.UIDocForm.initFullScreenStatus(\"" + contextID + "\");");
  }

  @SuppressWarnings("unused")
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public void activate() {}
  public void deActivate() {}

  public Node getCurrentNode() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
  }

  public String getLastModifiedDate() throws Exception {
    return getLastModifiedDate(getCurrentNode());
  }

  public synchronized void renderField(String name) throws Exception {
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

  private synchronized List<String> getAddedListCategory(List<String> taxonomyList, List<String> existingList) {
    List<String> addedList = new ArrayList<String>();
    for(String addedCategory : taxonomyList) {
      if(!existingList.contains(addedCategory)) addedList.add(addedCategory);
    }
    return addedList;
  }

  private synchronized List<String> getRemovedListCategory(List<String> taxonomyList, List<String> existingList) {
    List<String> removedList = new ArrayList<String>();
    for(String existedCategory : existingList) {
      if(!taxonomyList.contains(existedCategory)) removedList.add(existedCategory);
    }
    return removedList;
  }

  public static Node saveDocument (Event <UIDocumentForm> event) throws Exception {
    UIDocumentForm documentForm = event.getSource();
    UIJCRExplorer uiExplorer = documentForm.getAncestorOfType(UIJCRExplorer.class);
    List inputs = documentForm.getChildren();
    UIApplication uiApp = documentForm.getAncestorOfType(UIApplication.class);
    boolean hasCategories = false;
    String categoriesPath = "";
    TaxonomyService taxonomyService = documentForm.getApplicationComponent(TaxonomyService.class);
    if (documentForm.isAddNew()) {
      for (int i = 0; i < inputs.size(); i++) {
        UIFormInput input = (UIFormInput) inputs.get(i);
        if ((input.getName() != null) && input.getName().equals("name")) {
          String valueName = input.getValue().toString();
          if (!Utils.isNameValid(valueName, Utils.SPECIALCHARACTER)) {
            uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-not-allowed", null,
                                                    ApplicationMessage.WARNING));

            return null;
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
          categoriesPath = categoriesPath.concat(category).concat(",");
        }

        if (listTaxonomy != null && listTaxonomy.size() > 0) {
          try {
            for (String categoryPath : listTaxonomy) {
              index = categoryPath.indexOf("/");
              if (index < 0) {
                taxonomyService.getTaxonomyTree(categoryPath);
              } else {
                taxonomyService.getTaxonomyTree(categoryPath.substring(0, index))
                .getNode(categoryPath.substring(index + 1));
              }
            }
          } catch (Exception e) {
            uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories",
                                                    null,
                                                    ApplicationMessage.WARNING));

            return null;
          }
        }
      }
    }
    Map inputProperties = DialogFormUtil.prepareMap(inputs, documentForm.getInputProperties(), documentForm.getInputOptions());
    Node newNode = null;
    String nodeType;
    Node homeNode;
    Node currentNode = uiExplorer.getCurrentNode();
    if(documentForm.isAddNew()) {
      UIDocumentFormController uiDFController = documentForm.getParent();
      homeNode = currentNode;
      nodeType = uiDFController.getChild(UIDocumentForm.class).getContentType();
      if(homeNode.isLocked()) {
        homeNode.getSession().addLockToken(LockUtil.getLockToken(homeNode));
      }
    } else {
      Node documentNode = documentForm.getNode();
      for (String removedNode : documentForm.getRemovedNodes()) {
        documentNode.getNode(removedNode).remove();
      }
      homeNode = documentNode.getParent();
      nodeType = documentNode.getPrimaryNodeType().getName();
      if(documentNode.isLocked()) {
        String lockToken = LockUtil.getLockToken(documentNode);
        if(lockToken != null && !lockToken.isEmpty()) {
          documentNode.getSession().addLockToken(lockToken);
        }
      }
    }
    try {
      CmsService cmsService = documentForm.getApplicationComponent(CmsService.class);
      cmsService.getPreProperties().clear();
      String addedPath = cmsService.storeNode(nodeType, homeNode, inputProperties, documentForm.isAddNew());
      try {
        newNode = (Node)homeNode.getSession().getItem(addedPath);
        //Broadcast the add file activity
        if(documentForm.isAddNew()) {
          ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
          ActivityCommonService   activityService = WCMCoreUtils.getService(ActivityCommonService.class);
          if (newNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) 
              && activityService.isBroadcastNTFileEvents(newNode)) {
            listenerService.broadcast(ActivityCommonService.FILE_CREATED_ACTIVITY, null, newNode);
            newNode.getSession().save();
          } else if(activityService.isAcceptedNode(newNode)) {
            listenerService.broadcast(ActivityCommonService.NODE_CREATED_ACTIVITY, null, newNode);
            newNode.getSession().save();
          }
        }

        if(newNode.isLocked()) {
          newNode.getSession().addLockToken(LockUtil.getLockToken(newNode));
        }
        List<Node> listTaxonomyTrees = taxonomyService.getAllTaxonomyTrees();
        List<Node> listExistedTaxonomy = taxonomyService.getAllCategories(newNode);
        List<String> listExistingTaxonomy = new ArrayList<String>();

        for (Node existedTaxonomy : listExistedTaxonomy) {
          for (Node taxonomyTrees : listTaxonomyTrees) {
            if (existedTaxonomy.getPath().contains(taxonomyTrees.getPath())) {
              listExistingTaxonomy.add(taxonomyTrees.getName()
                                       + existedTaxonomy.getPath().substring(taxonomyTrees.getPath().length()));
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

            } catch (Exception e) {
              continue;
            }
          }
        } else {
          List<Value> vals = new ArrayList<Value>();
          if (newNode.hasProperty("exo:category")) newNode.setProperty("exo:category", vals.toArray(new Value[vals.size()]));
          newNode.save();
        }
        uiExplorer.setCurrentPath(newNode.getPath());
        uiExplorer.setWorkspaceName(newNode.getSession().getWorkspace().getName());
        uiExplorer.refreshExplorer(newNode, true);
        uiExplorer.updateAjax(event);
        return newNode;
      } catch(Exception e) {
        uiExplorer.getSession().refresh(false);
        uiExplorer.updateAjax(event);
      }
      uiExplorer.getSession().save();
      uiExplorer.updateAjax(event);
    } catch (AccessControlException ace) {
      throw new AccessDeniedException(ace.getMessage());
    } catch(VersionException ve) {
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.in-versioning", null,
                                              ApplicationMessage.WARNING));

      return null;
    } catch(ItemNotFoundException item) {
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.item-not-found", null,
                                              ApplicationMessage.WARNING));

      return null;
    } catch(AccessDeniedException accessDeniedException) {
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.repository-exception-permission", null,
                                              ApplicationMessage.WARNING));

      return null;
    } catch(ItemExistsException existedex) {
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.not-allowed-same-name-sibling",
                                              null,
                                              ApplicationMessage.WARNING));

      return null;
    } catch(ConstraintViolationException constraintViolationException) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error occurrs", constraintViolationException);
      }
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.constraintviolation-exception",
                                              null,
                                              ApplicationMessage.WARNING));

      return null;
    } catch(RepositoryException repo) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error occurrs", repo);
      }
      uiApp.addMessage(new ApplicationMessage("UIDocumentForm.msg.repository-exception", null, ApplicationMessage.WARNING));

      return null;
    } catch(NumberFormatException nume) {
      String key = "UIDocumentForm.msg.numberformat-exception";
      uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));

      return null;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error occurs", e);
      }
      String key = "UIDocumentForm.msg.cannot-save";
      uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));

      return null;
    } finally {
      documentForm.releaseLock();
    }
    return null;
  }

  public static void closeForm (Event<UIDocumentForm> event) throws Exception {
    UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
    if(uiExplorer != null) {
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      event.getSource().releaseLock();
      if (uiDocumentWorkspace.getChild(UIDocumentFormController.class) != null) {
        uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
      } else
        uiExplorer.cancelAction();
      uiExplorer.updateAjax(event);
    }
  }

  static  public class SaveActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm documentForm = event.getSource();
      synchronized (documentForm) {
        UIJCRExplorer uiExplorer = documentForm.getAncestorOfType(UIJCRExplorer.class);
        UIApplication uiApp = documentForm.getAncestorOfType(UIApplication.class);

        Node newNode = UIDocumentForm.saveDocument(event);
        if (newNode != null) {
          event.getRequestContext().setAttribute("nodePath",newNode.getPath());
          UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
          UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
          uiDocumentWorkspace.removeChild(UIDocumentFormController.class);
          documentForm.setIsUpdateSelect(false);
          EditDocumentActionComponent.editDocument(event, null, uiExplorer, uiExplorer, uiExplorer.getCurrentNode(), uiApp);
        }
      }
    }
  }

  static public class ShowComponentActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm uiForm = event.getSource();
      UIDocumentFormController uiContainer = uiForm.getParent();
      uiForm.isShowingComponent = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      Map fieldPropertiesMap = uiForm.componentSelectors.get(fieldName);

      String classPath = (String)fieldPropertiesMap.get("selectorClass");
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class clazz = Class.forName(classPath, true, cl);
      String rootPath = (String)fieldPropertiesMap.get("rootPath");
      UIComponent uiComp = uiContainer.createUIComponent(clazz, null, null);
      String selectorParams = (String)fieldPropertiesMap.get("selectorParams");
      UIJCRExplorer explorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      if (uiComp instanceof UIOneNodePathSelector) {
        String repositoryName = explorer.getRepositoryName();
        SessionProvider provider = explorer.getSessionProvider();
        String wsFieldName = (String) fieldPropertiesMap.get("workspaceField");
        String wsName = "";
        if (wsFieldName != null && wsFieldName.length() > 0) {
          if (uiForm.<UIFormInputBase> getUIInput(wsFieldName) != null) {
            wsName = (String) uiForm.<UIFormInputBase> getUIInput(wsFieldName).getValue();
            ((UIOneNodePathSelector) uiComp).setIsDisable(wsName, true);
          } else {
            wsName = explorer.getCurrentWorkspace();
            ((UIOneNodePathSelector) uiComp).setIsDisable(wsName, false);
          }
        }
        if (selectorParams != null) {
          String[] arrParams = selectorParams.split(",");
          if (arrParams.length == 4) {
            ((UIOneNodePathSelector) uiComp).setAcceptedNodeTypesInPathPanel(new String[] {
                Utils.NT_FILE, Utils.NT_FOLDER, Utils.NT_UNSTRUCTURED, Utils.EXO_TAXONOMY });
            wsName = arrParams[1];
            rootPath = arrParams[2];
            ((UIOneNodePathSelector) uiComp).setIsDisable(wsName, true);
            if (arrParams[3].indexOf(";") > -1) {
              ((UIOneNodePathSelector) uiComp).setAcceptedMimeTypes(arrParams[3].split(";"));
            } else {
              ((UIOneNodePathSelector) uiComp).setAcceptedMimeTypes(new String[] { arrParams[3] });
            }
          }
        }
        if (rootPath == null)
          rootPath = "/";
        ((UIOneNodePathSelector) uiComp).setRootNodeLocation(repositoryName, wsName, rootPath);
        ((UIOneNodePathSelector) uiComp).setShowRootPathSelect(true);
        ((UIOneNodePathSelector) uiComp).init(provider);
      } else if (uiComp instanceof UIOneTaxonomySelector) {
        NodeHierarchyCreator nodeHierarchyCreator = uiForm.getApplicationComponent(NodeHierarchyCreator.class);
        String workspaceName = uiForm.getDMSWorkspace();
        ((UIOneTaxonomySelector) uiComp).setIsDisable(workspaceName, false);
        String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
        Session session = explorer.getSessionByWorkspace(workspaceName);
        Node rootTree = (Node) session.getItem(rootTreePath);
        NodeIterator childrenIterator = rootTree.getNodes();
        while (childrenIterator.hasNext()) {
          Node childNode = childrenIterator.nextNode();
          rootTreePath = childNode.getPath();
          break;
        }

        ((UIOneTaxonomySelector) uiComp).setRootNodeLocation(uiForm.repositoryName,
                                                             workspaceName,
                                                             rootTreePath);
        ((UIOneTaxonomySelector) uiComp).init(WCMCoreUtils.getSystemSessionProvider());
      }
      uiContainer.initPopup(uiComp);
      String param = "returnField=" + fieldName;
      String[] params = selectorParams == null ? new String[] { param } : new String[] { param,
          "selectorParams=" + selectorParams };
      ((ComponentSelector) uiComp).setSourceComponent(uiForm, params);
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
      UIDocumentForm.closeForm(event);
    }
  }

  static  public class ChangeTypeActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      UIDocumentForm uiDocumentForm = event.getSource();
      UIDocumentFormController uiDCFormController = uiDocumentForm.getParent();
      UISelectDocumentForm uiSelectForm = uiDCFormController.getChild(UISelectDocumentForm.class);
      uiSelectForm.setRendered(true);
      uiDocumentForm.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDCFormController);
    }
  }

  static  public class SaveAndCloseActionListener extends EventListener<UIDocumentForm> {
    public void execute(Event<UIDocumentForm> event) throws Exception {
      Node newNode = UIDocumentForm.saveDocument(event);
      if (newNode != null) {
        event.getRequestContext().setAttribute("nodePath",newNode.getPath());
      }
      UIDocumentForm.closeForm(event);
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
              if(uiSet.getValue().size() == 0) uiSet.setValue(new ArrayList<Value>());
              UIOneTaxonomySelector uiOneTaxonomySelector =
                  uiFormController.createUIComponent(UIOneTaxonomySelector.class, null, null);
              TaxonomyService taxonomyService = uiDocumentForm.getApplicationComponent(TaxonomyService.class);
              List<Node> lstTaxonomyTree = taxonomyService.getAllTaxonomyTrees();
              if (lstTaxonomyTree.size() == 0) throw new AccessDeniedException();
              String workspaceName = lstTaxonomyTree.get(0).getSession().getWorkspace().getName();
              uiOneTaxonomySelector.setIsDisable(workspaceName, false);
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

          return;
        } catch (Exception e) {
          JCRExceptionManager.process(uiApp, e);

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
        if (LOG.isWarnEnabled()) {
          LOG.warn(ne.getMessage());
        }
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
    }
  }
}
