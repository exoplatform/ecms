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
package org.exoplatform.clouddrive;

import junit.framework.TestCase;

import org.exoplatform.clouddrive.CloudDrive.Command;
import org.exoplatform.clouddrive.exodrive.ExoDriveUser;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveException;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveRepository;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveService;
import org.exoplatform.clouddrive.exodrive.service.FileStore;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: TestCloudDriveService.java 00000 Sep 12, 2012 pnedonosko $
 */
public class TestCloudDriveService extends TestCase {

  protected static final Log     LOG               = ExoLogger.getLogger(TestCloudDriveService.class);

  public static final String     USER1_NAME        = "user1";

  public static final String     USER2_NAME        = "user2";

  public static final String     FILE_NAME_PATTERN = "test_file";

  private CloudDriveService      cdService;

  private PortalContainer        container;

  private RepositoryService      repositoryService;

  private SessionProviderService sessionProviders;

  private Repository             repository;

  private Session                session;

  private String                 testWorkspace;

  private String                 testPath;

  private Node                   testRoot;

  private CloudUser              cloudUser;

  private CloudProvider          provider;

  private ExoDriveRepository     exoDrives;

  private CloudDrive             drive;

  /**
   * setUp.
   * 
   * @throws java.lang.Exception
   */
  protected void setUp() throws Exception {
    super.setUp();

    // String containerConf =
    // TestCloudDriveService.class.getResource("/conf/portal/test-clouddrive-configuration.xml").toString();
    // StandaloneContainer.addConfigurationURL(containerConf);

    container = PortalContainer.getInstance();
    repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
    repositoryService.setCurrentRepositoryName(System.getProperty("gatein.jcr.repository.default"));

    sessionProviders = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);

    // login via Authenticator
    Authenticator authr = (Authenticator) container.getComponentInstanceOfType(Authenticator.class);
    String user = authr.validateUser(new Credential[] { new UsernameCredential("root"),
        new PasswordCredential("") });
    ConversationState.setCurrent(new ConversationState(authr.createIdentity(user)));

    // and set session provider to the service
    SessionProvider sessionProvider = new SessionProvider(ConversationState.getCurrent());
    sessionProvider.setCurrentRepository(repositoryService.getCurrentRepository());
    sessionProvider.setCurrentWorkspace("collaboration");
    sessionProviders.setSessionProvider(null, sessionProvider);

    session = sessionProviders.getSessionProvider(null).getSession(sessionProvider.getCurrentWorkspace(),
                                                                   sessionProvider.getCurrentRepository());

    testRoot = session.getRootNode().addNode("testCloudDriveService", "nt:folder");
    session.save();
    testWorkspace = session.getWorkspace().getName();
    testPath = testRoot.getPath();

    cdService = (CloudDriveService) container.getComponentInstanceOfType(CloudDriveService.class);

    provider = cdService.getProvider("exo"); // eXo Drive provider

    cloudUser = new ExoDriveUser("cloudTester", "tester@exoplatform.com", provider);

    ExoDriveService exoDriveServices = (ExoDriveService) container.getComponentInstanceOfType(ExoDriveService.class);
    exoDrives = exoDriveServices.open(repositoryService.getCurrentRepository().getConfiguration().getName());

    // create 1- files in the exo drive
    int filesCount = 10;

    String fileContentPattern = "test";
    exoDrives.createUser(cloudUser.getUsername());
    for (int i = 1; i <= filesCount; i++) {
      String fname = FILE_NAME_PATTERN + i + ".txt";
      FileStore fs = exoDrives.create(cloudUser.getUsername(), fname, "text/plain", Calendar.getInstance());
      InputStream stream = new ByteArrayInputStream((fileContentPattern + i).getBytes());
      fs.write(stream);
      stream.close();
    }
  }

  /**
   * tearDown.
   * 
   * @throws java.lang.Exception
   */
  protected void tearDown() throws Exception {

    try {
      if (drive != null) {
        drive.disconnect();
      }
      // cdService.disconnect(cloudUser);
    } catch (NotConnectedException e) {
      LOG.warn("tearDown() Drive wasn't connected:" + e.getMessage());
    }

    testRoot.remove();
    session.save();
    session.logout();

    for (FileStore fs : exoDrives.listFiles(cloudUser.getUsername())) {
      fs.remove();
    }

    super.tearDown();
  }

  protected void assertNodesExist(NodeIterator nodes, String... expected) throws RepositoryException {
    List<String> names = new ArrayList<String>(Arrays.asList(expected));
    while (nodes.hasNext()) {
      Node node = nodes.nextNode();
      String nname = node.getName();
      if (names.contains(nname)) {
        names.remove(nname);
      }
    }

    if (names.size() > 0) {
      fail("Expected nodes not exist: " + names);
    }
  }

//  protected void assertFilesExist(List<FileStore> files, String... expected) {
//    List<String> names = new ArrayList<String>(Arrays.asList(expected));
//    for (FileStore f : files) {
//      if (names.contains(f.getName())) {
//        names.remove(f.getName());
//      }
//    }
//
//    if (names.size() > 0) {
//      fail("Expected files not exist: " + names);
//    }
//  }

  protected void assertFilesExist(List<FileStore> files, String... expectedNames) {
    List<String> names = Arrays.asList(expectedNames);
    List<String> expected = new ArrayList<String>();
    for (FileStore f : files) {
      if (names.contains(f.getName())) {
        expected.add(f.getName());
      }
    }

    if (expected.size() != expectedNames.length) {
      fail("Expected files not exist: " + expectedNames);
    }
  }

  protected void connect(CloudDrive drive) throws CloudDriveException, RepositoryException {
    try {
      drive.connect().await();
    } catch (InterruptedException e) {
      LOG.warn("Caller of connect command interrupted.", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      Throwable err = e.getCause();
      if (err instanceof CloudDriveException) {
        throw (CloudDriveException) err;
      } else if (err instanceof RepositoryException) {
        throw (RepositoryException) err;
      } else if (err instanceof RuntimeException) {
        throw (RuntimeException) err;
      } else {
        throw new UndeclaredThrowableException(err, "Error connecting drive: " + err.getMessage());
      }
    }
  }
  
  /**
   * Test if drive connected well, has expected nodetypes and subnodes.
   * 
   * @throws RepositoryException
   */
  public void testConnect() throws RepositoryException {
    // call cloudDrives
    String driveName = provider.getName() + " - " + cloudUser.getEmail();
    Node driveNode = testRoot.addNode(driveName, "nt:folder");
    testRoot.save();
    try {
      drive = cdService.createDrive(cloudUser, driveNode);
      connect(drive);
    } catch (CloudDriveException e) {
      LOG.error("testConnect(): ", e);
      fail("Error: " + e);
    }

    // test what it did
    assertTrue("Drive node is not a ecd:cloudDrive", driveNode.isNodeType("ecd:cloudDrive"));
    assertTrue("Drive should have ecd:connected property with value true",
               driveNode.getProperty("ecd:connected").getBoolean());

    assertTrue("Drive node should have sub-files", driveNode.getNodes().getSize() > 0);

    Node file1 = driveNode.getNode(FILE_NAME_PATTERN + "1.txt");
    assertTrue("Drive file is not a nt:file", file1.isNodeType("nt:file"));
    assertTrue("Drive file is not a ecd:cloudFile", file1.isNodeType("ecd:cloudFile"));

    Node resource1 = file1.getNode("jcr:content");
    assertTrue("Drive file content is not a nt:resource", resource1.isNodeType("nt:resource"));
    assertTrue("Drive file content is not a ecd:cloudFileResource",
               resource1.isNodeType("ecd:cloudFileResource"));
  }

  /**
   * Test if drive disconnected well and doesn't have subnodes.
   * 
   * @throws RepositoryException
   */
  public void testDisconnect() throws RepositoryException {
    // connect
    String driveName = provider.getName() + " - " + cloudUser.getEmail();
    Node driveNode = testRoot.addNode(driveName, "nt:folder");
    testRoot.save();
    try {
      drive = cdService.createDrive(cloudUser, driveNode);
      connect(drive);
    } catch (CloudDriveException e) {
      LOG.error("testDisconnect(): ", e);
      fail("Error: " + e);
    }

    try {
      drive.disconnect();
      // cdService.disconnect(cloudUser);
    } catch (CloudDriveException e) {
      LOG.error("testDisconnect(): ", e);
      fail("Error: " + e);
    }

    // test what it did
    assertTrue(testRoot.hasNode(driveName));

    assertTrue("Drive node is not a ecd:cloudDrive", driveNode.isNodeType("ecd:cloudDrive"));
    assertFalse("Drive should have ecd:connected property with value false",
                driveNode.getProperty("ecd:connected").getBoolean());

    assertTrue("Drive node should not have sub-files", driveNode.getNodes().getSize() == 0);
  }

  public void testSynchronize() throws RepositoryException, ExecutionException, InterruptedException {
    try {
      String driveName = provider.getName() + " - " + cloudUser.getEmail();
      Node driveNode = testRoot.addNode(driveName, "nt:folder");
      testRoot.save();
      
      // connect
      drive = cdService.createDrive(cloudUser, driveNode);
      connect(drive);

      // create local node in JCR
      Node syncNode1 = driveNode.addNode("test_to_sync1", "nt:file");
      Node syncRes1 = syncNode1.addNode("jcr:content", "nt:resource");
      // nt:resource
      syncRes1.setProperty("jcr:mimeType", "text/plain");
      syncRes1.setProperty("jcr:data", "data to sync #1");
      syncRes1.setProperty("jcr:lastModified", Calendar.getInstance());

      Node syncNode2 = driveNode.addNode("test_to_sync2", "nt:file");
      Node syncRes2 = syncNode2.addNode("jcr:content", "nt:resource");
      // nt:resource
      syncRes2.setProperty("jcr:mimeType", "text/plain");
      syncRes2.setProperty("jcr:data", "data to sync #2");
      syncRes2.setProperty("jcr:lastModified", Calendar.getInstance());

      driveNode.save();

      // synchronize the whole drive, only cloud files should be on the drive
      drive.synchronize().await();

      // check if both files are in the drive storage
      assertFilesExist(exoDrives.listFiles(cloudUser.getUsername()), "test_to_sync1", "test_to_sync2");

      // check if nodes still exist in JCR
      assertNodesExist(driveNode.getNodes(), "test_to_sync1", "test_to_sync2");

    } catch (CloudDriveException e) {
      LOG.error("testSynchronize(): ", e);
      fail("Error: " + e);
    } catch (ExoDriveException e) {
      LOG.error("testSynchronize(): ", e);
      fail("Error: " + e);
    }
  }

  // FIXME not high priority 
  public void skip_testSynchronizeNode() throws RepositoryException, InterruptedException {
    try {
      String driveName = provider.getName() + " - " + cloudUser.getEmail();
      Node driveNode = testRoot.addNode(driveName, "nt:folder");
      testRoot.save();
      
      // connect
      drive = cdService.createDrive(cloudUser, driveNode);
      connect(drive);

      // create local node in JCR
      Node syncNode1 = driveNode.addNode("test_to_sync1", "nt:file");
      Node syncRes1 = syncNode1.addNode("jcr:content", "nt:resource");
      // nt:resource
      syncRes1.setProperty("jcr:mimeType", "text/plain");
      syncRes1.setProperty("jcr:data", "data to sync #1");
      syncRes1.setProperty("jcr:lastModified", Calendar.getInstance());

      Node syncNode2 = driveNode.addNode("test_to_sync2", "nt:file");
      Node syncRes2 = syncNode2.addNode("jcr:content", "nt:resource");
      // nt:resource
      syncRes2.setProperty("jcr:mimeType", "text/plain");
      syncRes2.setProperty("jcr:data", "data to sync #2");
      syncRes2.setProperty("jcr:lastModified", Calendar.getInstance());

      driveNode.save();

      // synchronize the whole drive
      Command syncCmd = drive.synchronize(); // TODO ?
      // respect async behaviour
      int i = 0;
      while (!syncCmd.isDone()) {
        assertTrue("Sync command not finished in time", i <= 50);
        Thread.sleep(200);
        i++;
      }

      // check if file well added to cloud drive
      assertFilesExist(exoDrives.listFiles(cloudUser.getUsername()), "test_to_sync2");

      // check if node are on the drive storage
      assertNodesExist(driveNode.getNodes(), "test_to_sync2");

    } catch (CloudDriveException e) {
      LOG.error("testSynchronizeNode(): ", e);
      fail("Error: " + e);
    } catch (ExoDriveException e) {
      LOG.error("testSynchronizeNode(): ", e);
      fail("Error: " + e);
    }
  }
}
