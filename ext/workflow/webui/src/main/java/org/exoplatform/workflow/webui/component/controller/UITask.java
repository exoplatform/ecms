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
package org.exoplatform.workflow.webui.component.controller;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.workflow.Form;
import org.exoplatform.services.workflow.Process;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.WorkflowFormsService;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.UIFormUploadInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.wysiwyg.UIFormWYSIWYGInput;
import org.exoplatform.workflow.webui.component.BJARResourceResolver;
import org.exoplatform.workflow.webui.component.InputInfo;
import org.exoplatform.workflow.webui.component.UISelectable;
import org.exoplatform.workflow.webui.component.VariableMaps;
import org.exoplatform.workflow.webui.utils.LockUtil;
import org.exoplatform.workflow.webui.utils.Utils;
/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 9, 2009
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "classpath:templates/controller/UITask.gtmpl",
    events = {
        @EventConfig(listeners = UITask.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UITask.StartProcessActionListener.class),
        @EventConfig(listeners = UITask.EndOfStateActionListener.class),
        @EventConfig(listeners = UITask.TransitionActionListener.class),
        @EventConfig(listeners = UITask.SelectUserActionListener.class, phase = Phase.DECODE)

    }
)
public class UITask extends UIForm implements UISelectable {

  public static final String MANAGE_TRANSITION = "manageTransition";
  private static final String TEXT = "text";
  private static final String TEXTAREA = "textarea";
  private static final String WYSIWYG = "wysiwyg";
  private static final String DATE = "date";
  private static final String DATE_TIME = "datetime";
  private static final String SELECT = "select";
  private static final String UPLOAD = "upload";
  private static final String CHECK_BOX = "checkbox";
  private static final String RADIO_BOX = "radiobox";
  private static final String NODE_VIEW = "nodeview";
  private static final String NODE_EDIT = "nodeedit";
  private static final String LABEL_ENCODING = ".label";
  private static final String NODE_PATH_VARIABLE = "nodePath";
  private static final String WORKSPACE_VARIABLE = "srcWorkspace";
  private static final String REPOSITORY_VARIABLE = "repository";
  private static final String DEST_PATH = "exo:destPath";
  private static final String DEST_WORKSPACE = "exo:destWorkspace";

  private static final String DELEGATE_FIELD = "delegator";
  private Form form;
  private boolean isStart_;
  private String identification_;
  private WorkflowServiceContainer serviceContainer;
  private WorkflowFormsService formsService;
  private RepositoryService jcrService;
  private List<InputInfo> inputInfo_;

  private static final Log LOG = ExoLogger.getExoLogger(UITask.class);

  public UITask() {
    serviceContainer = getApplicationComponent(WorkflowServiceContainer.class);
    formsService = getApplicationComponent(WorkflowFormsService.class);
    jcrService = getApplicationComponent(RepositoryService.class);
    inputInfo_ = new ArrayList<InputInfo>();
  }

  public String getTemplate() {
    if(isCustomizedView()) {
        return getIdentification() + ":/" + getCustomizedView();
    }
    return getComponentConfig().getTemplate();
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    if(isCustomizedView()) return new BJARResourceResolver(serviceContainer);
    return super.getTemplateResourceResolver(context, getComponentConfig().getTemplate());
  }

  public String getManageTransition() { return MANAGE_TRANSITION; }

  public String getStateImageURL() {
    try {
      Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      if (isStart()) {
        Process process = serviceContainer.getProcess(identification_);
        form = formsService.getForm(identification_, process.getStartStateName(), locale);
      } else {
        Task task = serviceContainer.getTask(identification_);
        form = formsService.getForm(task.getProcessId(), task.getTaskName(), locale);
      }
      return form.getStateImageURL();
    } catch (Exception e) {
      return "";
    }
  }

  @SuppressWarnings("unchecked")
  public void updateUITree() throws Exception {
    clean();
    UITaskManager uiTaskManager = getParent();
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    Map variablesForService = new HashMap();
    if (isStart_) {
      Process process = serviceContainer.getProcess(identification_);
      form = formsService.getForm(identification_, process.getStartStateName(), locale);
    } else {
      Task task = serviceContainer.getTask(identification_);
      String processInstanceId = task.getProcessInstanceId();
      variablesForService = serviceContainer.getVariables(processInstanceId, identification_);
      form = formsService.getForm(task.getProcessId(), task.getTaskName(), locale);
    }
    String workspaceName = (String) variablesForService.get(WORKSPACE_VARIABLE);
    String repository = (String) variablesForService.get(REPOSITORY_VARIABLE);
    if(repository == null) {
      repository = jcrService.getCurrentRepository().getConfiguration().getName();
    }
    ManageableRepository mRepository = jcrService.getCurrentRepository();
    SessionProviderService sessionProviderService = Util.getUIPortal().getApplicationComponent(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    List variables = form.getVariables();
    UIFormInput input = null;
    int i = 0;
    for (Iterator iter = variables.iterator(); iter.hasNext(); i++) {
      Map attributes = (Map) iter.next();
      String name = (String) attributes.get("name");
      String component = (String) attributes.get("component");
      String editableString = (String) attributes.get("editable");
      boolean editable = true;
      if (editableString != null && !"".equals(editableString)) {
        editable = new Boolean(editableString).booleanValue();
      }
      boolean mandatory = false;
      String mandatoryString = (String) attributes.get("mandatory");
      if (mandatoryString != null && !"".equals(mandatoryString)) {
        mandatory = new Boolean(mandatoryString).booleanValue();
      }
      boolean visiable = true;
      String visiableString = (String) attributes.get("visiable");
      if (visiableString != null && !"".equals(visiableString)) {
        visiable = new Boolean(visiableString).booleanValue();
      }
      Object value = variablesForService.get(name);

      if (NODE_EDIT.equals(component)) {
        String nodePath = (String)variablesForService.get(NODE_PATH_VARIABLE);
        Node dialogNode = (Node)sessionProvider.getSession(workspaceName,mRepository).getItem(nodePath);
        String nodetype = dialogNode.getPrimaryNodeType().getName();

        try {
          Class clazz = Class.forName("org.exoplatform.contentvalidation.webui.UIDocumentForm");
          UIComponent uiComponent = createUIComponent(clazz, null, null);
          Method[] methods = clazz.getDeclaredMethods();
          int count = 0;
          for (Method m : methods) {
            if (m.getName().trim().equals("setNodePath")) {
              m.invoke(uiComponent, nodePath);
              count++;
            } else if (m.getName().trim().equals("setTemplateNode")) {
              m.invoke(uiComponent, nodetype);
              count++;
            } else if (m.getName().trim().equals("setRepositoryName")) {
              m.invoke(uiComponent, repository);
              count++;
            } else if (m.getName().trim().equals("setWorkspace")) {
              m.invoke(uiComponent, workspaceName);
              count++;
            }
            if (count == 4) break;
          }

          Task task = serviceContainer.getTask(identification_);
          form = formsService.getForm(task.getProcessId(), task.getTaskName(), locale);
          uiTaskManager.addChild(uiComponent);
          uiComponent.setRendered(false);
        } catch (ClassNotFoundException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }

      } else if (NODE_VIEW.equals(component)) {
        String nodePath = (String) variablesForService.get(NODE_PATH_VARIABLE);
        Node viewNode = (Node) sessionProvider.getSession(workspaceName, mRepository).getItem(nodePath);
        try {
          Class clazz = Class.forName("org.exoplatform.contentvalidation.webui.UIDocumentContent");
          UIComponent uiComponent = createUIComponent(clazz, null, null);
          Method[] methods = clazz.getDeclaredMethods();
          for (Method m : methods) {
            if (m.getName().trim().equals("setNode")) {
              m.invoke(uiComponent, viewNode);
              break;
            }
          }
          uiTaskManager.addChild(uiComponent);
          uiComponent.setRendered(false);
        } catch (ClassNotFoundException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
      } else {
        if (component == null || TEXT.equals(component)) {
          input = new UIFormStringInput(name, (String) value);
          ((UIFormStringInput)input).setEditable(editable);
        } else if (TEXTAREA.equals(component)) {
          input = new UIFormTextAreaInput(name, null, (String) value);
          ((UIFormTextAreaInput)input).setEditable(editable);
        } else if (WYSIWYG.equals(component)) {
          input = new UIFormWYSIWYGInput(name, name, (String) value);
          ((UIFormWYSIWYGInput)input).setToolBarName(UIFormWYSIWYGInput.DEFAULT_TOOLBAR);
          ((UIFormWYSIWYGInput)input).setEditable(editable);
        } else if (DATE.equals(component) || DATE_TIME.equals(component)) {
          if(value == null) 
          {
            input = new UIFormDateTimeInput(name, null, new Date(), DATE_TIME.equals(component));
          } else {
            if(value instanceof String) {
              if(value.toString().length() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                input = new UIFormDateTimeInput(name, null, dateFormat.parse(value.toString()), DATE_TIME.equals(component));  
              } else {
                input = new UIFormDateTimeInput(name, null, new Date(), DATE_TIME.equals(component));
              }
            } else {
              input = new UIFormDateTimeInput(name, null, (Date)value, DATE_TIME.equals(component));
            }
              
          }
          if (!visiable) {
            input.setValue("");
          }
          input.addValidator(DateTimeValidator.class);
        } else if (SELECT.equals(component)) {
          String baseKey = name + ".select-";
          String baseKeyValue = name + ".valueselect-";
          List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
          int j = 0;
          String select0 = (String) variablesForService.get(baseKey + j);
          if (select0 == null) {
            ResourceBundle bundle = form.getResourceBundle();
              while (true) {
                String property = null;
                String propertyValue = null;
                try {
                  property = bundle.getString(baseKey + j);
                  propertyValue = bundle.getString(baseKeyValue + j);
                } catch (MissingResourceException e) {
                  if (LOG.isWarnEnabled()) {
                    LOG.warn(e.getMessage());
                  }
                }
                  if (property == null) break;
                  if (propertyValue != null && propertyValue.length() != 0)
                    options.add(new SelectItemOption<String>(property, propertyValue));
                  else
                    options.add(new SelectItemOption<String>(property, property));
                  j++;
              }
          } else {
            while (true) {
              String property = (String) variablesForService.get(baseKey + j);
              String propertyValue = (String) variablesForService.get(baseKeyValue + j);
              if (property == null)
                break;
              if (propertyValue != null && propertyValue.length() != 0)
                options.add(new SelectItemOption<String>(property, propertyValue));
              else
                options.add(new SelectItemOption<String>(property, property));
              j++;
            }
          }
          input = new UIFormSelectBox(name, (String) value, options);
          ((UIFormSelectBox)input).setEditable(editable);
        } else if (CHECK_BOX.equals(component)) {
          ResourceBundle bundle = form.getResourceBundle();
          String key = name + ".checkbox";
          if (value instanceof Boolean) {
            input = new UIFormCheckBoxInput<Boolean>(name, bundle.getString(key), ((Boolean) value).booleanValue());
          } else {
            input = new UIFormCheckBoxInput<Boolean>(name, bundle.getString(key), Boolean.valueOf(
                (String) value).booleanValue());
          }
          ((UIFormCheckBoxInput)input).setEditable(editable);
        } else if (UPLOAD.equals(component)) {
          input = new UIFormUploadInput(name, name);
        } else if (RADIO_BOX.equals(component)) {
          String baseKey = name + ".radiobox-";
          List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
          int j = 0;
          String select0 = (String) variablesForService.get(baseKey + j);
          if (select0 == null) {
            ResourceBundle bundle = form.getResourceBundle();
            try {
              while (true) {
                String property = bundle.getString(baseKey + j);
                options.add(new SelectItemOption<String>(property, property));
                j++;
              }
            } catch (MissingResourceException e) {
              if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage());
              }
            }
          } else {
            while (true) {
              String property = (String) variablesForService.get(baseKey + j);
              if (property == null) break;
              options.add(new SelectItemOption<String>(property, property));
              j++;
            }
          }
          input = new UIFormRadioBoxInput(name, (String) value, options);
        }
        ResourceBundle res = form.getResourceBundle();
        String inputName = name;
        try {
          inputName = res.getString(name + LABEL_ENCODING);
        } catch(Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
        inputInfo_.add(new InputInfo("", "", inputName, input, mandatory));
        if (mandatory) {
          input.addValidator(MandatoryValidator.class);
        }
        addUIFormInput(input);
      }
    }
  }

  public void setIsStart(boolean b) { isStart_ = b; }
  public boolean isStart() { return isStart_; }

  public ResourceBundle getWorkflowBundle() { return form.getResourceBundle(); }

  public List getInputInfo() { return inputInfo_; }

  public List getSubmitButtons() { return form.getSubmitButtons(); }

  public boolean isCustomizedView() { return form.isCustomizedView(); }
  public String getCustomizedView() { return form.getCustomizedView(); }

  public void setIdentification(String identification) { this.identification_ = identification; }
  public String getIdentification() { return identification_; }

  public VariableMaps prepareVariables(String decision) throws Exception {
    VariableMaps maps = prepareWorkflowVariables(getChildren(), decision);
    return maps;
  }

  @SuppressWarnings("unchecked")
  public VariableMaps prepareWorkflowVariables(Collection inputs, String decision) throws Exception {
    Map<String, Object> workflowVariables = new HashMap<String, Object>();
    Map jcrVariables = new HashMap();
    String name = "";
    for (Iterator iter = inputs.iterator(); iter.hasNext();) {
      UIFormInput input = (UIFormInput) iter.next();
      name = input.getName();
      Object value = "";
      if (input instanceof UIFormStringInput) {
        value = ((UIFormStringInput) input).getValue();
      } else if (input instanceof UIFormDateTimeInput) {
        Calendar calendar = ((UIFormDateTimeInput) input).getCalendar();
        value = ((calendar == null) ? (name.equals("startDate") ? (new GregorianCalendar().getTime())
                                                               : (""))
                                   : (calendar.getTime()));
      } else if (input instanceof UIFormWYSIWYGInput) {
        value = ((UIFormWYSIWYGInput)input).getValue();
      } else if (input instanceof UIFormTextAreaInput) {
        value = ((UIFormTextAreaInput) input).getValue();
      } else if (input instanceof UIFormCheckBoxInput) {
        value = new Boolean(((UIFormCheckBoxInput) input).isChecked()).toString();
      } else if (input instanceof UIFormSelectBox) {
        value = ((UIFormSelectBox) input).getValue();
      } else if (input instanceof UIFormRadioBoxInput) {
        value = ((UIFormRadioBoxInput) input).getValue();
      } else if (input instanceof UIFormUploadInput) {
        value = ((UIFormUploadInput) input).getUploadDataAsStream();
      }
      if (value == null) value = "";
      workflowVariables.put(name, value);
    }
    if (this.form.getSubmitButtons().size() > 0) {
      HashMap<String, Object> map = (HashMap)this.form.getSubmitButtons().get(0);
      String variable = (String)map.get("variable");
      if (variable != null && variable.length() != 0) workflowVariables.put(variable, decision);
    }
    if ("delegate".equals(decision)) workflowVariables.put("delegate", "true");
    String repository = jcrService.getCurrentRepository().getConfiguration().getName();
    workflowVariables.put(Utils.REPOSITORY, repository);
    return new VariableMaps(workflowVariables, jcrVariables);
  }

  public void clean() {
    UITaskManager uiTaskManager = getParent();
    try {
      Class clazz1 = Class.forName("org.exoplatform.contentvalidation.webui.UIDocumentForm");
      Class clazz2 = Class.forName("org.exoplatform.contentvalidation.webui.UIDocumentContent");
      uiTaskManager.removeChild(clazz2);
      uiTaskManager.removeChild(clazz1);
    } catch (ClassNotFoundException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    inputInfo_.clear();
  }

  public static class StartProcessActionListener extends EventListener<UITask> {
    public void execute(Event<UITask> event) throws Exception {
      UITask uiTask = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
      String remoteUser = pcontext.getRemoteUser();
      if (remoteUser == null) remoteUser = "anonymous";
      VariableMaps maps = uiTask.prepareVariables(objectId);
      Map variables = maps.getWorkflowVariables();
      uiTask.serviceContainer.startProcess(remoteUser, uiTask.identification_, variables);
      uiTask.getAncestorOfType(UIPopupContainer.class).deActivate();
    }
  }

  public static class EndOfStateActionListener extends EventListener<UITask> {
    public void execute(Event<UITask> event) throws Exception {
      UITask uiTask = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      VariableMaps maps = uiTask.prepareVariables(objectId);
      try {
        Map variables = maps.getWorkflowVariables();
        uiTask.serviceContainer.endTask(uiTask.identification_, variables);
      } catch (Exception ex) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(ex.getMessage(), ex);
        }
      }
      uiTask.getAncestorOfType(UIPopupContainer.class).deActivate();
    }
  }

  private Node getAction(Node node, String actionName) throws Exception {
    if (node.hasNode(Utils.EXO_ACTIONS + "/"+ actionName)) {
      return node.getNode(Utils.EXO_ACTIONS + "/"+ actionName);
    }
    return null;
  }

  static  public class CancelActionListener extends EventListener<UITask> {
    public void execute(Event<UITask> event) throws Exception {
      UIPopupContainer uiPopup = event.getSource().getAncestorOfType(UIPopupContainer.class);
      uiPopup.deActivate();
    }
  }

  public static class TransitionActionListener extends EventListener<UITask> {
    public void execute(Event<UITask> event) throws Exception {
      UITask uiTask = event.getSource();
      List submitButtons = uiTask.form.getSubmitButtons();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      VariableMaps maps = uiTask.prepareVariables(objectId);
      Task task = uiTask.serviceContainer.getTask(uiTask.identification_);
      String processInstanceId = task.getProcessInstanceId();
      Map variablesForService = uiTask.serviceContainer.getVariables(processInstanceId, uiTask.identification_);
      String nodePath = (String)variablesForService.get("nodePath");
      String srcPath = (String)variablesForService.get("srcPath");
      String srcWorkspace = (String)variablesForService.get("srcWorkspace");
      RepositoryService repositoryService = uiTask.getApplicationComponent(RepositoryService.class);
      if (objectId.equals("delegate")) {
        String delegate = (String)maps.getWorkflowVariables().get(UITask.DELEGATE_FIELD);
        UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
        // Check delegated user if is empty
        if (delegate.length() == 0) {
            uiApp.addMessage(new ApplicationMessage("UITask.msg.has-not-got-delegate", null,
                ApplicationMessage.WARNING));
            
            return;
        }
        // Check existence of delegated user
        OrganizationService organizationService = uiTask.getApplicationComponent(OrganizationService.class);
        if (organizationService.getUserHandler().findUserByName(delegate) == null)
        {
          uiApp.addMessage(new ApplicationMessage("UITask.msg.user-not-exist", null,
              ApplicationMessage.WARNING));
          return;
        }
      }
      SessionProviderService sessionProviderService = Util.getUIPortal().getApplicationComponent(SessionProviderService.class);
      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      for (Iterator iterator = submitButtons.iterator(); iterator.hasNext();) {
        Map attributes = (Map) iterator.next();
        String name = (String) attributes.get("name");
        if (objectId.equals(name)) {
          String transition = (String) attributes.get("transition");
          try {
            if(nodePath != null) {
              Session session = sessionProvider.getSession(srcWorkspace, repositoryService.getCurrentRepository());
              Node node = (Node)session.getItem(nodePath);
              if(node.isLocked() && node.hasProperty(DEST_PATH) && node.hasProperty(DEST_WORKSPACE)) {
                String actionName = (String)variablesForService.get("actionName");
                Node actionNode = uiTask.getAction((Node)session.getItem(srcPath), actionName);
                String destPath = actionNode.getProperty(DEST_PATH).getString() + nodePath.substring(nodePath.lastIndexOf("/"));
                String destWorkspace = actionNode.getProperty(DEST_WORKSPACE).getString();
                Session desSession = sessionProvider.getSession(destWorkspace, repositoryService.getCurrentRepository());
                Node destNode = (Node)desSession.getItem(destPath);
                LockUtil.changeLockToken(nodePath, destNode);
              }
            }
            Map variables = maps.getWorkflowVariables();
            uiTask.serviceContainer.endTask(uiTask.identification_, variables, transition);
            uiTask.getAncestorOfType(UIPopupContainer.class).deActivate();
            return;
          } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(e.getMessage(), e);
            }
          }
        }
      }
    }
  }

  static  public class SelectUserActionListener extends EventListener<UITask> {
    public void execute(Event<UITask> event) throws Exception {
      UITask uiTask = event.getSource();
      UITaskManager uiTaskManager = uiTask.getParent();
      uiTaskManager.initPopupSelectUser(UITask.DELEGATE_FIELD);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaskManager);
    }
  }

  public void doSelect(String selectField, Object value) throws Exception {
    for (UIComponent uiInput : getChildren()) {
      if (((UIFormInput) uiInput).getName().equals(selectField)) {
        ((UIFormStringInput) uiInput).setValue(value.toString());
      }
    }
  }

}
