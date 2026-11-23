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
 */
package org.exoplatform.services.ecm.dms.scripts;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

import groovy.lang.GroovyClassLoader;

/**
 * Runs the trash folder action's script as the platform ships it, from the
 * ecm-wcm-extension sources, on the node an addition under the trash home
 * names.
 */
public class TrashFolderScriptTest {

  private static final String SCRIPT_PATH = "ecm-wcm-extension/src/main/webapp/WEB-INF/conf/dms-extension/dms/artifacts/scripts/"
      + "ecm-explorer/action/TrashFolder.groovy";

  private static final String WORKSPACE   = "collaboration";

  private static final String NODE_PATH   = "/Trash/Users";

  private CmsScript           script;

  private Session             session;

  private Node                node;

  @Before
  public void setUp() throws Exception {
    RepositoryService repositoryService = mock(RepositoryService.class);
    SessionProviderService sessionProviderService = mock(SessionProviderService.class);
    ManageableRepository repository = mock(ManageableRepository.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    session = mock(Session.class);
    node = mock(Node.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(sessionProviderService.getSystemSessionProvider(null)).thenReturn(sessionProvider);
    when(sessionProvider.getSession(WORKSPACE, repository)).thenReturn(session);
    when(session.getItem(NODE_PATH)).thenReturn(node);

    // core/services runs its tests from its own directory, two levels below
    // the repository's root
    File moduleDirectory = new File(System.getProperty("basedir", System.getProperty("user.dir"))).getAbsoluteFile();
    File scriptFile = new File(moduleDirectory.getParentFile().getParentFile(), SCRIPT_PATH);
    try (GroovyClassLoader groovyClassLoader = new GroovyClassLoader(getClass().getClassLoader())) {
      script = (CmsScript) groovyClassLoader.parseClass(scriptFile)
                                            .getConstructor(RepositoryService.class, SessionProviderService.class)
                                            .newInstance(repositoryService, sessionProviderService);
    }
  }

  @Test
  public void testANodeAddedUnderTheTrashHomeGetsItsRestoreLocation() throws Exception {
    script.execute(nodeAddedContext());

    verify(node).addMixin("exo:restoreLocation");
    verify(node).setProperty("exo:restorePath", NODE_PATH);
    verify(node).setProperty("exo:restoreWorkspace", WORKSPACE);
    verify(session).save();
  }

  @Test
  public void testAFolderOfTheRecycleBinGetsNoRestoreLocation() throws Exception {
    when(node.isNodeType("exo:recycleBinFolder")).thenReturn(true);

    script.execute(nodeAddedContext());

    verify(node, never()).addMixin(anyString());
    verify(node, never()).setProperty(anyString(), nullable(String.class));
    verify(session, never()).save();
  }

  private Map<String, Object> nodeAddedContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("nodePath", WORKSPACE + ":" + NODE_PATH);
    context.put("srcWorkspace", WORKSPACE);
    context.put("srcPath", NODE_PATH);
    context.put("eventType", Event.NODE_ADDED);
    return context;
  }

}
