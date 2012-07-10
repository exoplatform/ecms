package org.exoplatform.services.wcm.publication;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.HashMap;

import javax.jcr.Node;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-publication-configuration.xml")
  })
public class TestWCMComposer extends BaseECMSTestCase {

  WCMComposer               wcmComposer             = null;
  
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    wcmComposer = (WCMComposer) container.getComponentInstanceOfType(WCMComposer.class);
  }

  @BeforeMethod
  public void setUp() throws Exception {
    applySystemSession();
  }

	/**
	 * test getContent for an authorized node
	 * @throws Exception
	 */
  @Test
	public void testGetContentAuthorized() throws Exception {
		
		HashMap<String, String> filters = new HashMap<String, String>();

		String nodeIdentifier = "/sites content";
		Node node = wcmComposer.getContent(COLLABORATION_WS, nodeIdentifier, filters, sessionProvider);
		
		assertNotNull(node);
	}
	
	/**
	 * test getContent for an non authorized node
	 * @throws Exception
	 */
  @Test
	public void testGetContentNotAuthorized() throws Exception {
		
		HashMap<String, String> filters = new HashMap<String, String>();

		String nodeIdentifier = "/exo:application";
		Node node = wcmComposer.getContent(COLLABORATION_WS, nodeIdentifier, filters, sessionProvider);
		
		assertNull(node);
	}
}