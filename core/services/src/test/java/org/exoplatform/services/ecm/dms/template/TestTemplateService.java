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
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SARL
 * June 09, 2009
 */
public class TestTemplateService extends BaseWCMTestCase {

  private TemplateService templateService;
  private String expectedArticleDialogPath  = "/exo:ecm/templates/exo:article/dialogs/dialog1";
  private String expectedArticleViewPath  = "/exo:ecm/templates/exo:article/views/view1";
  private String expectedHTMLFileDialogPath = "/exo:ecm/templates/exo:htmlFile/dialogs/dialog1";
  private String expectedTemplateLabel      = "Article";
  private NodeHierarchyCreator nodeHierarchyCreator;
  private String cmsTemplatesBasePath;
  private Session sessionDMS;

  static private final String DMSSYSTEM_WS = "dms-system";
  static private final String EXO_ARTICLE  = "exo:article";
  
  static private final String DEMO_ID      = "demo";
  
  public void setUp() throws Exception {
    super.setUp();
    templateService = (TemplateService)container.getComponentInstanceOfType(TemplateService.class);
    nodeHierarchyCreator = (NodeHierarchyCreator)container.getComponentInstanceOfType(NodeHierarchyCreator.class);
    cmsTemplatesBasePath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_TEMPLATES_PATH);
    applySystemSession();   
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
    assertEquals(expectedArticleViewPath, templateService.getDefaultTemplatePath(false, EXO_ARTICLE));
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
    Node ddd = root.addNode("DDD", "nt:file");
    Node contentNode = ddd.addNode("jcr:content", "nt:resource");
    ddd.addMixin("exo:presentationable");
    ddd.setProperty("exo:presentationType", "exo:htmlFile");
    contentNode.setProperty("jcr:encoding", "UTF-8");
    contentNode.setProperty("jcr:mimeType", "text/html");    
    contentNode.setProperty("jcr:data", "Hello");
    contentNode.setProperty("jcr:lastModified", new GregorianCalendar());  
    bbb.setProperty("exo:title", "Hello");
    sessionDMS.save();

    assertEquals(expectedArticleDialogPath, templateService.getTemplatePath(bbb, true));
    assertEquals(expectedHTMLFileDialogPath, templateService.getTemplatePath(ddd, true));
    assertEquals(expectedArticleDialogPath, templateService.getTemplatePath(true, EXO_ARTICLE, "dialog1"));
    assertEquals(expectedArticleViewPath, templateService.getTemplatePath(false, EXO_ARTICLE, "view1"));
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
    assertEquals(expectedArticleViewPath, templateService.getTemplatePathByAnonymous(false, EXO_ARTICLE));
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
//    assertEquals(expectedArticleDialogPath, templateService.getTemplatePathByUser(true, EXO_ARTICLE, ROOT_ID));    
    assertEquals(expectedArticleDialogPath, templateService.getTemplatePathByUser(true, EXO_ARTICLE, IdentityConstants.ANONIM));
    assertEquals(expectedArticleViewPath, templateService.getTemplatePathByUser(false, EXO_ARTICLE, IdentityConstants.ANONIM));
    try {
      templateService.getTemplatePathByUser(true, EXO_ARTICLE, DEMO_ID);
      fail("Dummy user can not get the template in this case");
    }catch ( Exception e) {
      //Test okie.
    }
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
    assertEquals(expectedArticleDialogPath, templateService.getTemplatePathByUser(true, EXO_ARTICLE, IdentityConstants.ANONIM));
    assertEquals(expectedArticleViewPath, templateService.getTemplatePathByUser(false, EXO_ARTICLE, IdentityConstants.ANONIM));
//    assertEquals(null, templateService.getTemplate(null));
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
    assertEquals(1, templateService.getAllTemplatesOfNodeType(false, "exo:sample",
        sessionProviderService_.getSystemSessionProvider(null)).getSize());
    assertEquals(null, templateService.getAllTemplatesOfNodeType(false, "exo:cssFile",
        sessionProviderService_.getSystemSessionProvider(null)));
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
  public void testManagedNodeType() throws Exception {
    assertTrue(templateService.isManagedNodeType("exo:article"));
    templateService.removeManagedNodeType("exo:article");
    assertFalse(templateService.isManagedNodeType("exo:article"));
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
//    assertEquals(null, templateService.getTemplateRoles(null));
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
   * Test method: TemplateServiceImpl.getAllConfiguredNodeTypes()
   * Input      : N/A
   * Expect     : an array of configured node types 
   * @throws      Exception
   */
  public void testGetAllConfiguredNodeTypes() throws Exception {
    assertTrue(templateService.getAllConfiguredNodeTypes().size()>0);
  }
  /**
   * Test method: TemplateServiceImpl.buildDialogForm()
   * Input      : N/A
   * Expect     : a String as the form dialog built 
   * @throws      Exception
   */
  public void testBuildDialogForm() throws Exception {
    assertTrue(templateService.buildDialogForm(EXO_ARTICLE).length() >0);
  }
  /**
   * Test method: TemplateServiceImpl.buildDialogForm()
   * Input      : N/A
   * Expect     : a String as the form viewer built 
   * @throws      Exception
   */
  public void testBuildViewForm() throws Exception {
    assertTrue(templateService.buildViewForm(EXO_ARTICLE).length() >0);
  }
  /**
   * Test method: TemplateServiceImpl.buildDialogForm()
   * Input      : N/A
   * Expect     : a String as the stylesheet of nodetype 
   * @throws      Exception
   */
  public void testBuildStyleSheet() throws Exception {
    assertTrue(templateService.buildStyleSheet(EXO_ARTICLE).length() >0);
  }
  /**
   * Test method: TemplateServiceImpl.getSkinPath()
   * Input      : N/A
   * Expect     : a String as the stylesheet of nodetype 
   * @throws      Exception
   */
  public void testGetSkinPath() throws Exception{
      assertNotNull(templateService.getSkinPath(EXO_ARTICLE, "Stylesheet", "en"));
      assertNotNull(templateService.getSkinPath(EXO_ARTICLE, "Stylesheet", "ar"));
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
