package org.exoplatform.services.wcm.publication;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class TestWCMComposer extends TestCase {
  protected PortalContainer container;

  protected Session         session;

  SessionProvider           sessionProvider         = null;

  SessionProviderService    sessionProviderService_ = null;

  WCMComposer               wcmComposer             = null;

  String                    repository              = "repository";

  String                    workspace               = "collaboration";

	@Override
  public void setUp() throws Exception {
    super.setUp();
    container = PortalContainer.getInstance();
    sessionProviderService_ = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    session = repositoryService.getRepository(repository).getSystemSession(workspace);
    
    wcmComposer = (WCMComposer) container.getComponentInstanceOfType(WCMComposer.class);
  }

	/**
	 * test getContent for an authorized node
	 * @throws Exception
	 */
	public void testGetContentAuthorized() throws Exception {
	  
	  sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
		
		HashMap<String, String> filters = new HashMap<String, String>();

		String nodeIdentifier = "/sites content";
		Node node = wcmComposer.getContent(repository, workspace, nodeIdentifier, filters, sessionProvider);
		
		assertNotNull(node);
	}
	
	/**
	 * test getContent for an non authorized node
	 * @throws Exception
	 */
	public void testGetContentNotAuthorized() throws Exception {
    sessionProvider = sessionProviderService_.getSystemSessionProvider(null);	  
		
		HashMap<String, String> filters = new HashMap<String, String>();

		String nodeIdentifier = "/exo:application";
		Node node = wcmComposer.getContent(repository, workspace, nodeIdentifier, filters, sessionProvider);
		
		assertNull(node);
	}
}