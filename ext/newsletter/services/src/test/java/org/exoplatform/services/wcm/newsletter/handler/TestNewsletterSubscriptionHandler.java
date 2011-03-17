package org.exoplatform.services.wcm.newsletter.handler;

import java.util.ArrayList;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class TestNewsletterSubscriptionHandler.
 */
public class TestNewsletterSubscriptionHandler extends BaseWCMTestCase {

  /** The session provider. */
  private SessionProvider sessionProvider;

  /** The newsletter subscription config. */
  private NewsletterSubscriptionConfig newsletterSubscriptionConfig;

  /** The newsletter subscription handler. */
  private NewsletterSubscriptionHandler newsletterSubscriptionHandler;

  /** The categories node. */
  private Node categoriesNode;

  /** The user home node. */
  private Node userHomeNode;

  private Node newsletterApplicationNode;

  /** The newsletter manager service. */
  NewsletterManagerService newsletterManagerService;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    newsletterApplicationNode = (Node) session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication");
    categoriesNode = newsletterApplicationNode.getNode("Categories");
    categoriesNode.setProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR, new String[]{this.userRoot});
    userHomeNode = newsletterApplicationNode.getNode("Users");
    session.save();

    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    NewsletterCategoryHandler newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    NewsletterCategoryConfig newsletterCategoryConfig = new NewsletterCategoryConfig();
    newsletterCategoryConfig.setName("CategoryNameNewsletterSubcription");
    newsletterCategoryConfig.setTitle("TitleCategory");
    newsletterCategoryConfig.setDescription("DescriptionCategory");
    newsletterCategoryConfig.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, "classic", newsletterCategoryConfig);

    newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    newsletterSubscriptionConfig  = new NewsletterSubscriptionConfig();
    newsletterSubscriptionConfig.setCategoryName("CategoryNameNewsletterSubcription");
    newsletterSubscriptionConfig.setName("NameNewsletterSubcription");
    newsletterSubscriptionConfig.setTitle("TitleNewsletterSubcription");
    newsletterSubscriptionConfig.setDescription("DescriptionNewsletterSubcription");
  }

  /**
   * Test add subscription.
   *
   * @throws Exception the exception
   */
  public void testAddSubscription() throws Exception {
    Node sub = null;
    try{
      newsletterSubscriptionHandler.add(sessionProvider, "classicPortal", newsletterSubscriptionConfig);
    }catch(Exception ex){}
    try{
      sub = categoriesNode.getNode("CategoryNameNewsletterSubcription").getNode("NameNewsletterSubcription");
    }catch(Exception ex){}
    assertNull(sub);

    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
    sub = categoriesNode.getNode("CategoryNameNewsletterSubcription").getNode("NameNewsletterSubcription");
    assertNotNull(sub);
    assertEquals(sub.getName(), newsletterSubscriptionConfig.getName());
    assertEquals(sub.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_CATEGORY_NAME).getString(), newsletterSubscriptionConfig.getCategoryName());
    assertEquals(sub.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_TITLE).getString(), newsletterSubscriptionConfig.getTitle());
    assertEquals(sub.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION).getString(), newsletterSubscriptionConfig.getDescription());
  }

  /**
   * Test edit subscription.
   *
   * @throws Exception the exception
   */
  public void testEditSubscription() throws Exception {
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);

    newsletterSubscriptionConfig.setTitle("Sub Title");
    newsletterSubscriptionConfig.setDescription("Sub Desc");
    newsletterSubscriptionConfig.setRedactor(newsletterSubscriptionConfig.getRedactor() + ",demo");
    newsletterSubscriptionHandler.edit(sessionProvider, classicPortal, newsletterSubscriptionConfig);

    Node sub = categoriesNode.getNode("CategoryNameNewsletterSubcription").getNode("NameNewsletterSubcription");
    assertEquals(sub.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_TITLE).getString(), newsletterSubscriptionConfig.getTitle());
    assertEquals(sub.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION).getString(), newsletterSubscriptionConfig.getDescription());

    // edit subscription with wrong portal's name
    newsletterSubscriptionConfig.setDescription("i have just edited");
    newsletterSubscriptionHandler.edit(sessionProvider, classicPortal + "wrong", newsletterSubscriptionConfig);
    sub = categoriesNode.getNode("CategoryNameNewsletterSubcription").getNode("NameNewsletterSubcription");
    assertNotSame(sub.getProperty(NewsletterConstant.SUBSCRIPTION_PROPERTY_DECRIPTION).getString(), newsletterSubscriptionConfig.getDescription());
  }

  /**
   * Test delete subscription.
   *
   * @throws Exception the exception
   */
  public void testDeleteSubscription() throws Exception {
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
    newsletterSubscriptionHandler.delete(sessionProvider, classicPortal, "CategoryNameNewsletterSubcription", newsletterSubscriptionConfig);
    assertEquals(1, categoriesNode.getNode("CategoryNameNewsletterSubcription").getNodes().getSize());

    // Delete subscription which is not alreadly exist in system
    try{
      newsletterSubscriptionHandler.delete(sessionProvider, classicPortal, "CategoryNameNewsletterSubcription_donotExist", newsletterSubscriptionConfig);
    }catch(Exception ex){}
    assertEquals(1, categoriesNode.getNode("CategoryNameNewsletterSubcription").getNodes().getSize());
  }

  /**
   * Test get subscriptions by category.
   *
   * @throws Exception the exception
   */
  public void testGetSubscriptionsByCategory() throws Exception {
    for(int i = 0 ; i < 5; i++) {
      newsletterSubscriptionConfig = new	NewsletterSubscriptionConfig();
      newsletterSubscriptionConfig.setCategoryName("CategoryNameNewsletterSubcription");
      newsletterSubscriptionConfig.setName("Sub_"+i);
      newsletterSubscriptionConfig.setTitle("Title_"+i);
      newsletterSubscriptionConfig.setDescription("Desc_"+i);
      newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
    }

    assertEquals(5, newsletterSubscriptionHandler.getSubscriptionsByCategory(sessionProvider, classicPortal, "CategoryNameNewsletterSubcription").size());
  }

  /**
   * Test get subscription ids by public user.
   *
   * @throws Exception the exception
   */
  public void testGetSubscriptionIdsByPublicUser() throws Exception {
    java.util.List<String> list = new ArrayList<String>();
    for(int i = 0 ; i < 5; i++) {
      newsletterSubscriptionConfig = new	NewsletterSubscriptionConfig();
      newsletterSubscriptionConfig.setCategoryName("CategoryNameNewsletterSubcription");
      newsletterSubscriptionConfig.setName("Sub_"+i);
      newsletterSubscriptionConfig.setTitle("Title_"+i);
      newsletterSubscriptionConfig.setDescription("Desc_"+i);
      newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
      list.add(newsletterSubscriptionConfig.getCategoryName() + "#" + newsletterSubscriptionConfig.getName());
    }

    NewsletterPublicUserHandler newsletterPublicUserHandler = newsletterManagerService.getPublicUserHandler();
    newsletterPublicUserHandler.subscribe(sessionProvider, classicPortal, "abc@local.com", list, "http://asdasd.com",
                        new String[]{"2df12 ads", "21df21asdf#2d1f#asdf", "adf asf"});
    assertEquals(5, newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, classicPortal, "abc@local.com").size());
  }

  /**
   * Test get subscriptions by name.
   *
   * @throws Exception the exception
   */
  public void testGetSubscriptionsByName() throws Exception {
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
    NewsletterSubscriptionConfig sub = newsletterSubscriptionHandler.getSubscriptionsByName(sessionProvider, classicPortal, "CategoryNameNewsletterSubcription", newsletterSubscriptionConfig.getName());
    assertEquals("NameNewsletterSubcription", sub.getName());

    sub = newsletterSubscriptionHandler.getSubscriptionsByName(sessionProvider, classicPortal, "CategoryNameNewsletterSubcription", newsletterSubscriptionConfig.getName() + "Wrong");
    assertEquals(null, sub);
  }

  /**
   * Test get number of newsletters waiting.
   *
   * @throws Exception the exception
   */
  public void testGetNumberOfNewslettersWaiting() throws Exception {

    NewsletterCategoryConfig newsletterCategoryConfig = new NewsletterCategoryConfig();
    NewsletterCategoryHandler newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    newsletterCategoryConfig.setName("CategoryName");
    newsletterCategoryConfig.setTitle("CategoryTitle");
    newsletterCategoryConfig.setDescription("CategoryDescription");
    newsletterCategoryConfig.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);

    Node categoryNode = categoriesNode.getNode("CategoryName");

    newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();
    newsletterSubscriptionConfig.setCategoryName("CategoryName");
    newsletterSubscriptionConfig.setName("SubscriptionName");
    newsletterSubscriptionConfig.setTitle("SubscriptionTitle");
    newsletterSubscriptionConfig.setDescription("SubscriptionDescription");
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);

    Node subscriptionNode = categoryNode.getNode("SubscriptionName");
    for(int i = 0; i < 5; i++) {
      Node nodeTemp = createWebcontentNode(subscriptionNode, "NewsletterEntry"+i, "test content of this node NewsletterEntry" + i, null, null);
      nodeTemp.addMixin(NodetypeConstant.EXO_NEWSLETTER_ENTRY);
      nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_DATE, Calendar.getInstance());
      nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_STATUS, NewsletterConstant.STATUS_AWAITING);
      nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_SUBSCRIPTION_NAME, "SubscriptionName");
      nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_CATEGORY_NAME, "CategoryName");
      nodeTemp.setProperty(NewsletterConstant.ENTRY_PROPERTY_TYPE, "TemplateName");
    }
    session.save();
    long numNewsletterWaiting = newsletterSubscriptionHandler.getNumberOfNewslettersWaiting(sessionProvider, classicPortal, "CategoryName", "SubscriptionName");
    assertEquals(5, numNewsletterWaiting);
  }

  public void testGetAllRedactors() throws Exception{
    NewsletterCategoryConfig newsletterCategoryConfig = new NewsletterCategoryConfig();
    NewsletterCategoryHandler newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    newsletterCategoryConfig.setName("CategoryName");
    newsletterCategoryConfig.setTitle("CategoryTitle");
    newsletterCategoryConfig.setDescription("CategoryDescription");
    newsletterCategoryConfig.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);

    categoriesNode.getNode("CategoryName");

    newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();
    newsletterSubscriptionConfig.setCategoryName("CategoryName");
    newsletterSubscriptionConfig.setName("SubscriptionName");
    newsletterSubscriptionConfig.setTitle("SubscriptionTitle");
    newsletterSubscriptionConfig.setDescription("SubscriptionDescription");
    newsletterSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
    assertEquals(this.userRoot, NewsletterConstant.getAllRedactor(this.classicPortal, session).get(0));
  }

  public void testGetSubscriptionsByRedactor()throws Exception{
    assertEquals(0, newsletterSubscriptionHandler.getSubscriptionByRedactor(this.classicPortal, newsletterSubscriptionConfig.getCategoryName(), this.userRoot, sessionProvider).size());
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    NodeIterator categories = categoriesNode.getNodes();
    while(categories.hasNext()) {
      categories.nextNode().remove();
    }

    NodeIterator users = userHomeNode.getNodes();
    while(users.hasNext()) {
      users.nextNode().remove();
    }
    session.save();
  }
}
