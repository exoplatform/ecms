/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 29, 2012  
 */
@ComponentConfig (
    template =  "app:/groovy/webui/component/explorer/UIDocumentNodeList.gtmpl",
    events = {
        @EventConfig(listeners = UIDocumentNodeList.ExpandNodeActionListener.class)
    }
)
public class UIDocumentNodeList extends UIContainer {

  private static final Log      LOG                                = ExoLogger.getLogger(UIDocumentNodeList.class.getName());
  
  private UIPageIterator        pageIterator_;
  
  private int padding_;
  
  public UIDocumentNodeList() throws Exception {
    pageIterator_ = addChild(UIPageIterator.class, null, null);
    padding_ = 0;
  }
  
  @SuppressWarnings("unchecked")
  public List<Node> getNodeChildrenList() throws Exception {
    return NodeLocation.getNodeListByLocationList(pageIterator_.getCurrentPageData());
  }
  
  public void setPageList(PageList p) throws Exception {
    pageIterator_.setPageList(p);
    updateUIDocumentNodeListChildren();
  }
  
  public int getPadding() { return padding_; }
  public void setPadding(int value) { padding_ = value; }
  
  public void setCurrentNode(Node node) throws Exception {
    UIDocumentInfo uiDocInfo = getAncestorOfType(UIDocumentInfo.class);
    setPageList(uiDocInfo.getPageList(node.getPath()));
  }
  
  public void updateUIDocumentNodeListChildren() throws Exception {
    Set<String> ids = new HashSet<String>();
    //get all ids of UIDocumentNodeList children
    for (UIComponent component : getChildren()) {
      if (component instanceof UIDocumentNodeList) {
        ids.add(component.getId());
      }
    }
    //remove all UIDocumentNodeList children
    for (String id : ids) {
      this.removeChildById(id);
    }
    //add new UIDocumentNodeList children
    for (Node node : getNodeChildrenList()) {
      if (node.isNodeType(NodetypeConstant.NT_FOLDER) || node.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)) {
        addUIDocList(getID(node));
      }
    }
  }
  
  public UIPageIterator getContentPageIterator() {
    return pageIterator_;
  }
  
  public String getID(Node node) throws Exception {
    return this.getClass().getSimpleName() + String.valueOf(Math.abs(node.getPath().hashCode())); 
  }
  
  public UIComponent addUIDocList(String id) throws Exception {
    UIDocumentNodeList child = addChild(UIDocumentNodeList.class, null, id);
    child.setPadding(padding_ + 1);
    child.getContentPageIterator().setId(child.getId() + "PageIterator");
    return child;
  }
  
  static public class ExpandNodeActionListener extends EventListener<UIDocumentNodeList> {
    public void execute(Event<UIDocumentNodeList> event) throws Exception {
      UIDocumentNodeList uicomp = event.getSource();

      NodeFinder nodeFinder = uicomp.getApplicationComponent(NodeFinder.class);
      String uri = event.getRequestContext().getRequestParameter(OBJECTID);
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      try {
        // Manage ../ and ./
        uri = LinkUtils.evaluatePath(uri);
        // Just in order to check if the node exists
        Item item = nodeFinder.getItem(workspaceName, uri);
        if ((item instanceof Node) && Utils.isInTrash((Node) item)) {
          return;
        }
        Node clickedNode = (Node)item;
//        UIDocumentNodeList uiDocNodeListChild = uicomp.addChild(UIDocumentNodeList.class, null, 
//                                                                String.valueOf(clickedNode.getPath().hashCode()));
        UIDocumentNodeList uiDocNodeListChild = uicomp.getChildById(uicomp.getID(clickedNode));
        uiDocNodeListChild.setCurrentNode(clickedNode);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocNodeListChild);
      } catch(ItemNotFoundException nu) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.null-exception", null, ApplicationMessage.WARNING)) ;

        return ;
      } catch(PathNotFoundException pa) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.path-not-found", null, ApplicationMessage.WARNING)) ;

        return ;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied", null, ApplicationMessage.WARNING)) ;

        return ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }
  
}
