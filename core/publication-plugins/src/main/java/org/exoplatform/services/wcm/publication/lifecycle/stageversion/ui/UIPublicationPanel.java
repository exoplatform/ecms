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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionData;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionLog;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com
 * Mar 2, 2009
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublicationPanel.gtmpl",
                 events = {
                   @EventConfig(listeners=UIPublicationPanel.DraftActionListener.class),
                   @EventConfig(listeners=UIPublicationPanel.LiveActionListener.class),
                   @EventConfig(name= "obsolete", listeners= UIPublicationPanel.ObsoleteActionListener.class),
                   @EventConfig(listeners=UIPublicationPanel.ChangeVersionActionListener.class),
                   @EventConfig(listeners=UIPublicationPanel.PreviewVersionActionListener.class),
                   @EventConfig(listeners=UIPublicationPanel.RestoreVersionActionListener.class),
                   @EventConfig(listeners=UIPublicationPanel.SeeAllVersionActionListener.class),
                   @EventConfig(listeners=UIPublicationPanel.CloseActionListener.class)
                 }
    )

public class UIPublicationPanel extends UIForm {

  /** The current node. */
  private NodeLocation currentNodeLocation;

  /** The current revision. */
  private NodeLocation currentRevisionLocation;

  /** The revisions data map. */
  private Map<String,VersionData> revisionsDataMap = new HashMap<String,VersionData>();

  /** The viewed revisions. */
  private List<NodeLocation> viewedRevisions = new ArrayList<NodeLocation>(3);

  private WCMPublicationService wcmPublicationService;

  private String sitename;

  private String remoteuser;

  /**
   * Instantiates a new uI publication panel.
   *
   * @throws Exception the exception
   */
  public UIPublicationPanel() throws Exception {
    wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
    sitename = Util.getPortalRequestContext().getPortalOwner();
    remoteuser = Util.getPortalRequestContext().getRemoteUser();
  }

  /**
   * Inits the.
   *
   * @param node the node
   *
   * @throws Exception the exception
   */
  public void init(Node node) throws Exception {
    currentNodeLocation = NodeLocation.getNodeLocationByNode(node);
    currentRevisionLocation = NodeLocation.getNodeLocationByNode(node);
    this.viewedRevisions = NodeLocation.getLocationsByNodeList(getLatestRevisions(3,node));
    this.revisionsDataMap = getRevisionData(node);
    //In some cases as copy a a node, we will lost all version of the node
    //So we will clean all publication data
    cleanPublicationData(node);
  }

  /**
   * Clean publication data.
   *
   * @param node the node
   *
   * @throws Exception the exception
   */
  private void cleanPublicationData(Node node) throws Exception {
    if(viewedRevisions.size() == 1 && revisionsDataMap.size()>1) {
      node.setProperty(StageAndVersionPublicationConstant.REVISION_DATA_PROP,new Value[] {});
      node.setProperty(StageAndVersionPublicationConstant.HISTORY,new Value[] {});
      node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP,(javax.jcr.Value)null);
      node.save();
      this.revisionsDataMap = getRevisionData(node);
    }
  }

  /**
   * Gets the all revisions.
   *
   * @param node the node
   *
   * @return the all revisions
   *
   * @throws Exception the exception
   */
  public List<Node> getAllRevisions(Node node) throws Exception {
    List<Node> allversions = new ArrayList<Node>();
    VersionIterator iterator = node.getVersionHistory().getAllVersions();
    for(;iterator.hasNext();) {
      Version version = iterator.nextVersion();
      if (version.getName().equals("jcr:rootVersion")) continue;
      allversions.add(version);
    }
    Collections.sort(allversions, new NodeNameComparator());
    //current node is a revision
    allversions.add(node);
    Collections.reverse(allversions);
    return allversions;
  }

  /**
   * Gets the current node.
   *
   * @return the current node
   */
  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNodeLocation);
  }

  /**
   * Gets the current revision.
   *
   * @return the current revision
   */
  public Node getCurrentRevision() {
    return NodeLocation.getNodeByLocation(currentRevisionLocation);
  }

  /**
   * Gets the revision author.
   *
   * @param revision the revision
   *
   * @return the revision author
   *
   * @throws Exception the exception
   */
  public String getRevisionAuthor(Node revision) throws Exception{
    VersionData revisionData = revisionsDataMap.get(revision.getUUID());
    Node currentNode = getCurrentNode();
    if(revisionData!= null)
      return revisionData.getAuthor();
    if(revision.getUUID().equalsIgnoreCase(currentNode.getUUID())) {
      return currentNode.getProperty("exo:owner").getString();
    }
    return null;
  }

  /**
   * Gets the revision by uuid.
   *
   * @param revisionUUID the revision uuid
   *
   * @return the revision by uuid
   *
   * @throws Exception the exception
   */
  public Node getRevisionByUUID(String revisionUUID) throws Exception {
    Session session = getCurrentNode().getSession();
    return session.getNodeByUUID(revisionUUID);
  }

  /**
   * Gets the revision created date.
   *
   * @param revision the revision
   *
   * @return the revision created date
   *
   * @throws Exception the exception
   */
  public String getRevisionCreatedDate(Node revision) throws Exception {
    UIPublicationContainer container = getAncestorOfType(UIPublicationContainer.class);
    DateFormat dateFormater = container.getDateTimeFormater();
    Calendar calendar = null;
    if(revision instanceof Version) {
      calendar= ((Version)revision).getCreated();
    }else {
      if(revision.hasProperty("exo:dateCreated")){
        calendar = revision.getProperty("exo:dateCreated").getDate();
      }else{
        calendar = revision.getProperty("jcr:created").getDate();
      }
    }
    return dateFormater.format(calendar.getTime());
  }

  /**
   * Gets the revisions.
   *
   * @return the revisions
   */
  public List<Node> getRevisions() {
    return NodeLocation.getNodeListByLocationList(viewedRevisions);
  }

  /**
   * Gets the revision state.
   *
   * @param revision the revision
   *
   * @return the revision state
   *
   * @throws Exception the exception
   */
  public String getRevisionState(Node revision) throws Exception{
    VersionData revisionData = revisionsDataMap.get(revision.getUUID());
    Node currentNode = getCurrentNode();
    if(revisionData!= null)
      return revisionData.getState();
    if(revision.getUUID().equalsIgnoreCase(currentNode.getUUID())) {
      return currentNode.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).getString();
    }
    return null;
  }

  /**
   * Sets the current revision.
   *
   * @param revision the new current revision
   */
  public void setCurrentRevision(Node revision) {
    currentRevisionLocation = NodeLocation.getNodeLocationByNode(revision);
  }

  /**
   * Sets the revisions.
   *
   * @param revisions the new revisions
   */
  public void setRevisions(List<Node> revisions) {
    this.viewedRevisions = NodeLocation.getLocationsByNodeList(revisions);
  }

  /**
   * Update panel.
   *
   * @throws Exception the exception
   */
  public void updatePanel() throws Exception{
    UIPublicationContainer publicationContainer = getAncestorOfType(UIPublicationContainer.class);
    UIPublicationHistory publicationHistory = publicationContainer.getChild(UIPublicationHistory.class);
    publicationHistory.updateGrid();
    Node currentNode = getCurrentNode();
    this.revisionsDataMap = getRevisionData(currentNode);
    this.viewedRevisions = NodeLocation.getLocationsByNodeList(getLatestRevisions(3,currentNode));
  }

  /**
   * Gets the latest revisions.
   *
   * @param limit the limit
   * @param node the node
   *
   * @return the latest revisions
   *
   * @throws Exception the exception
   */
  private List<Node> getLatestRevisions(int limit, Node node) throws Exception {
    List<Node> allversions = getAllRevisions(node);
    List<Node> latestVersions = new ArrayList<Node>();
    if(allversions.size() > limit) {
      latestVersions = allversions.subList(0, limit);
    } else {
      latestVersions = allversions;
    }
    return latestVersions;
  }

  /**
   * Gets the revision data.
   *
   * @param node the node
   *
   * @return the revision data
   *
   * @throws Exception the exception
   */
  private Map<String, VersionData> getRevisionData(Node node) throws Exception{
    Map<String,VersionData> map = new HashMap<String,VersionData>();
    try {
      for(Value v: node.getProperty(StageAndVersionPublicationConstant.REVISION_DATA_PROP).getValues()) {
        VersionData versionData = VersionData.toVersionData(v.getString());
        map.put(versionData.getUUID(),versionData);;
      }
    } catch (Exception e) {
      return map;
    }
    return map;
  }

  /**
   * The listener interface for receiving changeVersionAction events.
   * The class that is interested in processing a changeVersionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeVersionActionListener<code> method. When
   * the changeVersionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeVersionActionEvent
   */
  public static class ChangeVersionActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      String versionUUID = event.getRequestContext().getRequestParameter(OBJECTID);
      Node revision = publicationPanel.getRevisionByUUID(versionUUID);
      publicationPanel.setCurrentRevision(revision);
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CloseActionEvent
   */
  public static class CloseActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      UIPopupContainer uiPopupContainer = (UIPopupContainer) publicationPanel.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  /**
   * The listener interface for receiving draftAction events.
   * The class that is interested in processing a draftAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addDraftActionListener<code> method. When
   * the draftAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see DraftActionEvent
   */
  public static class DraftActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      Node currentNode = publicationPanel.getCurrentNode();
      publicationPanel.wcmPublicationService.updateLifecyleOnChangeContent(currentNode,
                                                                           publicationPanel.sitename,
                                                                           publicationPanel.remoteuser,
                                                                           PublicationDefaultStates.DRAFT);
      try {
        publicationPanel.updatePanel();
      } catch (Exception e) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
        //        JCRExceptionManager.process(uiApp,e);
      }
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving liveAction events.
   * The class that is interested in processing a liveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addLiveActionListener<code> method. When
   * the liveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see LiveActionEvent
   */
  public static class LiveActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      Node currentNode = publicationPanel.getCurrentNode();
      publicationPanel.wcmPublicationService.updateLifecyleOnChangeContent(currentNode,
                                                                           publicationPanel.sitename,
                                                                           publicationPanel.remoteuser,
                                                                           PublicationDefaultStates.PUBLISHED);
      try {
        publicationPanel.updatePanel();
      } catch (Exception e) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
        //        JCRExceptionManager.process(uiApp,e);
      }
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving obsoleteAction events.
   * The class that is interested in processing a obsoleteAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addObsoleteActionListener<code> method. When
   * the obsoleteAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ObsoleteActionEvent
   */
  public static class ObsoleteActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      Node currentNode = publicationPanel.getCurrentNode();
      publicationPanel.wcmPublicationService.updateLifecyleOnChangeContent(currentNode,
                                                                           publicationPanel.sitename,
                                                                           publicationPanel.remoteuser,
                                                                           PublicationDefaultStates.OBSOLETE);
      try {
        publicationPanel.updatePanel();
      } catch (Exception e) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
        //        JCRExceptionManager.process(uiApp,e);
      }
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving previewVersionAction events.
   * The class that is interested in processing a previewVersionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPreviewVersionActionListener<code> method. When
   * the previewVersionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see PreviewVersionActionEvent
   */
  public static class PreviewVersionActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      UIVersionViewer versionViewer = publicationContainer.createUIComponent(UIVersionViewer.class, null, "UIVersionViewer");
      String versionUUID = event.getRequestContext().getRequestParameter(OBJECTID);
      Node revision = publicationPanel.getRevisionByUUID(versionUUID);
      Node frozenNode = revision;
      if(revision instanceof Version) {
        frozenNode = revision.getNode("jcr:frozenNode") ;
      }
      versionViewer.setOriginalNode(publicationPanel.getCurrentNode());
      versionViewer.setNode(frozenNode);
      if(versionViewer.getTemplate() == null || versionViewer.getTemplate().trim().length() == 0) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class) ;
        uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.have-no-view-template", null)) ;        
        return ;
      }
      if (publicationContainer.getChildById("UIVersionViewer") == null) publicationContainer.addChild(versionViewer);
      else publicationContainer.replaceChild("UIVersionViewer", versionViewer);
      publicationContainer.setActiveTab(versionViewer, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving restoreVersionAction events.
   * The class that is interested in processing a restoreVersionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRestoreVersionActionListener<code> method. When
   * the restoreVersionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see RestoreVersionActionEvent
   */
  public static class RestoreVersionActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      Node currentNode = publicationPanel.getCurrentNode();
      String versionUUID = event.getRequestContext().getRequestParameter(OBJECTID);
      Version version = (Version)publicationPanel.getRevisionByUUID(versionUUID);

      String userId = "";
      try {
        userId = Util.getPortalRequestContext().getRemoteUser();
      } catch (Exception ex) {
        userId = currentNode.getSession().getUserID();
      }
      //restore the version
      try {
        currentNode.restore(version,true);
        ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
        ActivityCommonService activityService = WCMCoreUtils.getService(ActivityCommonService.class);
        if (activityService.isAcceptedNode(currentNode)) {
          listenerService.broadcast(ActivityCommonService.NODE_REVISION_CHANGED, currentNode, version.getName());
        }
        if(!currentNode.isCheckedOut())
          currentNode.checkout();
        Value[] values = currentNode.getProperty("publication:revisionData").getValues();
        String currentState = "";
        for(Value value : values) {
          String revisionData = value.getString();
          if(revisionData.indexOf(PublicationDefaultStates.PUBLISHED) > 0) {
            currentState = PublicationDefaultStates.PUBLISHED;
            break;
          } else {
            currentState = PublicationDefaultStates.OBSOLETE;
          }
        }
        //set current state
        currentNode.setProperty("publication:currentState", PublicationDefaultStates.DRAFT);
        //set revision data
        Map<String, VersionData> revisionsDataMap = publicationPanel.getRevisionData(currentNode);
        revisionsDataMap.get(currentNode.getUUID()).setState(PublicationDefaultStates.DRAFT);
        List<Value> valueList = new ArrayList<Value>();
        ValueFactory factory = currentNode.getSession().getValueFactory();
        for(VersionData versionData: revisionsDataMap.values()) {
          valueList.add(factory.createValue(versionData.toStringValue()));
        }
        currentNode.setProperty(StageAndVersionPublicationConstant.REVISION_DATA_PROP,valueList.toArray(new Value[]{}));
        //log the history
        VersionLog versionLog = 
            new VersionLog(currentNode.getName(), 
                           currentNode.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).getString(), 
                           userId, 
                           GregorianCalendar.getInstance(), 
                           StageAndVersionPublicationConstant.PUBLICATION_LOG_RESTORE_VERSION + " from " + 
                               version.getName());
        values = currentNode.getProperty(StageAndVersionPublicationConstant.HISTORY).getValues();
        ValueFactory valueFactory = currentNode.getSession().getValueFactory();
        List<Value> list = new ArrayList<Value>(Arrays.asList(values));
        list.add(valueFactory.createValue(versionLog.toString()));
        currentNode.setProperty(StageAndVersionPublicationConstant.HISTORY, list.toArray(new Value[] {}));
        //save data and update ui
        currentNode.getSession().save();
        publicationPanel.setCurrentRevision(currentNode);
        publicationPanel.updatePanel();
      } catch (Exception e) {
        UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class);
        //        JCRExceptionManager.process(uiApp,e);
      }

      UIApplication uiApp = publicationPanel.getAncestorOfType(UIApplication.class) ;
      uiApp.addMessage(new ApplicationMessage("UIPublicationPanel.msg.restore-complete", null)) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiApp) ;

      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving seeAllVersionAction events.
   * The class that is interested in processing a seeAllVersionAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSeeAllVersionActionListener<code> method. When
   * the seeAllVersionAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SeeAllVersionActionEvent
   */
  public static class SeeAllVersionActionListener extends EventListener<UIPublicationPanel> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationPanel> event) throws Exception {
      UIPublicationPanel publicationPanel = event.getSource();
      publicationPanel.setRevisions(publicationPanel.getAllRevisions(publicationPanel.getCurrentNode()));
      UIPublicationContainer publicationContainer = publicationPanel.getAncestorOfType(UIPublicationContainer.class);
      publicationContainer.setActiveTab(publicationPanel, event.getRequestContext());
    }
  }

  private class NodeNameComparator implements Comparator<Node> {

    @Override
    public int compare(Node node0, Node node1) {
      try {
        String name0 = node0.getName();
        String name1 = node1.getName();
        try {
          int name0Int = Integer.parseInt(name0);
          int name1Int = Integer.parseInt(name1);
          return name0Int - name1Int;
        } catch (NumberFormatException e) {
          return  name0.compareTo(name1);
        }
      } catch (RepositoryException e) {
        return 0;
      }
    }
  }
}
