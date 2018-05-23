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
package org.exoplatform.ecm.webui.component.explorer.versions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.pdfviewer.ObjectKey;
import org.exoplatform.services.pdfviewer.PDFViewerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.connector.viewer.PDFViewerRESTService;
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
 * Created by The eXo Platform SARL
 * Implement: lxchiati
 *            lebienthuy@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/versions/UIVersionInfo.gtmpl",
    events = {
        @EventConfig(listeners = UIVersionInfo.SelectActionListener.class),
        @EventConfig(listeners = UIVersionInfo.RestoreVersionActionListener.class, confirm = "UIVersionInfo.msg.confirm-restore"),
        @EventConfig(listeners = UIVersionInfo.CompareVersionActionListener.class),
        @EventConfig(listeners = UIVersionInfo.DeleteVersionActionListener.class, confirm = "UIVersionInfo.msg.confirm-delete"),
        @EventConfig(listeners = UIVersionInfo.CloseActionListener.class),
        @EventConfig(listeners = UIVersionInfo.AddSummaryActionListener.class)
    }
)

public class UIVersionInfo extends UIContainer  {
  private static final Log LOG  = ExoLogger.getLogger(UIVersionInfo.class.getName());

  protected VersionNode rootVersion_ ;
  protected String rootOwner_;
  protected String rootVersionNum_;
  protected VersionNode curentVersion_;
  protected NodeLocation node_ ;
  private UIPageIterator uiPageIterator_ ;
  private List<VersionNode> listVersion = new ArrayList<VersionNode>() ;

  private static final String CACHE_NAME = "ecms.PDFViewerRestService";


  public UIVersionInfo() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "VersionInfoIterator").setRendered(false);
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  @SuppressWarnings("rawtypes")
  public List getListRecords() throws Exception { return uiPageIterator_.getCurrentPageData(); }

  public void updateGrid() throws Exception {
    listVersion.clear();
    Node currentNode = getCurrentNode();
    rootVersion_ = new VersionNode(currentNode, currentNode.getSession());
    curentVersion_ = rootVersion_;

    listVersion = getNodeVersions(getRootVersionNode().getChildren());
    VersionNode currentNodeTuple = new VersionNode(currentNode, currentNode.getSession());
    if(!listVersion.isEmpty()) {
      int lastVersionNum = Integer.parseInt(listVersion.get(0).getName());
      setRootVersionNum(String.valueOf(++lastVersionNum));
    } else {
      setRootVersionNum(String.valueOf(listVersion.size() + 1));
      currentNodeTuple.setDisplayName("0");
    }
    listVersion.add(0, currentNodeTuple);

    ListAccess<VersionNode> recordList = new ListAccessImpl<VersionNode>(VersionNode.class, listVersion);
    LazyPageList<VersionNode> dataPageList = new LazyPageList<VersionNode>(recordList, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  private boolean isRestoredVersions(List<VersionNode> list)  {
    try {
      for (int i = 0; i < list.size(); i++) {
        if (getVersionLabels(list.get(i)).length > 0) {
          if (isRestoredLabel(getVersionLabels(list.get(i))[0])) return true;
        }
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }

  private boolean isRestoredLabel(String label) {
    try {
      String from = label.substring(label.indexOf("_") - 1).split("_")[0];
      String to = label.substring(label.indexOf("_") - 1).split("_")[1];
      Integer.parseInt(from);
      Integer.parseInt(to);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String[] getVersionLabels(VersionNode version) throws Exception {
    VersionHistory vH = NodeLocation.getNodeByLocation(node_).getVersionHistory();
    String[] labels;
    if (StringUtils.isNotBlank(version.getName()) && !getRootVersionNum().equals(version.getName())) {
      Version versionNode = vH.getVersion(version.getName());
      labels = vH.getVersionLabels(versionNode);
    } else {
      labels= vH.getVersionLabels(vH.getRootVersion());
    }
    return labels;
  }

  public boolean isBaseVersion(VersionNode versionNode) throws Exception {
    if (!isRestoredVersions(listVersion)) {
      return isRootVersion(versionNode);
    } else {
      return versionNode.getPath().equals(getCurrentNode().getPath());
    }
  }

  public boolean hasPermission(Node node) throws Exception {
    if (getCurrentNode().getPath().startsWith("/Groups/spaces")) {
      MembershipEntry mem = new MembershipEntry("/spaces/" + getCurrentNode().getPath().split("/")[3], "manager");
      return (ConversationState.getCurrent().getIdentity().getMemberships().contains(mem)
          || ConversationState.getCurrent().getIdentity().getUserId().equals(node.getProperty("exo:lastModifier").getString()));

    } else {
      return true;
    }
  }

  public boolean isRootVersion(VersionNode versionNode) throws Exception {
    return (versionNode.getUUID().equals(getCurrentNode().getUUID()));
  }

  public VersionNode getRootVersionNode() throws Exception {  return rootVersion_ ; }

  public String getRootOwner() throws Exception {  return rootOwner_ ; }

  public void setRootOwner(String user) {  this.rootOwner_ = user; }

  private List<VersionNode> getNodeVersions(List<VersionNode> children) throws Exception {
    List<VersionNode> child = new ArrayList<VersionNode>() ;
    for(int i = 0; i < children.size(); i ++){
      listVersion.add(children.get(i));
      child = children.get(i).getChildren() ;
      if(!child.isEmpty()) getNodeVersions(child) ;
    }
    listVersion.sort(new Comparator<VersionNode>() {
      @Override
      public int compare(VersionNode v1, VersionNode v2) {
        try {
          if (Integer.parseInt(v1.getName()) < Integer.parseInt(v2.getName()))
            return 1;
          else
            return 0;
        }catch (Exception e) {
          return 0;
        }
      }
    });
    return listVersion;
  }

  public void activate() {
    try {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
      if (node_ == null) {
        node_ = NodeLocation.getNodeLocationByNode(uiExplorer.getCurrentNode());
      }
      updateGrid();
    } catch (Exception e) {
      LOG.error("Unexpected error!", e);
    }
  }

  public VersionNode getCurrentVersionNode() { return curentVersion_ ;}

  public Node getVersion(String versionName) throws RepositoryException {
    Node currentNode = getCurrentNode();
    if ((StringUtils.isBlank(versionName) && StringUtils.isBlank(getCurrentVersionNode().getName()))
        || (StringUtils.isNotBlank(versionName) && StringUtils.isNotBlank(getCurrentVersionNode().getName())
            && getCurrentVersionNode().getName().equals(versionName))) {
      return currentNode;
    }
    for (VersionNode versionNode : listVersion) {
      if(versionNode.getName().equals(versionName)) {
        return currentNode.getVersionHistory().getVersion(versionName);
      }
    }
    return null;
  }

  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(node_);
  }

  public void setCurrentNode(Node node) {
    node_ = NodeLocation.getNodeLocationByNode(node);
  }

  public List<VersionNode> getListVersion() {
    return listVersion;
  }

  public void setListVersion(List<VersionNode> listVersion) {
    this.listVersion = listVersion;
  }

  public String getLinkInDocumentsApp(String nodePath) throws Exception {
    DocumentService documentService = WCMCoreUtils.getService(DocumentService.class);
    return documentService.getLinkInDocumentsApp(nodePath);
  }

  public void setRootVersionNum(String rootVersionNum) {
    this.rootVersionNum_ = rootVersionNum;
  }

  public String getRootVersionNum() {
    return rootVersionNum_;
  }

  private boolean isWebContent() throws Exception {
    Node currentNode = getCurrentNode();
    if (currentNode != null) {
      return currentNode.isNodeType(Utils.EXO_WEBCONTENT);
    }
    return false;
  }

  static  public class RestoreVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class) ;
      PDFViewerService pdfViewerService = WCMCoreUtils.getService(PDFViewerService.class);
      CacheService caService = WCMCoreUtils.getService(CacheService.class);
      ExoCache<Serializable, Object> pdfCache;
      if(pdfViewerService != null){
        pdfCache = pdfViewerService.getCache();
      }else{
        pdfCache = caService.getCacheInstance(CACHE_NAME);
      }
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      VersionNode currentVersionNode = uiVersionInfo.rootVersion_.findVersionNode(objectId);
      String fromVersionName  = currentVersionNode.getName() ;
      UIApplication uiApp = uiVersionInfo.getAncestorOfType(UIApplication.class) ;
      Node currentNode = uiVersionInfo.getCurrentNode();
      uiExplorer.addLockToken(currentNode);
      try {
        if(!currentNode.isCheckedOut()) {
          currentNode.checkout();
        }
        AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
        Version addedVersion = autoVersionService.autoVersion(currentNode);
        currentNode.restore(fromVersionName,true);
        if(!currentNode.isCheckedOut()) {
          currentNode.checkout();
        }
        StringBuilder bd = new StringBuilder();
        bd.append(((ManageableRepository)currentNode.getSession().getRepository()).getConfiguration().getName()).
                append("/").append(currentNode.getSession().getWorkspace().getName()).append("/").
                append(currentNode.getUUID());
        StringBuilder bd1 = new StringBuilder().append(bd).append("/jcr:lastModified");
        StringBuilder bd2 = new StringBuilder().append(bd).append("/jcr:baseVersion");
        pdfCache.remove(new ObjectKey(bd.toString()));
        pdfCache.remove(new ObjectKey(bd1.toString()));
        pdfCache.remove(new ObjectKey(bd2.toString()));

        int lastVersionIndice = Integer.parseInt(addedVersion.getName());

        String restoredFromMsg = "UIDiff.label.restoredFrom_" + fromVersionName + "_" + (lastVersionIndice + 1);
        VersionHistory versionHistory = currentNode.getVersionHistory();
        versionHistory.addVersionLabel(versionHistory.getRootVersion().getName(), restoredFromMsg, false);

        ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
        ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
        try {
          if (listenerService!=null && activityService !=null && activityService.isAcceptedNode(currentNode)) {
            listenerService.broadcast(ActivityCommonService.NODE_REVISION_CHANGED, currentNode, fromVersionName);
          }
        }catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Can not notify NodeMovedActivity: " + e.getMessage());
          }
        }
      } catch(JCRInvalidItemStateException invalid) {
        uiApp.addMessage(new ApplicationMessage("UIVersionInfo.msg.invalid-item-state", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch(NullPointerException nuException){
        uiApp.addMessage(new ApplicationMessage("UIVersionInfo.msg.invalid-item-state", null,
            ApplicationMessage.WARNING)) ;

        return;
      } catch(Exception e) {
        //JCRExceptionManager.process(uiApp, e);
        uiApp.addMessage(new ApplicationMessage("UIVersionInfo.msg.invalid-item-state", null,
            ApplicationMessage.WARNING)) ;

        return;
      }
      uiVersionInfo.activate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
      uiExplorer.setIsHidePopup(true) ;
    }
  }

  static public class DeleteVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class);
      for (UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false);
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiVersionInfo.curentVersion_ = uiVersionInfo.getRootVersionNode().findVersionNode(objectId);
      Node node = uiVersionInfo.getCurrentNode();
      UIApplication app = uiVersionInfo.getAncestorOfType(UIApplication.class);
      try {
        node.getSession().save();
        node.getSession().refresh(false);
        VersionHistoryUtils.removeVersion(uiVersionInfo.getCurrentNode(), uiVersionInfo.curentVersion_.getName() );
        uiVersionInfo.rootVersion_ = new VersionNode(node, uiExplorer.getSession());
        uiVersionInfo.curentVersion_ = uiVersionInfo.rootVersion_;
        if (!node.isCheckedOut())
          node.checkout();
        uiExplorer.getSession().save();
        uiVersionInfo.activate();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo);
      } catch (ReferentialIntegrityException rie) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", rie);
        }
        app.addMessage(new ApplicationMessage("UIVersionInfo.msg.error-removing-referenced-version", null, ApplicationMessage.ERROR));
        return;
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        app.addMessage(new ApplicationMessage("UIVersionInfo.msg.error-removing-version", null, ApplicationMessage.ERROR));
        return;
      }
    }
  }

  static  public class CompareVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIDocumentWorkspace uiDocumentWorkspace = uiVersionInfo.getAncestorOfType(UIDocumentWorkspace.class);
      for (UIComponent uiChild : uiDocumentWorkspace.getChildren()) {
        uiChild.setRendered(false);
      }
      String fromVersionName = event.getRequestContext().getRequestParameter("versions").split(",")[0];
      String toVersionName = event.getRequestContext().getRequestParameter("versions").split(",")[1];
      UIDiff uiDiff = uiDocumentWorkspace.getChild(UIDiff.class);
      uiDiff.setVersions(uiVersionInfo.getVersion(fromVersionName), uiVersionInfo.getVersion(toVersionName));
      uiDiff.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace);
    }
  }

  static public class SelectActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource() ;
      String path = event.getRequestContext().getRequestParameter(OBJECTID) ;
      VersionNode root = uiVersionInfo.getRootVersionNode() ;
      VersionNode selectedVersion= root.findVersionNode(path);
      selectedVersion.setExpanded(!selectedVersion.isExpanded()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
    }
  }

  static public class CloseActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        if (uiChild.isRendered()) {
          uiChild.setRendered(false);
          return ;
        }
      }
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.updateAjax(event) ;
    }
  }

  public static class AddSummaryActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      String currentVersionName = uiVersionInfo.curentVersion_.getName();
      if(StringUtils.isBlank(currentVersionName)) {
        currentVersionName = uiVersionInfo.getRootVersionNum();
      }
      String summary = event.getRequestContext().getRequestParameter("value") + "_" + currentVersionName;
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiVersionInfo.getAncestorOfType(UIApplication.class) ;
      Node currentNode = uiExplorer.getCurrentNode() ;
      if(!Utils.isNameValid(summary, Utils.SPECIALCHARACTER)) {
        uiApp.addMessage(new ApplicationMessage("UILabelForm.msg.label-invalid",
            null, ApplicationMessage.WARNING)) ;
        return ;
      }
      try{
        if(StringUtils.isNotBlank(summary) && !currentNode.getVersionHistory().hasVersionLabel(summary)) {
          Version currentVersion = null;
          if(currentVersionName.equals(uiVersionInfo.getRootVersionNum())) {
            currentVersion = currentNode.getVersionHistory().getRootVersion();
          } else {
            currentVersion = currentNode.getVersionHistory().getVersion(currentVersionName);
          }
          String[] versionLabels = currentNode.getVersionHistory().getVersionLabels(currentVersion);
          for(String label : versionLabels) {
            currentNode.getVersionHistory().removeVersionLabel(label);
          }
          currentNode.getVersionHistory().addVersionLabel(currentVersion.getName(), summary, false);
        }
      } catch (VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UILabelForm.msg.label-exist", new Object[]{summary})) ;
        return ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
    }
  }
}
