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
package org.exoplatform.wcm.webui.selector;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.UINodeTreeBuilder;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
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
 * minh.dang@exoplatform.com
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

  /** The WORKSPAC e_ name. */
  static private String WORKSPACE_NAME = "workspaceName";

  /** The ROO t_ nod e_ info. */
  static private String ROOT_NODE_INFO = "rootNodeInfo";

  /** The ROO t_ nod e_ path. */
  static private String ROOT_NODE_PATH = "rootNodePath";

  /** The ws list_. */
  private List<String> wsList_;

  /** The is show system_. */
  private boolean isShowSystem_ = true;

  /**
   * Instantiates a new uI workspace list.
   *
   * @throws Exception the exception
   */
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

  /**
   * Sets the checks if is show system.
   *
   * @param isShowSystem the new checks if is show system
   */
  public void setIsShowSystem(boolean isShowSystem) { isShowSystem_ = isShowSystem; }

  /**
   * Checks if is show system workspace.
   *
   * @return true, if is show system workspace
   */
  public boolean isShowSystemWorkspace() { return isShowSystem_; }

  /**
   * Sets the show root path select.
   *
   * @param isRender the new show root path select
   */
  public void setShowRootPathSelect(boolean isRender) {
    UIFormInputSetWithAction uiInputAction = getChildById(ROOT_NODE_INFO);
    uiInputAction.setRendered(isRender);
  }

  /**
   * Sets the workspace list.
   *
   * @param repository the new workspace list
   *
   * @throws Exception the exception
   */
  @Deprecated
  public void setWorkspaceList(String repository) throws Exception {
    setWorkspaceList();
  }
  
  /**
   * Sets the workspace list.
   *
   *
   * @throws Exception the exception
   */
  public void setWorkspaceList() throws Exception {
    wsList_ = new ArrayList<String>();
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository mrepository= repositoryService.getCurrentRepository();
    String[] wsNames = mrepository.getWorkspaceNames();
    String systemWsName = mrepository.getConfiguration().getSystemWorkspaceName();
    List<SelectItemOption<String>> workspace = new ArrayList<SelectItemOption<String>>();
    for(String wsName : wsNames) {
      if(!isShowSystem_) {
        if(!wsName.equals(systemWsName)) {
          workspace.add(new SelectItemOption<String>(wsName,  wsName));
          wsList_.add(wsName);
        }
      } else {
        workspace.add(new SelectItemOption<String>(wsName,  wsName));
        wsList_.add(wsName);
      }
    }
    UIFormSelectBox uiWorkspaceList = getUIFormSelectBox(WORKSPACE_NAME);
    uiWorkspaceList.setOptions(workspace);
    UIOneNodePathSelector uiBrowser = getParent();
    if(uiBrowser.getWorkspaceName() != null) {
      if(wsList_.contains(uiBrowser.getWorkspaceName())) {
        uiWorkspaceList.setValue(uiBrowser.getWorkspaceName());
      }
    }
  }  

  /**
   * Sets the is disable.
   *
   * @param wsName the ws name
   * @param isDisable the is disable
   */
  public void setIsDisable(String wsName, boolean isDisable) {
    if(wsList_.contains(wsName)) getUIFormSelectBox(WORKSPACE_NAME).setValue(wsName);
    getUIFormSelectBox(WORKSPACE_NAME).setDisabled(isDisable);
  }

  /**
   * Gets the root node.
   *
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   *
   * @return the root node
   *
   * @throws RepositoryException the repository exception
   * @throws RepositoryConfigurationException the repository configuration exception
   */
  private Node getRootNode(String workspaceName) throws RepositoryException,
                                                                       RepositoryConfigurationException {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    SessionProvider sessionProvider =  WCMCoreUtils.getSystemSessionProvider();
    Session session = sessionProvider.getSession(workspaceName, manageableRepository);
    return session.getRootNode();
  }

  /**
   * The listener interface for receiving changeWorkspaceAction events.
   * The class that is interested in processing a changeWorkspaceAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeWorkspaceActionListener<code> method. When
   * the changeWorkspaceAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeWorkspaceActionEvent
   */
  static public class ChangeWorkspaceActionListener extends EventListener<UIWorkspaceList> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIWorkspaceList> event) throws Exception {
      UIWorkspaceList uiWorkspaceList = event.getSource();
      UIOneNodePathSelector uiJBrowser = uiWorkspaceList.getParent();
      String wsName = uiWorkspaceList.getUIFormSelectBox(WORKSPACE_NAME).getValue();
      uiJBrowser.setWorkspaceName(wsName);
      UINodeTreeBuilder uiTreeJCRExplorer = uiJBrowser.getChild(UINodeTreeBuilder.class);
      UIApplication uiApp = uiWorkspaceList.getAncestorOfType(UIApplication.class);
      try {
        uiTreeJCRExplorer.setRootTreeNode(uiWorkspaceList.getRootNode(wsName));
      } catch (AccessDeniedException ade) {
        uiWorkspaceList.getUIFormSelectBox(WORKSPACE_NAME).setValue("collaboration");
        uiApp.addMessage(new ApplicationMessage("UIWorkspaceList.msg.AccessDeniedException",
                                                null,
                                                ApplicationMessage.WARNING));
        return;
      } catch(Exception e) {
        return;
      }
      uiTreeJCRExplorer.buildTree();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiJBrowser);
    }
  }

  /**
   * The listener interface for receiving addRootNodeAction events.
   * The class that is interested in processing a addRootNodeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddRootNodeActionListener<code> method. When
   * the addRootNodeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddRootNodeActionEvent
   */
  static public class AddRootNodeActionListener extends EventListener<UIWorkspaceList> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIWorkspaceList> event) throws Exception {
      UIWorkspaceList uiWorkspaceList = event.getSource();
      UIOneNodePathSelector uiJBrowser = uiWorkspaceList.getParent();
      String returnField = uiJBrowser.getReturnFieldName();
      String workspaceName = uiJBrowser.getWorkspaceName();
      RepositoryService repositoryService = uiWorkspaceList.getApplicationComponent(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
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
