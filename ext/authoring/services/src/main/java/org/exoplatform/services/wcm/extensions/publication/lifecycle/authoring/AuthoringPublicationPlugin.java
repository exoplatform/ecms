package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;
import javax.portlet.PortletMode;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.extensions.publication.impl.PublicationManagerImpl;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui.UIPublicationContainer;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationPlugin;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionData;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionLog;
import org.exoplatform.services.wcm.publication.listener.navigation.NavigationEventListenerDelegate;
import org.exoplatform.services.wcm.publication.listener.page.PageEventListenerDelegate;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class AuthoringPublicationPlugin extends StageAndVersionPublicationPlugin {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(AuthoringPublicationPlugin.class);
  
  /** The page event listener delegate. */
  private PageEventListenerDelegate       pageEventListenerDelegate;

  /** The navigation event listener delegate. */
  private NavigationEventListenerDelegate navigationEventListenerDelegate;

  /**
   * Instantiates a new stage and version publication plugin.
   */
  public AuthoringPublicationPlugin() {
    pageEventListenerDelegate = new PageEventListenerDelegate(AuthoringPublicationConstant.LIFECYCLE_NAME, null);
    navigationEventListenerDelegate = new NavigationEventListenerDelegate(AuthoringPublicationConstant.LIFECYCLE_NAME, null);
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#changeState
   * (javax.jcr.Node, java.lang.String, java.util.HashMap)
   */

  public void changeState(Node node, 
                          String newState, 
                          HashMap<String, String> context) throws IncorrectStateUpdateLifecycleException,
                                                                                      Exception {
    String versionName = context.get(StageAndVersionPublicationConstant.CURRENT_REVISION_NAME);
    String logItemName = versionName;
    String userId = "";
    try {
      userId = Util.getPortalRequestContext().getRemoteUser();
    } catch (Exception e) {
    userId = node.getSession().getUserID();
  }
    Node selectedRevision = null;
    if (node.getName().equals(versionName) || versionName == null) {
      selectedRevision = node;
      logItemName = node.getName();
    } else {
      selectedRevision = node.getVersionHistory().getVersion(versionName);
    }

    Map<String, VersionData> revisionsMap = getRevisionData(node);
    VersionLog versionLog = null;
    ValueFactory valueFactory = node.getSession().getValueFactory();
    String containerName = context.get("containerName");
    try {
      if (containerName==null) containerName = PortalContainer.getCurrentPortalContainerName();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    if (PublicationDefaultStates.PENDING.equals(newState)) {
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_DRAFT);
      addLog(node, versionLog);
      VersionData versionData = revisionsMap.get(node.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(newState);
      } else {
        versionData = new VersionData(node.getUUID(), newState, userId);
      }
      revisionsMap.put(node.getUUID(), versionData);
      addRevisionData(node, revisionsMap.values());

    } else if (PublicationDefaultStates.APPROVED.equals(newState)) {

      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  AuthoringPublicationConstant.CHANGE_TO_APPROVED);
      addLog(node, versionLog);
      VersionData versionData = revisionsMap.get(node.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(newState);
      } else {
        versionData = new VersionData(node.getUUID(), newState, userId);
      }
      revisionsMap.put(node.getUUID(), versionData);
      addRevisionData(node, revisionsMap.values());

    } else if (PublicationDefaultStates.STAGED.equals(newState)) {

      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  AuthoringPublicationConstant.CHANGE_TO_STAGED);
      addLog(node, versionLog);
      VersionData versionData = revisionsMap.get(node.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(newState);
      } else {
        versionData = new VersionData(node.getUUID(), newState, userId);
      }
      revisionsMap.put(node.getUUID(), versionData);
      addRevisionData(node, revisionsMap.values());

    } else if (PublicationDefaultStates.ENROLLED.equalsIgnoreCase(newState)) {
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_LIFECYCLE);
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, newState);
      VersionData revisionData = new VersionData(node.getUUID(), newState, userId);
      revisionsMap.put(node.getUUID(), revisionData);
      addRevisionData(node, revisionsMap.values());
      addLog(node, versionLog);
    } else if (PublicationDefaultStates.UNPUBLISHED.equalsIgnoreCase(newState)) {
      Value value = valueFactory.createValue(selectedRevision);
      Value liveRevision = null;
      if (node.hasProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)) {
        liveRevision = node.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)
                               .getValue();
      }
      if (liveRevision != null && value.getString().equals(liveRevision.getString())) {
        node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP,
                         valueFactory.createValue(""));
      }
      versionLog = new VersionLog(selectedRevision.getName(),
                                  PublicationDefaultStates.UNPUBLISHED,
                                  userId,
                                  new GregorianCalendar(),
                                  AuthoringPublicationConstant.CHANGE_TO_UNPUBLISHED);
      VersionData versionData = revisionsMap.get(selectedRevision.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(PublicationDefaultStates.UNPUBLISHED);
      } else {
        versionData = new VersionData(selectedRevision.getUUID(),
                                      PublicationDefaultStates.UNPUBLISHED,
                                      userId);
      }
      revisionsMap.put(selectedRevision.getUUID(), versionData);
      addLog(node, versionLog);
      // change base version to unpublished state
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE,
                       PublicationDefaultStates.UNPUBLISHED);
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.OBSOLETE.equals(newState)) {
      Value value = valueFactory.createValue(selectedRevision);
      Value liveRevision = null;
      if (node.hasProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)) {
        liveRevision = node.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)
                               .getValue();
      }
      if (liveRevision != null && value.getString().equals(liveRevision.getString())) {
        node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP,
                         valueFactory.createValue(""));
      }
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  AuthoringPublicationConstant.CHANGE_TO_OBSOLETED);
      addLog(node, versionLog);
      VersionData versionData = revisionsMap.get(node.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(newState);
      } else {
        versionData = new VersionData(node.getUUID(), newState, userId);
      }
      revisionsMap.put(node.getUUID(), versionData);
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.ARCHIVED.equalsIgnoreCase(newState)) {
      Value value = valueFactory.createValue(selectedRevision);
      Value liveRevision = node.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)
                               .getValue();
      if (liveRevision != null && value.getString().equals(liveRevision.getString())) {
        node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP,
                         valueFactory.createValue(""));
      }
      versionLog = new VersionLog(selectedRevision.getName(),
                                  PublicationDefaultStates.ARCHIVED,
                                  userId,
                                  new GregorianCalendar(),
                                  AuthoringPublicationConstant.CHANGE_TO_ARCHIVED);
      VersionData versionData = revisionsMap.get(selectedRevision.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(PublicationDefaultStates.ARCHIVED);
      } else {
        versionData = new VersionData(selectedRevision.getUUID(),
                                      PublicationDefaultStates.ARCHIVED,
                                      userId);
      }
      revisionsMap.put(selectedRevision.getUUID(), versionData);
      addLog(node, versionLog);
      // change base version to archived state
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE,
                       PublicationDefaultStates.ARCHIVED);
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.DRAFT.equalsIgnoreCase(newState)) {
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_DRAFT);
      addLog(node, versionLog);
      VersionData versionData = revisionsMap.get(node.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(newState);
      } else {
        versionData = new VersionData(node.getUUID(), newState, userId);
      }
      revisionsMap.put(node.getUUID(), versionData);
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.PUBLISHED.equals(newState)) {
      if (!node.isCheckedOut()) {
        node.checkout();
      }
      Version liveVersion = node.checkin();
      node.checkout();
      // Change current live revision to unpublished
      Node oldLiveRevision = getLiveRevision(node);
      if (oldLiveRevision != null) {
        VersionData versionData = revisionsMap.get(oldLiveRevision.getUUID());
        if (versionData != null) {
          versionData.setAuthor(userId);
          versionData.setState(PublicationDefaultStates.UNPUBLISHED);
        } else {
          versionData = new VersionData(oldLiveRevision.getUUID(),
                                        PublicationDefaultStates.UNPUBLISHED,
                                        userId);
        }
        revisionsMap.put(oldLiveRevision.getUUID(), versionData);
        versionLog = new VersionLog(oldLiveRevision.getName(),
                                    PublicationDefaultStates.UNPUBLISHED,
                                    userId,
                                    new GregorianCalendar(),
                                    AuthoringPublicationConstant.CHANGE_TO_UNPUBLISHED);
        addLog(node, versionLog);
      }
      versionLog = new VersionLog(liveVersion.getName(),
                                  newState,
                                  userId,
                                  new GregorianCalendar(),
                                  AuthoringPublicationConstant.CHANGE_TO_LIVE);
      addLog(node, versionLog);
      // change base version to published state
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE,
                       PublicationDefaultStates.PUBLISHED);
      VersionData editableRevision = revisionsMap.get(node.getUUID());
      if (editableRevision != null) {

        PublicationManagerImpl publicationManagerImpl = WCMCoreUtils.getService(PublicationManagerImpl.class, containerName);
        String lifecycleName = node.getProperty("publication:lifecycle").getString();
        Lifecycle lifecycle = publicationManagerImpl.getLifecycle(lifecycleName);
        List<State> states = lifecycle.getStates();
        if (states == null || states.size() <= 0) {
          editableRevision.setState(PublicationDefaultStates.ENROLLED);
        } else {
          editableRevision.setState(states.get(0).getState());
        }
        editableRevision.setAuthor(userId);

      } else {
        editableRevision = new VersionData(node.getUUID(),
                                           PublicationDefaultStates.ENROLLED,
                                           userId);
      }
      revisionsMap.put(node.getUUID(), editableRevision);
      versionLog = new VersionLog(node.getBaseVersion().getName(),
                                  PublicationDefaultStates.DRAFT,
                                  userId,
                                  new GregorianCalendar(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_LIFECYCLE);
      Value liveVersionValue = valueFactory.createValue(liveVersion);
      node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP, liveVersionValue);
      node.setProperty(StageAndVersionPublicationConstant.LIVE_DATE_PROP, new GregorianCalendar());
      VersionData liveRevisionData = new VersionData(liveVersion.getUUID(),
                                                     PublicationDefaultStates.PUBLISHED,
                                                     userId);
      revisionsMap.put(liveVersion.getUUID(), liveRevisionData);
      addRevisionData(node, revisionsMap.values());
    }

    if (!"__system".equals(userId)) {
      node.setProperty("publication:lastUser", userId);
    }

    if (!node.isNew())
      node.save();
    
    //raise event to notify that state is changed
    if (!PublicationDefaultStates.ENROLLED.equalsIgnoreCase(newState)) {
      
      ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
      CmsService cmsService = WCMCoreUtils.getService(CmsService.class);

      if ("true".equalsIgnoreCase(context.get(StageAndVersionPublicationConstant.IS_INITIAL_PHASE))) {
        listenerService.broadcast(StageAndVersionPublicationConstant.POST_INIT_STATE_EVENT, cmsService, node);
      } else {
        listenerService.broadcast(StageAndVersionPublicationConstant.POST_CHANGE_STATE_EVENT, cmsService, node);
      }
    }
    
    ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class, containerName);
    listenerService.broadcast(AuthoringPublicationConstant.POST_UPDATE_STATE_EVENT, null, node);
  }
  
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.ecm.publication.PublicationPlugin#getPossibleStates
   * ()
   */
  public String[] getPossibleStates() {
    return new String[] { PublicationDefaultStates.ENROLLED, PublicationDefaultStates.DRAFT,
        PublicationDefaultStates.PENDING, PublicationDefaultStates.PUBLISHED,
        PublicationDefaultStates.OBSOLETE };
  }

  public String getLifecycleName() {
    return AuthoringPublicationConstant.LIFECYCLE_NAME;
  }

  public String getLifecycleType() {
    return AuthoringPublicationConstant.PUBLICATION_LIFECYCLE_TYPE;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getStateUI
   * (javax.jcr.Node, org.exoplatform.webui.core.UIComponent)
   */
  public UIForm getStateUI(Node node, UIComponent component) throws Exception {
    UIPublicationContainer publicationContainer = component.createUIComponent(UIPublicationContainer.class,
                                                                              null,
                                                                              null);
    publicationContainer.initContainer(node);
    return publicationContainer;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.ecm.publication.PublicationPlugin#addMixin(javax
   * .jcr.Node)
   */
  public void addMixin(Node node) throws Exception {
    node.addMixin(AuthoringPublicationConstant.PUBLICATION_LIFECYCLE_TYPE);
    if (!node.isNodeType(AuthoringPublicationConstant.MIX_VERSIONABLE)) {
      node.addMixin(AuthoringPublicationConstant.MIX_VERSIONABLE);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#canAddMixin
   * (javax.jcr.Node)
   */
  public boolean canAddMixin(Node node) throws Exception {
    return node.canAddMixin(AuthoringPublicationConstant.PUBLICATION_LIFECYCLE_TYPE);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecycleOnChangeNavigation
   * (org.exoplatform.portal.config.model.PageNavigation, java.lang.String)
   */
  public void updateLifecycleOnChangeNavigation(NavigationContext navigationContext, String remoteUser) throws Exception {
    navigationEventListenerDelegate.updateLifecycleOnChangeNavigation(navigationContext,
                                                                      remoteUser,
                                                                      this);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecycleOnRemovePage(org.exoplatform.portal.config.model.Page,
   * java.lang.String)
   */
  public void updateLifecycleOnRemovePage(Page page, String remoteUser) throws Exception {
    pageEventListenerDelegate.updateLifecycleOnRemovePage(page, remoteUser, this);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecyleOnChangePage(org.exoplatform.portal.config.model.Page,
   * java.lang.String)
   */
  public void updateLifecyleOnChangePage(Page page, String remoteUser) throws Exception {
    pageEventListenerDelegate.updateLifecyleOnChangePage(page, remoteUser, this);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecyleOnCreateNavigation
   * (org.exoplatform.portal.config.model.PageNavigation)
   */
  public void updateLifecyleOnCreateNavigation(NavigationContext navigationContext) throws Exception {
    navigationEventListenerDelegate.updateLifecyleOnCreateNavigation(navigationContext);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecyleOnCreatePage(org.exoplatform.portal.config.model.Page,
   * java.lang.String)
   */
  public void updateLifecyleOnCreatePage(Page page, String remoteUser) throws Exception {
    pageEventListenerDelegate.updateLifecyleOnCreatePage(page, remoteUser, this);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecyleOnRemoveNavigation
   * (org.exoplatform.portal.config.model.PageNavigation)
   */
  public void updateLifecyleOnRemoveNavigation(NavigationContext navigationContext) throws Exception {
    navigationEventListenerDelegate.updateLifecyleOnRemoveNavigation(navigationContext);
  }

  /**
   * Adds the log.
   *
   * @param node the node
   * @param versionLog the version log
   * @throws Exception the exception
   */
  private void addLog(Node node, VersionLog versionLog) throws Exception {
    Value[] values = node.getProperty(AuthoringPublicationConstant.HISTORY).getValues();
    ValueFactory valueFactory = node.getSession().getValueFactory();
    List<Value> list = new ArrayList<Value>(Arrays.asList(values));
    list.add(valueFactory.createValue(versionLog.toString()));
    node.setProperty(AuthoringPublicationConstant.HISTORY, list.toArray(new Value[] {}));
  }

  /**
   * Adds the revision data.
   *
   * @param node the node
   * @param list the list
   * @throws Exception the exception
   */
  private void addRevisionData(Node node, Collection<VersionData> list) throws Exception {
    List<Value> valueList = new ArrayList<Value>();
    ValueFactory factory = node.getSession().getValueFactory();
    for (VersionData versionData : list) {
      valueList.add(factory.createValue(versionData.toStringValue()));
    }
    node.setProperty(AuthoringPublicationConstant.REVISION_DATA_PROP,
                     valueList.toArray(new Value[] {}));
  }

  /**
   * Gets the revision data.
   *
   * @param node the node
   * @return the revision data
   * @throws Exception the exception
   */
  private Map<String, VersionData> getRevisionData(Node node) throws Exception {
    Map<String, VersionData> map = new HashMap<String, VersionData>();
    try {
      for (Value v : node.getProperty(AuthoringPublicationConstant.REVISION_DATA_PROP).getValues()) {
        VersionData versionData = VersionData.toVersionData(v.getString());
        map.put(versionData.getUUID(), versionData);
      }
    } catch (Exception e) {
      return map;
    }
    return map;
  }

  /**
   * In this publication process, we put the content in Draft state when editing
   * it.
   */
  public void updateLifecyleOnChangeContent(Node node, String remoteUser, String newState) throws Exception {

    String state = node.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).getString();
    if (newState == null) {
      PublicationManagerImpl publicationManagerImpl = WCMCoreUtils.getService(PublicationManagerImpl.class);
      Lifecycle lifecycle = publicationManagerImpl.getLifecycle(node.getProperty("publication:lifecycle")
                                                                    .getString());
      List<State> states = lifecycle.getStates();
      if (states != null && states.size() > 0) {
        newState = states.get(0).getState();
      }
    }
    if (state.equals(newState))
      return;

    HashMap<String, String> context = new HashMap<String, String>();
    changeState(node, newState, context);
  }

  /**
   * Gets the live revision.
   *
   * @param node the node
   * @return the live revision
   */
  private Node getLiveRevision(Node node) {
    try {
      String nodeVersionUUID = node.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP)
                                   .getString();
      if ("".equals(nodeVersionUUID)
          && PublicationDefaultStates.PUBLISHED.equals(node.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE)
                                                           .getString()))
        return node;
      return node.getVersionHistory().getSession().getNodeByUUID(nodeVersionUUID);
    } catch (Exception e) {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.ecm.publication.PublicationPlugin#getNodeView(
   * javax.jcr.Node, java.util.Map)
   */
  public Node getNodeView(Node node, Map<String, Object> context) throws Exception {
    // don't display content if state is enrolled or obsolete
    WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
    String currentState = wcmPublicationService.getContentState(node);
    if (PublicationDefaultStates.ENROLLED.equals(currentState)
        || PublicationDefaultStates.OBSOLETE.equals(currentState))
      return null;

    // if current mode is edit mode
    if (context==null || context.get(WCMComposer.FILTER_MODE).equals(WCMComposer.MODE_EDIT) ||
        PortletMode.EDIT.toString().equals(context.get(WCMComposer.PORTLET_MODE)))
      return node;

    // if current mode is live mode
    Node liveNode = getLiveRevision(node);
    if (liveNode != null) {
      if (liveNode.hasNode("jcr:frozenNode")) {
        return liveNode.getNode("jcr:frozenNode");
      } else {
        return liveNode;
      }
    } else
      return null;

  }

}
