package org.exoplatform.services.wcm.newsletter.handler;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class TestNewsletterTemplateHandler.
 */
public class TestNewsletterTemplateHandler extends BaseWCMTestCase {
  private static Log log = ExoLogger.getLogger(TestNewsletterTemplateHandler.class);

  /** The session provider. */
  private SessionProvider sessionProvider;

  /** The newsletter category handler. */
  private NewsletterCategoryHandler newsletterCategoryHandler;

  /** The newsletter category config. */
  private NewsletterCategoryConfig newsletterCategoryConfig;

  /** The newsletter subscription config. */
  private NewsletterSubscriptionConfig newsletterSubscriptionConfig;

  /** The newsletter subscription handler. */
  private NewsletterSubscriptionHandler newsletterSubscriptionHandler;

  /** The newsletter manager service. */
  private NewsletterManagerService newsletterManagerService;

  /** The newsletter template handler. */
  private NewsletterTemplateHandler newsletterTemplateHandler;

  /** The subscription node. */
  private Node subscriptionNode;

  /** The newsletter application node. */
  private Node newsletterApplicationNode ;

  private Node defaultTemplateNode;

  /** The categories node. */
  private Node categoriesNode;

  /** The nodes temp. */
  private Node nodesTemp;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    newsletterTemplateHandler = newsletterManagerService.getTemplateHandler();
    newsletterApplicationNode = (Node) session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication");
    categoriesNode = newsletterApplicationNode.getNode("Categories");
    defaultTemplateNode = newsletterApplicationNode.addNode("DefaultTemplates");
  }

  /**
   * Test get template.
   *
   * @throws Exception the exception
   */
  public void testGetTemplate() throws Exception {
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    newsletterCategoryConfig = new NewsletterCategoryConfig();
    newsletterCategoryConfig.setName("CategoryName");
    newsletterCategoryConfig.setTitle("CategoryTitle");
    newsletterCategoryConfig.setDescription("CategoryDescription");
    newsletterCategoryConfig.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);

    newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();
    newsletterSubscriptionConfig.setCategoryName("CategoryName");
    newsletterSubscriptionConfig.setName("SubscriptionName");
    newsletterSubscriptionConfig.setTitle("SubscriptionTitle");
    newsletterSubscriptionConfig.setDescription("SubScriptionDescription");
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);

    subscriptionNode = categoriesNode.getNode("CategoryName/SubscriptionName");
    nodesTemp 	= createWebcontentNode(subscriptionNode, "testTemplate", null, null, null);
    newsletterTemplateHandler.convertAsTemplate(sessionProvider, nodesTemp.getPath(), classicPortal, newsletterCategoryConfig.getName());
    session.save();
    // Get a template is exist in system
    Node nodeTmpl = newsletterTemplateHandler.getTemplate(sessionProvider, classicPortal, newsletterCategoryConfig, "testTemplate");
    assertNotNull(nodeTmpl);

    // get template with portal name is wrong
    nodeTmpl = newsletterTemplateHandler.getTemplate(sessionProvider, classicPortal + "Wrong", newsletterCategoryConfig, "testTemplate");
    assertNotNull(nodeTmpl);

    // get template which is not alreadly exist in system
    nodeTmpl = newsletterTemplateHandler.getTemplate(sessionProvider, classicPortal, newsletterCategoryConfig, "testTemplateNotExist");
    assertNull(nodeTmpl);

    // get template with name is null
    nodeTmpl = newsletterTemplateHandler.getTemplate(sessionProvider, classicPortal, newsletterCategoryConfig, null);
    assertNotNull(nodeTmpl);
  }

  /**
   * Test convert as template.
   *
   * @throws Exception the exception
   */
  public void testConvertAsTemplate() throws Exception {
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    NewsletterCategoryConfig newsletterCategoryConfig1 = new NewsletterCategoryConfig();
    newsletterCategoryConfig1.setName("CategoryName1");
    newsletterCategoryConfig1.setTitle("CategoryTitle1");
    newsletterCategoryConfig1.setDescription("CategoryDescription1");
    newsletterCategoryConfig1.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig1);

    NewsletterSubscriptionConfig newsletterSubscriptionConfig1 = new NewsletterSubscriptionConfig();
    newsletterSubscriptionConfig1.setCategoryName("CategoryName1");
    newsletterSubscriptionConfig1.setName("SubscriptionName1");
    newsletterSubscriptionConfig1.setTitle("SubscriptionTitle1");
    newsletterSubscriptionConfig1.setDescription("SubScriptionDescription1");
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig1);

    subscriptionNode = categoriesNode.getNode("CategoryName1/SubscriptionName1");
    for(int i = 0 ; i < 5; i++) {
      try{
        nodesTemp = subscriptionNode.getNode("testTemplate " + i);
      }catch(Exception ex){
        nodesTemp = createWebcontentNode(subscriptionNode, "testTemplate"+i, null, null, null);
      }
      newsletterTemplateHandler.convertAsTemplate(sessionProvider, nodesTemp.getPath(), classicPortal, newsletterCategoryConfig1.getName());
    }
    session.save();
    List<Node> listTemplates = newsletterTemplateHandler.getTemplates(sessionProvider, classicPortal, newsletterCategoryConfig1);
    assertEquals(5, listTemplates.size());

    try{
      if (log.isInfoEnabled()) {
        log.info("Convert a webcontent to template which is already exist in system");
      }
      newsletterTemplateHandler.convertAsTemplate(sessionProvider, nodesTemp.getPath(), classicPortal, newsletterCategoryConfig1.getName());
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't convert");
      }
    }
    listTemplates = newsletterTemplateHandler.getTemplates(sessionProvider, classicPortal, newsletterCategoryConfig1);
    assertEquals(5, listTemplates.size());
  }

  /**
   * Test get templates.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public void testGetTemplates() throws Exception {
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    NewsletterCategoryConfig newsletterCategoryConfig2 = new NewsletterCategoryConfig();
    newsletterCategoryConfig2.setName("CategoryName2");
    newsletterCategoryConfig2.setTitle("CategoryTitle2");
    newsletterCategoryConfig2.setDescription("CategoryDescription2");
    newsletterCategoryConfig2.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig2);

    NewsletterSubscriptionConfig newsletterSubscriptionConfig2 = new NewsletterSubscriptionConfig();
    newsletterSubscriptionConfig2.setCategoryName("CategoryName2");
    newsletterSubscriptionConfig2.setName("SubscriptionName2");
    newsletterSubscriptionConfig2.setTitle("SubscriptionTitle2");
    newsletterSubscriptionConfig2.setDescription("SubScriptionDescription2");
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig2);

    subscriptionNode = categoriesNode.getNode("CategoryName2/SubscriptionName2");
    for(int i = 0 ; i < 5; i++) {
      try{
        nodesTemp = subscriptionNode.getNode("testTemplate "+i);
      }catch(Exception ex){
        nodesTemp = createWebcontentNode(subscriptionNode, "testTemplate"+i, null, null, null);
      }
      newsletterTemplateHandler.convertAsTemplate(sessionProvider, nodesTemp.getPath(), classicPortal, newsletterCategoryConfig2.getName());
    }
    session.save();

    List listTemplates = newsletterTemplateHandler.getTemplates(sessionProvider, classicPortal, newsletterCategoryConfig2);
    assertEquals(5, listTemplates.size());

    // Get templates with wrong portal's name
    listTemplates = newsletterTemplateHandler.getTemplates(sessionProvider, classicPortal + "Wrong", newsletterCategoryConfig2);
    assertNull(listTemplates);
  }

  /**
   * Tear down
   */
  public void tearDown() throws Exception {
    super.tearDown();
    NodeIterator categories = newsletterApplicationNode.getNodes("categories");
    while(categories.hasNext()) {
      categories.nextNode().remove();
    }

    session.getItem(defaultTemplateNode.getPath()).remove();
    session.save();
  }
}
