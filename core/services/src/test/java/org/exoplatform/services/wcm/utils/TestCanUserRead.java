/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.services.wcm.utils;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * A document is readable for a user only through that user's own session.
 */
public class TestCanUserRead extends BaseWCMTestCase {

  private static final String ROOT_NODE = "testCanUserRead";

  /** A plain member of the platform, in no administrators group */
  private static final String USER     = "demo";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    applySystemSession();
    container.getComponentInstanceOfType(IdentityRegistry.class)
             .register(new Identity(USER, Set.of(new MembershipEntry("/platform/users", "member"))));
  }

  @Override
  public void tearDown() throws Exception {
    container.getComponentInstanceOfType(IdentityRegistry.class).unregister(USER);
    for (Session workspaceSession : new Session[] { session, sessionProvider.getSession(DMSSYSTEM_WS, repository) }) {
      if (workspaceSession.getRootNode().hasNode(ROOT_NODE)) {
        workspaceSession.getRootNode().getNode(ROOT_NODE).remove();
        workspaceSession.save();
      }
    }
    super.tearDown();
  }

  public void testAUserReadsOnlyWhatTheirOwnSessionReads() throws Exception {
    Node parent = session.getRootNode().addNode(ROOT_NODE);
    ExtendedNode restricted = (ExtendedNode) parent.addNode("restricted");
    restricted.addMixin("exo:privilegeable");
    restricted.setPermission("root", PermissionType.ALL);
    restricted.removePermission(IdentityConstants.ANY);
    ExtendedNode open = (ExtendedNode) parent.addNode("open");
    open.addMixin("exo:privilegeable");
    open.setPermission("root", PermissionType.ALL);
    open.setPermission("*:/platform/users", new String[] { PermissionType.READ });
    open.removePermission(IdentityConstants.ANY);
    session.save();
    LinkManager linkManager = container.getComponentInstanceOfType(LinkManager.class);
    ExtendedNode link = (ExtendedNode) linkManager.createLink(parent, "exo:symlink", restricted, "linkToRestricted");
    // the link itself is readable, its target is not
    if (!link.isNodeType("exo:privilegeable")) {
      link.addMixin("exo:privilegeable");
    }
    link.setPermission("*:/platform/users", new String[] { PermissionType.READ });
    ExtendedNode linkToOpen = (ExtendedNode) linkManager.createLink(parent, "exo:symlink", open, "linkToOpen");
    session.save();
    Session otherWorkspace = sessionProvider.getSession(DMSSYSTEM_WS, repository);
    ExtendedNode elsewhere = (ExtendedNode) otherWorkspace.getRootNode().addNode(ROOT_NODE);
    elsewhere.addMixin("exo:privilegeable");
    elsewhere.setPermission("root", PermissionType.ALL);
    elsewhere.setPermission("*:/platform/users", new String[] { PermissionType.READ });
    elsewhere.removePermission(IdentityConstants.ANY);
    otherWorkspace.save();

    assertTrue(WCMCoreUtils.canUserRead("root", COLLABORATION_WS, restricted.getIdentifier()));
    assertTrue(WCMCoreUtils.canUserRead(USER, COLLABORATION_WS, open.getIdentifier()));
    assertTrue("the default workspace when none is given", WCMCoreUtils.canUserRead(USER, null, open.getIdentifier()));
    assertTrue("the workspace given", WCMCoreUtils.canUserRead(USER, DMSSYSTEM_WS, elsewhere.getIdentifier()));
    assertFalse(WCMCoreUtils.canUserRead(USER, COLLABORATION_WS, restricted.getIdentifier()));
    assertTrue("a link to a document the user can read", WCMCoreUtils.canUserRead(USER, COLLABORATION_WS, linkToOpen.getIdentifier()));
    assertFalse("a link to a document the user can't read", WCMCoreUtils.canUserRead(USER, COLLABORATION_WS, link.getIdentifier()));
    assertFalse(WCMCoreUtils.canUserRead(USER, COLLABORATION_WS, "missing"));
    assertFalse(WCMCoreUtils.canUserRead(null, COLLABORATION_WS, open.getIdentifier()));
  }
}
