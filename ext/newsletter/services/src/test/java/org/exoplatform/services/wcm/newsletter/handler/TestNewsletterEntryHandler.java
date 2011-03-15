package org.exoplatform.services.wcm.newsletter.handler;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.config.NewsletterManagerConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class TestNewsletterEntryHandler.
 */
public class TestNewsletterEntryHandler extends BaseWCMTestCase {

  /** The session provider. */
  private SessionProvider sessionProvider;

  /** The newsletter application node. */
  private Node newsletterApplicationNode;

  /** The categories node. */
  private Node categoriesNode;

  /** The category node. */
  private Node categoryNode;

  /** The subscription node. */
  private Node subscriptionNode;

  /** The node temp. */
  private Node nodeTemp;

  /** The newsletter manager service. */
  private NewsletterManagerService newsletterManagerService;

  /** The newsletter category handler. */
  private NewsletterCategoryHandler newsletterCategoryHandler;

  /** The newsletter subscription handler. */
  private NewsletterSubscriptionHandler newsletterSubscriptionHandler;

  /** The newsletter entry handler. */
  private NewsletterEntryHandler newsletterEntryHandler;

  /** The newsletter category config. */
  private NewsletterCategoryConfig newsletterCategoryConfig;

  /** The newsletter subscription config. */
  private NewsletterSubscriptionConfig newsletterSubscriptionConfig;

  /** The is added. */
  private static boolean isAdded = false;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    System.out.println("\n\n\n\n-----------------------> TestNewsletterEntryHandler");
    newsletterApplicationNode = (Node) session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication");
    categoriesNode = newsletterApplicationNode.getNode("Categories");

    sessionProvider = WCMCoreUtils.getSystemSessionProvider();

    newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    newsletterEntryHandler = newsletterManagerService.getEntryHandler();

    if(!isAdded){
      newsletterCategoryConfig = new NewsletterCategoryConfig();
      newsletterCategoryConfig.setName("CategoryName");
      newsletterCategoryConfig.setTitle("CategoryTitle");
      newsletterCategoryConfig.setDescription("CategoryDescription");
      newsletterCategoryConfig.setModerator("root");
      newsletterCategoryHandler.add(sessionProvider, "classic", newsletterCategoryConfig);
      session.save();

      categoryNode = categoriesNode.getNode("CategoryName");

      newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();
      newsletterSubscriptionConfig.setCategoryName("CategoryName");
      newsletterSubscriptionConfig.setName("SubscriptionName");
      newsletterSubscriptionConfig.setTitle("SubscriptionTitle");
      newsletterSubscriptionConfig.setDescription("SubscriptionDescription");
      newsletterSubscriptionHandler.add(sessionProvider, "classic", newsletterSubscriptionConfig);

      subscriptionNode = categoryNode.getNode("SubscriptionName");

      for(int i = 0; i < 5; i++) {
        nodeTemp = createWebcontentNode(subscriptionNode, "NewsletterEntry"+i, "test content of this node NewsletterEntry" + i, null, null);
        nodeTemp.addMixin(NodetypeConstant.EXO_NEWSLETTER_ENTRY);
        nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_DATE, Calendar.getInstance());
        nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS, NewsletterConstant.STATUS_AWAITING);
        nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_SUBSCRIPTION_NAME, "SubscriptionName");
        nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_CATEGORY_NAME, "CategoryName");
        nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_TYPE, "TemplateName");
      }
      session.save();
    } else {
      newsletterCategoryConfig = newsletterCategoryHandler.getCategoryByName(sessionProvider, "classic", "CategoryName");
    }
    isAdded = true;
  }

  /**
   * Test delete newsletter entry.
   *
   * @throws Exception the exception
   */
  public void testDeleteNewsletterEntry() throws Exception {
    List<String> listIds = Arrays.asList(new String[]{"NewsletterEntry0", "NewsletterEntry1"});
    newsletterEntryHandler.delete(sessionProvider, "classic", "CategoryName", "SubscriptionName", listIds);
    long countEntry = subscriptionNode.getNodes().getSize();
    assertEquals(3, countEntry);
  }

  /**
   * Test get newsletter entries by subscription.
   *
   * @throws Exception the exception
   */
  public void testGetNewsletterEntriesBySubscription() throws Exception {
    List<NewsletterManagerConfig> listNewslleterEntries = newsletterEntryHandler
      .getNewsletterEntriesBySubscription(sessionProvider, "classic", "CategoryName", "SubscriptionName");
    assertEquals(3, listNewslleterEntries.size());
  }

  /**
   * Test get newsletter entry.
   *
   * @throws Exception the exception
   */
  public void testGetNewsletterEntry() throws Exception {
    NewsletterManagerConfig newsletterManagerConfig = newsletterEntryHandler
      .getNewsletterEntry(sessionProvider, "classic", "CategoryName", "SubscriptionName", "NewsletterEntry2");
    assertEquals("NewsletterEntry2", newsletterManagerConfig.getNewsletterName());
  }

  /**
   * Test get newsletter entry by path.
   *
   * @throws Exception the exception
   */
  public void testGetNewsletterEntryByPath() throws Exception {
    String path = "/sites content/live/classic/ApplicationData/NewsletterApplication/Categories/CategoryName/SubscriptionName/NewsletterEntry2";
    NewsletterManagerConfig newsletterManagerConfig = newsletterEntryHandler
      .getNewsletterEntryByPath(sessionProvider, path);
    assertEquals("NewsletterEntry2", newsletterManagerConfig.getNewsletterName());
  }

  /**
   * Test get content.
   *
   * @throws Exception the exception
   */
  public void testGetContent() throws Exception {
    String strContent = newsletterEntryHandler
      .getContent(sessionProvider, "classic", "CategoryName", "SubscriptionName", "NewsletterEntry2");
    assertTrue(strContent.indexOf("test content of this node NewsletterEntry2") > 0);
  }

  /**
   * Test get content entry.
   *
   * @throws Exception the exception
   */
  public void testGetContentEntry() throws Exception {
    if(nodeTemp == null)
      nodeTemp = ((Node) session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication"))
              .getNode("Categories").getNode("CategoryName").getNode("SubscriptionName").getNode("NewsletterEntry2");
    String strContent = newsletterEntryHandler.getContent(sessionProvider, nodeTemp);
    assertTrue(strContent.indexOf("test content of this node NewsletterEntry2") > 0);
  }

  /**
   * TearDow.
   */
  public void tearDown() {
    try {
      super.tearDown();
      NodeIterator nodeIterator = categoriesNode.getNodes();
      while(nodeIterator.hasNext()) {
        nodeIterator.nextNode().remove();
      }
    } catch (Exception e) {}
  }
}
