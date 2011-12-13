/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;

import junit.framework.TestCase;
import static org.mockito.Mockito.*;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 10, 2010
 */
public class TestUIPublicationPanel extends TestCase {



  public void testIsAuthorizedByRole() throws Exception {
    UIPublicationPanel panel = mock(UIPublicationPanel.class);

    Identity tom = createIdentity("tom","validator:/org/human-resources");
    Identity bill = createIdentity("bill","redactor:/org/human-resources","validator:/org/finances");

    // configuring a mock node with the expected ACL
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    entries.add(new AccessControlEntry("*:/org/finance", PermissionType.READ));
    entries.add(new AccessControlEntry("*:/org/human-resources", PermissionType.SET_PROPERTY));
    AccessControlList acl = new AccessControlList("foo", entries);
    NodeImpl node = mock(NodeImpl.class);
    when(node.getACL()).thenReturn(acl);

    State state = new State();
    state.setRole("validator"); //

    // make sure the actual code we test is not mocked!
    when(panel.isAuthorizedByRole(any(State.class), any(Identity.class), any(NodeImpl.class))).thenCallRealMethod();

    assertTrue("tom should be allowed", panel.isAuthorizedByRole(state, tom, node));
    assertFalse("bill should not be allowed", panel.isAuthorizedByRole(state, bill, node));

  }


  public void testIsAuthorizedByRoles() throws Exception {
    UIPublicationPanel panel = mock(UIPublicationPanel.class);

    Identity tom = createIdentity("tom","validator:/org/human-resources");
    Identity bill = createIdentity("bill","redactor:/org/human-resources","validator:/org/finances");
    Identity bart = createIdentity("bart","member:/org/human-resources");

    // configuring a mock node with the expected ACL
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    entries.add(new AccessControlEntry("*:/org/finance", PermissionType.READ));
    entries.add(new AccessControlEntry("*:/org/human-resources", PermissionType.SET_PROPERTY));
    AccessControlList acl = new AccessControlList("foo", entries);
    NodeImpl node = mock(NodeImpl.class);
    when(node.getACL()).thenReturn(acl);

    State state = new State();
    state.setRoles(Arrays.asList(new String[] {"validator", "redactor"})); //

    // make sure the actual code we test is not mocked!
    when(panel.isAuthorizedByRole(any(State.class), any(Identity.class), any(NodeImpl.class))).thenCallRealMethod();

    assertTrue("tom should be allowed", panel.isAuthorizedByRole(state, tom, node));
    assertTrue("bill should be allowed", panel.isAuthorizedByRole(state, bill, node));
    assertFalse("bart should not be allowed", panel.isAuthorizedByRole(state, bart, node));
  }


  public void testIsAuthorizedByMemberships() throws Exception {
    UIPublicationPanel panel = mock(UIPublicationPanel.class);

    Identity tom = createIdentity("tom","validator:/org/human-resources");
    Identity bill = createIdentity("bill","author:/CA/alerteInformatique","validator:/CA/informations");

    List<String> memberships = new ArrayList<String>();
    memberships.add("author:/CA/communicationDG");
    memberships.add("author:/CA/alerteSanitaire");
    memberships.add("author:/CA/alerteInformatique");
    memberships.add("author:/CA/informations");

    State state = new State();
    state.setMemberships(memberships);

    // make sure the actual code we test is not mocked!
    when(panel.isAuthorizedByMembership(any(State.class), any(Identity.class))).thenCallRealMethod();

    assertFalse("tom should not be allowed", panel.isAuthorizedByMembership(state, tom));
    assertTrue("bill should be allowed", panel.isAuthorizedByMembership(state, bill));
  }

  public void testIsAuthorizedByMembership() throws Exception {
    UIPublicationPanel panel = mock(UIPublicationPanel.class);

    Identity tom = createIdentity("tom","validator:/org/human-resources");
    Identity bill = createIdentity("bill","redactor:/org/human-resources","redactor:/org/finance");

    State state = new State();
    state.setMembership("redactor:/org/finance");

    // make sure the actual code we test is not mocked!
    when(panel.isAuthorizedByMembership(any(State.class), any(Identity.class))).thenCallRealMethod();

    assertFalse("tom should not be allowed", panel.isAuthorizedByMembership(state, tom));
    assertTrue("bill should be allowed", panel.isAuthorizedByMembership(state, bill));
  }


  public void testCheckAllowed() throws Exception {
    UIPublicationPanel panel = mock(UIPublicationPanel.class);

    // mock the identity registry by our users
    IdentityRegistry registry = new IdentityRegistry(null);
    registerUser(registry, "tom","validator:/org/human-resources");
    registerUser(registry, "bill","redactor:/org/human-resources","validator:/org/finances");
    when(panel.getApplicationComponent(IdentityRegistry.class)).thenReturn(registry);

    // configuring a mock node with the expected ACL
    List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>();
    entries.add(new AccessControlEntry("*:/org/finances", PermissionType.READ));
    entries.add(new AccessControlEntry("*:/org/human-resources", PermissionType.SET_PROPERTY));
    AccessControlList acl = new AccessControlList("foo", entries);
    NodeImpl node = mock(NodeImpl.class);
    when(node.getACL()).thenReturn(acl);

    State state = new State();
    state.setMembership("redactor:/org/finances");
    state.setRole("validator");

    // make sure the actual code we test is not mocked!
    when(panel.canReachState(any(State.class), anyString(), any(NodeImpl.class))).thenCallRealMethod();
    when(panel.isAuthorizedByMembership(any(State.class), any(Identity.class))).thenCallRealMethod();
    when(panel.isAuthorizedByRole(any(State.class), any(Identity.class), any(NodeImpl.class))).thenCallRealMethod();

    assertTrue("tom should be allowed", panel.canReachState(state, "tom", node)); // not
                                                                                  // allowed
                                                                                  // by
                                                                                  // membership,
                                                                                  // allowed
                                                                                  // by
                                                                                  // role
    assertFalse("bill should be allowed", panel.canReachState(state, "bill", node)); // not
                                                                                     // allowed
                                                                                     // by
                                                                                     // membership,
                                                                                     // not
                                                                                     // allowed
                                                                                     // by
                                                                                     // role

  }

  /**
   * Creates a new identity into an identity registry. Identity contains memberships.
   * @param registry the registry when to register the identity
   * @param userId for the identity to register
   * @param memberships list of memberships to assign to this user
   */
  private void registerUser(IdentityRegistry registry, String userId, String ... memberships) {
    Identity identity = createIdentity(userId, memberships);
    registry.register(identity);
  }


  private Identity createIdentity(String userId, String... memberships) {
    Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
    for (String membership : memberships) {
      membershipEntries.add(new MembershipEntry(membership.split(":")[1], membership.split(":")[0]));
    }
    Identity identity = new Identity(userId,membershipEntries);
    return identity;
  }

}
