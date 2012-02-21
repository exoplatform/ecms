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
package org.exoplatform.ecm.webui.tree.selectone;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.UINodeTreeBuilder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 21, 2007 2:32:49 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/ecm/webui/form/UIFormWithoutAction.gtmpl",
    events = {
      @EventConfig(listeners = UIWorkspaceList.ChangeWorkspaceActionListener.class),
      @EventConfig(listeners = UIWorkspaceList.AddRootNodeActionListener.class)
    }
)
public class UIWorkspaceList extends UIForm {

  static private String WORKSPACE_NAME = "workspaceName";
  static private String ROOT_NODE_INFO = "rootNodeInfo";
  static private String ROOT_NODE_PATH = "rootNodePath";

  private List<String> wsList_;
  private boolean isShowSystem_ = true;

  private static final Log LOG = ExoLogger.getLogger(UIWorkspaceList.class);

  public UIWorkspaceList() throws Exception {
    List<SelectItemOption<String>> wsList = new ArrayList<SelectItemOption<String>>();
    UIFormSelectBox uiWorkspaceList = new UIFormSelectBox(WORKSPACE_NAME, WORKSPACE_NAME, wsList);
    uiWorkspaceList.setOnChange("ChangeWorkspace");
    addUIFormInput(uiWorkspaceList);
    UIFormInputSetWithAction rootNodeInfo = new UIFormInputSetWithAction(ROOT_NODE_INFO);
    rootNodeInfo.addUIFormInput(new UIFormInputInfo(ROOT_NODE_PATH, ROOT_NODE_PATH, null));
    String[] actionInfor = {"AddRootNode"};
    rootNodeInfo.setActionInfo(ROOT_NODE_PATH, actionInfor);
    rootNodeInfo.showActionInfo(true);
    rootNodeInfo.setRendered(false);
    addUIComponentInput(rootNodeInfo);
  }

  public void setIsShowSystem(boolean isShowSystem) { isShowSystem_ = isShowSystem; }

  public boolean isShowSystemWorkspace() { return isShowSystem_; }

  public void setShowRootPathSelect(boolean isRender) {
    UIFormInputSetWithAction uiInputAction = getChildById(ROOT_NODE_INFO);
    uiInputAction.setRendered(isRender);
  }

  @Deprecated
  public void setWorkspaceList(String repository) throws Exception {
    setWorkspaceList();
  }
  
  public void setWorkspaceList() throws Exception {
    wsList_ = new ArrayList<String>();
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    String[] wsNames = repositoryService.getCurrentRepository().getWorkspaceNames();
    String systemWsName = repositoryService.getCurrentRepository()
                                           .getConfiguration()
                                           .getSystemWorkspaceName();
    List<SelectItemOption<String>> workspace = new ArrayList<SelectItemOption<String>>();
    for (String wsName : wsNames) {
      if (!isShowSystem_) {
        if (!wsName.equals(systemWsName)) {
          workspace.add(new SelectItemOption<String>(wsName, wsName));
          wsList_.add(wsName);
        }
      } else {
        workspace.add(new SelectItemOption<String>(wsName, wsName));
        wsList_.add(wsName);
      }
    }
    UIFormSelectBox uiWorkspaceList = getUIFormSelectBox(WORKSPACE_NAME);
    uiWorkspaceList.setOptions(workspace);
    UIOneNodePathSelector uiBrowser = getParent();
    if (uiBrowser.getWorkspaceName() != null) {
      if (wsList_.contains(uiBrowser.getWorkspaceName())) {
        uiWorkspaceList.setValue(uiBrowser.getWorkspaceName());
      }
    }
  }  

  public void setIsDisable(String wsName, boolean isDisable) {
    if(wsList_.contains(wsName)) getUIFormSelectBox(WORKSPACE_NAME).setValue(wsName);
    getUIFormSelectBox(WORKSPACE_NAME).setDisabled(isDisable);
  }

  private Node getRootNode(String workspaceName) throws RepositoryException {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    return WCMCoreUtils.getUserSessionProvider().getSession(workspaceName, manageableRepository).getRootNode();
  }

  static public class ChangeWorkspaceActionListener extends EventListener<UIWorkspaceList> {
    public void execute(Event<UIWorkspaceList> event) throws Exception {
      UIWorkspaceList uiWorkspaceList = event.getSource();
      UIOneNodePathSelector uiJBrowser = uiWorkspaceList.getParent();
      String wsName = uiWorkspaceList.getUIFormSelectBox(WORKSPACE_NAME).getValue();
      uiJBrowser.setWorkspaceName(wsName);
      UINodeTreeBuilder uiTreeBuilder = uiJBrowser.getChild(UINodeTreeBuilder.class);
      UIBreadcumbs uiBreadcumbs = uiJBrowser.getChild(UIBreadcumbs.class);
      if (uiBreadcumbs != null) uiBreadcumbs.getPath().clear();
      UIApplication uiApp = uiWorkspaceList.getAncestorOfType(UIApplication.class);
      try {
        uiTreeBuilder.setRootTreeNode(uiWorkspaceList.getRootNode(wsName));
      } catch (AccessDeniedException ade) {
        uiWorkspaceList.getUIFormSelectBox(WORKSPACE_NAME).setValue("collaboration");
        uiApp.addMessage(new ApplicationMessage("UIWorkspaceList.msg.AccessDeniedException",
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected error occurs", e);
        }
        return;
      }

      uiTreeBuilder.buildTree();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiJBrowser);
    }
  }

  static public class AddRootNodeActionListener extends EventListener<UIWorkspaceList> {
    public void execute(Event<UIWorkspaceList> event) throws Exception {
      UIWorkspaceList uiWorkspaceList = event.getSource();
      UIOneNodePathSelector uiJBrowser = uiWorkspaceList.getParent();
      String returnField = uiJBrowser.getReturnFieldName();
      String workspaceName = uiJBrowser.getWorkspaceName();
      RepositoryService repositoryService = uiWorkspaceList.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession(workspaceName, manageableRepository);
      String value = session.getRootNode().getPath();
      if (!uiJBrowser.isDisable()) {
        StringBuffer sb = new StringBuffer();
        sb.append(uiJBrowser.getWorkspaceName()).append(":").append(value);
        value = sb.toString();
      }
      ((UISelectable)uiJBrowser.getSourceComponent()).doSelect(returnField, value);
    }
  }
}
