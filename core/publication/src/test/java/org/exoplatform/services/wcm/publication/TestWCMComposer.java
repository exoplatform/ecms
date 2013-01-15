package org.exoplatform.services.wcm.publication;

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
	

  public void tearDown() throws Exception {
    super.tearDown();
  }

}