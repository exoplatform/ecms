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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.*;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.control.action.ManageVersionsActionComponent;
import org.exoplatform.ecm.webui.component.explorer.versions.UIActivateVersion;
import org.exoplatform.ecm.webui.component.explorer.versions.UIVersionInfo;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.link.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.*;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 29, 2012
 */
@ComponentConfig (
    template =  "app:/groovy/webui/component/explorer/UIDocumentNodeList.gtmpl",
    events = {
        @EventConfig(listeners = UIDocumentNodeList.ExpandNodeActionListener.class),
        @EventConfig(listeners = UIDocumentNodeList.CollapseNodeActionListener.class),
        @EventConfig(listeners = UIDocumentNodeList.ManageVersionsActionListener.class),
        @EventConfig(listeners = UIDocumentNodeList.MoreActionListener.class)
    }
)
public class UIDocumentNodeList extends UIContainer {

  private static final Log      LOG                                = ExoLogger.getLogger(UIDocumentNodeList.class.getName());

  private UIPageIterator        pageIterator_;

  private LinkManager linkManager_;

  private List dataList_;

  private int padding_;

  private boolean showMoreButton_ = true;

  public UIDocumentNodeList() throws Exception {
    linkManager_ = WCMCoreUtils.getService(LinkManager.class);
    addChild(ManageVersionsActionComponent.class, null, null);
    pageIterator_ = addChild(UIPageIterator.class, null, "UIDocumentNodeListPageIterator");
    padding_ = 0;
  }

  @SuppressWarnings("unchecked")
  public List<Node> getNodeChildrenList() throws Exception {
    return NodeLocation.getNodeListByLocationList(showMoreButton_ ? dataList_ : pageIterator_.getCurrentPageData());
  }

  public void setPageList(PageList p) throws Exception {
    pageIterator_.setPageList(p);
    dataList_ = new ArrayList();
    if (p != null && p.getAvailable() > 0) {
      dataList_.addAll(p.getPage(1));
    }
    updateUIDocumentNodeListChildren();
  }

  public int getPadding() { return padding_; }
  public void setPadding(int value) { padding_ = value; }

  public boolean isShowMoreButton() {
    return showMoreButton_ && (pageIterator_ != null) &&
            (pageIterator_.getPageList() != null) &&
            (dataList_ != null) && (dataList_.size() < pageIterator_.getPageList().getAvailable());
  }
  public void setShowMoreButton(boolean value) { showMoreButton_ = value; }

  public void setCurrentNode(Node node) throws Exception {
    setPageList(this.getPageList(node.getPath()));
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
      if (node instanceof NodeLinkAware) {
        node = ((NodeLinkAware)node).getRealNode();
      }
      try {
        Node targetNode = linkManager_.isLink(node) ? linkManager_.getTarget(node) : node;
        if (targetNode.isNodeType(NodetypeConstant.NT_FOLDER) || targetNode.isNodeType(NodetypeConstant.NT_UNSTRUCTURED)) {
          addUIDocList(getID(node));
        }
      } catch(ItemNotFoundException ine) {
        continue;
      }
    }
  }

  public UIPageIterator getContentPageIterator() {
    return pageIterator_;
  }

  public String getID(Node node) throws Exception {
    return this.getAncestorOfType(UIDocumentInfo.class).getClass().getSimpleName() +
           this.getClass().getSimpleName() + String.valueOf(Math.abs(node.getPath().hashCode()));
  }

  public UIComponent addUIDocList(String id) throws Exception {
    UIDocumentNodeList child = addChild(UIDocumentNodeList.class, null, id);
    child.setPadding(padding_ + 1);
    child.getContentPageIterator().setId(child.getId() + "PageIterator");
    return child;
  }

  /**
   * gets the name of file
   * @param file the file
   * @param title the title
   * @return name of file
   * @throws Exception
   */
  public String getFileName(Node file, String title) throws Exception {
    if (!file.isNodeType(NodetypeConstant.NT_FILE) || title == null) {
      return title;
    } else {
      int index = title.lastIndexOf('.');
      if (index != -1) {
        return title.substring(0, index);
      } else {
        return title;
      }
    }
  }

  /**
   * gets the extension of file
   * @param file the file
   * @param title the title
   * @return extension of file
   * @throws Exception
   */
  public String getFileExtension(Node file, String title) throws Exception {
    if (!file.isNodeType(NodetypeConstant.NT_FILE) || title == null) {
      return "";
    } else {
      int index = title.lastIndexOf('.');
      if (index != -1) {
        return title.substring(index);
      } else {
        return "";
      }
    }
  }

  /**
   * gets date presentation of file
   * @param file the file
   * @return file date presentation
   * @throws Exception
   */
  public String getFileDate(Node file) throws Exception {
    String createdDate = this.getDatePropertyValue(file, NodetypeConstant.EXO_DATE_CREATED);
    String modifiedDate = this.getDatePropertyValue(file, NodetypeConstant.EXO_LAST_MODIFIED_DATE);
    return StringUtils.isEmpty(modifiedDate) ||
            equalDates(file, NodetypeConstant.EXO_DATE_CREATED, NodetypeConstant.EXO_LAST_MODIFIED_DATE)?
            getLabel("CreatedOn") + " " + createdDate : getLabel("Updated") + " " +  modifiedDate;
  }

  private boolean equalDates(Node node, String p1, String p2) {
    Calendar pr1 = null;
    Calendar pr2 = null;
    try {
      pr1 = node.getProperty(p1).getDate();
    } catch (PathNotFoundException e) {
      pr1 = null;
    } catch (ValueFormatException e) {
      pr1 = null;
    } catch (RepositoryException e) {
      pr1 = null;
    }
    try {
      pr2 = node.getProperty(p2).getDate();
    } catch (PathNotFoundException e) {
      pr2 = null;
    } catch (ValueFormatException e) {
      pr2 = null;
    } catch (RepositoryException e) {
      pr2 = null;
    }
    if ((pr1 == null) && (pr2 == null)) return true;
    if ((pr1 == null) || (pr2 == null)) return false;
    return Math.abs(pr1.getTimeInMillis() - pr2.getTimeInMillis()) < 3000;
  }

  public String getDatePropertyValue(Node node, String propertyName) throws Exception {
    try {
      Property property = node.getProperty(propertyName);
      if(property != null) {
        Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, locale);
        return dateFormat.format(property.getDate().getTime());
      }
    } catch(PathNotFoundException PNE) {
      return "";
    }
    return "";
  }

  /**
   * gets label
   * @param id the id
   * @return label
   */
  public String getLabel(String id)  {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    try {
      return res.getString("UIDocumentNodeList.label." + id);
    } catch (MissingResourceException ex) {
      return id;
    }
  }

  /**
   * gets version number of the node
   * @param file the node
   * @return version number
   */
  protected String getVersionNumber(Node file) throws Exception {
    String currentVersion = null;
    if (file.isNodeType(NodetypeConstant.MIX_VERSIONABLE)){
      try {
        if (file.isNodeType(VersionHistoryUtils.MIX_DISPLAY_VERSION_NAME) &&
                file.hasProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY)) {
          //Get max version ID
          int max = (int) file.getProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY).getLong();
          currentVersion = String.valueOf(max-1);
        }else {
          currentVersion = file.getBaseVersion().getName();
          if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
        }
      }catch (Exception e) {
        currentVersion ="0";
      }
      return "V"+currentVersion;
    }
    return "";
  }


  public String getAuthorName(Node file) throws Exception {
    String userName = getAncestorOfType(UIDocumentInfo.class).getPropertyValue(file, NodetypeConstant.EXO_LAST_MODIFIER);
    if (StringUtils.isEmpty(userName) || IdentityConstants.SYSTEM.equals(userName)) {
      return StringUtils.EMPTY;
    }
    return String.format("%s %s",
            getLabel("by"),
            userName.equals(ConversationState.getCurrent().getIdentity().getUserId()) ? getLabel("you") : userName);
  }
  public String getFileSize(Node file) throws Exception {
    return org.exoplatform.services.cms.impl.Utils.fileSize(file);
  }

  @SuppressWarnings("unchecked")
  private PageList<Object> getPageList(String path) throws Exception {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    Preference pref = uiExplorer.getPreference();
    
    DocumentProviderUtils docProviderUtil = DocumentProviderUtils.getInstance();
    if (docProviderUtil.canSortType(pref.getSortType()) && uiExplorer.getAllItemByTypeFilterMap().isEmpty()) {
      return docProviderUtil.getPageList(
               uiExplorer.getWorkspaceName(), 
               path, 
               pref, 
               uiExplorer.getAllItemFilterMap(), 
               uiExplorer.getAllItemByTypeFilterMap(),
               (NodeLinkAware) ItemLinkAware.newInstance(uiExplorer.getWorkspaceName(), path, 
                                                uiExplorer.getNodeByPath(path, uiExplorer.getSession())));

    }
    
    List<Node> nodeList = null;

    UIDocumentInfo uiDocInfo = this.getAncestorOfType(UIDocumentInfo.class);
    int nodesPerPage = pref.getNodesPerPage();

    Set<String> allItemByTypeFilterMap = uiExplorer.getAllItemByTypeFilterMap();
    if (allItemByTypeFilterMap.size() > 0)
      nodeList = uiDocInfo.filterNodeList(uiExplorer.getChildrenList(path, !pref.isShowPreferenceDocuments()));
    else
      nodeList = uiDocInfo.filterNodeList(uiExplorer.getChildrenList(path, pref.isShowPreferenceDocuments()));

    ListAccess<Object> nodeAccList =
        new ListAccessImpl<Object>(Object.class, NodeLocation.getLocationsByNodeList(nodeList));
    return new LazyPageList<Object>(nodeAccList, nodesPerPage);
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
//        uiExplorer.setSelectNode(workspaceName, uri);
        Node clickedNode = (Node)item;
//        UIDocumentNodeList uiDocNodeListChild = uicomp.addChild(UIDocumentNodeList.class, null,
//                                                                String.valueOf(clickedNode.getPath().hashCode()));
        UIDocumentNodeList uiDocNodeListChild = uicomp.getChildById(uicomp.getID(clickedNode));
        uiDocNodeListChild.setCurrentNode(clickedNode);
        uicomp.getAncestorOfType(UIDocumentInfo.class).getExpandedFolders().add(uri);
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

  static public class CollapseNodeActionListener extends EventListener<UIDocumentNodeList> {
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
        UIDocumentNodeList uiDocNodeListChild = uicomp.getChildById(uicomp.getID(clickedNode));
        uiDocNodeListChild.setPageList(EmptySerializablePageList.get());
        uicomp.getAncestorOfType(UIDocumentInfo.class).getExpandedFolders().remove(uri);
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


  public static class ManageVersionsActionListener extends EventListener<UIDocumentNodeList> {
    public void execute(Event<UIDocumentNodeList> event) throws Exception {
      NodeFinder nodeFinder = event.getSource().getApplicationComponent(NodeFinder.class);
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      String uri = event.getRequestContext().getRequestParameter(OBJECTID);
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      // Manage ../ and ./
      uri = LinkUtils.evaluatePath(uri);
      // Just in order to check if the node exists
      Node currentNode = (Node)nodeFinder.getItem(workspaceName, uri);
      uiExplorer.setIsHidePopup(false);
      if (currentNode.canAddMixin(Utils.MIX_VERSIONABLE)) {
        UIPopupContainer.activate(UIActivateVersion.class, 400);
        event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
      } else if (currentNode.isNodeType(Utils.MIX_VERSIONABLE)) {
        UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
        UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
        UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
        uiVersionInfo.setCurrentNode(currentNode);
        uiVersionInfo.setRootOwner(currentNode.getProperty("exo:lastModifier").getString());
        uiVersionInfo.activate();
        uiDocumentWorkspace.setRenderedChild(UIVersionInfo.class);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace);
      }
    }
  }

  public static class MoreActionListener extends EventListener<UIDocumentNodeList> {
    public void execute(Event<UIDocumentNodeList> event) throws Exception {
      UIDocumentNodeList uicomp = event.getSource();

      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      try {
        int currentPage = uicomp.dataList_.size() / uicomp.pageIterator_.getPageList().getPageSize();
        uicomp.dataList_.addAll(uicomp.pageIterator_.getPageList().getPage(currentPage + 1));
        event.getRequestContext().addUIComponentToUpdateByAjax(uicomp);
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
