package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;
import javax.portlet.PortletMode;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.extensions.publication.impl.PublicationManagerImpl;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui.UIPublicationContainer;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionData;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionLog;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class AuthoringPublicationPlugin extends  WebpagePublicationPlugin {

  /** The log. */
  private static final Log      LOG = ExoLogger.getLogger(AuthoringPublicationPlugin.class.getName());
  private ListenerService       listenerService;
  private ActivityCommonService activityService;

  /**
   * Instantiates a new stage and version publication plugin.
   */
  public AuthoringPublicationPlugin() {
    listenerService = WCMCoreUtils.getService(ListenerService.class);
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
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
    // Add mixin mix:versionable
    if (node.canAddMixin(Utils.MIX_VERSIONABLE)) {
      node.addMixin(Utils.MIX_VERSIONABLE);
      node.save();
    }
    
    String versionName = context.get(AuthoringPublicationConstant.CURRENT_REVISION_NAME);
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
    
    if (containerName==null) containerName = PortalContainer.getCurrentPortalContainerName();
    
    if (PublicationDefaultStates.PENDING.equals(newState)) {
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  AuthoringPublicationConstant.CHANGE_TO_PENDING);
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

      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE, newState);
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

      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE, newState);
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
                                  AuthoringPublicationConstant.ENROLLED_TO_LIFECYCLE);
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE, newState);
      VersionData revisionData = new VersionData(node.getUUID(), newState, userId);
      revisionsMap.put(node.getUUID(), revisionData);
      addRevisionData(node, revisionsMap.values());
      addLog(node, versionLog);
    } else if (PublicationDefaultStates.UNPUBLISHED.equalsIgnoreCase(newState)) {
      versionLog = new VersionLog(selectedRevision.getName(),
                                  PublicationDefaultStates.UNPUBLISHED,
                                  userId,
                                  new GregorianCalendar(),
                                  AuthoringPublicationConstant.CHANGE_TO_UNPUBLISHED);
      
      
      VersionData selectedVersionData = revisionsMap.get(selectedRevision.getUUID());
      if (selectedVersionData != null) {
        selectedVersionData.setAuthor(userId);
        selectedVersionData.setState(PublicationDefaultStates.UNPUBLISHED);
      } else {
        selectedVersionData = new VersionData(selectedRevision.getUUID(),
                                      PublicationDefaultStates.UNPUBLISHED,
                                      userId);
      }
      VersionData versionData = revisionsMap.get(node.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(PublicationDefaultStates.UNPUBLISHED);
      } else {
        versionData = new VersionData(selectedRevision.getUUID(),
                                      PublicationDefaultStates.UNPUBLISHED,
                                      userId);
      }
      revisionsMap.put(node.getUUID(), versionData);
      revisionsMap.put(selectedRevision.getUUID(), selectedVersionData);
      
      addLog(node, versionLog);
      // change base version to unpublished state
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE,
                       PublicationDefaultStates.UNPUBLISHED);
      Value value = valueFactory.createValue(selectedRevision);
      Value liveRevision = null;
      if (node.hasProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP)) {
        liveRevision = node.getProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP)
                               .getValue();
      }
      if (liveRevision != null && value.getString().equals(liveRevision.getString())) {
        node.setProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP,
                         valueFactory.createValue(""));
      }
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.OBSOLETE.equals(newState)) {
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(selectedRevision.getName(),
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  AuthoringPublicationConstant.CHANGE_TO_OBSOLETED);
      addLog(node, versionLog);
      VersionData versionData = revisionsMap.get(selectedRevision.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(newState);
      } else {
    	versionData = new VersionData(selectedRevision.getUUID(), newState, userId);
      }
      revisionsMap.put(selectedRevision.getUUID(), versionData);
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.ARCHIVED.equalsIgnoreCase(newState)) {
      Value value = valueFactory.createValue(selectedRevision);
      Value liveRevision = null;
      if (node.hasProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP)) {
        liveRevision = node.getProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP).getValue();
      }
      if (liveRevision != null && value.getString().equals(liveRevision.getString())) {
        node.setProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP,
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
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE,
                       PublicationDefaultStates.ARCHIVED);
      addRevisionData(node, revisionsMap.values());
    } else if (PublicationDefaultStates.DRAFT.equalsIgnoreCase(newState)) {
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE, newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  userId,
                                  GregorianCalendar.getInstance(),
                                  AuthoringPublicationConstant.CHANGE_TO_DRAFT);
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
      node.setProperty(AuthoringPublicationConstant.LIVE_DATE_PROP, new GregorianCalendar());
      node.save();
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
      node.setProperty(AuthoringPublicationConstant.CURRENT_STATE,
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
                                  AuthoringPublicationConstant.ENROLLED_TO_LIFECYCLE);
      Value liveVersionValue = valueFactory.createValue(liveVersion);
      node.setProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP, liveVersionValue);
      VersionData liveRevisionData = new VersionData(liveVersion.getUUID(),
                                                     PublicationDefaultStates.PUBLISHED,
                                                     userId);
      revisionsMap.put(liveVersion.getUUID(), liveRevisionData);
      addRevisionData(node, revisionsMap.values());
    }

    if (!IdentityConstants.SYSTEM.equals(userId)) {
      node.setProperty("publication:lastUser", userId);
    }

    if (!node.isNew())
      node.save();

    //raise event to notify that state is changed
    if (!PublicationDefaultStates.ENROLLED.equalsIgnoreCase(newState)) {

      CmsService cmsService = WCMCoreUtils.getService(CmsService.class);

      if ("true".equalsIgnoreCase(context.get(AuthoringPublicationConstant.IS_INITIAL_PHASE))) {
        listenerService.broadcast(AuthoringPublicationConstant.POST_INIT_STATE_EVENT, cmsService, node);
      } else {
        listenerService.broadcast(AuthoringPublicationConstant.POST_CHANGE_STATE_EVENT, cmsService, node);
        if (activityService.isAcceptedNode(node)) {
          listenerService.broadcast(ActivityCommonService.STATE_CHANGED_ACTIVITY, node, newState);
        }
      }
    }

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
    String nodetypes = System.getProperty("wcm.nodetypes.ignoreversion");
    if(nodetypes == null || nodetypes.length() == 0)
       nodetypes = "exo:webContent";
    if(!Utils.NT_FILE.equals(node.getPrimaryNodeType().getName()) || Utils.isMakeVersionable(node, nodetypes.split(","))) {
      if (!node.isNodeType(AuthoringPublicationConstant.MIX_VERSIONABLE)) {
        node.addMixin(AuthoringPublicationConstant.MIX_VERSIONABLE);
      }
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

    String state = node.getProperty(AuthoringPublicationConstant.CURRENT_STATE).getString();
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
      String nodeVersionUUID = node.getProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP)
                                   .getString();
      if ("".equals(nodeVersionUUID)
          && PublicationDefaultStates.PUBLISHED.equals(node.getProperty(AuthoringPublicationConstant.CURRENT_STATE)
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
        || PublicationDefaultStates.UNPUBLISHED.equals(currentState))
      return null;

    // if current mode is edit mode
    if (context==null || WCMComposer.MODE_EDIT.equals(context.get(WCMComposer.FILTER_MODE)) ||
        PortletMode.EDIT.toString().equals(context.get(WCMComposer.PORTLET_MODE)))
      return node;

    // if current mode is live mode
    Node liveNode = getLiveRevision(node);
    if (liveNode != null) {
      if (liveNode.hasNode("jcr:frozenNode")) {
        return liveNode.getNode("jcr:frozenNode");
      } 
      return liveNode;
    }
    return null;
  }

  @Override
  /**
   * In this publication process, we put the content in Draft state when editing it.
   */
  public void updateLifecyleOnChangeContent(Node node, String remoteUser)
  throws Exception {
    updateLifecyleOnChangeContent(node, remoteUser, PublicationDefaultStates.DRAFT);
  }

  @Override
  public List<String> getListUserNavigationUri(Page page, String remoteUser) throws Exception {
    List<String> listPageNavigationUri = new ArrayList<String>();
    for (String portalName : getRunningPortals(remoteUser)) {

      UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
      UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig(portalName,
                                                                                   remoteUser,
                                                                                   PortalRequestContext.USER_PORTAL_CONTEXT);
      UserPortal userPortal = userPortalCfg.getUserPortal();

      // get nodes
      List<UserNavigation> navigationList = userPortal.getNavigations();
      for (UserNavigation nav : navigationList) {
        UserNode root = userPortal.getNode(nav, Scope.ALL, null, null);
        List<UserNode> userNodeList = PublicationUtil.findUserNodeByPageId(root, page.getPageId());
        for (UserNode node : userNodeList) {
          listPageNavigationUri.add(PublicationUtil.setMixedNavigationUri(portalName, node.getURI()));
        }
      }
    }
    return listPageNavigationUri;
  }

  @Override
  public byte[] getStateImage(Node node, Locale locale) throws IOException, FileNotFoundException,
          Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getUserInfo(Node node, Locale locale) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values) throws Exception {
    ClassLoader cl=this.getClass().getClassLoader();
    ResourceBundleService bundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    ResourceBundle resourceBundle= bundleService.getResourceBundle(AuthoringPublicationConstant.LOCALIZATION, locale, cl);
    String result = "";
    try {
      result = resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      result = key;
    }
    if(values != null) {
      return String.format(result, (Object[])values);
    }
    return result;
  }

  private List<String> getRunningPortals(String userId) throws Exception {
    List<String> listPortalName = new ArrayList<String>();
    DataStorage service = WCMCoreUtils.getService(DataStorage.class);
    Query<PortalConfig> query = new Query<PortalConfig>(null, null, null, null, PortalConfig.class) ;
    PageList pageList = service.find(query) ;
    UserACL userACL = WCMCoreUtils.getService(UserACL.class);
    for(Object object:pageList.getAll()) {
      PortalConfig portalConfig = (PortalConfig)object;
      if(userACL.hasPermission(portalConfig)) {
        listPortalName.add(portalConfig.getName());
      }
    }
    return listPortalName;
  }
}
