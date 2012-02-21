/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Mar 4, 2010
 * 3:33:41 PM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UISelectRestorePath.SaveActionListener.class),
      @EventConfig(listeners = UISelectRestorePath.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UISelectRestorePath.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UISelectRestorePath extends UIForm implements UIPopupComponent, UISelectable {

  final static public String FIELD_PATH                  = "PathNode";

  final static public String FORM_INPUT                  = "formInput";

  final static public String POPUP_PATH                  = "UIPopupPathFoRestore";

  final static public String FORM_MESSAGE                = "UIFormMessage";

  final static public String CHOOSE_PATH_TO_RESTORE_NODE = "ChooseTagToRestoreNode";

  private final static Log   LOG                         = ExoLogger.getLogger(UISelectRestorePath.class);

  private NodeLocation               trashHomeNode;

  private String             repository;

  private String             srcPath;

  public Node getTrashHomeNode() { 
    return NodeLocation.getNodeByLocation(trashHomeNode);
  }
  
  public void setTrashHomeNode(Node trashHomeNode) {
    this.trashHomeNode = NodeLocation.getNodeLocationByNode(trashHomeNode);
  }

  public String getRepository() { return repository; }
  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getSrcPath() { return srcPath; }
  public void setSrcPath(String srcPath) {
    this.srcPath = srcPath;
  }

  public void activate() throws Exception {
    //this.addChild(new UIFormMessage(CHOOSE_PATH_TO_RESTORE_NODE));
    UIFormInputSet uiFormInputAction = new UIFormInputSetWithAction("UIFormInputSetWithAction");

    UIFormStringInput homePathField = new UIFormStringInput(FORM_INPUT, FORM_INPUT, null);
    homePathField.setValue("");
    homePathField.setEditable(false);

    uiFormInputAction.addUIFormInput(homePathField);
    uiFormInputAction.setId(FIELD_PATH);
    ((UIFormInputSetWithAction)uiFormInputAction).setActionInfo(FORM_INPUT, new String[]{"Add"});

    this.addUIFormInput(uiFormInputAction);
    setActions(new String[] {"Save", "Cancel"});
  }

  public void deActivate() throws Exception {
  }

  public void doSelect(String selectField, Object value) throws Exception {
    String valueNodeName = String.valueOf(value).trim();
    UIFormInputSetWithAction uiFormInputAction = getChild(UIFormInputSetWithAction.class);
    uiFormInputAction.getChild(UIFormStringInput.class).setValue(valueNodeName);
    this.getAncestorOfType(UIPopupContainer.class).removeChildById(POPUP_PATH);
  }


  static  public class CancelActionListener extends EventListener<UISelectRestorePath> {
    public void execute(Event<UISelectRestorePath> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  static  public class AddActionListener extends EventListener<UISelectRestorePath> {
    public void execute(Event<UISelectRestorePath> event) throws Exception {
      UISelectRestorePath uiSelectRestorePath =  event.getSource();
      UIPopupContainer uiPopupContainer = uiSelectRestorePath.getAncestorOfType(UIPopupContainer.class);
      UIJCRExplorer uiExplorer = uiSelectRestorePath.getAncestorOfType(UIJCRExplorer.class);
      String workspaceName = uiExplorer.getCurrentWorkspace();

      UIPopupWindow uiPopupWindow = initPopup(uiPopupContainer, POPUP_PATH);
      UIOneNodePathSelector uiNodePathSelector = uiPopupContainer.createUIComponent(UIOneNodePathSelector.class,
                                                                                    null,
                                                                                    null);

      uiNodePathSelector.setIsDisable(workspaceName, false);
      uiNodePathSelector.setShowRootPathSelect(true);
      uiNodePathSelector.setRootNodeLocation(uiExplorer.getRepositoryName(), workspaceName, "/");
      uiNodePathSelector.setAcceptedNodeTypesInPathPanel(new String[] {Utils.NT_UNSTRUCTURED, Utils.NT_FOLDER}) ;
      uiNodePathSelector.setAcceptedNodeTypesInTree(new String[] {Utils.NT_UNSTRUCTURED, Utils.NT_FOLDER});
      uiNodePathSelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_TRASH_FOLDER});
      uiNodePathSelector.setExceptedNodeTypesInTree(new String[] {Utils.EXO_TRASH_FOLDER});
      if(WCMCoreUtils.isAnonim()) {
        uiNodePathSelector.init(WCMCoreUtils.createAnonimProvider()) ;
      } else if(workspaceName.equals(getSystemWorkspaceName(uiExplorer))){
        uiNodePathSelector.init(WCMCoreUtils.getSystemSessionProvider()) ;
      } else {
        uiNodePathSelector.init(WCMCoreUtils.getUserSessionProvider()) ;
      }
      String param = "returnField=" + FIELD_PATH;
      uiNodePathSelector.setSourceComponent(uiSelectRestorePath, new String[]{param});
      uiPopupWindow.setUIComponent(uiNodePathSelector);
      //uiPopupWindow.setRendered(true);
      uiPopupWindow.setShow(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }

    private String getSystemWorkspaceName(UIJCRExplorer uiExplorer) throws RepositoryException {
      RepositoryService repositoryService = uiExplorer.getApplicationComponent(RepositoryService.class);
      return repositoryService.getCurrentRepository().getConfiguration().getSystemWorkspaceName();
    }

    private UIPopupWindow initPopup(UIPopupContainer uiPopupContainer, String id) throws Exception {
      UIPopupWindow uiPopup = uiPopupContainer.getChildById(id);
      if (uiPopup == null) {
        uiPopup = uiPopupContainer.addChild(UIPopupWindow.class, null, id);
      }
      uiPopup.setWindowSize(700, 350);
      uiPopup.setShow(false);
      uiPopup.setResizable(true);
      return uiPopup;
    }
  }

  static  public class SaveActionListener extends EventListener<UISelectRestorePath> {
    public void execute(Event<UISelectRestorePath> event) throws Exception {
      UISelectRestorePath uiSelectRestorePath = event.getSource();
      UIJCRExplorer uiExplorer = uiSelectRestorePath.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiSelectRestorePath.getAncestorOfType(UIApplication.class);
      String fullRestorePath = uiSelectRestorePath.
                                getChild(UIFormInputSetWithAction.class).
                                getChild(UIFormStringInput.class).getValue();
      int colonIndex = fullRestorePath.indexOf(':');
      if (colonIndex == -1) return;
      String restoreWorkspace = fullRestorePath.substring(0, colonIndex);
      String restorePath = fullRestorePath.substring(colonIndex + 1);
      Node trashNode = (Node) uiSelectRestorePath.getTrashHomeNode()
                                                 .getSession()
                                                 .getItem(uiSelectRestorePath.getSrcPath());
      trashNode.setProperty(TrashService.RESTORE_WORKSPACE, restoreWorkspace);
      trashNode.setProperty(TrashService.RESTORE_PATH, restorePath +
                                                       (restorePath.endsWith("/") ? "" : '/') +
                                                       trashNode.getName());
      TrashService trashService = uiSelectRestorePath.getApplicationComponent(TrashService.class);
      try {
        trashService.restoreFromTrash(uiSelectRestorePath.getTrashHomeNode(),
                                      uiSelectRestorePath.getSrcPath(),
                                      uiExplorer.getSessionProvider());
        UIPopupContainer uiPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
        uiPopupContainer.removeChild(UISelectRestorePath.class);
        uiExplorer.updateAjax(event);
      } catch (PathNotFoundException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Path not found! Maybe, it was removed or path changed, can't restore node :" + trashNode.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
      } catch (LockException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("node is locked, can't restore node :" + trashNode.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
      } catch (VersionException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("node is checked in, can't restore node:" + trashNode.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
      } catch (AccessDeniedException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("access denied, can't restore of node:" + trashNode.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
      } catch (ConstraintViolationException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("access denied, can't restore of node:" + trashNode.getPath());
        }
        JCRExceptionManager.process(uiApp, e);
        
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs", e);
        }
        JCRExceptionManager.process(uiApp, e);
        
      }
    }
  }

}
