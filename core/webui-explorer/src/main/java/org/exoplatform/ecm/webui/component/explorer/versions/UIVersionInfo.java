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

import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.*;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import java.io.InputStream;
import java.util.*;

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
        @EventConfig(listeners = UIVersionInfo.ViewVersionActionListener.class),
        @EventConfig(listeners = UIVersionInfo.CompareVersionActionListener.class),
        @EventConfig(listeners = UIVersionInfo.DeleteVersionActionListener.class, confirm = "UIVersionInfo.msg.confirm-delete"),
        @EventConfig(listeners = UIVersionInfo.CloseActionListener.class),
        @EventConfig(listeners = UIVersionInfo.AddSummaryActionListener.class)
    }
)

public class UIVersionInfo extends UIContainer  {
  private static final Log LOG  = ExoLogger.getLogger(UIVersionInfo.class.getName());

  protected VersionNode rootVersion_ ;
  protected VersionNode curentVersion_;
  protected NodeLocation node_ ;
  private UIPageIterator uiPageIterator_ ;
  private List<VersionNode> listVersion = new ArrayList<VersionNode>() ;
  public UIVersionInfo() throws Exception {
    //addChild(UIViewVersion.class, null, null).setRendered(false);
    //addChild(UIDiff.class, null, null).setRendered(false) ;
    uiPageIterator_ = addChild(UIPageIterator.class, null, "VersionInfoIterator").setRendered(false);
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public List getListRecords() throws Exception { return uiPageIterator_.getCurrentPageData(); }

  @SuppressWarnings("unchecked")
  public void updateGrid() throws Exception {
    listVersion.clear();
    listVersion = getNodeVersions(getRootVersionNode().getChildren());
    Collections.reverse(listVersion);
    ListAccess<VersionNode> recordList = new ListAccessImpl<VersionNode>(VersionNode.class, listVersion);
    LazyPageList<VersionNode> dataPageList = new LazyPageList<VersionNode>(recordList, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  public String[] getVersionLabels(VersionNode version) throws Exception {
    VersionHistory vH = NodeLocation.getNodeByLocation(node_).getVersionHistory();
    Version versionNode = vH.getVersion(version.getName());
    return vH.getVersionLabels(versionNode);
  }

  public boolean isBaseVersion(VersionNode versionNode) throws Exception {
    if (NodeLocation.getNodeByLocation(node_).getBaseVersion().getName().equals(versionNode.getName())) return true ;
    return false ;
  }

  public VersionNode getRootVersionNode() throws Exception {  return rootVersion_ ; }

  private List<VersionNode> getNodeVersions(List<VersionNode> children) throws Exception {
    List<VersionNode> child = new ArrayList<VersionNode>() ;
    for(int i = 0; i < children.size(); i ++){
      listVersion.add(children.get(i));
      child = children.get(i).getChildren() ;
      if(!child.isEmpty()) getNodeVersions(child) ;
    }
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
      UIDocumentWorkspace uiDocumentWorkspace = getAncestorOfType(UIDocumentWorkspace.class);
      uiDocumentWorkspace.getChild(UIViewVersion.class).update();
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

  public String getDownloadLink(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    if(!node.getPrimaryNodeType().getName().equals(Utils.NT_FILE)) {
      node = NodeLocation.getNodeByLocation(node_);
    }
    Node jcrContentNode = node.getNode(Utils.JCR_CONTENT) ;
    InputStream input = jcrContentNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  static  public class ViewVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIDocumentWorkspace uiDocumentWorkspace = uiVersionInfo.getAncestorOfType(UIDocumentWorkspace.class);
      for(UIComponent uiChild : uiDocumentWorkspace.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      UIViewVersion uiViewVersion = uiDocumentWorkspace.getChild(UIViewVersion.class) ;
      if ( !(uiVersionInfo.curentVersion_.getName().equals("jcr:rootVersion"))) {
        Node frozenNode = uiVersionInfo.curentVersion_.getNode("jcr:frozenNode") ;
        uiViewVersion.setNode(frozenNode) ;
      }
      if(uiViewVersion.getTemplate() == null || uiViewVersion.getTemplate().trim().length() == 0) {
        UIApplication uiApp = uiVersionInfo.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIVersionInfo.msg.have-no-view-template", null)) ;

        return ;
      }
      uiViewVersion.setRendered(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace) ;
    }
  }

  static  public class RestoreVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiVersionInfo.getAncestorOfType(UIJCRExplorer.class) ;
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      UIApplication uiApp = uiVersionInfo.getAncestorOfType(UIApplication.class) ;
      uiExplorer.addLockToken(NodeLocation.getNodeByLocation(uiVersionInfo.node_));
      try {
        Node restoredNode =NodeLocation.getNodeByLocation(uiVersionInfo.node_);
        String versionName = uiVersionInfo.curentVersion_.getName();
        restoredNode.restore(versionName, true);
        ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
        ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
        try {
          if (listenerService!=null && activityService !=null && activityService.isAcceptedNode(restoredNode)) {
            listenerService.broadcast(ActivityCommonService.NODE_REVISION_CHANGED, restoredNode, versionName);
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
      Node node = uiVersionInfo.getCurrentNode() ;
      if(!node.isCheckedOut()) node.checkout() ;
      uiExplorer.getSession().save() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
      uiExplorer.setIsHidePopup(true) ;
      uiExplorer.updateAjax(event) ;
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
      if(Integer.parseInt(version1) < Integer.parseInt(version2)) {
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

  /*static public class CloseViewActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIViewVersion uiViewVersion = uiVersionInfo.getChild(UIViewVersion.class);
      if(uiViewVersion.isRendered()) {
        uiViewVersion.setRendered(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo);
        return;
      }
    }
  }*/

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
