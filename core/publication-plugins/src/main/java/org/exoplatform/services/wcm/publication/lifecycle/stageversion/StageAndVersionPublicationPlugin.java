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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion;

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
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.PortletPreferences;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.pom.config.POMSession;
import org.exoplatform.portal.pom.config.POMSessionManager;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionData;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionLog;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui.UIPublicationContainer;
import org.exoplatform.services.wcm.publication.listener.navigation.NavigationEventListenerDelegate;
import org.exoplatform.services.wcm.publication.listener.page.PageEventListenerDelegate;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * The Class StageAndVersionPublicationPlugin.
 */
@SuppressWarnings("deprecation")
public class StageAndVersionPublicationPlugin extends WebpagePublicationPlugin{

  /** The page event listener delegate. */
  private PageEventListenerDelegate pageEventListenerDelegate;

  /** The navigation event listener delegate. */
  private NavigationEventListenerDelegate navigationEventListenerDelegate;

  private DataStorage dataStorage;

  private POMSessionManager pomManager;

  private POMSession pomSession;

  private WCMComposer composer;

  /**
   * Instantiates a new stage and version publication plugin.
   */
  public StageAndVersionPublicationPlugin() {
    pageEventListenerDelegate = new PageEventListenerDelegate(StageAndVersionPublicationConstant.LIFECYCLE_NAME,
                                                              ExoContainerContext.getCurrentContainer());
    navigationEventListenerDelegate = new NavigationEventListenerDelegate(StageAndVersionPublicationConstant.LIFECYCLE_NAME,
                                                                          ExoContainerContext.getCurrentContainer());
    dataStorage = WCMCoreUtils.getService(DataStorage.class);
    pomManager = WCMCoreUtils.getService(POMSessionManager.class);
    composer = WCMCoreUtils.getService(WCMComposer.class);
  }

  public String getLifecycleType() {
    return StageAndVersionPublicationConstant.PUBLICATION_LIFECYCLE_TYPE;
  }


  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#addMixin(javax.jcr.Node)
   */
  public void addMixin(Node node) throws Exception {
    node.addMixin(StageAndVersionPublicationConstant.PUBLICATION_LIFECYCLE_TYPE);
    if(!node.isNodeType(StageAndVersionPublicationConstant.MIX_VERSIONABLE)) {
      node.addMixin(StageAndVersionPublicationConstant.MIX_VERSIONABLE);
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#canAddMixin(javax.jcr.Node)
   */
  public boolean canAddMixin(Node node) throws Exception {
    return node.canAddMixin(StageAndVersionPublicationConstant.PUBLICATION_LIFECYCLE_TYPE);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.ecm.publication.PublicationPlugin#changeState(
   * javax.jcr.Node, java.lang.String, java.util.HashMap)
   */
  public void changeState(Node node,
                          String newState,
                          HashMap<String,
                          String> context) throws IncorrectStateUpdateLifecycleException, Exception {
    String versionName = context.get(StageAndVersionPublicationConstant.CURRENT_REVISION_NAME);
    String logItemName = versionName;
    ConversationState conversationState = ConversationState.getCurrent();
    String userId = conversationState.getIdentity().getUserId();
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
    if (PublicationDefaultStates.ENROLLED.equalsIgnoreCase(newState)) {
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  node.getSession().getUserID(),
                                  GregorianCalendar.getInstance(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_LIFECYCLE);
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE,newState);
      VersionData revisionData = new VersionData(node.getUUID(),newState,userId);
      revisionsMap.put(node.getUUID(),revisionData);
      addRevisionData(node,revisionsMap.values());
      addLog(node,versionLog);
    } else if (PublicationDefaultStates.DRAFT.equalsIgnoreCase(newState)) {
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE,newState);
      versionLog = new VersionLog(logItemName,
                                  newState,
                                  node.getSession().getUserID(),
                                  GregorianCalendar.getInstance(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_DRAFT);
      addLog(node,versionLog);
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
      //Change current live revision to obsolete
      Node oldLiveRevision = getLiveRevision(node);
      if (oldLiveRevision != null) {
        VersionData versionData = revisionsMap.get(oldLiveRevision.getUUID());
        if (versionData != null) {
          versionData.setAuthor(userId);
          versionData.setState(PublicationDefaultStates.OBSOLETE);
        } else {
          versionData = new VersionData(oldLiveRevision.getUUID(),
                                        PublicationDefaultStates.OBSOLETE,
                                        userId);
        }
        revisionsMap.put(oldLiveRevision.getUUID(), versionData);
        versionLog = new VersionLog(oldLiveRevision.getName(),
                                    PublicationDefaultStates.OBSOLETE,
                                    userId,
                                    new GregorianCalendar(),
                                    StageAndVersionPublicationConstant.PUBLICATION_LOG_OBSOLETE);
        addLog(node, versionLog);
      }
      versionLog = new VersionLog(liveVersion.getName(),
                                  newState,
                                  userId,
                                  new GregorianCalendar(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_LIVE);
      addLog(node,versionLog);
      //change base version to published state
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, PublicationDefaultStates.PUBLISHED);
      VersionData editableRevision = revisionsMap.get(node.getUUID());
      if (editableRevision != null) {
        editableRevision.setAuthor(userId);
        editableRevision.setState(PublicationDefaultStates.ENROLLED);
      } else {
        editableRevision = new VersionData(node.getUUID(),
                                           PublicationDefaultStates.ENROLLED,
                                           userId);
      }
      revisionsMap.put(node.getUUID(),editableRevision);
      versionLog = new VersionLog(node.getBaseVersion().getName(),
                                  PublicationDefaultStates.DRAFT,
                                  userId,
                                  new GregorianCalendar(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_LIFECYCLE);
      //Change all live revision to obsolete
      Value  liveVersionValue = valueFactory.createValue(liveVersion);
      node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP,liveVersionValue);
      node.setProperty(StageAndVersionPublicationConstant.LIVE_DATE_PROP,new GregorianCalendar());
      VersionData liveRevisionData = new VersionData(liveVersion.getUUID(), PublicationDefaultStates.PUBLISHED,userId);
      revisionsMap.put(liveVersion.getUUID(),liveRevisionData);
      addRevisionData(node,revisionsMap.values());
    } else if (PublicationDefaultStates.OBSOLETE.equalsIgnoreCase(newState)) {
      Value value = valueFactory.createValue(selectedRevision);
      Value liveRevision = getValue(node,StageAndVersionPublicationConstant.LIVE_REVISION_PROP);
      if (liveRevision != null && value.getString().equals(liveRevision.getString())) {
        node.setProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP,
                         valueFactory.createValue(""));
      }
      versionLog = new VersionLog(selectedRevision.getName(),
                                  PublicationDefaultStates.OBSOLETE,
                                  userId,
                                  new GregorianCalendar(),
                                  StageAndVersionPublicationConstant.PUBLICATION_LOG_OBSOLETE);
      VersionData versionData = revisionsMap.get(selectedRevision.getUUID());
      if (versionData != null) {
        versionData.setAuthor(userId);
        versionData.setState(PublicationDefaultStates.OBSOLETE);
      } else {
        versionData = new VersionData(selectedRevision.getUUID(),
                                      PublicationDefaultStates.OBSOLETE,
                                      userId);
      }
      revisionsMap.put(selectedRevision.getUUID(),versionData);
      addLog(node,versionLog);
      //change base version to published state
      node.setProperty(StageAndVersionPublicationConstant.CURRENT_STATE, PublicationDefaultStates.OBSOLETE);
      addRevisionData(node,revisionsMap.values());
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
    
    NodeLocation location = NodeLocation.make(node);
    composer.updateContent(location.getRepository(),
                           location.getWorkspace(),
                           location.getPath(),
                           new HashMap<String, String>());
  }
  
  /**
   * Gets the value.
   *
   * @param node the node
   * @param prop the prop
   *
   * @return the value
   */
  private Value getValue(Node node, String prop) {
    try {
      return node.getProperty(prop).getValue();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the live revision.
   *
   * @param node the node
   *
   * @return the live revision
   */
  private Node getLiveRevision(Node node) {
    try {
      String nodeVersionUUID = node.getProperty(StageAndVersionPublicationConstant.LIVE_REVISION_PROP).getString();
      if ("".equals(nodeVersionUUID)
          && PublicationDefaultStates.PUBLISHED.equals(node.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE)
                                                           .getString()))
            return node;
      return node.getVersionHistory().getSession().getNodeByUUID(nodeVersionUUID);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Adds the log.
   *
   * @param node the node
   * @param versionLog the version log
   *
   * @throws Exception the exception
   */
  private void addLog(Node node, VersionLog versionLog) throws Exception{
    Value[] values = node.getProperty(StageAndVersionPublicationConstant.HISTORY).getValues();
    ValueFactory valueFactory = node.getSession().getValueFactory();
    List<Value> list = new ArrayList<Value>(Arrays.asList(values));
    list.add(valueFactory.createValue(versionLog.toString()));
    node.setProperty(StageAndVersionPublicationConstant.HISTORY,list.toArray(new Value[]{}));
  }

  /**
   * Adds the revision data.
   *
   * @param node the node
   * @param list the list
   *
   * @throws Exception the exception
   */
  private void addRevisionData(Node node, Collection<VersionData> list) throws Exception {
    List<Value> valueList = new ArrayList<Value>();
    ValueFactory factory = node.getSession().getValueFactory();
    for(VersionData versionData: list) {
      valueList.add(factory.createValue(versionData.toStringValue()));
    }
    node.setProperty(StageAndVersionPublicationConstant.REVISION_DATA_PROP,valueList.toArray(new Value[]{}));
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

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.ecm.publication.PublicationPlugin#
   * getLocalizedAndSubstituteMessage(java.util.Locale, java.lang.String,
   * java.lang.String[])
   */
  public String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values) throws Exception {
    ClassLoader cl=this.getClass().getClassLoader();
    ResourceBundle resourceBundle= ResourceBundle.getBundle(StageAndVersionPublicationConstant.LOCALIZATION, locale, cl);
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

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getNodeView(javax.jcr.Node, java.util.Map)
   */
  public Node getNodeView(Node node, Map<String, Object> context) throws Exception {
    // don't display content if state is enrolled or obsolete
    WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
    String currentState = wcmPublicationService.getContentState(node);
    if (PublicationDefaultStates.ENROLLED.equals(currentState)
        || PublicationDefaultStates.OBSOLETE.equals(currentState))
      return null;

    // if current mode is edit mode
    if (context.get(WCMComposer.FILTER_MODE).equals(WCMComposer.MODE_EDIT)) return node;

    // if current mode is live mode
    Node liveNode = getLiveRevision(node);
    if(liveNode != null) {
      return liveNode.getNode("jcr:frozenNode");
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getPossibleStates()
   */
  public String[] getPossibleStates() {
    return new String[] { PublicationDefaultStates.ENROLLED, PublicationDefaultStates.DRAFT,
        PublicationDefaultStates.PUBLISHED, PublicationDefaultStates.OBSOLETE };
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getStateImage(javax.jcr.Node, java.util.Locale)
   */
  public byte[] getStateImage(Node arg0, Locale arg1) throws IOException,
  FileNotFoundException,
  Exception {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getStateUI(
   *     javax.jcr.Node, org.exoplatform.webui.core.UIComponent)
   */
  public UIForm getStateUI(Node node, UIComponent component) throws Exception {
    UIPublicationContainer publicationContainer = component.createUIComponent(UIPublicationContainer.class, null, null);
    publicationContainer.initContainer(node);
    return publicationContainer;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getUserInfo(javax.jcr.Node, java.util.Locale)
   */
  public String getUserInfo(Node arg0, Locale arg1) throws Exception {
    return null;
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * publishContentToSCV(javax.jcr.Node,
   * org.exoplatform.portal.config.model.Page, java.lang.String)
   */
  public void publishContentToSCV(Node content, Page page, String portalOwnerName) throws Exception {
    if (pomManager.getSession() == null) pomSession = pomManager.openSession();
    // Create portlet
    Application<Portlet> portlet = new Application<Portlet>(ApplicationType.PORTLET);
    portlet.setShowInfoBar(false);

    //// generate new portlet's id
    WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    StringBuilder windowId = new StringBuilder();
    windowId.append(PortalConfig.PORTAL_TYPE)
            .append("#")
            .append(portalOwnerName)
            .append(":")
            .append(configurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET))
            .append("/")
            .append(IdGenerator.generate());

    //// Add preferences to portlet
    ArrayList<Preference> preferences = new ArrayList<Preference>();
    preferences.add(addPreference("repository",
                                  ((ManageableRepository) content.getSession().getRepository()).getConfiguration()
                                                                                               .getName()));
    preferences.add(addPreference("workspace", content.getSession().getWorkspace().getName()));
    preferences.add(addPreference("nodeIdentifier", content.getUUID()));
    preferences.add(addPreference("ShowQuickEdit", "true"));
    preferences.add(addPreference("ShowTitle", "true"));
    preferences.add(addPreference("ShowVote", "false"));
    preferences.add(addPreference("ShowComments", "false"));
    preferences.add(addPreference("ShowPrintAction", "true"));
    preferences.add(addPreference("isQuickCreate", "false"));
    savePortletPreferences(windowId.toString(), preferences, portalOwnerName);

    // Add portlet to page
//    ArrayList<Object> listPortlet = page.getChildren();
//    listPortlet.add(portlet);
//    page.setChildren(listPortlet);
    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    userPortalConfigService.update(page);
    if (pomSession != null) pomSession.close();
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * publishContentToCLV(javax.jcr.Node,
   * org.exoplatform.portal.config.model.Page, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public void publishContentToCLV(Node content,
                                  Page page,
                                  String clvPortletId,
                                  String portalOwnerName,
                                  String remoteUser) throws Exception {
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    ArrayList<Preference> preferences = new ArrayList<Preference>();
    PortletPreferences portletPreferences = dataStorage.getPortletPreferences(clvPortletId);
    if (portletPreferences == null) {
      preferences.add(addPreference("repository",
                                    ((ManageableRepository) content.getSession().getRepository()).getConfiguration()
                                                                                                 .getName()));
      preferences.add(addPreference("workspace", content.getSession().getWorkspace().getName()));
      preferences.add(addPreference("folderPath", content.getPath() + ";"));
      preferences.add(addPreference("formViewTemplatePath",
                                    wcmConfigurationService.
                                        getRuntimeContextParam(WCMConfigurationService.FORM_VIEW_TEMPLATE_PATH)));
      preferences.add(addPreference("paginatorTemplatePath",
                                    wcmConfigurationService.
                                        getRuntimeContextParam(WCMConfigurationService.PAGINATOR_TEMPLAET_PATH)));
      preferences.add(addPreference("itemsPerPage", "10"));
      preferences.add(addPreference("showQuickEditButton", "true"));
      preferences.add(addPreference("showRefreshButton", "false"));
      preferences.add(addPreference("showThumbnailsView", "true"));
      preferences.add(addPreference("showTitle", "true"));
      preferences.add(addPreference("showDateCreated", "true"));
      preferences.add(addPreference("showSummary", "true"));
      preferences.add(addPreference("showHeader", "true"));
      preferences.add(addPreference("source", "folder"));
      preferences.add(addPreference("mode", "ManualViewerMode"));
      preferences.add(addPreference("orderBy", "exo:title"));
      preferences.add(addPreference("orderType", "DESC"));

      Preference preference = new Preference();
      preference.setName("contents");
      ArrayList<String> contentValues = new ArrayList<String>();
      contentValues.add(content.getPath());
      preference.setValues(contentValues);
      preferences.add(preference);

      savePortletPreferences(clvPortletId, preferences, portalOwnerName);
      updateOnAddNodeProperties(page, content, clvPortletId, remoteUser);
    } else {
      String clvMode = "";
      Preference folderPreference = new Preference();
      Preference contentPreference = new Preference();
      int folderPrefIndex = 0;
      int contentPrefIndex = 0;
      List<?> listPrefs = portletPreferences.getPreferences();
      for (Object object: listPrefs) {
        Preference preference = (Preference) object;
        if ("mode".equals(preference.getName())) {
          clvMode = preference.getValues().get(0).toString();
        } else if ("contents".equals(preference.getName())) {
          contentPreference = preference;
          contentPrefIndex = listPrefs.indexOf(object);
        } else if ("folderPath".equals(preference.getName())) {
          folderPreference = preference;
          folderPrefIndex = listPrefs.indexOf(object);
        }
        preferences.add(preference);
      }
      if ("ManualViewerMode".equals(clvMode)) {
        ArrayList folderValues = new ArrayList(folderPreference.getValues());
        String folderValue = folderValues.get(0).toString();
        folderValues.set(0, content.getPath() + ";" + folderValue);
        folderPreference.setValues(folderValues);
        preferences.set(folderPrefIndex, folderPreference);

        ArrayList contentValues = new ArrayList(contentPreference.getValues());
        contentValues.add(0, content.getPath());
        contentPreference.setValues(contentValues);
        preferences.set(contentPrefIndex, contentPreference);

        savePortletPreferences(clvPortletId, preferences, portalOwnerName);
        updateOnAddNodeProperties(page, content, clvPortletId, remoteUser);
      }
    }
  }

  /**
   * Update on add node properties.
   *
   * @param page the page
   * @param content the content
   * @param clvPortletId the clv portlet id
   * @param remoteUser the remote user
   *
   * @throws Exception the exception
   */
  private void updateOnAddNodeProperties(Page page,
                                         Node content,
                                         String clvPortletId,
                                         String remoteUser) throws Exception {
    if (content.canAddMixin("publication:webpagesPublication"))
      content.addMixin("publication:webpagesPublication");
    List<String> listExistedNavigationNodeUri = PublicationUtil.getValuesAsString(content,
                                                                                  "publication:navigationNodeURIs");
    List<String> listPageNavigationUri = getListUserNavigationUri(page, remoteUser);
    if (listPageNavigationUri.isEmpty())
      return;
    for (String uri : listPageNavigationUri) {
      if (!listExistedNavigationNodeUri.contains(uri)) {
        listExistedNavigationNodeUri.add(uri);
      }
    }

    List<String> nodeAppIds = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    String mixedAppId = PublicationUtil.setMixedApplicationId(page.getPageId(), clvPortletId);
    if(nodeAppIds.contains(mixedAppId)) return;
    nodeAppIds.add(mixedAppId);

    List<String> nodeWebPageIds = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
    nodeWebPageIds.add(page.getPageId());

    Session session = content.getSession();
    ValueFactory valueFactory = session.getValueFactory();
    content.setProperty("publication:navigationNodeURIs",
                        PublicationUtil.toValues(valueFactory, listExistedNavigationNodeUri));
    content.setProperty("publication:applicationIDs", PublicationUtil.toValues(valueFactory,
                                                                               nodeAppIds));
    content.setProperty("publication:webPageIDs", PublicationUtil.toValues(valueFactory,
                                                                           nodeWebPageIds));
    session.save();
  }

  /**
   * Update on remove node properties.
   *
   * @param page the page
   * @param content the content
   * @param clvPortletId the clv portlet id
   * @param remoteUser the remote user
   *
   * @throws Exception the exception
   */
  private void updateOnRemoveNodeProperties(Page page,
                                            Node content,
                                            String clvPortletId,
                                            String remoteUser) throws Exception {
    List<String> listExistedApplicationId = PublicationUtil.getValuesAsString(content,
                                                                              "publication:applicationIDs");

    if (listExistedApplicationId.remove(PublicationUtil.setMixedApplicationId(page.getPageId(), clvPortletId))) {
      List<String> listExistedPageId = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
      listExistedPageId.remove(page.getPageId());

      List<String> listPageNavigationUri = getListUserNavigationUri(page, remoteUser);
      List<String> listExistedNavigationNodeUri = PublicationUtil.getValuesAsString(content,
                                                                                    "publication:navigationNodeURIs");
      List<String> listExistedNavigationNodeUriTmp = new ArrayList<String>();
      listExistedNavigationNodeUriTmp.addAll(listExistedNavigationNodeUri);
      for (String existedNavigationNodeUri : listExistedNavigationNodeUriTmp) {
        if (listPageNavigationUri.contains(existedNavigationNodeUri)) {
          listExistedNavigationNodeUri.remove(existedNavigationNodeUri);
        }
      }
      Session session = content.getSession();
      ValueFactory valueFactory = session.getValueFactory();
      content.setProperty("publication:applicationIDs", PublicationUtil.toValues(valueFactory, listExistedApplicationId));
      content.setProperty("publication:webPageIDs", PublicationUtil.toValues(valueFactory, listExistedPageId));
      content.setProperty("publication:navigationNodeURIs",
                          PublicationUtil.toValues(valueFactory, listExistedNavigationNodeUri));
      session.save();
    }
  }

  /**
   * Adds the preference.
   *
   * @param name the name
   * @param value the value
   *
   * @return the preference
   */
  private Preference addPreference(String name, String value) {
    Preference preference = new Preference();
    ArrayList<String> listValue = new ArrayList<String>();
    listValue.add(value);
    preference.setName(name);
    preference.setValues(listValue);
    return preference;
  }

  /**
   * Save portlet preferences.
   *
   * @param portletId the portlet id
   * @param listPreference the list preference
   * @param portalOwnerName the portal owner name
   *
   * @throws Exception the exception
   */
  private void savePortletPreferences(String portletId,
                                      ArrayList<Preference> listPreference,
                                      String portalOwnerName) throws Exception {
    if (pomManager.getSession() == null) pomSession = pomManager.openSession();
    PortletPreferences portletPreferences = new PortletPreferences();
    portletPreferences.setWindowId(portletId);
    portletPreferences.setPreferences(listPreference);
    dataStorage.save(portletPreferences);
    if (pomSession != null) pomSession.close();
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * suspendPublishedContentFromPage(javax.jcr.Node,
   * org.exoplatform.portal.config.model.Page, java.lang.String)
   */
  public void suspendPublishedContentFromPage(Node content, Page page, String remoteUser) throws Exception {
    // Remove content from CLV portlet
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    List<String> clvPortletsId = PublicationUtil.findAppInstancesByName(page,
                                                                        wcmConfigurationService.getRuntimeContextParam
                                                                            (WCMConfigurationService.CLV_PORTLET));
    if (content != null && !clvPortletsId.isEmpty()) {
      for (String clvPortletId : clvPortletsId) {
        PortletPreferences portletPreferences = dataStorage.getPortletPreferences(clvPortletId);
        if (portletPreferences != null) {
          ArrayList<Preference> preferences = new ArrayList<Preference>();
          for (Object preferenceTmp : portletPreferences.getPreferences()) {
            Preference preference = (Preference) preferenceTmp;
            if ("folderPath".equals(preference.getName()) && preference.getValues().size() > 0) {
              ArrayList<String> values = new ArrayList<String>();
              values.add(preference.getValues().get(0).toString().replaceAll(content.getPath() + ";", ""));
              preference.setValues(values);
            } else if ("contents".equals(preference.getName()) && preference.getValues().size() > 0) {
              List<String> values = preference.getValues();
              values.remove(content.getPath());
              preference.setValues(new ArrayList<String>(values));
            }
            preferences.add(preference);
          }
          dataStorage.save(portletPreferences);
          updateOnRemoveNodeProperties(page, content, clvPortletId, remoteUser);
        }
      }
    }
    // Remove content from SCV portlet
    String pageId = page.getPageId();
    List<String> mixedApplicationIDs = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    ArrayList<String> removedApplicationIDs = new ArrayList<String>();
    for(String mixedID: mixedApplicationIDs) {
      if (mixedID.startsWith(pageId)
          && mixedID.contains(wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET))) {
        String realAppID = PublicationUtil.parseMixedApplicationId(mixedID)[1];
        removedApplicationIDs.add(realAppID);
      }
    }
    if(removedApplicationIDs.size() == 0) return;
    PublicationUtil.removeApplicationFromPage(page, removedApplicationIDs);
    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    userPortalConfigService.update(page);
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.wcm.publication.WebpagePublicationPlugin#
   * updateLifecycleOnChangeNavigation
   * (org.exoplatform.portal.config.model.PageNavigation, java.lang.String)
   */
  public void updateLifecycleOnChangeNavigation(NavigationContext navigationContext, String remoteUser) throws Exception {
    navigationEventListenerDelegate.updateLifecycleOnChangeNavigation(navigationContext, remoteUser, this);
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
   * Gets the running portals.
   *
   * @param userId the user id
   *
   * @return the running portals
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
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

  /**
   * Gets the list page navigation uri.
   *
   * @param page the page
   * @param remoteUser the remote user
   *
   * @return the list page navigation uri
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
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

  /**
   * In this publication process, we put the content in Draft state when editing it.
   */
  public void updateLifecyleOnChangeContent(Node node, String remoteUser)
  throws Exception {
    updateLifecyleOnChangeContent(node, remoteUser, PublicationDefaultStates.DRAFT);
  }

  /**
   * In this publication process, we put the content in Draft state when editing
   * it.
   */
  public void updateLifecyleOnChangeContent(Node node, String remoteUser, String newState) throws Exception {

    String state = node.getProperty(StageAndVersionPublicationConstant.CURRENT_STATE).getString();

    if (state.equals(newState))
      return;

    HashMap<String, String> context = new HashMap<String, String>();
    changeState(node, newState, context);
  }
}
