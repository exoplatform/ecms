package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanEditDocFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNtFileFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          toannh@exoplatform.com
 * On Dec 10, 2014
 * Filter files can be open by Office or OS
 */
@ComponentConfig(
        events = {
                @EventConfig(listeners = OpenDocumentManageComponent.OpenDocumentActionListener.class)
        })
public class OpenDocumentManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[]{
          new IsNotTrashHomeNodeFilter(),
          new IsNotInTrashFilter(),
          new IsNotEditingDocumentFilter(),
          new IsNtFileFilter(),
          new CanEditDocFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static class OpenDocumentActionListener extends UIActionBarActionListener<OpenDocumentManageComponent> {
    public void processEvent(Event<OpenDocumentManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      String objId = event.getRequestContext().getRequestParameter(OBJECTID);

      Node currentNode=null;
      if(objId!=null){
        String _ws = objId.split(":")[0];
        String _nodePath = objId.split(":")[1];
        Session _session = uiExplorer.getSessionByWorkspace(_ws);
        currentNode = uiExplorer.getNodeByPath(_nodePath, _session);
      }else{
        currentNode = uiExplorer.getCurrentNode();
      }
      Utils.openDocumentInDesktop(currentNode, uiExplorer.getChild(UIPopupContainer.class), event);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

}
