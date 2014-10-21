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
package org.exoplatform.ecm.webui.form;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.pdfviewer.ObjectKey;
import org.exoplatform.services.pdfviewer.PDFViewerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 14, 2008
 */
public class DialogFormActionListeners {

  static public class RemoveDataActionListener extends EventListener<UIDialogForm> {
    public void execute(Event<UIDialogForm> event) throws Exception {
      UIDialogForm uiForm = event.getSource();
//      uiForm.isRemovePreference = true;
      String referenceNodePath = event.getRequestContext().getRequestParameter(UIDialogForm.OBJECTID);
      //String removedNode = event.getRequestContext().getRequestParameter('removedNode");
      uiForm.releaseLock();
      if (referenceNodePath.indexOf("$") > -1) {
        int index = referenceNodePath.indexOf("$");
        String removedNode = referenceNodePath.substring(index + 1);
        referenceNodePath = referenceNodePath.substring(0, index);
        if (StringUtils.isNotEmpty(removedNode)) {
          Node currentNode = uiForm.getNode();
          if (currentNode.isLocked()) {
            Object[] args = { currentNode.getPath() };
            org.exoplatform.wcm.webui.Utils.createPopupMessage(uiForm, "UIPermissionManagerGrid.msg.node-locked", args,
                ApplicationMessage.WARNING);
            return;
          }
          uiForm.addRemovedNode(removedNode);
        }
      }
      if (referenceNodePath.startsWith("/")) {
        Node referenceNode = (Node)uiForm.getSession().getItem(uiForm.getNodePath() + referenceNodePath);
        if(referenceNode.hasProperty(Utils.JCR_DATA)) {
          uiForm.removeData(referenceNodePath);
//          uiForm.setDataRemoved(true);
        }
      } else {
        Node currentNode = uiForm.getNode();
        if (currentNode.isLocked()) {
          Object[] args = { currentNode.getPath() };
          org.exoplatform.wcm.webui.Utils.createPopupMessage(uiForm, "UIPermissionManagerGrid.msg.node-locked", args,
              ApplicationMessage.WARNING);
          return;
        }
        if (currentNode.hasProperty(referenceNodePath)) {
          uiForm.removeData(referenceNodePath);
//          uiForm.setDataRemoved(true);
        }
      }
      clearPDFCached(uiForm.getNode());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }

    private void clearPDFCached(Node currentNode) throws Exception{
      PDFViewerService pdfViewerService = WCMCoreUtils.getService(PDFViewerService.class);
      String wsName = currentNode.getSession().getWorkspace().getName();
      String repoName = WCMCoreUtils.getRepository().getConfiguration().getName();
      String uuid = currentNode.getUUID();
      StringBuilder bd = new StringBuilder();
      StringBuilder bd1 = new StringBuilder();
      bd.append(repoName).append("/").append(wsName).append("/").append(uuid);
      bd1.append(bd).append("/jcr:lastModified");
      pdfViewerService.getCache().remove(new ObjectKey(bd.toString()));
      pdfViewerService.getCache().remove(new ObjectKey(bd1.toString()));
    }
  }

  static public class ChangeTabActionListener extends EventListener<UIDialogForm> {
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIDialogForm> event) throws Exception {
      UIDialogForm uiForm = event.getSource();
      uiForm.setSelectedTab(event.getRequestContext().getRequestParameter(UIDialogForm.OBJECTID));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

}
