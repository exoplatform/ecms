/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
*/
package org.exoplatform.services.cms.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.commons.api.settings.ExoFeatureService;
import org.exoplatform.services.cms.documents.security.DocumentSpaceGuard;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

/**
 * The legacy WebDAV never changes a document's access list as a property, and
 * applies its space's "Limit Access Management" setting to the ACL method and
 * to a version restore.
 */
public class WebDavServiceImplAccessListTest {

  private static final String REPO_PATH     = "collaboration/Groups/spaces/team/Documents/report.docx";

  private static final String DOCUMENT_PATH = "/Groups/spaces/team/Documents/report.docx";

  private WebDavServiceImpl   webDavService;

  private DocumentSpaceGuard  guard;

  private Node                document;

  private Node                storedDocument;

  @Before
  public void setUp() throws Exception {
    webDavService = mock(WebDavServiceImpl.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    ExoFeatureService featureService = mock(ExoFeatureService.class);
    when(featureService.isActiveFeature(anyString())).thenReturn(true);
    setField("exoFeatureService", featureService);
    NodeFinder nodeFinder = mock(NodeFinder.class);
    setField("nodeFinder", nodeFinder);
    RepositoryService repositoryService = mock(RepositoryService.class);
    setField("repositoryService", repositoryService);
    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = new RepositoryEntry();
    repositoryEntry.setName("repository");
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    Session systemSession = mock(ExtendedSession.class);
    when(repository.getSystemSession("collaboration")).thenReturn(systemSession);

    document = mock(Node.class);
    Session userSession = mock(Session.class);
    Workspace workspace = mock(Workspace.class);
    when(workspace.getName()).thenReturn("collaboration");
    when(userSession.getWorkspace()).thenReturn(workspace);
    when(document.getSession()).thenReturn(userSession);
    when(document.getPath()).thenReturn(DOCUMENT_PATH);
    when(nodeFinder.getItem(anyString(), anyString(), any(Boolean.class))).thenReturn(document);
    storedDocument = mock(Node.class);
    when(systemSession.getItem(DOCUMENT_PATH)).thenReturn(storedDocument);

    guard = mock(DocumentSpaceGuard.class);
    doReturn(guard).when(webDavService).getDocumentSpaceGuard();
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  public void testProppatchOfTheAccessListIsRefusedToEveryone() {
    when(guard.canManageAccess(any(), anyString(), any())).thenReturn(true);
    for (QName accessList : List.of(new QName("http://www.exoplatform.com/jcr/exo/1.0", "permissions", "exo"),
                                    new QName("http://www.exoplatform.com/jcr/exo/1.0", "permissions", "x"),
                                    new QName("exo:", "permissions", "exo"))) {
      HierarchicalProperty prop = new HierarchicalProperty(new QName("DAV:", "prop"));
      prop.addChild(new HierarchicalProperty(accessList));
      HierarchicalProperty set = new HierarchicalProperty(new QName("DAV:", "set"));
      set.addChild(prop);
      HierarchicalProperty body = new HierarchicalProperty(new QName("DAV:", "propertyupdate"));
      body.addChild(set);

      Response response = webDavService.proppatch("repository", REPO_PATH, null, null, null, body);

      assertEquals(accessList.toString(), HTTPStatus.FORBIDDEN, response.getStatus());
    }
  }

  @Test
  public void testProppatchOfAnotherPropertyIsApplied() {
    Response applied = Response.ok().build();
    doReturn(applied).when(webDavService).proppatchProperties(any(), any(), any(), any(), any(), any());
    HierarchicalProperty prop = new HierarchicalProperty(new QName("DAV:", "prop"));
    prop.addChild(new HierarchicalProperty(new QName("http://www.exoplatform.com/jcr/exo/1.0", "title", "exo")));
    HierarchicalProperty set = new HierarchicalProperty(new QName("DAV:", "set"));
    set.addChild(prop);
    HierarchicalProperty body = new HierarchicalProperty(new QName("DAV:", "propertyupdate"));
    body.addChild(set);

    assertSame(applied, webDavService.proppatch("repository", REPO_PATH, null, null, null, body));
  }

  @Test
  public void testAclIsAppliedForAMemberTheSpaceAllows() {
    when(guard.getAccessListChangeRefusal(document, "mary", "legacyWebDAV")).thenReturn(null);
    Response applied = Response.ok().build();
    doReturn(applied).when(webDavService).changeAccessList(any(), any(), any(), any(), any());

    assertSame(applied, webDavService.acl("repository", REPO_PATH, null, null, null));
  }

  @Test
  public void testAclIsRefusedToAMemberTheSpaceRefuses() {
    when(guard.getAccessListChangeRefusal(document, "mary", "legacyWebDAV")).thenReturn(DocumentSpaceGuard.ACCESS_MANAGEMENT_LIMITED);

    Response response = webDavService.acl("repository", REPO_PATH, null, null, null);

    assertEquals(HTTPStatus.FORBIDDEN, response.getStatus());
    assertEquals(DocumentSpaceGuard.ACCESS_MANAGEMENT_LIMITED, response.getEntity());
    verify(webDavService, never()).changeAccessList(any(), any(), any(), any(), any());
  }

  @Test
  public void testAclIsRefusedWhenTheSpaceKeepsItsDocumentsInside() {
    when(guard.getAccessListChangeRefusal(document, "mary", "legacyWebDAV")).thenReturn(DocumentSpaceGuard.KEPT_INSIDE_SHARE);

    Response response = webDavService.acl("repository", REPO_PATH, null, null, null);

    assertEquals(HTTPStatus.FORBIDDEN, response.getStatus());
    assertEquals(DocumentSpaceGuard.KEPT_INSIDE_SHARE, response.getEntity());
    verify(webDavService, never()).changeAccessList(any(), any(), any(), any(), any());
  }

  @Test
  public void testUncheckoutKeepsTheAccessListOfAMemberTheSpaceRefuses() throws Exception {
    when(guard.getAccessListChangeRefusal(document, "mary", null)).thenReturn(DocumentSpaceGuard.ACCESS_MANAGEMENT_LIMITED);
    Map<String, List<String>> accessList = Map.of("*:/spaces/team", List.of("read"));
    when(guard.getAccessList(storedDocument)).thenReturn(accessList);
    Response restored = Response.ok().build();
    doReturn(restored).when(webDavService).restoreCheckedOutVersion(any(), any(), any(), any());

    assertSame(restored, webDavService.uncheckout("repository", REPO_PATH, null, null));

    InOrder inOrder = inOrder(guard, webDavService, storedDocument);
    inOrder.verify(guard).getAccessList(storedDocument);
    inOrder.verify(webDavService).restoreCheckedOutVersion(any(), any(), any(), any());
    inOrder.verify(storedDocument).refresh(false);
    inOrder.verify(guard).restoreAccessList(storedDocument, accessList);
  }

  @Test
  public void testUncheckoutKeepsTheAccessListWhileTheSpaceKeepsItsDocumentsInside() throws Exception {
    when(guard.getAccessListChangeRefusal(document, "mary", null)).thenReturn(DocumentSpaceGuard.KEPT_INSIDE_SHARE);
    Map<String, List<String>> accessList = Map.of("*:/spaces/team", List.of("read"));
    when(guard.getAccessList(storedDocument)).thenReturn(accessList);
    Response restored = Response.ok().build();
    doReturn(restored).when(webDavService).restoreCheckedOutVersion(any(), any(), any(), any());

    assertSame(restored, webDavService.uncheckout("repository", REPO_PATH, null, null));

    InOrder inOrder = inOrder(guard, webDavService, storedDocument);
    inOrder.verify(guard).getAccessList(storedDocument);
    inOrder.verify(webDavService).restoreCheckedOutVersion(any(), any(), any(), any());
    inOrder.verify(guard).restoreAccessList(storedDocument, accessList);
  }

  @Test
  public void testUncheckoutWhoseAccessListCannotBePutBackIsAnError() throws Exception {
    when(guard.getAccessListChangeRefusal(document, "mary", null)).thenReturn(DocumentSpaceGuard.ACCESS_MANAGEMENT_LIMITED);
    doReturn(Response.ok().build()).when(webDavService).restoreCheckedOutVersion(any(), any(), any(), any());
    doThrow(new RepositoryException("refused")).when(guard).restoreAccessList(any(), any());

    assertEquals(HTTPStatus.INTERNAL_ERROR, webDavService.uncheckout("repository", REPO_PATH, null, null).getStatus());
  }

  @Test
  public void testUncheckoutLeavesTheAccessListToAMemberTheSpaceAllows() throws Exception {
    when(guard.getAccessListChangeRefusal(document, "mary", null)).thenReturn(null);
    Response restored = Response.ok().build();
    doReturn(restored).when(webDavService).restoreCheckedOutVersion(any(), any(), any(), any());

    assertSame(restored, webDavService.uncheckout("repository", REPO_PATH, null, null));

    verify(guard, never()).getAccessList(any());
    verify(guard, never()).restoreAccessList(any(), any());
  }

  private void setField(String name, Object value) throws Exception {
    Field field = WebDavServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(webDavService, value);
  }
}
