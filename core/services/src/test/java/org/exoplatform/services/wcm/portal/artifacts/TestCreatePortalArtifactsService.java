package org.exoplatform.services.wcm.portal.artifacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class TestCreatePortalArtifactsService extends BaseWCMTestCase {
	/** The CreatePortalArtifacts Service. */
  private CreatePortalArtifactsService createPortalArtifactsService;
  
  protected void afterContainerStart() {
    super.afterContainerStart(); 
    createPortalArtifactsService = getService(CreatePortalArtifactsService.class);
  }  
  
  public void setUp() throws Exception {
    applySystemSession();
  }
  
  @Test
  public void testAddPlugin() throws Exception {
  	CreatePortalPlugin portalPlugin = Mockito.mock(CreatePortalPlugin.class);
  	Mockito.when(portalPlugin.getName()).thenReturn("portalPlugin");
  	createPortalArtifactsService.addPlugin(portalPlugin);  	
  }
  
  @Test
  @SuppressWarnings("deprecation")
	public void testDeployArtifactsToPortal() throws Exception {
  	SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
  	createPortalArtifactsService.deployArtifactsToPortal(sessionProvider, "test1", "templateportal");
  } 
  
  @Test
  public void testDeployArtifactsToPortalWithTemplate() throws Exception {
  	SessionProvider sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
  	createPortalArtifactsService.deployArtifactsToPortal(sessionProvider, "portalplugin","templateportal");
  }
  
  @Test
  public void testAddIgnorePortalPlugin() throws Exception {
  	IgnorePortalPlugin ignorePortalPlugin = Mockito.mock(IgnorePortalPlugin.class);
  	CreatePortalArtifactsServiceImpl artifactService = (CreatePortalArtifactsServiceImpl)getService(CreatePortalArtifactsService.class);
  	String[] ignorePortals ={"acme","classic","wai"};
  	ArrayList<String> ignorePortalsList = new ArrayList<String>(); 
  	Collections.addAll(ignorePortalsList, ignorePortals);
  	Mockito.when(ignorePortalPlugin.getIgnorePortals()).thenReturn(ignorePortalsList);
  	artifactService.addIgnorePortalPlugin(ignorePortalPlugin);
  }
}
