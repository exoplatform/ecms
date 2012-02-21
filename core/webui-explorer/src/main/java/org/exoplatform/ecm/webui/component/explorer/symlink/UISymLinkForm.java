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
package org.exoplatform.ecm.webui.component.explorer.symlink;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.selector.content.one.UIContentBrowsePanelOne;
import org.exoplatform.wcm.webui.selector.content.one.UIContentSelectorOne;
import org.exoplatform.web.application.ApplicationMessage;
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
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Chien Nguyen Van
 *
 */

@ComponentConfigs( {
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
        @EventConfig(listeners = UISymLinkForm.SaveActionListener.class),
        @EventConfig(listeners = UISymLinkForm.CancelActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(type = UIFormMultiValueInputSet.class, id = "SymLinkMultipleInputset", events = {
        @EventConfig(listeners = UISymLinkForm.RemoveActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UISymLinkForm.AddActionListener.class, phase = Phase.DECODE) }) })

public class UISymLinkForm extends UIForm implements UIPopupComponent, UISelectable {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("explorer.symlink.UISymLinkForm");

  final static public String FIELD_NAME = "symLinkName";
  final static public String FIELD_PATH = "pathNode";
  final static public String FIELD_SYMLINK = "fieldPathNode";
  final static public String POPUP_SYMLINK = "UIPopupSymLink";

  final static public byte DRIVE_SELECTOR_MODE = 0;
  final static public byte WORSPACE_SELECTOR_MODE = 1;

  private byte selectorMode=DRIVE_SELECTOR_MODE;

  private boolean localizationMode = false;

  public UISymLinkForm() throws Exception {
  }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void initFieldInput() throws Exception {
    UIFormMultiValueInputSet uiFormMultiValue = createUIComponent(UIFormMultiValueInputSet.class,
                                                                  "SymLinkMultipleInputset",
                                                                  null);
    uiFormMultiValue.setId(FIELD_PATH);
    uiFormMultiValue.setName(FIELD_PATH);
    uiFormMultiValue.setEditable(false);
    uiFormMultiValue.setType(UIFormStringInput.class);
    addUIFormInput(uiFormMultiValue);
    if (!localizationMode)
      addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class));
  }

  public void enableLocalizationMode() {
    this.localizationMode = true;
  }

  public void doSelect(String selectField, Object value) throws Exception {
    String valueNodeName = String.valueOf(value).trim();
    String workspaceName = valueNodeName.substring(0, valueNodeName.lastIndexOf(":/"));
    valueNodeName = valueNodeName.substring(workspaceName.lastIndexOf(":")+1);
    List<String> listNodeName = new ArrayList<String>();
    listNodeName.add(valueNodeName);
    UIFormMultiValueInputSet uiFormMultiValueInputSet = getChild(UIFormMultiValueInputSet.class);
    uiFormMultiValueInputSet.setValue(listNodeName);
    String symLinkName = valueNodeName.substring(valueNodeName.lastIndexOf("/") + 1);
    int squareBracketIndex = symLinkName.indexOf('[');
    if (squareBracketIndex > -1)
      symLinkName = symLinkName.substring(0, squareBracketIndex);
    if (symLinkName.indexOf(".lnk") < 0) {
      StringBuffer sb = new StringBuffer();
      sb.append(symLinkName).append(".lnk");
      symLinkName = sb.toString();
    }
    symLinkName = Text.unescapeIllegalJcrChars(symLinkName);
    if (!localizationMode) getUIStringInput(FIELD_NAME).setValue(symLinkName);
    UISymLinkManager uiSymLinkManager = getParent();
    uiSymLinkManager.removeChildById(POPUP_SYMLINK);
  }

  static  public class SaveActionListener extends EventListener<UISymLinkForm> {
    public void execute(Event<UISymLinkForm> event) throws Exception {
      UISymLinkForm uiSymLinkForm = event.getSource();
      UIJCRExplorer uiExplorer = uiSymLinkForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiSymLinkForm.getAncestorOfType(UIApplication.class);
      String symLinkName = "";
      if (!uiSymLinkForm.localizationMode) symLinkName = uiSymLinkForm.getUIStringInput(FIELD_NAME).getValue();

      String pathNode = "";
      UIFormMultiValueInputSet uiSet = uiSymLinkForm.getChild(UIFormMultiValueInputSet.class);
      List<UIComponent> listChildren = uiSet.getChildren();
      for (UIComponent component : listChildren) {
        UIFormStringInput uiStringInput = (UIFormStringInput)component;
        if(uiStringInput.getValue() != null) {
          pathNode = uiStringInput.getValue().trim();
        }
      }

      Node node = uiExplorer.getCurrentNode() ;
      if(uiExplorer.nodeIsLocked(node)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null)) ;

        return ;
      }
      if(!uiSymLinkForm.localizationMode && (symLinkName == null || symLinkName.length() ==0)) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.name-invalid", null)) ;

        return ;
      }
      if(pathNode == null || pathNode.length() ==0) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.path-node-invalid", null));

        return ;
      }
      String workspaceName = pathNode.substring(0, pathNode.lastIndexOf(":/"));
      pathNode = pathNode.substring(pathNode.lastIndexOf(":/") + 1);
      /*
      String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")",
          "'", "#", ";", "}", "{", "/", "|", "\""};
      for(String filterChar : arrFilterChar) {
        if(symLinkName.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.name-not-allowed", null,
              ApplicationMessage.WARNING));

          return;
        }
      }
      */
      NodeFinder nodeFinder = uiSymLinkForm.getApplicationComponent(NodeFinder.class);
      try {
        nodeFinder.getItem(workspaceName, pathNode);
      } catch (ItemNotFoundException e) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.non-node", null,
            ApplicationMessage.WARNING));

        return;
      } catch (RepositoryException re) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.non-node", null,
            ApplicationMessage.WARNING));

        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected error occurs", e);
        }
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.non-node", null,
            ApplicationMessage.WARNING));

        return;
      }
      try {
        Node targetNode = (Node) nodeFinder.getItem(workspaceName, pathNode);
        if (uiSymLinkForm.localizationMode) {
          MultiLanguageService langService = uiSymLinkForm.getApplicationComponent(MultiLanguageService.class);
          langService.addSynchronizedLinkedLanguage(node, targetNode);
        } else {
          LinkManager linkManager = uiSymLinkForm.getApplicationComponent(LinkManager.class);
          linkManager.createLink(node, Utils.EXO_SYMLINK, targetNode, symLinkName);
        }
        uiExplorer.updateAjax(event);
      } catch (AccessControlException ace) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.repository-exception", null, ApplicationMessage.WARNING));

        return;
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.repository-exception", null, ApplicationMessage.WARNING));

        return;
      } catch(NumberFormatException nume) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.numberformat-exception", null, ApplicationMessage.WARNING));

        return;
      } catch(ConstraintViolationException cve) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.cannot-save", null, ApplicationMessage.WARNING));

        return;
      } catch(ItemExistsException iee) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.item-exists-exception", null, ApplicationMessage.WARNING));

        return;
      } catch(UnsupportedRepositoryOperationException unOperationException) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.UnsupportedRepositoryOperationException", null,
            ApplicationMessage.WARNING));

        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.cannot-save", null, ApplicationMessage.WARNING));

        return;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UISymLinkForm> {
    public void execute(Event<UISymLinkForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  static  public class RemoveActionListener extends EventListener<UIFormMultiValueInputSet> {
    public void execute(Event<UIFormMultiValueInputSet> event) throws Exception {
      UIFormMultiValueInputSet uiSet = event.getSource();
      UIComponent uiComponent = uiSet.getParent();
      if (uiComponent instanceof UISymLinkForm) {
        UISymLinkForm uiSymLinkForm = (UISymLinkForm)uiComponent;
        String id = event.getRequestContext().getRequestParameter(OBJECTID);
        UIFormStringInput uiInput = uiSymLinkForm.getUIStringInput(FIELD_NAME);
        if (uiInput!=null) uiInput.setValue("");
        uiSet.removeChildById(id);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiSymLinkForm);
      }
    }
  }

  static public class AddActionListener extends EventListener<UIFormMultiValueInputSet> {
    private String fixPath(String path, String driveName, String repo, UISymLinkForm uiSymlinkForm) throws Exception {
      if (path == null || path.length() == 0 ||
          driveName == null || driveName.length() == 0 ||
          repo == null || repo.length() == 0)
        return "";
      ManageDriveService managerDriveService = uiSymlinkForm.getApplicationComponent(ManageDriveService.class);
      DriveData driveData = managerDriveService.getDriveByName(driveName, repo);
      if (!path.startsWith(driveData.getHomePath()))
        return "";
      if ("/".equals(driveData.getHomePath()))
        return path;
      return path.substring(driveData.getHomePath().length());
    }
    public void execute(Event<UIFormMultiValueInputSet> event) throws Exception {
      UIFormMultiValueInputSet uiSet = event.getSource();
      UISymLinkForm uiSymLinkForm =  (UISymLinkForm) uiSet.getParent();
      UISymLinkManager uiSymLinkManager = uiSymLinkForm.getParent();
      UIJCRExplorer uiExplorer = uiSymLinkForm.getAncestorOfType(UIJCRExplorer.class);
      String workspaceName = uiExplorer.getCurrentWorkspace();
      String param = "returnField=" + FIELD_SYMLINK;
      UIPopupWindow uiPopupWindow = uiSymLinkManager.initPopupTaxonomy(POPUP_SYMLINK);

      if (uiSymLinkForm.isUseWorkspaceSelector()) {
        UIOneNodePathSelector uiNodePathSelector = uiSymLinkManager.createUIComponent(UIOneNodePathSelector.class, null, null);
        uiPopupWindow.setUIComponent(uiNodePathSelector);
        uiNodePathSelector.setIsDisable(workspaceName, false);
        uiNodePathSelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
        uiNodePathSelector.setRootNodeLocation(uiExplorer.getRepositoryName(), workspaceName, "/");
        uiNodePathSelector.setIsShowSystem(false);
        uiNodePathSelector.init(WCMCoreUtils.getUserSessionProvider());
        uiNodePathSelector.setSourceComponent(uiSymLinkForm, new String[]{param});
      }else {
        Node node =uiExplorer.getCurrentNode();
        UIContentSelectorOne uiNodePathSelector = uiSymLinkForm.createUIComponent(UIContentSelectorOne.class, null, null);
        uiPopupWindow.setUIComponent(uiNodePathSelector);
        uiNodePathSelector.init(uiExplorer.getDriveData().getName(),
                                fixPath(node == null ? "" : node.getPath(),
                                        uiExplorer.getDriveData().getName(),
                                        uiExplorer.getRepositoryName(),
                                        uiSymLinkForm));
        uiNodePathSelector.getChild(UIContentBrowsePanelOne.class).setSourceComponent(uiSymLinkForm, new String[] { param });
      }
      uiPopupWindow.setRendered(true);
      uiPopupWindow.setShow(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSymLinkManager);
    }
  }
  public void useWorkspaceSelector() {
    this.selectorMode = WORSPACE_SELECTOR_MODE;
  }
  public void useDriveSelector() {
    this.selectorMode = DRIVE_SELECTOR_MODE;
  }
  public boolean isUseWorkspaceSelector() {
    return this.selectorMode==WORSPACE_SELECTOR_MODE;
  }
}
