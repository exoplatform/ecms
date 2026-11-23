package org.exoplatform.services.cms.documents;

import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;

import javax.jcr.Node;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 9/10/15
 * Unit test to check AutoVersionService methods
 */
public class TestAutoVersionService extends BaseWCMTestCase {

  private AutoVersionService autoVersionService;

  public void setUp() throws Exception {
    super.setUp();
    autoVersionService = container.getComponentInstanceOfType(AutoVersionService.class);
    applySystemSession();
  }

  public void tearDown() throws Exception {
    clear();
    session.save();
    session.logout();
    super.tearDown();
  }
  /**
   * @throws Exception
   */
  private void clear() throws Exception {
    Node rootNode = session.getRootNode();

    Node documentNode = rootNode.getNode("document");
    documentNode.remove();
    session.save();
  }

  public void testAutoVersion() throws Exception{
    Node document = session.getRootNode().addNode("document");
    Node documentA = document.addNode("documentA", NodetypeConstant.NT_FILE);
    Node jcrContent = documentA.addNode("jcr:content", "nt:resource");
    jcrContent.setProperty("jcr:lastModified", new GregorianCalendar());
    jcrContent.setProperty("jcr:data", "");
    jcrContent.setProperty("jcr:mimeType", "text/html");

    session.save();
    ManageDriveService manageDriveService = container.getComponentInstanceOfType(ManageDriveService.class);
    // The null answer below means "outside the drives" only while the drives exist
    for (String driveName : autoVersionService.getDriveAutoVersion()) {
      assertNotNull("The drive " + driveName + " must exist for the check below to mean anything",
                    manageDriveService.getDriveByName(driveName.trim()));
    }
    assertNull("A node outside the auto-versioned drives is not versioned", autoVersionService.autoVersion(documentA));
    autoVersionService.autoVersion(documentA, true);
    long versionNumber = documentA.getVersionHistory().getAllVersions().getSize();
    assertEquals("Number version is not correct", versionNumber, 1);
  }

  public void testGetDriveAutoVersion() throws Exception{
    Node document = session.getRootNode().addNode("document");
    List<String> lstSupport = autoVersionService.getDriveAutoVersion();
    assertEquals("Get list supported drives is not correct", 2, lstSupport.size());

    assertTrue("List supported not contain Groups drive", lstSupport.contains("Groups"));
    assertTrue("List supported not contain Personal drive", lstSupport.contains("Personal Documents"));
  }
}
