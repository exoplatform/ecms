package org.exoplatform.services.wcm.publication;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecms.test.BaseECMSTestCase;


@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/wcm/test-publication-configuration.xml")
  })
public class TestWCMComposer extends BaseECMSTestCase {

  WCMComposer               wcmComposer             = null;
  
  public void setUp() throws Exception {
    super.setUp();
    wcmComposer = (WCMComposer) container.getComponentInstanceOfType(WCMComposer.class);
    applySystemSession();
  }

	/**
	 * test getContent for an authorized node
	 * @throws Exception
	 */
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
	public void testGetContentNotAuthorized() throws Exception {
		
		HashMap<String, String> filters = new HashMap<String, String>();

		String nodeIdentifier = "/exo:application";
		Node node = wcmComposer.getContent(COLLABORATION_WS, nodeIdentifier, filters, sessionProvider);
		
		assertNull(node);
	}
	

  public void tearDown() throws Exception {
    super.tearDown();
  }

}