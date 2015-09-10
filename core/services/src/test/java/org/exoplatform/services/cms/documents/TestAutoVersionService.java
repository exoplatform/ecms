package org.exoplatform.services.cms.documents;

import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import java.util.ArrayList;
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
    autoVersionService.autoVersion(documentA);
    long versionNumber = documentA.getVersionHistory().getAllVersions().getSize();
    assertEquals("Number version is not correct", versionNumber, 1);
  }

  public void testGetDriveAutoVersion() throws Exception{
    Node document = session.getRootNode().addNode("document");
    List<String> lstSupport = autoVersionService.getDriveAutoVersion();
    assertEquals("Get list supported drives is not correct", lstSupport.size(), 3);

    ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);

    List<String> lstHomePath = new ArrayList<>();
    for (String drive: lstSupport){
        lstHomePath.add(manageDriveService.getDriveByName(drive).getHomePath());
    }
    assertTrue("List supported not contant Persional drive", lstHomePath.contains(AutoVersionService.PERSONAL_DRIVE_PREFIX));
    assertTrue("List supported not contant Groups drive", lstHomePath.contains(AutoVersionService.GROUP_DRIVE_PREFIX));
  }
}
