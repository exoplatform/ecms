/*
 * Copyright (C) 2003-2014 eXo Platform SEA.
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

package org.exoplatform.ecm.webui.component.explorer.popup.service;

import javax.jcr.*;

import org.junit.Ignore;

import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * Aug 7, 2014
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
})
@Ignore
public class TestService extends AbstractKernelTest {
  //private Log log = ExoLogger.getExoLogger(TestService.class);

  protected final String REPO_NAME = "repository";
  protected final String DEFAULT_WS = "collaboration";

  protected static PortalContainer        container;
  private RepositoryService      repositoryService;
  protected ManageableRepository repository;
  protected SessionProvider sessionProvider;
  protected Session                session;
  protected SessionProviderService sessionProviderService_;
  protected ProviderBinder         providers;

  private org.exoplatform.social.core.identity.model.Identity rootIdentity;


  private String perm = PermissionType.READ+","+PermissionType.ADD_NODE+","+PermissionType.SET_PROPERTY;
  private String comment = "Comment";
  private String spaceName = "/spaces/space1";
  private String userName = "john";
  private String nodePath = "nodeToShare";
  private String activityId;
  private NodeLocation nodeLocation;
  private String spaceId;

  private LinkManager linkManager;

  /**
   * Clear current container
   */
  protected void tearDown() throws Exception {
    //delete node
    this.session.getRootNode().getNode(nodePath).remove();
    this.session.save();
    //delete activity
    ActivityManager manager = (ActivityManager)container.getComponentInstanceOfType(ActivityManager.class);
    manager.deleteActivity(activityId);
    //delete space
    SpaceService spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    spaceService.deleteSpace(spaceId);
    RequestLifeCycle.end();
  }

  private void initContainer() {
    container = PortalContainer.getInstance();
    repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
    sessionProviderService_ = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    String loginConf = this.getClass().getResource("/conf/standalone/login.conf").toString();
    System.setProperty("java.security.auth.login.config", loginConf);
    try {
      applySystemSession();
    } catch (Exception e) {
      fail();
      throw new RuntimeException("Failed to initialize standalone container: " + e.getMessage(), e);
    }
  }

  @Override
  protected void setUp() throws Exception {
    begin();
    ConversationState conversionState = ConversationState.getCurrent();
    if(conversionState == null) {
      conversionState = new ConversationState(new Identity("root"));
      ConversationState.setCurrent(conversionState);
    }

    initContainer();
    IdentityManager identityManager = container.getComponentInstanceOfType(IdentityManager.class);
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    linkManager =(LinkManager) container.getComponentInstanceOfType(LinkManager.class);

    this.nodeLocation = NodeLocation.getNodeLocationByNode(this.session.getRootNode().addNode(nodePath));
    session.save();
    SpaceService spaceService = (SpaceService)container.getComponentInstanceOfType(SpaceService.class);
    Space sp = getSpaceInstance(spaceService, "space1");
    spaceId = sp.getId();
    //init space node
    Node group = null;
    if(!session.getRootNode().hasNode("Groups"))group = session.getRootNode().addNode("Groups");
    else group = session.getRootNode().getNode("Groups");
    Node spaces = null;
    if(!group.hasNode("spaces"))spaces = group.addNode("spaces");
    else spaces = group.getNode("spaces");
    Node space = spaces.addNode(spaceName.split("/")[2]);
    space.addNode("Documents");
    session.save();
  }

  public void testShare() throws Exception {
  //share node
    DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, false);
    IShareDocumentService temp = (IShareDocumentService) container.getComponentInstanceOfType(IShareDocumentService.class);
    activityId = temp.publishDocumentToSpace(spaceName, NodeLocation.getNodeByLocation(nodeLocation), comment, perm);


    //Test symbolic link
    NodeIterator nodeIterator = session.getRootNode().getNode("Groups/spaces/space1/Documents/Shared").getNodes();
    assertEquals(1, nodeIterator.getSize());
    Node target = nodeIterator.nextNode();
    assertEquals("exo:symlink", target.getPrimaryNodeType().getName());
    Node origin = linkManager.getTarget(target,true);
    assertEquals("/" + nodePath, origin.getPath());
    //Test permission
    ExtendedNode extendedNode = (ExtendedNode) origin;
    assertTrue(!extendedNode.getACL().getPermissions("*:" + spaceName).isEmpty());
    //Test activity
    ActivityManager manager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    ExoSocialActivity activity = manager.getActivity(this.activityId);
    assertEquals(this.comment, activity.getTitle());
  }

  public void testShareUser() throws Exception {
    //share node
    DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, false);
    IShareDocumentService temp = (IShareDocumentService) container.getComponentInstanceOfType(IShareDocumentService.class);
    temp.publishDocumentToUser(userName, NodeLocation.getNodeByLocation(nodeLocation), comment, perm);


    //Test symbolic link
    NodeIterator nodeIterator = session.getRootNode().getNode("Users/j___/jo___/joh___/john/Private/Documents/Shared").getNodes();
    assertEquals(1, nodeIterator.getSize());
    Node target = nodeIterator.nextNode();
    assertEquals("exo:symlink", target.getPrimaryNodeType().getName());
    Node origin = linkManager.getTarget(target,true);
    assertEquals("/" + nodePath, origin.getPath());
    //Test permission
    ExtendedNode extendedNode = (ExtendedNode) origin;
    assertTrue(!extendedNode.getACL().getPermissions(userName).isEmpty());
  }

  public void applySystemSession() throws Exception{
    System.setProperty("gatein.tenant.repository.name", REPO_NAME);
    container = PortalContainer.getInstance();

    repositoryService.setCurrentRepositoryName(REPO_NAME);
    repository = repositoryService.getCurrentRepository();

    closeOldSession();
    sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    session = sessionProvider.getSession(DEFAULT_WS, repository);
    sessionProvider.setCurrentRepository(repository);
    sessionProvider.setCurrentWorkspace(DEFAULT_WS);
  }


  /**
   * Close current session
   */
  private void closeOldSession() {
    if (session != null && session.isLive()) {
      session.logout();
    }
  }

  private Space getSpaceInstance(SpaceService spaceService, String spaceName)
          throws Exception {
    Space space = new Space();
    space.setDisplayName(spaceName);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + spaceName);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.OPEN);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId(SpaceUtils.SPACE_GROUP + "/" + space.getPrettyName());
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] { "root"};
    String[] members = new String[] { "root" };
    String[] invitedUsers = new String[] {};
    String[] pendingUsers = new String[] {};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    spaceService.saveSpace(space, true);
    return space;
  }
  
}
