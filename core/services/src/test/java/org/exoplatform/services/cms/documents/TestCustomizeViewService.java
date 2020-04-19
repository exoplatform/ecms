package org.exoplatform.services.cms.documents;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.CustomizeViewPlugin;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.impl.StartableCustomizeViewServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

@RunWith(MockitoJUnitRunner.class)
public class TestCustomizeViewService extends BaseWCMTestCase {
  
  private StartableCustomizeViewServiceImpl customizeViewService;
  
  @Mock
  private CustomizeViewPlugin plugin1;
  
  @Mock
  private CustomizeViewPlugin plugin2;
  
  @Before
  public void setUp() throws Exception {
    super.setUp();
    ManageViewService manageViewService = WCMCoreUtils.getService(ManageViewService.class);
    customizeViewService = new StartableCustomizeViewServiceImpl(manageViewService);
    applySystemSession();
  }
  @Test
  public void testStart() throws Exception {
    customizeViewService.addCustomizeViewPlugin(plugin1);
    customizeViewService.addCustomizeViewPlugin(plugin2); 
    customizeViewService.start();
    verify(plugin1).init();
    verify(plugin2).init();
  }

}
