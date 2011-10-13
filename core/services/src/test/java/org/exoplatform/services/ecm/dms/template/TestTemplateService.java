/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.ecm.dms.template;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Created by The eXo Platform SARL
 * June 09, 2009
 */
public class TestTemplateService extends BaseDMSTestCase {

  private TemplateService templateService;
  private String expectedArticleDialogPath = "/exo:ecm/templates/exo:article/dialogs/dialog1";
  private String expectedTemplateLabel = "Article";
  private NodeHierarchyCreator nodeHierarchyCreator;
  private String cmsTemplatesBasePath;
  private Session sessionDMS;

  static private final String DMSSYSTEM_WS = "dms-system";
  static private final String EXO_ARTICLE = "exo:article";

  public void setUp() throws Exception {
    super.setUp();
    templateService = (TemplateService)container.getComponentInstanceOfType(TemplateService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    cmsTemplatesBasePath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_TEMPLATES_PATH);
    sessionDMS = sessionProviderService_.getSystemSessionProvider(null).getSession(DMSSYSTEM_WS, repository);
  }

  /**
   * Test TemplateServiceImpl.init()
   * Check all data initiated from repository in test-templates-configuration.xml file
   * @throws Exception
   */
  public void testInit() throws Exception {
    templateService.init();
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/nt:file"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:article"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:mail"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:podcast"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:addMetadataAction"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:sendMailAction"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:transformBinaryToTextAction"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:createRSSFeedAction"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:getMailAction"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:businessProcessModel"));
//    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:enableVersioning"));
//    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:autoVersioning"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:sample"));
//    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/rma:filePlan"));
//    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/kfx:document"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/mix:votable"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:comments"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/nt:resource"));
    assertTrue(sessionDMS.itemExists(cmsTemplatesBasePath + "/exo:taxonomyAction"));
  }

  /**
   * Test method: TemplateServiceImpl.getDefaultTemplatePath()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The name of NodeType
   * Expect: Return path of default template
   * @throws Exception
   */
  public void testGetDefaultTemplatePath() throws Exception {
    assertEquals(expectedArticleDialogPath, templateService.getDefaultTemplatePath(true, EXO_ARTICLE));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplatesHome()
   * Input: repository      String
   *                        The name of repository
   *        provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * Expect: Return node of default template
   * @throws Exception
   */
  public void testGetTemplatesHome() throws Exception {
    assertEquals("/exo:ecm/templates", templateService.getTemplatesHome(sessionProviderService_.getSystemSessionProvider(null)).getPath());
  }

  /**
   * Test method: TemplateServiceImpl.getTemplatePath()
   * Input: node            Node
   *                        The specified node
   *        isDialog        boolean
   *                        The boolean value which specify the type of template
   * Expect: Return path template of the specified node
   * @throws Exception
   */
  public void testGetTemplatePath() throws Exception {
    Node root = sessionDMS.getRootNode();
    Node aaa = root.addNode("AAA");
    Node bbb = root.addNode("BBB", "exo:article");
    bbb.setProperty("exo:title", "Hello");
    sessionDMS.save();

    assertEquals(expectedArticleDialogPath, templateService.getTemplatePath(bbb, true));
    assertEquals(expectedArticleDialogPath, templateService.getTemplatePath(true, EXO_ARTICLE, "dialog1"));
    try {
      templateService.getTemplatePath(aaa, true);
      fail("The content type: nt:unstructured doesn't be supported by any template");
    } catch (Exception ex) {
    }
  }

  /**
   * Test method: TemplateServiceImpl.getTemplatePathByAnonymous()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The specify name of node type
   *        repository      String
   *                        The name of repository
   * Expect: Return "/exo:ecm/templates/exo:article/dialogs/dialog1" is the path public template
   * @throws Exception
   */
  public void testGetTemplatePathByAnonymous() throws Exception {
    assertEquals(expectedArticleDialogPath, templateService.getTemplatePathByAnonymous(true, EXO_ARTICLE));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplatePathByAnonymous()
   * Input: nt:folder,nt:unstructured
   * Expect: Return set with 2 element nt:folder,nt:unstructured;
   * @throws Exception
   */
  public void testGetAllowanceFolderType() throws Exception {
    assertTrue(templateService.getAllowanceFolderType().contains("nt:unstructured"));
    assertTrue(templateService.getAllowanceFolderType().contains("nt:folder"));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplatePathByUser()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The specify name of node type
   *        userName        String
   *                        The current user
   *        repository      String
   *                        The name of repository
   * Expect: Return "/exo:ecm/templates/exo:article/dialogs/dialog1" is the template by user
   * @throws Exception
   */
  public void testGetTemplatePathByUser() throws Exception {
    //assertEquals(expectedArticleDialogPath, templateService.getTemplatePathByUser(true, EXO_ARTICLE, "root", REPO_NAME));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplate()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The specify name of node type
   *        templateName    String
   *                        The name of template
   *        repository      String
   *                        The name of repository
   * Expect: Return template file of the specified template
   * @throws Exception
   */
  public void testGetTemplate() throws Exception {
    assertNotNull(templateService.getTemplate(TemplateService.DIALOGS, EXO_ARTICLE, "dialog1"));
    assertNotNull(templateService.getTemplate(TemplateService.VIEWS, EXO_ARTICLE, "view1", REPO_NAME));
  }

  /**
   * Test method: TemplateServiceImpl.addTemplate()
   * Input: isDialog            boolean
   *                            The boolean value which specify the type of template
   *        nodeTypeName        String
   *                            The specify name of NodType
   *        label               String
   *                            The label of the specified template
   *        isDocumentTemplate  boolean
   *                            The boolean value which yes or no is DocumentTemplate
   *        templateName        String
   *                            The name of template
   *        roles               String[]
   *                            The roles of template
   *        templateFile        String
   *                            The file of template
   *        repository          String
   *                            The name of repository
   * Expect: Insert a new template
   * @throws Exception
   */
  public void testAddTemplate() throws Exception {
    String label = "AALabel";
    boolean isDocumentTemplate = true;
    String templateName = "AAName";
    String templateFile = "Hello";
    String[] roles = {"*"};
    assertNotNull(templateService.addTemplate(TemplateService.DIALOGS, EXO_ARTICLE, label, isDocumentTemplate,
        templateName, roles, new ByteArrayInputStream(templateFile.getBytes())));
    assertNotNull(templateService.getTemplate(TemplateService.DIALOGS, EXO_ARTICLE, templateName));
  }

  /**
   * Test method: TemplateServiceImpl.removeTemplate()
   * Input: isDialog          boolean
   *                          The boolean value which specify the type of template
   *        nodeTypeName      String
   *                          The specify name of NodType
   *        templateName      String
   *                          The name of template
   *        repository        String
   *                          The name of repository
   * Expect: Remove a template
   * @throws Exception
   */
  public void testRemoveTemplate() throws Exception {
    String label = "test template";
    boolean isDocumentTemplate = true;
    String templateName = "templateName";
    String templateFile = "Remove template Unit test";
    String[] roles = {"*"};
    assertNotNull(templateService.addTemplate(TemplateService.DIALOGS, EXO_ARTICLE, label, isDocumentTemplate,
        templateName, roles, new ByteArrayInputStream(templateFile.getBytes())));
    templateService.removeTemplate(TemplateService.DIALOGS, EXO_ARTICLE, templateName);
    try {
      templateService.getTemplate(TemplateService.DIALOGS, EXO_ARTICLE, templateName);
      fail();
    } catch (Exception ex) {
    }
  }

  /**
   * Test method: TemplateServiceImpl.isManagedNodeType()
   * Input: nodeTypeName    String
   *                        The name of NodeType
   *        repository      String
   *                        The name of repository
   * Expect: Return true is the given repository has nodeTypeName
   * @throws Exception
   */
  public void testIsManagedNodeType() throws Exception {
    assertTrue(templateService.isManagedNodeType(EXO_ARTICLE));
  }

  /**
   * Test method: TemplateServiceImpl.getDocumentTemplates()
   * Input: repository      String
   *                        The name of repository
   * Expect: all templates is document type of the specified repository
   * @throws Exception
   */
  public void testGetDocumentTemplates() throws Exception {
    List<String> listTemplates = templateService.getDocumentTemplates();
    assertTrue(listTemplates.contains("nt:file"));
    assertTrue(listTemplates.contains("exo:article"));
    assertTrue(listTemplates.contains("exo:podcast"));
    assertTrue(listTemplates.contains("exo:sample"));
//    assertTrue(listTemplates.contains("rma:filePlan"));
//    assertTrue(listTemplates.contains("kfx:document"));
  }

  /**
   * Test method: TemplateServiceImpl.getAllTemplatesOfNodeType()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The name of NodeType
   *        repository      String
   *                        The name of repository
   *        provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * Expect: Return all teamplate of the specified NodeType
   * @throws Exception
   */
  public void testGetAllTemplatesOfNodeType() throws Exception {
    assertEquals(1, templateService.getAllTemplatesOfNodeType(true, "exo:sample",
        sessionProviderService_.getSystemSessionProvider(null)).getSize());
  }

  /**
   * Test method: TemplateServiceImpl.removeManagedNodeType()
   * Input: nodeTypeName    String
   *                        The name of NodeType
   *        repository      String
   *                        The name of repository
   * Expect: Remove teamplate of the specified NodeType
   * @throws Exception
   */
  public void testRemoveManagedNodeType() throws Exception {
    assertTrue(templateService.isManagedNodeType("exo:podcast"));
    templateService.removeManagedNodeType("exo:podcast");
    assertFalse(templateService.isManagedNodeType("exo:podcast"));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplateLabel()
   * Input: nodeTypeName    String
   *                        The specified name of NodeType
   *        repository      String
   *                        The name of repository
   * Expect: Return "Article" the label of the specified template
   * @throws Exception
   */
  public void testGetTemplateLabel() throws Exception {
    assertEquals(expectedTemplateLabel, templateService.getTemplateLabel(EXO_ARTICLE));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplateRoles()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The name of NodeType
   *        templateName    String
   *                        The name of teamplate
   *        repository      String
   *                        The name of repository
   * Expect: Return "*" is roles of the specified template
   * @throws Exception
   */
  public void testGetTemplateRoles() throws Exception {
    Node templateNode = templateService.getTemplateNode(TemplateService.DIALOGS, EXO_ARTICLE, "dialog1", sessionProviderService_.getSystemSessionProvider(null));
    assertEquals("*", templateService.getTemplateRoles(templateNode));
  }

  /**
   * Test method: TemplateServiceImpl.getTemplateNode()
   * Input: isDialog        boolean
   *                        The boolean value which specify the type of template
   *        nodeTypeName    String
   *                        The name of NodeType
   *        templateName    String
   *                        The name of teamplate
   *        repository      String
   *                        The name of repository
   *        provider        SessionProvider
   *                        The SessionProvider object is used to managed Sessions
   * Expect: Return "/exo:ecm/templates/exo:article/dialogs/dialog1" is path template Node
   * @throws Exception
   */
  public void testGetTemplateNode() throws Exception {
    assertEquals(expectedArticleDialogPath, templateService.getTemplateNode(TemplateService.DIALOGS, EXO_ARTICLE, "dialog1",
        sessionProviderService_.getSystemSessionProvider(null)).getPath());
  }

  /**
   * Test method: TemplateServiceImpl.getCreationableContentTypes()
   * Input: node          The specified node
   * Expect: CreationableContent Types
   * @throws Exception
   */
  public void testGetCreationableContentTypes() throws Exception {
    Node root = sessionDMS.getRootNode();
    Node ddd = root.addNode("DDD", "exo:article");
    ddd.setProperty("exo:title", "Hello DDD");
    sessionDMS.save();

    List<String> listContentType = templateService.getCreationableContentTypes(ddd);
    assertTrue(listContentType.contains("nt:file"));
  }

  /**
   * Clean all templateTest node
   */
  public void tearDown() throws Exception {
    String[] paths = new String[] {"AAA", "BBB", "DDD", "EEE"};
    for (String path : paths) {
      if (sessionDMS.getRootNode().hasNode(path)) {
        sessionDMS.getRootNode().getNode(path).remove();
      }
    }
    sessionDMS.save();
    super.tearDown();
  }
}
