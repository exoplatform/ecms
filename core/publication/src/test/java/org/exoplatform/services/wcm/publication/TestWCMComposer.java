package org.exoplatform.services.wcm.publication;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.wcm.core.NodeLocation;

import java.util.HashMap;

import javax.jcr.Node;

public class TestWCMComposer extends BasePublicationTestCase {

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

	/**
	 * test getPaginatedContents result size for an authorized node
	 * @throws Exception
	 */
	public void testGetPaginatedContents() throws Exception{
		HashMap<String, String> filters = new HashMap<String, String>();
		String folderPath = "repository:collaboration:/sites content/live/web contents/site artifacts";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
		//test on imported web:content
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);
		assertEquals(1,result.getNumTotal());
	}

	/**
	 * test getPaginatedContents result size when FILTER_TOTAL is set
	 * @throws Exception
	 */
	public void testPaginatedContentsWithFilter() throws Exception{
		HashMap<String, String> filters = new HashMap<String, String>();
		String folderPath = "repository:collaboration:/sites content/live";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
		//test if FILTER_TOTAL value is already set
		filters.put(WCMComposer.FILTER_TOTAL,"2");
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);
		assertEquals(2,result.getNumTotal());
	}

  public void tearDown() throws Exception {
    super.tearDown();
  }

}