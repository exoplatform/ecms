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

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

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
  protected String rootVersionNum_;;
  protected VersionNode curentVersion_;
  protected NodeLocation node_ ;
  private UIPageIterator uiPageIterator_ ;
  private List<VersionNode> listVersion = new ArrayList<VersionNode>() ;


  public UIVersionInfo() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "VersionInfoIterator").setRendered(false);
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public List getListRecords() throws Exception { return uiPageIterator_.getCurrentPageData(); }

  @SuppressWarnings("unchecked")
  public void updateGrid() throws Exception {
    listVersion.clear();
    listVersion = getNodeVersions(getRootVersionNode().getChildren());
    if (!isRestoredVersions(listVersion)) {
      listVersion.add(0, getRootVersionNode());
    }
//    try {
//      if (isRestoredLabel(getVersionLabels(listVersion.get(2))[0])) listVersion.remove(1);
//    } catch (Exception e) {}
    setRootVersionNum(String.valueOf(listVersion.size()));
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
    Version versionNode = vH.getVersion(version.getName());
    return vH.getVersionLabels(versionNode);
  }

  public boolean isBaseVersion(VersionNode versionNode) throws Exception {
    if (!isRestoredVersions(listVersion))  return isRootversion(versionNode);
    else if (NodeLocation.getNodeByLocation(node_).getBaseVersion().getName().equals(versionNode.getName())) return true ;
    return false ;
  }

  public boolean isRootversion(VersionNode versionNode) throws Exception {
    return (versionNode.getAuthor().isEmpty());
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
    return listVersion ;
  }

  public void activate() {
    try {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
      if (node_ == null) {
        node_ = NodeLocation.getNodeLocationByNode(uiExplorer.getCurrentNode());
      }
      rootVersion_ = new VersionNode(NodeLocation.getNodeByLocation(node_)
                                                 .getVersionHistory()
                                                 .getRootVersion(), uiExplorer.getSession());
      curentVersion_ = rootVersion_;
      updateGrid();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error!", e.getMessage());
      }
    }
  }

  public VersionNode getCurrentVersionNode() { return curentVersion_ ;}

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

  static  public class RestoreVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class) ;
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
        AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
        autoVersionService.autoVersion(currentNode);
        currentNode.restore(fromVersionName,true);
        currentNode.checkout();
        Version restoredVersion = currentNode.checkin();
        currentNode.checkout();
        String versionName = restoredVersion.getName();
        uiVersionInfo.curentVersion_ = uiVersionInfo.rootVersion_.findVersionNode(restoredVersion.getPath());
        ResourceBundle res = event.getRequestContext().getApplicationResourceBundle() ;
        String restoredFromMsg = res.getString("UIDiff.label.restoredFrom").replace("{0}", fromVersionName);
        currentNode.getVersionHistory().addVersionLabel(versionName, restoredFromMsg+ "_" + versionName, false);
        ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
        ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
        try {
          if (listenerService!=null && activityService !=null && activityService.isAcceptedNode(currentNode)) {
            listenerService.broadcast(ActivityCommonService.NODE_REVISION_CHANGED, currentNode, versionName);
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

  static  public class DeleteVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class) ;
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      Node node = uiVersionInfo.getCurrentNode() ;
      VersionHistory versionHistory = node.getVersionHistory() ;
      UIApplication app = uiVersionInfo.getAncestorOfType(UIApplication.class) ;
      try {
        versionHistory.removeVersion(uiVersionInfo.curentVersion_ .getName());
        uiVersionInfo.rootVersion_.removeVersionInChild(uiVersionInfo.rootVersion_, uiVersionInfo.curentVersion_);
        uiVersionInfo.rootVersion_ = new VersionNode(node.getVersionHistory().getRootVersion(), uiExplorer.getSession()) ;
        uiVersionInfo.curentVersion_ = uiVersionInfo.rootVersion_ ;
        if(!node.isCheckedOut()) node.checkout() ;
        uiExplorer.getSession().save() ;
        uiVersionInfo.activate();
        event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
      } catch (ReferentialIntegrityException rie) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", rie);
        }
        app.addMessage(new ApplicationMessage("UIVersionInfo.msg.cannot-remove-version",null)) ;
        return;
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        app.addMessage(new ApplicationMessage("UIVersionInfo.msg.cannot-remove-version",null)) ;
        return;
      }
    }
  }

  static  public class CompareVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIDocumentWorkspace uiDocumentWorkspace = uiVersionInfo.getAncestorOfType(UIDocumentWorkspace.class);
      for(UIComponent uiChild : uiDocumentWorkspace.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String version1 = event.getRequestContext().getRequestParameter("versions").split(",")[0];
      String version2 = event.getRequestContext().getRequestParameter("versions").split(",")[1];
      UIDiff uiDiff = uiDocumentWorkspace.getChild(UIDiff.class) ;
      Node node = uiVersionInfo.getCurrentNode() ;
      VersionHistory versionHistory = node.getVersionHistory() ;
      uiDiff.setRootAuthor(uiVersionInfo.getRootOwner());
      uiDiff.setRootVersionNum(uiVersionInfo.getRootVersionNum());
      uiDiff.setOriginNodePath(uiVersionInfo.getCurrentNode().getPath());
      if (version1.equals(uiVersionInfo.getRootVersionNum())) {
        uiDiff.setVersions(versionHistory.getVersion(version2), uiVersionInfo.getCurrentNode().getVersionHistory().getRootVersion());
      } else if (version2.equals(uiVersionInfo.getRootVersionNum())) {
        uiDiff.setVersions(uiVersionInfo.getCurrentNode().getVersionHistory().getRootVersion(), versionHistory.getVersion(version1));
      } else if(Integer.parseInt(version1) < Integer.parseInt(version2)) {
        uiDiff.setVersions(versionHistory.getVersion(version1), versionHistory.getVersion(version2));
      } else {
        uiDiff.setVersions(versionHistory.getVersion(version2), versionHistory.getVersion(version1));
      }
      uiDiff.setRendered(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace) ;
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

  static  public class AddSummaryActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      String currentVersionName = uiVersionInfo.curentVersion_.getName();
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
        if(!currentNode.getVersionHistory().hasVersionLabel(summary)) {
          Version currentVersion = currentNode.getVersionHistory().getVersion(currentVersionName);
          for(String label : currentNode.getVersionHistory().getVersionLabels(currentVersion)) {
            currentNode.getVersionHistory().removeVersionLabel(label);
          }
          currentNode.getVersionHistory().addVersionLabel(currentVersionName, summary, false);
        }
      } catch (VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UILabelForm.msg.label-exist", new Object[]{summary})) ;
        return ;
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
    }
  }
}
