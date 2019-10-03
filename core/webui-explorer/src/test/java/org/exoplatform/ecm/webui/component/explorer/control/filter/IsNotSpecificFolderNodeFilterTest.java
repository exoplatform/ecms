package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

import javax.jcr.Node;
import java.util.HashMap;
import java.util.Map;

@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/mock-rest-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/ecms/configuration.xml")
})
public class IsNotSpecificFolderNodeFilterTest extends BaseECMSTestCase {

  private Node testFolder;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if(session.getRootNode().hasNode("testNotSpecificFolderNodeFilter")) {
      testFolder = session.getRootNode().getNode("testNotSpecificFolderNodeFilter");
      testFolder.remove();
      session.getRootNode().save();
    }
    testFolder = session.getRootNode().addNode("testNotSpecificFolderNodeFilter", "nt:unstructured");
  }

  public void testShouldReturnFalseWhenPersonalPublicFolder() throws Exception {
    // Given
    Node folderPublic = testFolder.addNode("Private", "nt:folder").addNode("Public", "nt:folder");
    session.getRootNode().save();
    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), folderPublic);
    IsNotSpecificFolderNodeFilter filter = new IsNotSpecificFolderNodeFilter();

    // When
    boolean accept = filter.accept(context);

    // Then
    assertFalse(accept);
  }

  public void testShouldReturnTrueWhenNotPersonalFolder() throws Exception {
    Node folderPublic = testFolder.addNode("Private", "nt:folder").addNode("Public", "nt:folder");
    Node folderVideo = folderPublic.addNode("Videos", "nt:folder");
    folderVideo.addMixin("exo:videoFolder");
    session.save();

    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), folderVideo);

    assertTrue(new IsNotSpecificFolderNodeFilter().accept(context));
  }

  public void testShouldReturnFalseWhenPersonalFolder() throws Exception {
    Node folderPrivate = testFolder.addNode("Private", "nt:folder");
    Node folderVideo = folderPrivate.addNode("Videos", "nt:folder");
    folderVideo.addMixin("exo:videoFolder");
    session.save();

    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), folderVideo);

    assertFalse(new IsNotSpecificFolderNodeFilter().accept(context));
  }

  public void testShouldReturnTrueWhenNotSpecificFolder() throws Exception {
    // Given
    Node folderTwo = testFolder.addNode("testFolderTwo", "nt:folder");
    session.getRootNode().save();
    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), folderTwo);
    IsNotSpecificFolderNodeFilter filter = new IsNotSpecificFolderNodeFilter();

    // When
    boolean accept = filter.accept(context);

    // Then
    assertTrue(accept);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
