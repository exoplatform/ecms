/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.exoplatform.clouddrive.BaseCloudDriveTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestJCRRemoveObservation.java 00000 Sep 12, 2012 pnedonosko $
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-clouddrive-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-clouddrive-configuration.xml") })
public class TestJCRRemoveObservation extends BaseCloudDriveTest {

  protected final Log LOG = ExoLogger.getLogger(TestJCRRemoveObservation.class);

  private Session     testSession;

  private Node        testRoot;

  /**
   * setUp.
   * 
   * @throws java.lang.Exception
   */
  public void setUp() throws Exception {
    super.setUp();

    testRoot = session.getRootNode().addNode("testRemoveObservation", "nt:unstructured");
    testRoot.addNode("test1");
    session.save();

    // SessionProvider testSessionProvider = new SessionProvider(ConversationState.getCurrent());
    // testSessionProvider.setCurrentRepository(repositoryService.getCurrentRepository());
    // testSessionProvider.setCurrentWorkspace("collaboration");
    // testSession = testSessionProvider.getSession("collaboration",
    // repositoryService.getCurrentRepository());
    testSession = repositoryService.getRepository(REPO_NAME).getSystemSession(WORKSPACE_NAME);
  }

  /**
   * tearDown.
   * 
   * @throws java.lang.Exception
   */
  protected void tearDown() throws Exception {
    testRoot.remove();
    session.save();

    testSession.logout();

    super.tearDown();
  }

  /**
   * Test if JCR Observation respects self-removing listeners. To prototype real env using few dummy listeners
   * also.
   * 
   * @throws RepositoryException
   */
  public void testListenerSelfRemoval() throws Exception {

    // dummy listener class
    class DummyListener implements EventListener {
      @Override
      public void onEvent(EventIterator events) {
        // event fired... do nothing
      }
    }

    ObservationManager observationManager = session.getWorkspace().getObservationManager();

    // and register dummy listener
    observationManager.addEventListener(new DummyListener(), Event.NODE_ADDED, testRoot.getPath(), false, null, null, true);

    // register self-removing listener
    //final ConversationState currentConvo = ConversationState.getCurrent();
    //final ManageableRepository currentRepository = repositoryService.getCurrentRepository();
    EventListener selfRemoving = new EventListener() {
      @Override
      public void onEvent(EventIterator events) {
        // event fired...
        // remove itself
        try {
          LOG.info("Removing listener " + this);

          //SessionProvider listenerSP = new SessionProvider(currentConvo);
          //Session listenerSession = listenerSP.getSession("collaboration", currentRepository);
          Session listenerSession = repositoryService.getRepository(REPO_NAME).getSystemSession(WORKSPACE_NAME);
          
          ObservationManager om = listenerSession.getWorkspace().getObservationManager();
          om.removeEventListener(this);

          for (EventListenerIterator listeners = om.getRegisteredEventListeners(); listeners.hasNext();) {
            EventListener l = listeners.nextEventListener();
            LOG.info("Left EventListener: " + l);
          }

          listenerSession.logout();
        } catch (Throwable e) {
          LOG.error("Unexpected error on listener removal: " + e, e);
        }
      }
    };
    observationManager.addEventListener(selfRemoving, Event.NODE_REMOVED, testRoot.getPath(), false, null, null, true);

    // one more dummy listener
    observationManager.addEventListener(new DummyListener(), Event.NODE_REMOVED, testRoot.getPath(), false, null, null, true);

    // test it in another session (listeners above are noLocal=true)
    testSession.refresh(false);
    try {
      String path = testRoot.getPath() + "/test1";
      testSession.getItem(path).remove();
      LOG.info("Removed node: " + path);
      testSession.save();
      Thread.sleep(2000); // wait for async observation
    } catch (Throwable e) {
      fail("Unexpected error: " + e);
    }

    // Ensure node was removed even more than smoothly
    assertFalse("Node test1 should be removed", testRoot.hasNode("test1"));

    // Ensure listener is not more registered (even in registration session)
    LOG.info("Listener selfRemoving: " + selfRemoving);
    for (EventListenerIterator listeners = observationManager.getRegisteredEventListeners(); listeners.hasNext();) {
      EventListener l = listeners.nextEventListener();
      LOG.info("Still existing EventListener: " + l);
      assertNotSame("Listener " + selfRemoving + " should be already removed", selfRemoving, l);
    }

    Thread.sleep(5000);
  }
}
