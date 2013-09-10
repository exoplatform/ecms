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
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.exceptions.SameAsDefaultLangException;
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
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL
 * Author : Chien Nguyen Van
 *
 */

@ComponentConfigs( {
    @ComponentConfig(lifecycle = UIFormLifecycle.class,
        template = "app:/groovy/webui/component/explorer/thumbnail/UIAddTranslationForm.gtmpl",
        events = {
      @EventConfig(listeners = UIAddTranslationForm.SaveActionListener.class),
      @EventConfig(listeners = UIAddTranslationForm.CancelActionListener.class, phase = Phase.DECODE) }),
    @ComponentConfig(type = UITranslationFormMultiValueInputSet.class, id = "SymLinkMultipleInputset", events = {
      @EventConfig(listeners = UIAddTranslationForm.RemoveActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIAddTranslationForm.SelectDocumentActionListener.class, phase = Phase.DECODE) }) })

public class UIAddTranslationForm extends UIForm implements UIPopupComponent, UISelectable {
  
  public static final String FIELD_PATH = "pathNode";
  public static final String FIELD_SYMLINK = "fieldPathNode";
  public static final String POPUP_SYMLINK = "UIPopupSymLink";

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIAddTranslationForm.class.getName());

  final static public byte DRIVE_SELECTOR_MODE = 0;
  final static public byte WORSPACE_SELECTOR_MODE = 1;

  private byte selectorMode=DRIVE_SELECTOR_MODE;

  public UIAddTranslationForm() throws Exception {
  }

  public void activate() {}
  public void deActivate() {}

  public void initFieldInput() throws Exception {
    UITranslationFormMultiValueInputSet uiTranslationFormMultiValue = createUIComponent(UITranslationFormMultiValueInputSet.class,
                                                                  "SymLinkMultipleInputset",
                                                                  null);
    uiTranslationFormMultiValue.setId(FIELD_PATH);
    uiTranslationFormMultiValue.setName(FIELD_PATH);
    uiTranslationFormMultiValue.setEditable(false);
    uiTranslationFormMultiValue.setType(UIFormStringInput.class);
    addUIFormInput(uiTranslationFormMultiValue);
  }

  public void doSelect(String selectField, Object value) throws Exception {
    String valueNodeName = String.valueOf(value).trim();
    String workspaceName = valueNodeName.substring(0, valueNodeName.lastIndexOf(":/"));
    valueNodeName = valueNodeName.substring(workspaceName.lastIndexOf(":")+1);
    List<String> listNodeName = new ArrayList<String>();
    listNodeName.add(valueNodeName);
    UITranslationFormMultiValueInputSet uiTranslationFormMultiValueInputSet = getChild(UITranslationFormMultiValueInputSet.class);
    uiTranslationFormMultiValueInputSet.setValue(listNodeName);
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
    UIAddTranslationManager uiAddTranslationManager = getParent();
    uiAddTranslationManager.removeChildById(POPUP_SYMLINK);
  }

  static  public class SaveActionListener extends EventListener<UIAddTranslationForm> {
    public void execute(Event<UIAddTranslationForm> event) throws Exception {
      UIAddTranslationForm uiTranslationForm = event.getSource();
      UIJCRExplorer uiExplorer = uiTranslationForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiTranslationForm.getAncestorOfType(UIApplication.class);
      String pathNode = "";
      UITranslationFormMultiValueInputSet uiSet = uiTranslationForm.getChild(UITranslationFormMultiValueInputSet.class);
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
      if(pathNode == null || pathNode.length() ==0) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.path-node-invalid", null));

        return ;
      }
      String workspaceName = pathNode.substring(0, pathNode.lastIndexOf(":/"));
      pathNode = pathNode.substring(pathNode.lastIndexOf(":/") + 1);
      /*
      String[] arrFilterChar = {"&", "$", "@", ":", "]", "[", "*", "%", "!", "+", "(", ")",
          "'", "#", ";", "}", "{", "/", "|", "\""};
      for(String filterChar : arrFilterChar) {
        if(symLinkName.indexOf(filterChar) > -1) {
          uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.name-not-allowed", null,
              ApplicationMessage.WARNING));

          return;
        }
      }
      */
      NodeFinder nodeFinder = uiTranslationForm.getApplicationComponent(NodeFinder.class);
      try {
        nodeFinder.getItem(workspaceName, pathNode);
      } catch (ItemNotFoundException e) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.non-node",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch (RepositoryException re) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.non-node",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected error occurs", e);
        }
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.non-node",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
      try {
        Node targetNode = (Node) nodeFinder.getItem(workspaceName, pathNode);
        MultiLanguageService langService = uiTranslationForm.getApplicationComponent(MultiLanguageService.class);
        langService.addSynchronizedLinkedLanguage(node, targetNode);
        uiExplorer.updateAjax(event);
      } catch (AccessControlException ace) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.repository-exception",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.repository-exception",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch(NumberFormatException nume) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.numberformat-exception",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch(ConstraintViolationException cve) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.cannot-save",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch(ItemExistsException iee) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.item-exists-exception",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch(UnsupportedRepositoryOperationException unOperationException) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.UnsupportedRepositoryOperationException",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch (SameAsDefaultLangException unOperationException) {
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.translation-node-same-language-default",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        uiApp.addMessage(new ApplicationMessage("UIAddTranslationForm.msg.cannot-save",
                                                null,
                                                ApplicationMessage.WARNING));

        return;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIAddTranslationForm> {
    public void execute(Event<UIAddTranslationForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  static  public class RemoveActionListener extends EventListener<UITranslationFormMultiValueInputSet> {
    public void execute(Event<UITranslationFormMultiValueInputSet> event) throws Exception {
      UITranslationFormMultiValueInputSet uiSet = event.getSource();
      UIComponent uiComponent = uiSet.getParent();
      if (uiComponent instanceof UIAddTranslationForm) {
        UIAddTranslationForm uiTranslationForm = (UIAddTranslationForm)uiComponent;
        String id = event.getRequestContext().getRequestParameter(OBJECTID);
        uiSet.removeChildById(id);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiTranslationForm);
      }
    }
  }

  static public class SelectDocumentActionListener extends EventListener<UITranslationFormMultiValueInputSet> {
    private String fixPath(String path, String driveName, String repo, UIAddTranslationForm uiAddTranslationForm) throws Exception {
      if (path == null || path.length() == 0 ||
          driveName == null || driveName.length() == 0 ||
          repo == null || repo.length() == 0)
        return "";
      ManageDriveService managerDriveService = uiAddTranslationForm.getApplicationComponent(ManageDriveService.class);
      DriveData driveData = managerDriveService.getDriveByName(driveName);
      if (!path.startsWith(driveData.getHomePath()))
        return "";
      if ("/".equals(driveData.getHomePath()))
        return path;
      return path.substring(driveData.getHomePath().length());
    }
    public void execute(Event<UITranslationFormMultiValueInputSet> event) throws Exception {
      UITranslationFormMultiValueInputSet uiSet = event.getSource();
      UIAddTranslationForm uiTranslationForm =  (UIAddTranslationForm) uiSet.getParent();
      UIAddTranslationManager uiAddTranslationManager = uiTranslationForm.getParent();
      UIJCRExplorer uiExplorer = uiTranslationForm.getAncestorOfType(UIJCRExplorer.class);
      String workspaceName = uiExplorer.getCurrentWorkspace();
      String param = "returnField=" + FIELD_SYMLINK;
      UIPopupWindow uiPopupWindow = uiAddTranslationManager.initPopupTaxonomy(POPUP_SYMLINK);

      if (uiTranslationForm.isUseWorkspaceSelector()) {
        UIOneNodePathSelector uiNodePathSelector = uiAddTranslationManager.createUIComponent(UIOneNodePathSelector.class, null, null);
        uiPopupWindow.setUIComponent(uiNodePathSelector);
        uiNodePathSelector.setIsDisable(workspaceName, false);
        uiNodePathSelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
        uiNodePathSelector.setRootNodeLocation(uiExplorer.getRepositoryName(), workspaceName, "/");
        uiNodePathSelector.setIsShowSystem(false);
        uiNodePathSelector.init(WCMCoreUtils.getUserSessionProvider());
        uiNodePathSelector.setSourceComponent(uiTranslationForm, new String[]{param});
      }else {
        Node node =uiExplorer.getCurrentNode();
        UIContentSelectorOne uiNodePathSelector = uiTranslationForm.createUIComponent(UIContentSelectorOne.class, null, null);
        uiPopupWindow.setUIComponent(uiNodePathSelector);
        uiNodePathSelector.init(uiExplorer.getDriveData().getName(),
                                fixPath(node == null ? "" : node.getPath(),
                                        uiExplorer.getDriveData().getName(),
                                        uiExplorer.getRepositoryName(),
                                        uiTranslationForm));
        uiNodePathSelector.getChild(UIContentBrowsePanelOne.class).setSourceComponent(uiTranslationForm, new String[] { param });
      }
      uiPopupWindow.setRendered(true);
      uiPopupWindow.setShow(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAddTranslationManager);
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
