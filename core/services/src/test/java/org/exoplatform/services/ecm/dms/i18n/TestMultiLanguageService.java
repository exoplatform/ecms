/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 * along with this program; if not, see<http://www.gnu.org/liceenses/>.
 *
 **************************************************************************/
package org.exoplatform.services.ecm.dms.i18n;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.exceptions.SameAsDefaultLangException;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jun 17, 2009
 */
public class TestMultiLanguageService extends BaseWCMTestCase {

  private static final String I18NMixin = "mix:i18n";

  private static final String VOTEABLE = "mix:votable";

  private static final String ARTICLE = "exo:article";

  private static final String FILE = "nt:file";

  private static final String RESOURCE = "nt:resource";

  private static final String TITLE = "exo:title";

  private static final String SUMMARY = "exo:summary";

  private static final String TEXT = "exo:text";

  private static final String CONTENT = "jcr:content";

  private static final String MIMETYPE = "jcr:mimeType";

  private static final String LASTMODIFIED = "jcr:lastModified";

  private static final String DATA = "jcr:data";

  private static final String LINK = "exo:link";

  private static final String TEMPLATE = "exo:template";

  private static final String  NTFOLDER     = "nt:folder";

  private MultiLanguageService multiLanguageService;

  public void setUp() throws Exception {
    super.setUp();
    multiLanguageService = (MultiLanguageService) container.getComponentInstanceOfType(MultiLanguageService.class);
    applySystemSession();
  }

  /**
   * Test method MultiLanguagetService.getSupportedLanguages()
   * Input: Add mixin type mix:i18n, add vi language for node
   * Expect: Node has two language: vi and English
   * @throws Exception
   */
  public void testGetSupportedLanguages() throws Exception {
    Node test = session.getRootNode().addNode("test", ARTICLE);
    test.addMixin(I18NMixin);
    test.setProperty(TITLE, "sport");
    test.setProperty(SUMMARY, "report of season");
    test.setProperty(TEXT, "sport is exciting");
    session.save();
    multiLanguageService.addLanguage(test, createMapInput1(), "vi", false);
    List<String> lstLanguages = multiLanguageService.getSupportedLanguages(test);
    assertTrue(lstLanguages.contains("en"));
    assertTrue(lstLanguages.contains("vi"));

  }

  /**
   * Create data for Node
   */
  private Map<String, JcrInputProperty>  createMapInput1() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + TITLE;
    String summaryPath = CmsService.NODE + "/" + SUMMARY;
    String textPath = CmsService.NODE + "/" + TEXT;
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(CmsService.NODE);

    inputProperty.setValue("test");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is title");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(summaryPath);
    inputProperty.setValue("this is summary");
    map.put(summaryPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(textPath);
    inputProperty.setValue("this is article content");
    map.put(textPath, inputProperty);
    return map;
  }

  /**
   * Create data for Node
   */
  private Map<String, JcrInputProperty>  createMapInput2() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + TITLE;
    String summaryPath = CmsService.NODE + "/" + SUMMARY;
    String textPath = CmsService.NODE + "/" + TEXT;
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(CmsService.NODE);

    inputProperty.setValue("test");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("football");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(summaryPath);
    inputProperty.setValue("report of season");
    map.put(summaryPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(textPath);
    inputProperty.setValue(null);
    map.put(textPath, inputProperty);
    return map;
  }

  /**
   *  Create binary data for node
   * @throws IOException
   */
  private Map<String, JcrInputProperty> createFileInput() throws IOException {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String data = CmsService.NODE + "/" + CONTENT + "/" + DATA;
    String mimeType = CmsService.NODE + "/" + CONTENT + "/" + MIMETYPE;

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(data);
    inputProperty.setValue("test");
    map.put(data, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(mimeType);
    inputProperty.setValue("text/xml");
    map.put(mimeType, inputProperty);
    return map;
  }

  /**
   * Create podcast node
   * @return
   * @throws IOException
   */
  private Map<String, JcrInputProperty> createPodcastMapInput() throws IOException {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + TITLE;
    String linkPath = CmsService.NODE + "/" + LINK;
    String data = CmsService.NODE + "/" + CONTENT + "/" + DATA;
    String mimeType = CmsService.NODE + "/" + CONTENT + "/" + MIMETYPE;
    String lastModified = CmsService.NODE + "/" + CONTENT + "/" + LASTMODIFIED;

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is podcast");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(linkPath);
    inputProperty.setValue("connect");
    map.put(linkPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(data);
    inputProperty.setValue("test");
    map.put(data, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(mimeType);
    inputProperty.setValue("text/xml");
    map.put(mimeType, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(lastModified);
    inputProperty.setValue(new GregorianCalendar());
    map.put(lastModified, inputProperty);

    return map;
  }

  /**
   * Test method MultiLanguagetService.addLanguage(Node node, Map inputs, String language, boolean isDefault, String nodeType)
   * Input: add mix:i18n, exo:title = sport, exo:summary = report of season, exo:text: sport is exciting
   *        add language = fr with default = false for test node;
   * Expect: test node have these above property. test node has node language fr. Node language fr has properties:
   *         exo:title = this is title; exo:summary = this is summary; exo:text: this is article content;
   * @throws Exception
   */
  public void testAddLanguage1() throws Exception {
    Node test = session.getRootNode().addNode("test", ARTICLE);
    test.addMixin(I18NMixin);
    test.setProperty(TITLE, "sport");
    test.setProperty(SUMMARY, "report of season");
    test.setProperty(TEXT, "sport is exciting");
    session.save();

    multiLanguageService.addLanguage(test, createMapInput1(), "fr", false);
    assertTrue(test.hasNode("languages/fr"));
    Node testlanguage = test.getNode("languages/fr");
    assertEquals("this is title", testlanguage.getProperty(TITLE).getString());
    assertEquals("this is summary", testlanguage.getProperty(SUMMARY).getString());
    assertEquals("this is article content", testlanguage.getProperty(TEXT).getString());
    assertEquals("sport", test.getProperty(TITLE).getString());
    assertEquals("report of season", test.getProperty(SUMMARY).getString());
    assertEquals("sport is exciting", test.getProperty(TEXT).getString());
  }


  /**
   * Test method MultiLanguagetService.addLanguage(Node node, Map inputs, String language, boolean isDefault)
   * Input: add mix:i18n, exo:title = "sport", exo:summary = "supporter", exo:text: "sport is exciting"
   *        add language = fr with default = false for test node;
   * Expect: test node have these above property. test node has node language fr. Node language fr has properties:
   *         exo:title = "this is title"; exo:summary = "this is summary"; exo:text: "this is article content";
   *         if add language = fr with default = true then node test has language = fr, with properties created in method  createMapInput2
   * @throws Exception
   */
  public void testAddLanguage2() throws Exception {
    Node test = session.getRootNode().addNode("test", ARTICLE);
    test.addMixin(I18NMixin);
    test.setProperty(TITLE, "sport");
    test.setProperty(SUMMARY, "supporter");
    test.setProperty(TEXT, "sport is exciting");
    session.save();

    multiLanguageService.addLanguage(test, createMapInput1(), "fr", false);
    assertTrue(test.hasNode("languages/fr"));
    Node testlanguage = test.getNode("languages/fr");
    assertEquals("this is title", testlanguage.getProperty(TITLE).getString());
    assertEquals("this is summary", testlanguage.getProperty(SUMMARY).getString());
    assertEquals("this is article content", testlanguage.getProperty(TEXT).getString());
    assertEquals("sport", test.getProperty(TITLE).getString());
    assertEquals("supporter", test.getProperty(SUMMARY).getString());
    assertEquals("sport is exciting", test.getProperty(TEXT).getString());

    multiLanguageService.addLanguage(test, createMapInput2(), "fr", true);
    assertEquals("fr", test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString());
    assertFalse(test.hasNode("languages/fr"));
    assertEquals("football", test.getProperty(TITLE).getString());
    assertEquals("report of season", test.getProperty(SUMMARY).getString());
    assertEquals("", test.getProperty(TEXT).getString());
  }

  /**
   * Test method MultiLanguagetService.addLanguage(Node node, Map inputs, String language, boolean isDefault)
   * Input: Add language fr for node test with child node is a nt:file node type
   * Expect: if language fr is added as not default language then language node is add following relative path = languages/fr
   *         if language fr is added as default language then content of test node contains data defined in method createFileInput()
   * @throws Exception
   */
  public void testAddLanguage3() throws Exception {
    Node test = session.getRootNode().addNode("test", FILE);
    Node testFile = test.addNode(CONTENT, RESOURCE);
    testFile.setProperty(DATA, "test");
    testFile.setProperty(MIMETYPE, "text/xml");
    testFile.setProperty(LASTMODIFIED, new GregorianCalendar());
    test.addMixin(I18NMixin);
    session.save();
    multiLanguageService.addLanguage(test, createFileInput(), "fr", false, CONTENT);
    String defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("en", defaultLanguage);
    assertTrue(test.hasNode("languages/fr"));
    Node testlanguage = test.getNode("languages/fr");
    assertTrue(testlanguage.hasNode(CONTENT));
    assertEquals("test", testlanguage.getNode(CONTENT).getProperty(DATA).getString());

    multiLanguageService.addLanguage(test, createFileInput(), "vi", true, CONTENT);
    defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("vi", defaultLanguage);
    assertTrue(test.hasNode(CONTENT));
    assertEquals("test", test.getNode(CONTENT).getProperty(DATA).getString());

    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String data = CmsService.NODE + "/" + CONTENT + "/" + DATA;
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(data);
    inputProperty.setValue(null);
    map.put(data, inputProperty);

    multiLanguageService.addLanguage(test, map, "ar", false, CONTENT);
    testlanguage = test.getNode("languages/ar");
    assertEquals("", testlanguage.getNode(CONTENT).getProperty(DATA).getString());

    inputProperty.setJcrPath(data);
    inputProperty.setValue(new byte[1]);
    map.put(data, inputProperty);

    multiLanguageService.addLanguage(test, map, "br", false, CONTENT);
    testlanguage = test.getNode("languages/br");
    assertNotNull(testlanguage.getNode(CONTENT).getProperty(DATA).getStream());

    inputProperty.setJcrPath(data);
    String[] stringData = { "data" };
    inputProperty.setValue(stringData);
    map.put(data, inputProperty);

    multiLanguageService.addLanguage(test, map, "tn", false, CONTENT);
    testlanguage = test.getNode("languages/tn");
    assertNotNull(testlanguage.getNode(CONTENT).getProperty(DATA).getStream());
  }

  /**
   * Test method MultiLanguagetService.addLanguage(Node node, Map inputs, String language, boolean isDefault) in case inputs map contain a multiple value property
   * Input: Add language fr for node test with primary type is exo:templates
   * Expect: if language fr is added as not default language then language node is add following relative path = languages/fr
   * @throws Exception
   */
  public void testAddLanguage4() throws Exception {
    Node test = session.getRootNode().addNode("test", TEMPLATE);
    test.setProperty("exo:templateFile", "test");
    String[] roles = { "*" };
    test.setProperty("exo:roles", roles);
    test.addMixin(I18NMixin);
    session.save();

    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String rolePath = CmsService.NODE + "/" + "exo:roles";
    String templateFilePath = CmsService.NODE + "/" + "exo:templateFile";
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(rolePath);
    inputProperty.setValue(roles);
    map.put(rolePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(templateFilePath);
    inputProperty.setValue("test");
    map.put(templateFilePath, inputProperty);

    multiLanguageService.addLanguage(test, map, "fr", false);
    String defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("en", defaultLanguage);
    assertTrue(test.hasNode("languages/fr"));
    Node testlanguage = test.getNode("languages/fr");
    assertEquals("*", testlanguage.getProperty("exo:roles").getValues()[0].getString());
  }

  /**
   * Test method MultiLanguagetService.setDefault()
   * Input: Add language = fr as default language, then set fr as default language
   * Expect: language default of node test is language. Properties of test node are properties defined in creatMapInput1() method
   * @throws Exception
   */
  public void testSetDefault() throws Exception {
    Node test = session.getRootNode().addNode("test", ARTICLE);
    test.addMixin(I18NMixin);
    test.setProperty(TITLE, "sport");
    test.setProperty(TEXT, "sport is exciting");
    session.save();
    multiLanguageService.addLanguage(test, createMapInput1(), "fr", false);
    assertTrue(test.hasNode("languages/fr"));
    Node testlanguage = test.getNode("languages/fr");
    testlanguage.addNode(CONTENT);
    assertEquals("this is title", testlanguage.getProperty(TITLE).getString());
    assertEquals("this is summary", testlanguage.getProperty(SUMMARY).getString());
    assertEquals("this is article content", testlanguage.getProperty(TEXT).getString());
    multiLanguageService.setDefault(test, "fr", REPO_NAME);
    String defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("fr", defaultLanguage);
    assertFalse(test.hasNode("languages/fr"));
  }

  public void testSetDefault2() throws Exception {
    Node test = session.getRootNode().addNode("test", FILE);
    Node testFile = test.addNode(CONTENT, RESOURCE);
    testFile.setProperty(DATA, "test");
    testFile.setProperty(MIMETYPE, "text/xml");
    testFile.setProperty(LASTMODIFIED, new GregorianCalendar());
    test.addMixin(I18NMixin);
    test.addMixin(VOTEABLE);

    session.save();
    multiLanguageService.addLanguage(test, createFileInput(), "fr", false, CONTENT);
    assertTrue(test.hasNode("languages/fr"));
    multiLanguageService.setDefault(test, "fr", REPO_NAME);
    String defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("fr", defaultLanguage);
    assertFalse(test.hasNode("languages/fr"));
  }

  public void testSetDefault3() throws Exception {
    Node test = session.getRootNode().addNode("test", FILE);
    Node testFile = test.addNode(CONTENT, RESOURCE);
    testFile.setProperty(DATA, getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg"));
    testFile.setProperty(MIMETYPE, "image/jpg");
    testFile.setProperty(LASTMODIFIED, new GregorianCalendar());
    test.addMixin(I18NMixin);
    test.addMixin(VOTEABLE);

    session.save();
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String data = CmsService.NODE + "/" + CONTENT + "/" + DATA;
    String mimeType = CmsService.NODE + "/" + CONTENT + "/" + MIMETYPE;

    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(data);
    inputProperty.setValue(getClass().getResourceAsStream("/conf/dms/artifacts/images/ThumnailView.jpg"));
    map.put(data, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(mimeType);
    inputProperty.setValue("image/jpg");
    map.put(mimeType, inputProperty);

    multiLanguageService.addLanguage(test, map, "fr", false, CONTENT);
    assertTrue(test.hasNode("languages/fr"));
    multiLanguageService.setDefault(test, "fr", REPO_NAME);
    String defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("fr", defaultLanguage);
    assertFalse(test.hasNode("languages/fr"));
  }

  /**
   * Test method MultiLanguagetService.getDefault()
   * Input: add mix:i18n for node test, then add language fr for test node, then set language of node is fr
   * Expect: language default of node is  fr
   * @throws Exception
   */
  public void testGetDefault() throws Exception {
    Node test = session.getRootNode().addNode("test", ARTICLE);
    test.setProperty(TITLE, "Document");
    test.addMixin(I18NMixin);
    session.save();
    String defaultLanguage = multiLanguageService.getDefault(test);
    assertEquals("en", defaultLanguage);
    multiLanguageService.addLanguage(test, createMapInput1(), "fr", false);
    multiLanguageService.setDefault(test, "fr", REPO_NAME);
    assertEquals("fr", multiLanguageService.getDefault(test));
  }

  /**
   * Test method MultiLanguagetService.addFileLanguage(Node node, String fileName, Value value, String mimeType, String language, String repositoryName, boolean isDefault)
   * Input:  add child node nt:file ("/conf/standalone/system-configuration.xml") to node
   *        language fr for node with child node nt:file ("/conf/standalone/system-configuration.xml")
   *        language vi for node with child node nt:file ("/conf/standalone/system-configuration.xml") as default language
   * Expect: data of child node nt:file of test node is  ("/conf/standalone/system-configuration.xml")
   *         data of child node nt:file of node in path: /test/languages/English/test/jcr:content is
   *           ("/conf/standalone/system-configuration.xml")
   *
   * @throws Exception
   */
  public void testAddFileLanguage1() throws Exception {
    Node test = session.getRootNode().addNode("test", FILE);
    Node testFile = test.addNode(CONTENT, RESOURCE);
    testFile.setProperty(DATA, "test");
    testFile.setProperty(MIMETYPE, "text/xml");
    testFile.setProperty(LASTMODIFIED, new GregorianCalendar());
    test.addMixin(I18NMixin);
    test.addMixin(VOTEABLE);

    session.save();
    Value contentValue = session.getValueFactory().createValue("test");

    multiLanguageService.addFileLanguage(test, "system-configuration.xml" , contentValue, "text/xml", "fr", REPO_NAME, false);
    String defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("en", defaultLanguage);

    Value contentValue1 = session.getValueFactory().createValue("test");
    multiLanguageService.addFileLanguage(test, "test-configuration.xml" , contentValue1, "text/xml", "vi", REPO_NAME, true);
    defaultLanguage = test.getProperty(MultiLanguageService.EXO_LANGUAGE).getString();
    assertEquals("vi", defaultLanguage);
  }

  /**
   * Get language node by language MultiLanguageService.getLanguage()
   * Input language fr is not default language and default language
   * Expect: if fr is not default language then method return fr node, else return null
   * @throws Exception
   */
  public void testGetLanguage() throws Exception {
    Node test = session.getRootNode().addNode("test", FILE);
    Node testFile = test.addNode(CONTENT, RESOURCE);
    testFile.setProperty(MIMETYPE, "text/xml");
    testFile.setProperty(LASTMODIFIED, new GregorianCalendar());
    testFile.setProperty(DATA, "test");

    test.addMixin(I18NMixin);
    session.save();
    multiLanguageService.addFileLanguage(test, "fr" , createFileInput(), false);
    assertTrue(test.hasNode("languages/fr"));
    Node node = multiLanguageService.getLanguage(test, "fr");
    assertTrue(node.hasNode(CONTENT));
    assertEquals("test", node.getNode(CONTENT).getProperty(DATA).getString());

    multiLanguageService.addFileLanguage(test, "pt" , createFileInput(), false);
    assertTrue(test.hasNode("languages/pt"));
    node = multiLanguageService.getLanguage(test, "pt_br");
    assertTrue(node.hasNode(CONTENT));
    assertEquals("test", node.getNode(CONTENT).getProperty(DATA).getString());
  }
  /** Add synchronized linked language node to another node MultiLanguageService.addSynchronizedLinkedLanguage()
   * Input create a node then link a French translation node to it, then try to link a English(default language) translation node to it
   * Expect: linking successfully if the source node has different language from the default one of target node and vice versa
   * @throws Exception
   */
  public void testAddSynchronizedLinkedLanguage() throws Exception{
    Node test = session.getRootNode().addNode("article1", ARTICLE);
    test.addMixin(I18NMixin);
    test.setProperty(TITLE, "sport");
    test.setProperty(SUMMARY, "report of season");
    test.setProperty(TEXT, "sport is exciting");

    Node test2 = session.getRootNode().addNode("article2", ARTICLE);
    test2.addMixin(I18NMixin);
    test2.setProperty(TITLE, "sport");
    test2.setProperty(SUMMARY, "french version");
    test2.setProperty(TEXT, "le sport est passionnant");
    test2.setProperty(MultiLanguageService.EXO_LANGUAGE, "fr");
    session.save();
    multiLanguageService.addSynchronizedLinkedLanguage(test, test2);
    List<String> lstLanguages = multiLanguageService.getSupportedLanguages(test);
    assertTrue(lstLanguages.contains("en"));
    assertTrue(lstLanguages.contains("fr"));

    Node test3 = session.getRootNode().addNode("article3", ARTICLE);
    test3.addMixin(I18NMixin);
    test3.setProperty(TITLE, "sport");
    test3.setProperty(SUMMARY, "english version");
    test3.setProperty(TEXT, "sport is exciting");
    session.save();
    try {
      multiLanguageService.addSynchronizedLinkedLanguage(test, test3);
      fail();
    } catch (SameAsDefaultLangException ex) {
      assertTrue(true);
    }
  }

  /**
   * Test addLinkedLanguage the case that force to replace existing language symlink.
   *
   * @throws Exception
   */
  public void testAddLinkedLanguage() throws Exception {
    Node test = session.getRootNode().addNode("test");
    Node article1 = test.addNode("article1", ARTICLE);
    article1.addMixin(I18NMixin);
    article1.setProperty(TITLE, "sport");
    article1.setProperty(SUMMARY, "report of season");
    article1.setProperty(TEXT, "sport is exciting");

    Node article2 = test.addNode("article2", ARTICLE);
    article2.addMixin(I18NMixin);
    article2.setProperty(TITLE, "sport");
    article2.setProperty(SUMMARY, "french version");
    article2.setProperty(TEXT, "le sport est passionnant");
    article2.setProperty(MultiLanguageService.EXO_LANGUAGE, "fr");
    session.save();

    multiLanguageService.addLinkedLanguage(article1, article2);

    List<String> lstLanguages = multiLanguageService.getSupportedLanguages(article1);
    assertTrue(lstLanguages.contains("en"));
    assertTrue(lstLanguages.contains("fr"));
    Node frNode = multiLanguageService.getLanguage(article1, "fr");
    assertTrue(frNode.getName().equals("article2"));

    Node article3 = test.addNode("article3", ARTICLE);
    article3.addMixin(I18NMixin);
    article3.setProperty(TITLE, "sport");
    article3.setProperty(SUMMARY, "french version");
    article3.setProperty(TEXT, "le sport est passionnant");
    article3.setProperty(MultiLanguageService.EXO_LANGUAGE, "fr");
    session.save();
    multiLanguageService.addLinkedLanguage(article1, article3, true);

    lstLanguages = multiLanguageService.getSupportedLanguages(article1);
    assertTrue(lstLanguages.contains("en"));
    assertTrue(lstLanguages.contains("fr"));
    frNode = multiLanguageService.getLanguage(article1, "fr");
    assertTrue(frNode.getName().equals("article3"));
  }

  /**
   * Clean data test
   */
  public void tearDown() throws Exception {
    if (session.itemExists("/test")) {
      Node test = session.getRootNode().getNode("test");
      test.remove();
      session.save();
    }
    super.tearDown();
  }

}
