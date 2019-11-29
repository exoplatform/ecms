package org.exoplatform.social.plugin.doc.selector;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.*;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.drives.DriveData;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-root-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/jcr/jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-portal-configuration.xml") })
public class BreadcrumbLocationTest extends BaseCommonsTestCase {

  @Override
  protected void beforeRunBare() {
    // This is used to make a workaround for embedded file path
    // see
    // org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver.DMSMimeTypeResolver()
    System.setProperty("mock.portal.dir", System.getProperty("gatein.test.output.path") + "/test-classes");
    super.beforeRunBare();
    ExoContainerContext.setCurrentContainer(getContainer());
    begin();
  }

  @Override
  protected void afterRunBare() {
    super.afterRunBare();
    end();
  }

  public void testAddLocation() throws Exception {
    BreadcrumbLocation breadcrumbLocation = new BreadcrumbLocation();
    try {
      breadcrumbLocation.addLocation(null);
      fail("should throw an exception using null parameter");
    } catch (Exception e) {
      // Expected
    }
    try {
      breadcrumbLocation.addLocation(2);
      fail("should throw an exception using an unknown parameter type");
    } catch (Exception e) {
      // Expected
    }
    assertTrue(breadcrumbLocation.isEmpty());
    assertFalse(breadcrumbLocation.isFolder());

    DriveData driveData = new DriveData();
    String driveName = ".platform.users";
    driveData.setName(driveName);
    breadcrumbLocation.addLocation(driveData);

    assertFalse(breadcrumbLocation.isEmpty());
    assertTrue(breadcrumbLocation.isFolder());
  }

  public void testCurrentFolderTitle() throws Exception {
    BreadcrumbLocation breadcrumbLocation = new BreadcrumbLocation();
    DriveData driveData = getDriveData();
    breadcrumbLocation.addLocation(driveData);
    assertEquals(driveData.getName(), breadcrumbLocation.getCurrentFolderTitle());

    breadcrumbLocation.addLocation("/Users");
    assertEquals("Users", breadcrumbLocation.getCurrentFolderTitle());
  }

  public void testGetBreadCrumbTitle() throws Exception {
    BreadcrumbLocation breadcrumbLocation = new BreadcrumbLocation();
    DriveData driveData = getDriveData();
    breadcrumbLocation.addLocation(driveData);
    assertEquals(driveData.getName(), breadcrumbLocation.getBreadCrumbTitle(driveData));
    assertNull(breadcrumbLocation.getBreadCrumbTitle("/Users"));

    breadcrumbLocation.addLocation("/Users");
    assertEquals("Users", breadcrumbLocation.getBreadCrumbTitle("/Users"));
  }

  public void testGetWorkSpace() throws Exception {
    BreadcrumbLocation breadcrumbLocation = new BreadcrumbLocation();
    assertNull(breadcrumbLocation.getWorkspace());

    DriveData driveData = getDriveData();
    breadcrumbLocation.addLocation(driveData);
    assertEquals(driveData.getWorkspace(), breadcrumbLocation.getWorkspace());
  }

  public void testGetCurrentFolderBreadcrumb() throws Exception {
    BreadcrumbLocation breadcrumbLocation = new BreadcrumbLocation();
    assertNull(breadcrumbLocation.getCurrentFolderBreadcrumb());

    DriveData driveData = getDriveData();
    breadcrumbLocation.addLocation(driveData);
    assertEquals(driveData.getName(), breadcrumbLocation.getCurrentFolderBreadcrumb());

    breadcrumbLocation.addLocation("/Users");
    assertEquals(driveData.getName() + " > Users", breadcrumbLocation.getCurrentFolderBreadcrumb());
  }

  private DriveData getDriveData() {
    DriveData driveData = new DriveData();
    driveData.setName("driveName");
    driveData.setHomePath("/");
    driveData.setWorkspace("portal-test");
    return driveData;
  }

}
