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
import javax.jcr.version.VersionHistory;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
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
        @EventConfig(listeners = UIVersionInfo.RestoreVersionActionListener.class),
        @EventConfig(listeners = UIVersionInfo.ViewVersionActionListener.class),
        @EventConfig(listeners = UIVersionInfo.AddLabelActionListener.class),
        @EventConfig(listeners = UIVersionInfo.CompareVersionActionListener.class),
        @EventConfig(listeners = UIVersionInfo.DeleteVersionActionListener.class, confirm = "UIVersionInfo.msg.confirm-delete"),
        @EventConfig(listeners = UIVersionInfo.RemoveLabelActionListener.class),
        @EventConfig(listeners = UIVersionInfo.CloseActionListener.class),
        @EventConfig(listeners = UIVersionInfo.CloseViewActionListener.class)
    }
)

public class UIVersionInfo extends UIContainer implements UIPopupComponent {
  private static final Log LOG  = ExoLogger.getLogger(UIVersionInfo.class.getName());

  protected VersionNode rootVersion_ ;
  protected VersionNode curentVersion_;
  protected NodeLocation node_ ;
  public UIVersionInfo() throws Exception {
    addChild(UILabelForm.class, null, null).setRendered(false);
    addChild(UIRemoveLabelForm.class, null, null).setRendered(false);
    addChild(UIViewVersion.class, null, null).setRendered(false);
    addChild(UIDiff.class, null, null).setRendered(false) ;
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
      getChild(UIViewVersion.class).update();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error!", e.getMessage());
      }
    }
  }

  public void deActivate() {}

  public VersionNode getCurrentVersionNode() { return curentVersion_ ;}
  
  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(node_);
  }
  
  public void setCurrentNode(Node node) {
    node_ = NodeLocation.getNodeLocationByNode(node);
  }

  public boolean isViewVersion() {
    UIViewVersion uiViewVersion = getChild(UIViewVersion.class);
    if(uiViewVersion.isRendered()) return true;
    return false;
  }

  static  public class ViewVersionActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      UIViewVersion uiViewVersion = uiVersionInfo.getChild(UIViewVersion.class) ;
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
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
    }
  }

  static  public class AddLabelActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      uiVersionInfo.getChild(UILabelForm.class).setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
    }
  }

  static  public class RemoveLabelActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiVersionInfo.curentVersion_  = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      uiVersionInfo.getChild(UIRemoveLabelForm.class).update() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
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
        event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo.getAncestorOfType(UIPopupContainer.class)) ;
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
      for(UIComponent uiChild : uiVersionInfo.getChildren()) {
        uiChild.setRendered(false) ;
      }
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID) ;
      VersionNode node = uiVersionInfo.rootVersion_.findVersionNode(objectId) ;
      UIDiff uiDiff = uiVersionInfo.getChild(UIDiff.class) ;
      uiDiff.setVersions(uiVersionInfo.getCurrentNode().getBaseVersion(),
                         node.getName(), node.getCreatedTime(), node.getWs(), node.getPath());
      uiDiff.setRendered(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
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
      uiExplorer.cancelAction() ;
    }
  }

  static public class CloseViewActionListener extends EventListener<UIVersionInfo> {
    public void execute(Event<UIVersionInfo> event) throws Exception {
      UIVersionInfo uiVersionInfo = event.getSource();
      UIViewVersion uiViewVersion = uiVersionInfo.getChild(UIViewVersion.class);
      if(uiViewVersion.isRendered()) {
        uiViewVersion.setRendered(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo);
        return;
      }
    }
  }
}
