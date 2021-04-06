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
package org.exoplatform.ecm.webui.component.explorer.control.action;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanAddSymlinkFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsLockForSymlink;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotSymlinkFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.symlink.UISymLinkManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 6 mai 2009
 */
@ComponentConfig(
     events = {
       @EventConfig(listeners = AddSymLinkActionComponent.AddSymLinkActionListener.class)
     }
 )

public class AddSymLinkActionComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS =
    Arrays.asList(new UIExtensionFilter[]{new CanAddNodeFilter(),
                                          new CanAddSymlinkFilter(),
                                          new IsLockForSymlink(),
                                          new IsCheckedOutFilter(),
                                          new IsNotSymlinkFilter(),
                                          new IsNotTrashHomeNodeFilter(),
                                          new IsNotInTrashFilter(),
                                          new IsNotEditingDocumentFilter()});

  private static final Log LOG = ExoLogger.getLogger(AddSymLinkActionComponent.class.getName());

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class AddSymLinkActionListener extends UIActionBarActionListener<AddSymLinkActionComponent> {
    public void processEvent(Event<AddSymLinkActionComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      Node currentNode = uiExplorer.getCurrentNode();
      UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
      LinkManager linkManager = uiWorkingArea.getApplicationComponent(LinkManager.class);
      String symLinkName;
      try {
        if ((srcPath != null) && (srcPath.indexOf(";") > -1)) {

          String[] nodePaths = srcPath.split(";");
          for (int i = 0; i < nodePaths.length; i++) {
            Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePaths[i]);
            String wsName = null;
            if (matcher.find()) {
              wsName = matcher.group(1);
              nodePaths[i] = matcher.group(2);
            } else {
              throw new IllegalArgumentException("The ObjectId is invalid '" + nodePaths[i] + "'");
            }
            Session userSession = uiExplorer.getSessionByWorkspace(wsName);
            // Use the method getNodeByPath because it is link aware
            Node selectedNode = uiExplorer.getNodeByPath(nodePaths[i], userSession, false);
            // Reset the path to manage the links that potentially create
            // virtual path
            nodePaths[i] = selectedNode.getPath();
            if (linkManager.isLink(selectedNode)) {
              Object[] args = { selectedNode.getPath() };
              uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.selected-is-link", args,
                  ApplicationMessage.WARNING));
              
              continue;
            }
            try {
              if (selectedNode.getName().indexOf(".lnk") > -1)
                symLinkName = selectedNode.getName();
              else
                symLinkName = selectedNode.getName() + ".lnk";
              linkManager.createLink(currentNode, Utils.EXO_SYMLINK, selectedNode, symLinkName);
            } catch (Exception e) {
              Object[] arg = { Text.unescapeIllegalJcrChars(selectedNode.getPath()),
                  Text.unescapeIllegalJcrChars(currentNode.getPath()) };
              uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.create-link-problem",
                                                      arg,
                                                      ApplicationMessage.WARNING));

              return;
            }
          }
          uiExplorer.updateAjax(event);
        } else {
          if (srcPath != null) {
            Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
            String wsName = null;
            if (matcher.find()) {
              wsName = matcher.group(1);
              srcPath = matcher.group(2);
            } else {
              throw new IllegalArgumentException("The ObjectId is invalid '" + srcPath + "'");
            }
            Session userSession = uiExplorer.getSessionByWorkspace(wsName);
            // Use the method getNodeByPath because it is link aware
            Node selectedNode = uiExplorer.getNodeByPath(srcPath, userSession, false);
            // Reset the path to manage the links that potentially create
            // virtual path
            srcPath = selectedNode.getPath();
            // Reset the session to manage the links that potentially change of
            // workspace
            userSession = selectedNode.getSession();
            if (linkManager.isLink(selectedNode)) {
              Object[] args = { selectedNode.getPath() };
              uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.selected-is-link", args,
                  ApplicationMessage.WARNING));
              
              return;
            }
            if (selectedNode.getName().indexOf(".lnk") > -1)
              symLinkName = selectedNode.getName();
            else
              symLinkName = selectedNode.getName() + ".lnk";
            try {
              linkManager.createLink(currentNode, Utils.EXO_SYMLINK, selectedNode, symLinkName);
            } catch (Exception e) {
              Object[] arg = { Text.unescapeIllegalJcrChars(selectedNode.getPath()),
                  Text.unescapeIllegalJcrChars(currentNode.getPath()) };
              uiApp.addMessage(new ApplicationMessage("UIWorkingArea.msg.create-link-problem",
                                                      arg,
                                                      ApplicationMessage.WARNING));

              return;
            }
            uiExplorer.updateAjax(event);
          } else {
        UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
        UISymLinkManager uiSymLinkManager = event.getSource().createUIComponent(UISymLinkManager.class, null, null);
        uiSymLinkManager.useDriveSelector();
        UIPopupContainer.activate(uiSymLinkManager, 600, 190);
        event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
          }
        }
      } catch (AccessControlException ace) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.repository-exception", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (AccessDeniedException ade) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.repository-exception", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (NumberFormatException nume) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.numberformat-exception", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (ConstraintViolationException cve) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.cannot-save", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (ItemExistsException iee) {
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.item-exists-exception", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs while adding a symlink to the node", e);
        }
        uiApp.addMessage(new ApplicationMessage("UISymLinkForm.msg.cannot-save", null,
            ApplicationMessage.WARNING));
        
        return;
      }
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
