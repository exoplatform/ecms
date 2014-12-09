package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import org.exoplatform.ecm.webui.component.explorer.UIConfirmMessage;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;

import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotFolderChildFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIOpenDocumentForm;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by toannh on 11/26/14.
 * Filter files can be open by Office or OS
 */
@ComponentConfig(
        events = {
                @EventConfig(listeners = OpenDocumentManageComponent.OpenDocumentActionListener.class)
        })
public class OpenDocumentManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] {
          new IsNotTrashHomeNodeFilter(),
          new IsNotInTrashFilter(),
          new IsDocumentFilter(),
          new IsNotEditingDocumentFilter(),
          new IsNotFolderChildFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class OpenDocumentActionListener extends UIActionBarActionListener<OpenDocumentManageComponent> {
    public void processEvent(Event<OpenDocumentManageComponent> event) throws Exception {
      HttpServletRequest httpServletRequest = Util.getPortalRequestContext().getRequest();
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      String objId = event.getRequestContext().getRequestParameter(OBJECTID);

      String repo = WCMCoreUtils.getRepository().getConfiguration().getName();
      Node currentNode=null;
      if(objId!=null){
        String _ws = objId.split(":")[0];
        String _nodePath = objId.split(":")[1];
        Session _session = uiExplorer.getSessionByWorkspace(_ws);
        currentNode = uiExplorer.getNodeByPath(_nodePath, _session);
      }else{
        currentNode = uiExplorer.getCurrentNode();
      }
      String nodePath=currentNode.getPath();
      String ws = currentNode.getSession().getWorkspace().getName();
      String filePath = httpServletRequest.getScheme()+ "://" + httpServletRequest.getServerName() + ":"
              +httpServletRequest.getServerPort() + "/"
              + WCMCoreUtils.getRestContextName()+ "/private/jcr/" + repo + "/" + ws + nodePath;

      if(!PermissionUtil.canSetProperty(currentNode) && currentNode.isLocked()){
        String[] userLock = {currentNode.getLock().getLockOwner()};

        UIWorkingArea uiWorkingArea = event.getSource().getParent();
        UIOpenDocumentForm uiOpenDocumentForm = uiWorkingArea.createUIComponent(UIOpenDocumentForm.class, null, null);
        uiOpenDocumentForm.setId("UIReadOnlyFileConfirmMessage");
        uiOpenDocumentForm.setMessageKey("UIPopupMenu.msg.lock-node-read-only");
        uiOpenDocumentForm.setArguments(userLock);
        uiOpenDocumentForm.setFilePath(nodePath);
        uiOpenDocumentForm.setWorkspace(ws);
        uiOpenDocumentForm.setAbsolutePath(filePath);
        UIPopupWindow popUp = uiExplorer.getChild(UIPopupWindow.class);
        popUp.setUIComponent(uiOpenDocumentForm);

        popUp.setShowMask(true);
        popUp.setShow(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(popUp);
      }else{
        event.getRequestContext().getJavascriptManager().require("SHARED/openDocumentInOffice")
              .addScripts("eXo.ecm.OpenDocumentInOffice.openDocument('"+filePath+"', '"+ws+"', '"+nodePath+"');");
      }
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
