package org.exoplatform.ecm.connector.dlp;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;

import org.exoplatform.BaseConnectorTestCase;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class TestFileDlpConnector extends BaseConnectorTestCase {

  private FileDlpConnector     fileDlpConnector;

  private ManageableRepository manageableRepository;

  private IndexingService      indexingService;

  @Test
  public void testProcessItem() throws Exception {

    // Given
    InitParams initParams = new InitParams();
    ValueParam dlpKeywordsParam = new ValueParam();
    dlpKeywordsParam.setName("dlp.keywords");
    dlpKeywordsParam.setValue("keyword1,keyword2");
    initParams.addParameter(dlpKeywordsParam);
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("enable", "true");
    constructorParams.setProperty("displayName", "file");
    constructorParams.setProperty("type", "file");
    initParams.addParameter(constructorParams);
    fileDlpConnector = new FileDlpConnector(initParams, repositoryService, indexingService);
    manageableRepository = repositoryService.getCurrentRepository();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(COLLABORATION_WS, manageableRepository);
    Node rootNode = session.getRootNode();
    rootNode.addNode(FileDlpConnector.DLP_SECURITY_FOLDER);
    Node testNode = rootNode.addNode("test");
    testNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    session.save();

    // When
    fileDlpConnector.processItem(testNode.getUUID());

    // Then
    assertEquals(testNode.getPath(), "/" + FileDlpConnector.DLP_SECURITY_FOLDER + "/test");
  }
}
