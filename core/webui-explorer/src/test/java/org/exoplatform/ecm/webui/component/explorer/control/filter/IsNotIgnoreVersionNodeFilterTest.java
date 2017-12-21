package org.exoplatform.ecm.webui.component.explorer.control.filter;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

import javax.jcr.Node;
import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/mock-rest-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/ecms/configuration.xml")
})
public class IsNotIgnoreVersionNodeFilterTest extends BaseECMSTestCase {

  private Node testFolder;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if(session.getRootNode().hasNode("testFolderNotIgnoreVersionNodeFilter")) {
      testFolder = session.getRootNode().getNode("testFolderNotIgnoreVersionNodeFilter");
      testFolder.remove();
      session.getRootNode().save();
    }
    testFolder = session.getRootNode().addNode("testFolderNotIgnoreVersionNodeFilter", "nt:folder");
  }

  public void testShouldReturnTrueWhenNoParamAndNodeFile() throws Exception {
    // Given
    System.clearProperty(IsNotIgnoreVersionNodeFilter.NODETYPES_IGNOREVERSION_PARAM);

    Node file = testFolder.addNode("testFile", "nt:file");
    Node contentNode = file.addNode("jcr:content", "nt:resource");
    contentNode.setProperty("jcr:encoding", "UTF-8");
    contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
    contentNode.setProperty("jcr:mimeType", "application/excel");
    contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

    session.getRootNode().save();

    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), file);

    IsNotIgnoreVersionNodeFilter filter = new IsNotIgnoreVersionNodeFilter();

    // When
    boolean accept = filter.accept(context);

    // Then
    assertTrue(accept);
  }

  public void testShouldReturnFalseWhenNoParamAndNodeWebContent() throws Exception {
    // Given
    System.clearProperty(IsNotIgnoreVersionNodeFilter.NODETYPES_IGNOREVERSION_PARAM);

    Node webContent = testFolder.addNode("testWebContent", "exo:webContent");
    webContent.setProperty("exo:title", "Title");

    session.getRootNode().save();

    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), webContent);

    IsNotIgnoreVersionNodeFilter filter = new IsNotIgnoreVersionNodeFilter();

    // When
    boolean accept = filter.accept(context);

    // Then
    assertFalse(accept);
  }

  public void testShouldReturnFalseWhenNodeChildOfWebContent() throws Exception {
    // Given
    System.setProperty(IsNotIgnoreVersionNodeFilter.NODETYPES_IGNOREVERSION_PARAM, "exo:webContent,nt:file");

    Node webContent = testFolder.addNode("testWebContent", "exo:webContent");
    webContent.setProperty("exo:title", "Title");
    Node webContentChild = webContent.addNode("testChildWebContent", "nt:folder");

    session.getRootNode().save();

    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), webContentChild);

    IsNotIgnoreVersionNodeFilter filter = new IsNotIgnoreVersionNodeFilter();

    // When
    boolean accept = filter.accept(context);

    // Then
    assertFalse(accept);
  }

  /**
   * Test when the user can see the file but not its parent (case of a shared
   * document between spaces).
   * @throws Exception
   */
  public void testShouldReturnTrueWhenNoParamAndNodeChildOfWebContent() throws Exception {
    // Given
    System.clearProperty(IsNotIgnoreVersionNodeFilter.NODETYPES_IGNOREVERSION_PARAM);

    testFolder.addMixin("exo:privilegeable");
    ((NodeImpl)testFolder).setPermission("*:/platform/administrators", new String[] {"read", "add_node", "set_property", "remove"});
    Node file = testFolder.addNode("testFile", "nt:file");
    file.addMixin("exo:privilegeable");
    ((NodeImpl)file).setPermission("any", new String[] {"read"});
    Node contentNode = file.addNode("jcr:content", "nt:resource");
    contentNode.setProperty("jcr:encoding", "UTF-8");
    contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
    contentNode.setProperty("jcr:mimeType", "application/excel");
    contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

    session.getRootNode().save();

    applyUserSession("marry", "gtn", "collaboration");

    Node fileMarry = (Node) session.getItem("/testFolderNotIgnoreVersionNodeFilter/testFile");

    Map<String, Object> context = new HashMap<>();
    context.put(Node.class.getName(), fileMarry);

    IsNotIgnoreVersionNodeFilter filter = new IsNotIgnoreVersionNodeFilter();

    // When
    boolean accept = filter.accept(context);

    // Then
    assertTrue(accept);
  }

  @Override
  public void tearDown() throws Exception {
    System.clearProperty(IsNotIgnoreVersionNodeFilter.NODETYPES_IGNOREVERSION_PARAM);

    super.tearDown();
  }
}
