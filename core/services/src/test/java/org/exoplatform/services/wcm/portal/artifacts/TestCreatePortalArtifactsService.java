package org.exoplatform.services.wcm.portal.artifacts;

import java.util.ArrayList;
import java.util.Collections;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.mockito.Mockito;

public class TestCreatePortalArtifactsService extends BaseWCMTestCase {
	/** The CreatePortalArtifacts Service. */
  private CreatePortalArtifactsService createPortalArtifactsService;
  
  public void setUp() throws Exception {
    super.setUp();
    createPortalArtifactsService = getService(CreatePortalArtifactsService.class);
    applySystemSession();
  }
  
  public void testAddPlugin() throws Exception {
  	CreatePortalPlugin portalPlugin = Mockito.mock(CreatePortalPlugin.class);
  	Mockito.when(portalPlugin.getName()).thenReturn("portalPlugin");
  	createPortalArtifactsService.addPlugin(portalPlugin);  	
  }
  
  @SuppressWarnings("deprecation")
	public void testDeployArtifactsToPortal() throws Exception {
  	SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
  	createPortalArtifactsService.deployArtifactsToPortal(sessionProvider, "test1", "templateportal");
  } 
  
  public void testDeployArtifactsToPortalWithTemplate() throws Exception {
  	SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
  	createPortalArtifactsService.deployArtifactsToPortal(sessionProvider, "portalplugin","templateportal");
  }
  
  public void testAddIgnorePortalPlugin() throws Exception {
  	IgnorePortalPlugin ignorePortalPlugin = Mockito.mock(IgnorePortalPlugin.class);
  	CreatePortalArtifactsServiceImpl artifactService = (CreatePortalArtifactsServiceImpl)getService(CreatePortalArtifactsService.class);
  	String[] ignorePortals ={"acme","classic","wai"};
  	ArrayList<String> ignorePortalsList = new ArrayList<String>(); 
  	Collections.addAll(ignorePortalsList, ignorePortals);
  	Mockito.when(ignorePortalPlugin.getIgnorePortals()).thenReturn(ignorePortalsList);
  	artifactService.addIgnorePortalPlugin(ignorePortalPlugin);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
