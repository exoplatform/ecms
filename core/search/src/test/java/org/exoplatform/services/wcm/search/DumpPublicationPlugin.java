/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.exoplatform.commons.utils.ListAccess;
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
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.publication.listener.navigation.NavigationEventListenerDelegate;
import org.exoplatform.services.wcm.publication.listener.page.PageEventListenerDelegate;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 30, 2008
 */
public class DumpPublicationPlugin extends WebpagePublicationPlugin{

  /** The Constant DEFAULT_STATE. */
  public static final String DEFAULT_STATE = PublicationDefaultStates.DRAFT;

  /** The Constant PUBLICATION. */
  public static final String PUBLICATION = "publication:publication";

  /** The Constant LIFECYCLE_PROP. */
  public static final String LIFECYCLE_PROP = "publication:lifecycleName";

  /** The Constant CURRENT_STATE. */
  public static final String CURRENT_STATE = "publication:currentState";

  /** The Constant HISTORY. */
  public static final String HISTORY = "publication:history";

  /** The Constant WCM_PUBLICATION_MIXIN. */
  public static final String WCM_PUBLICATION_MIXIN = "publication:simplePublication";

  /** The Constant LIFECYCLE_NAME. */
  public static final String LIFECYCLE_NAME = "Simple publication";

  /** The Constant LOCALE_FILE. */
  private static final String LOCALE_FILE = "locale.services.publication.lifecycle.simple.SimplePublication";

  /** The Constant IMG_PATH. */
  public static final String IMG_PATH = "artifacts/";

  /** The page event listener delegate. */
  private PageEventListenerDelegate pageEventListenerDelegate;

  /** The navigation event listener delegate. */
  private NavigationEventListenerDelegate navigationEventListenerDelegate;

  /**
   * Instantiates a new wCM publication plugin.
   */
  public DumpPublicationPlugin() {
    pageEventListenerDelegate = new PageEventListenerDelegate(LIFECYCLE_NAME,
                                                              ExoContainerContext.getCurrentContainer());
    navigationEventListenerDelegate = new NavigationEventListenerDelegate(LIFECYCLE_NAME,
                                                                          ExoContainerContext.getCurrentContainer());

  }

  public String getLifecycleType() {
    return WCM_PUBLICATION_MIXIN;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#addMixin(javax.jcr.Node)
   */
  public void addMixin(Node node) throws Exception {
    node.addMixin(WCM_PUBLICATION_MIXIN);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#canAddMixin(javax.jcr.Node)
   */
  public boolean canAddMixin(Node node) throws Exception {
    List<String> runningPortals = getRunningPortals(node.getSession().getUserID());
    if(runningPortals.size() == 0) {
      throw new AccessControlException("Current user doesn't have access permission to any portal");
    }
    if (node.isLocked()) {
      throw new LockException("This node is locked");
    }

    if (!node.isCheckedOut()) {
      throw new VersionException("This node is checked-in");
    }

    return node.canAddMixin(WCM_PUBLICATION_MIXIN);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#changeState(javax.jcr.Node, java.lang.String, java.util.HashMap)
   */
  public void changeState(Node node, String newState, HashMap<String, String> context) throws IncorrectStateUpdateLifecycleException, Exception {
    Session session = node.getSession();
    node.setProperty(CURRENT_STATE, newState);
    PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class);

    if (newState.equals(PublicationDefaultStates.DRAFT)) {
      String lifecycleName = node.getProperty("publication:lifecycleName").getString();
      String[] logs = new String[] {new Date().toString(), PublicationDefaultStates.DRAFT, session.getUserID(), "PublicationService.SimplePublicationPlugin.changeState.enrolled", lifecycleName};
      publicationService.addLog(node, logs);
    } else if (newState.equals(PublicationDefaultStates.PUBLISHED)) {
      String[] logs = new String[] {new Date().toString(), PublicationDefaultStates.PUBLISHED, session.getUserID(), "PublicationService.SimplePublicationPlugin.changeState.published"};
      publicationService.addLog(node, logs);
    } else if (newState.equals(PublicationDefaultStates.ENROLLED)) {
      String[] logs = new String[] {new Date().toString(), PublicationDefaultStates.ENROLLED, session.getUserID(), "PublicationService.SimplePublicationPlugin.changeState.published"};
      publicationService.addLog(node, logs);
    } else {
      throw new Exception("WCMPublicationPlugin.changeState : Unknown state : " + node.getProperty(CURRENT_STATE).getString());
    }

    session.save();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getPossibleStates()
   */
  public String[] getPossibleStates() { return new String[] { PublicationDefaultStates.ENROLLED, PublicationDefaultStates.DRAFT, PublicationDefaultStates.PUBLISHED}; }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getStateImage(javax.jcr.Node, java.util.Locale)
   */
  public byte[] getStateImage(Node node, Locale locale) throws IOException,
  FileNotFoundException, Exception {

    byte[] bytes = null;
    String fileName= "WCM";
    String currentState = node.getProperty(CURRENT_STATE).getString();
    if (PublicationDefaultStates.PUBLISHED.equals(currentState)) {
      fileName+="Published";
    } else {
      fileName+="Unpublished";
    }
    String fileNameLocalized =fileName+"_"+locale.getLanguage();
    String completeFileName=IMG_PATH+fileNameLocalized+".gif";

    InputStream in = this.getClass().getClassLoader().getResourceAsStream(completeFileName);
    if (in==null) {
      completeFileName=IMG_PATH+fileName+".gif";
      in = this.getClass().getClassLoader().getResourceAsStream(completeFileName);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    transfer(in, out);
    bytes = out.toByteArray();
    return bytes;
  }

  /** The Constant BUFFER_SIZE. */
  private static final int BUFFER_SIZE = 512;

  /**
   * Transfer.
   *
   * @param in the in
   * @param out the out
   *
   * @return the int
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static int transfer(InputStream in, OutputStream out) throws IOException {
    int total = 0;
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead = in.read( buffer );
    while ( bytesRead != -1 ) {
      out.write( buffer, 0, bytesRead );
      total += bytesRead;
      bytesRead = in.read( buffer );
    }
    return total;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getStateUI(javax.jcr.Node, org.exoplatform.webui.core.UIComponent)
   */
  public UIForm getStateUI(Node node, UIComponent component) throws Exception {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getUserInfo(javax.jcr.Node, java.util.Locale)
   */
  public String getUserInfo(Node node, Locale locale) throws Exception {

    return null;
  }

  /**
   * Retrives all  the running portals.
   *
   * @param userId the user id
   *
   * @return the running portals
   *
   * @throws Exception the exception
   */
  private List<String> getRunningPortals(String userId) throws Exception {
    POMSessionManager manager = WCMCoreUtils.getService(POMSessionManager.class);
    POMSession session = null;
    if (manager.getSession() == null) session = manager.openSession();
    List<String> listPortalName = new ArrayList<String>();
    DataStorage service = WCMCoreUtils.getService(DataStorage.class);
    Query<PortalConfig> query = new Query<PortalConfig>(null, null, null, null, PortalConfig.class) ;
    ListAccess<PortalConfig> pageList = service.find2(query) ;
    List<PortalConfig> portalConfigs = WCMCoreUtils.getAllElementsOfListAccess(pageList);
    UserACL userACL = WCMCoreUtils.getService(UserACL.class);
    for(PortalConfig portalConfig : portalConfigs) {
      if(userACL.hasPermission(portalConfig)) {
        listPortalName.add(portalConfig.getName());
      }
    }
    if (session != null) session.close();
    return listPortalName;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#publishContentToPage(javax.jcr.Node, org.exoplatform.portal.config.model.Page)
   */
  public void publishContentToPage(Node content, Page page) throws Exception {
    Application<Portlet> portlet = new Application<Portlet>(ApplicationType.PORTLET);
    portlet.setShowInfoBar(false);

    // Create portlet
    WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    StringBuilder windowId = new StringBuilder();
    windowId.append(PortalConfig.PORTAL_TYPE)
            .append("#")
            .append(org.exoplatform.portal.webui.util.Util.getUIPortal().getSiteKey().getName())
            .append(":")
            .append(configurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET))
            .append("/")
            .append(IdGenerator.generate());
//    portlet.setInstanceId(windowId.toString());

    // Add preferences to portlet
    PortletPreferences portletPreferences = new PortletPreferences();
    portletPreferences.setWindowId(windowId.toString());
//    portletPreferences.setOwnerType(PortalConfig.PORTAL_TYPE);
//    portletPreferences.setOwnerId(org.exoplatform.portal.webui.util.Util.getUIPortal().getOwner());
    ArrayList<Preference> listPreference = new ArrayList<Preference>();

    Preference preferenceR = new Preference();
    ArrayList<String> listValue = new ArrayList<String>();
    listValue.add(((ManageableRepository) content.getSession().getRepository()).getConfiguration().getName());
    preferenceR.setName("repository");
    preferenceR.setValues(listValue);
    listPreference.add(preferenceR);

    Preference preferenceW = new Preference();
    listValue = new ArrayList<String>();
    listValue.add(content.getSession().getWorkspace().getName());
    preferenceW.setName("workspace");
    preferenceW.setValues(listValue);
    listPreference.add(preferenceW);

    Preference preferenceN = new Preference();
    listValue = new ArrayList<String>();
    listValue.add(content.getUUID());
    preferenceN.setName("nodeIdentifier");
    preferenceN.setValues(listValue);
    listPreference.add(preferenceN);

    Preference preferenceQ = new Preference();
    listValue = new ArrayList<String>();
    listValue.add("true");
    preferenceQ.setName("ShowQuickEdit");
    preferenceQ.setValues(listValue);
    listPreference.add(preferenceQ);

    portletPreferences.setPreferences(listPreference);

    DataStorage dataStorage = WCMCoreUtils.getService(DataStorage.class);
    dataStorage.save(portletPreferences);

    // Add portlet to page
//    ArrayList<Object> listPortlet = page.getChildren();
//    listPortlet.add(portlet);
//    page.setChildren(listPortlet);
    dataStorage.save(page);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#suspendPublishedContentFromPage(javax.jcr.Node, org.exoplatform.portal.config.model.Page)
   */
  public void suspendPublishedContentFromPage(Node content, Page page) throws Exception {
    String pageId = page.getPageId();
    List<String> mixedApplicationIDs = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    ArrayList<String> removedApplicationIDs = new ArrayList<String>();
    for(String mixedID: mixedApplicationIDs) {
      if(mixedID.startsWith(pageId)) {
        String realAppID = PublicationUtil.parseMixedApplicationId(mixedID)[1];
        removedApplicationIDs.add(realAppID);
      }
    }
    if(removedApplicationIDs.size() == 0) return;
    PublicationUtil.removeApplicationFromPage(page, removedApplicationIDs);
    WCMCoreUtils.getService(DataStorage.class).save(page);
  }

  /**
   * Gets the services.
   *
   * @param page the page
   *
   * @return the services
   *
   * @throws Exception the exception
   */
  public List<String> getListUserNavigationUri(Page page, String remoteUser) throws Exception {
  List<String> listPageNavigationUri = new ArrayList<String>();
  for (String portalName : getRunningPortals(remoteUser)) {

    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    UserPortalConfig userPortalCfg = userPortalConfigService.getUserPortalConfig(portalName,
                                              remoteUser,
                                                                                 PortalRequestContext.USER_PORTAL_CONTEXT);
    UserPortal userPortal = userPortalCfg.getUserPortal();

    //get nodes
    List<UserNavigation> navigationList = userPortal.getNavigations();
    for(UserNavigation nav : navigationList) {
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
   * Checks if is shared portal.
   *
   * @param portalName the portal name
   *
   * @return true, if is shared portal
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unused")
  private boolean isSharedPortal(String portalName) throws Exception{
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Node sharedPortal = livePortalManagerService.getLiveSharedPortal(sessionProvider);
    return sharedPortal.getName().equals(portalName);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getNodeView(javax.jcr.Node, java.util.Map)
   */
  public Node getNodeView(Node node, Map<String, Object> context) throws Exception {
    WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
    String contentState = wcmPublicationService.getContentState(node);

    // if node is obsolette
    if (PublicationDefaultStates.OBSOLETE.equals(contentState)) return null;

    // if current mode is edit mode
    if (context.get(WCMComposer.FILTER_MODE).equals(WCMComposer.MODE_EDIT)) return node;

    // if current mode is live mode and content is NOT draft
    if (!PublicationDefaultStates.DRAFT.equals(contentState)) return node;

    // else
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#updateLifecycleOnChangeNavigation(org.exoplatform.portal.config.model.PageNavigation)
   */
  public void updateLifecycleOnChangeNavigation(NavigationContext navigationContext, String remoteUser) throws Exception {
    navigationEventListenerDelegate.updateLifecycleOnChangeNavigation(navigationContext, remoteUser, this);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#updateLifecycleOnRemovePage(org.exoplatform.portal.config.model.Page)
   */
  public void updateLifecycleOnRemovePage(Page page, String remoteUser) throws Exception {
    pageEventListenerDelegate.updateLifecycleOnRemovePage(page, remoteUser, this);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#updateLifecyleOnChangePage(org.exoplatform.portal.config.model.Page)
   */
  public void updateLifecyleOnChangePage(Page page, String remoteUser) throws Exception {
    pageEventListenerDelegate.updateLifecyleOnChangePage(page, remoteUser, this);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#updateLifecyleOnCreateNavigation(org.exoplatform.portal.config.model.PageNavigation)
   */
  public void updateLifecyleOnCreateNavigation(NavigationContext navigationContext) throws Exception {
    navigationEventListenerDelegate.updateLifecyleOnCreateNavigation(navigationContext);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#updateLifecyleOnCreatePage(org.exoplatform.portal.config.model.Page)
   */
  public void updateLifecyleOnCreatePage(Page page, String remoteUser) throws Exception {
    pageEventListenerDelegate.updateLifecyleOnCreatePage(page, remoteUser, this);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.publication.WebpagePublicationPlugin#updateLifecyleOnRemoveNavigation(org.exoplatform.portal.config.model.PageNavigation)
   */
  public void updateLifecyleOnRemoveNavigation(NavigationContext navigationContext) throws Exception {
    navigationEventListenerDelegate.updateLifecyleOnRemoveNavigation(navigationContext);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.ecm.publication.PublicationPlugin#getLocalizedAndSubstituteMessage(java.util.Locale, java.lang.String, java.lang.String[])
   */
  public String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values)
  throws Exception {
    ClassLoader cl=this.getClass().getClassLoader();
    ResourceBundle resourceBundle= ResourceBundle.getBundle(LOCALE_FILE, locale, cl);
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

  @Override
  public void publishContentToCLV(Node content,
                                  Page page,
                                  String clvPortletId,
                                  String portalOwnerName,
                                  String remoteUser) throws Exception {
  }

  @Override
  public void publishContentToSCV(Node content, Page page, String portalOwnerName) throws Exception {
  }

  @Override
  public void suspendPublishedContentFromPage(Node content, Page page, String remoteUser) throws Exception {
  }

  @Override
  public void updateLifecyleOnChangeContent(Node node, String remoteUser) throws Exception {
    updateLifecyleOnChangeContent(node, remoteUser, PublicationDefaultStates.DRAFT);
  }

  @Override
  public void updateLifecyleOnChangeContent(Node node, String remoteUser, String newState) throws Exception {
    String state = node.getProperty(CURRENT_STATE).getString();

    if (PublicationDefaultStates.DRAFT.equalsIgnoreCase(state)
        && PublicationDefaultStates.DRAFT.equals(newState))
      return;

    HashMap<String, String> context = new HashMap<String, String>();
    changeState(node, newState, context);

  }
}
