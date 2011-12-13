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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.form.validator.IllegalDMSCharValidator;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.relations.RelationsService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 07, 2006
 * 08:57:15 AM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIRenameForm.SaveActionListener.class),
      @EventConfig(listeners = UIRenameForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)

public class UIRenameForm extends UIForm implements UIPopupComponent {

  final static public String FIELD_NAME          = "nameField";

  final static public String FIELD_TITLE        = "titleField";

  final static private String RELATION_PROP     = "exo:relation";

  private NodeLocation               renameNode_;

  public UIRenameForm() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_TITLE, FIELD_TITLE).addValidator(MandatoryValidator.class)
                                                                  .addValidator(IllegalDMSCharValidator.class));
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME).addValidator(MandatoryValidator.class)
                                                                .addValidator(IllegalDMSCharValidator.class)
                                                                .addValidator(ECMNameValidator.class));
  }

  public void update(Node renameNode) throws Exception {
    renameNode_ = NodeLocation.getNodeLocationByNode(renameNode);
    String renamePath = renameNode.getPath() ;
    String oldName = Text.unescapeIllegalJcrChars(
        renamePath.substring(renamePath.lastIndexOf("/") + 1, renamePath.length())) ;
    getUIStringInput(FIELD_NAME).setValue(oldName);
    UIFormStringInput titleInput = getUIStringInput(FIELD_TITLE);
    if (renameNode.hasProperty(Utils.EXO_TITLE)) {
      String oldTitle = renameNode.getProperty(Utils.EXO_TITLE).getString();
      if (oldTitle != null && oldTitle.trim().length() > 0) {
        titleInput.setValue(oldTitle);
      } else {
        titleInput.setValue(oldName);
      }
    } else {
      titleInput.setValue(oldName);
    }
  }

  private void changeLockForChild(String srcPath, Node parentNewNode) throws Exception {
    if(parentNewNode.hasNodes()) {
      NodeIterator newNodeIter = parentNewNode.getNodes();
      String newSRCPath = null;
      while(newNodeIter.hasNext()) {
        Node newChildNode = newNodeIter.nextNode();
        newSRCPath = newChildNode.getPath().replace(parentNewNode.getPath(), srcPath);
        if(newChildNode.isLocked()) LockUtil.changeLockToken(newSRCPath, newChildNode);
        if(newChildNode.hasNodes()) changeLockForChild(newSRCPath, newChildNode);
      }
    }
  }

  static  public class SaveActionListener extends EventListener<UIRenameForm> {
    public void execute(Event<UIRenameForm> event) throws Exception {
      UIRenameForm uiRenameForm = event.getSource();
      RelationsService relationsService = uiRenameForm.getApplicationComponent(RelationsService.class);
      UIJCRExplorer uiJCRExplorer = uiRenameForm.getAncestorOfType(UIJCRExplorer.class);
      List<Node> refList = new ArrayList<Node>();
      boolean isReference = false;
      PropertyIterator references = null;
      UIApplication uiApp = uiRenameForm.getAncestorOfType(UIApplication.class);
      Session nodeSession = null;
      String newName = Text.escapeIllegalJcrChars(
          uiRenameForm.getUIStringInput(FIELD_NAME).getValue().trim());
      // String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!",
      // "+", "(", ")", "'", "#", ";", "}", "{", "/", "|", "\""};
      // if (!Utils.isNameValid(newName, arrFilterChar)) {
      // uiApp.addMessage(new
      // ApplicationMessage("UIFolderForm.msg.name-not-allowed", null,
      // ApplicationMessage.WARNING));
      // 
      // return;
      // }
      Node currentNode = NodeLocation.getNodeByLocation(uiRenameForm.renameNode_);
      if (currentNode.getName().equals(newName) && sameTitle(uiRenameForm, currentNode)) {
        uiJCRExplorer.cancelAction();
        return;
      }
      nodeSession = currentNode.getSession();
      String srcPath = uiRenameForm.renameNode_.getPath();
      String destPath;
      //test lock
      if (uiJCRExplorer.nodeIsLocked(currentNode)) {

        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null,
            ApplicationMessage.WARNING));
        
        uiJCRExplorer.updateAjax(event);
        return;
      }
      //change name and title main process
      try {
        //change name
        if (!currentNode.getName().equals(newName)) {
          // get list of nodes that have reference to this node
          try {
            references = currentNode.getReferences();
            isReference = true;
          } catch (RepositoryException e) {
            isReference = false;
          }
          if (isReference && references != null) {
            if (references.getSize() > 0) {
              while (references.hasNext()) {
                Property pro = references.nextProperty();
                Node refNode = pro.getParent();
                if (refNode.hasProperty(RELATION_PROP)) {
                  refList.add(refNode);
                  relationsService.removeRelation(refNode, uiRenameForm.renameNode_.getPath());
                  refNode.save();
                }
              }
            }
          }
          Node parent = currentNode.getParent();
          if(parent.getPath().equals("/")) destPath = "/" + newName;
          else destPath = parent.getPath() + "/" + newName;
          uiJCRExplorer.addLockToken(parent);
          nodeSession.getWorkspace().move(srcPath,destPath);
          String currentPath = uiJCRExplorer.getCurrentPath();
          if(srcPath.equals(uiJCRExplorer.getCurrentPath())) {
            uiJCRExplorer.setCurrentPath(destPath) ;
          } else if(currentPath.startsWith(srcPath)) {
            uiJCRExplorer.setCurrentPath(destPath + currentPath.substring(currentPath.lastIndexOf("/")));
          }
          nodeSession.save();
          for(int i = 0; i < refList.size(); i ++) {
            Node addRef = refList.get(i);
            relationsService.addRelation(addRef, destPath, nodeSession.getWorkspace().getName());
            addRef.save();
          }
          Node destNode = (Node) nodeSession.getItem(destPath);
          if (destNode.isLocked())
            LockUtil.changeLockToken(currentNode, destNode);
          uiRenameForm.changeLockForChild(srcPath, destNode);
          if(destNode.canAddMixin("exo:modify")) {
              destNode.addMixin("exo:modify");            
          }
          destNode.setProperty(Utils.EXO_LASTMODIFIER, nodeSession.getUserID());
          currentNode = destNode;
        }
        //change title
        if (!sameTitle(uiRenameForm, currentNode)) {
          String newTitle = uiRenameForm.getUIStringInput(FIELD_TITLE).getValue();
          if (newTitle.length() > 0) {
            if (!currentNode.hasProperty(Utils.EXO_TITLE))
              currentNode.addMixin(Utils.EXO_RSS_ENABLE);
            currentNode.setProperty(Utils.EXO_TITLE, newTitle);
          }
        }

        nodeSession.save();
        uiJCRExplorer.updateAjax(event);
        
      } catch (AccessDeniedException ace) {
        if (nodeSession != null)
          nodeSession.refresh(false);
        uiJCRExplorer.refreshExplorer();
        uiJCRExplorer.cancelAction();
        Object[] args = { currentNode.getName() };
        uiApp.addMessage(new ApplicationMessage("UIRenameForm.msg.rename-denied",
                                                args,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiRenameForm);
      } catch (VersionException ve) {
        if (nodeSession != null)
          nodeSession.refresh(false);
        uiJCRExplorer.refreshExplorer();
        uiJCRExplorer.cancelAction();
        uiApp.addMessage(new ApplicationMessage("UIRenameForm.msg.version-exception",
                                                null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiRenameForm);
      } catch (ConstraintViolationException cons) {
        if (nodeSession != null)
          nodeSession.refresh(false);
        uiJCRExplorer.refreshExplorer();
        Object[] args = { currentNode.getPrimaryNodeType().getName() };
        uiApp.addMessage(new ApplicationMessage("UIRenameForm.msg.constraintViolation-exception",
                                                args,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiRenameForm);
      } catch (LockException lockex) {
        uiJCRExplorer.cancelAction();
        Object[] agrs = { currentNode.getPrimaryNodeType().getName() };
        uiApp.addMessage(new ApplicationMessage("UIRenameForm.msg.lock-exception",
                                                agrs,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiRenameForm);
      } catch (Exception e) {
        if (org.apache.commons.lang.exception.ExceptionUtils.getRootCause(e) instanceof javax.jcr.NamespaceException) {
          Object[] agrs = { newName.substring(0, newName.indexOf(":")) };
          uiApp.addMessage(new ApplicationMessage("UIRenameForm.msg.unknown-prefix",
                                                  agrs,
                                                  ApplicationMessage.WARNING));
        } else {
          Object[] agrs = { currentNode.getName() };
          uiApp.addMessage(new ApplicationMessage("UIRenameForm.msg.rename-error",
                                                  agrs,
                                                  ApplicationMessage.WARNING));
        }
        event.getRequestContext().addUIComponentToUpdateByAjax(uiRenameForm);
      }
    }

    private boolean sameTitle(UIRenameForm uiRenameForm, Node node) throws Exception {
      String newTitle = uiRenameForm.getUIStringInput(FIELD_TITLE).getValue();
      if (!node.hasProperty(Utils.EXO_TITLE))
        return (newTitle == null || newTitle.length() == 0);
      return node.getProperty(Utils.EXO_TITLE).getString().equals(newTitle);
    }
  }

  static  public class CancelActionListener extends EventListener<UIRenameForm> {
    public void execute(Event<UIRenameForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction() ;
    }
  }

  public void activate() throws Exception {
  }

  public void deActivate() throws Exception { }

}
