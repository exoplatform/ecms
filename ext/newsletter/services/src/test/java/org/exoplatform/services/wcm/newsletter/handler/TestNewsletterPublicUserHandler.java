package org.exoplatform.services.wcm.newsletter.handler;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class TestNewsletterPublicUserHandler.
 */
public class TestNewsletterPublicUserHandler extends BaseWCMTestCase {

  /** The session provider. */
  private SessionProvider sessionProvider;

  /** The categories node. */
  @SuppressWarnings("unused")
  private Node categoriesNode;

  /** The user home node. */
  @SuppressWarnings("unused")
  private Node userHomeNode;

  /** The newsletter manager service. */
  private NewsletterManagerService newsletterManagerService;

  /** The newsletter category config. */
  private NewsletterCategoryConfig newsletterCategoryConfig;

  /** The newsletter category handler. */
  private NewsletterCategoryHandler newsletterCategoryHandler;

  /** The newsletter subscription config. */
  private NewsletterSubscriptionConfig newsletterSubscriptionConfig;

  /** The newsletter subscription handler. */
  private NewsletterSubscriptionHandler newsletterSubscriptionHandler;

  /** The newsletter public user handler. */
  private NewsletterPublicUserHandler newsletterPublicUserHandler;

  /** The list subs. */
  private List<String> listSubs;

  /** The user email. */
  private String userEmail = "test@local.com";

  private String userEmail2 = "test2@local.com";

  private static Log log = ExoLogger.getLogger(TestNewsletterPublicUserHandler.class);

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    Node newsletterApplicationNode = session.getRootNode().getNode("sites content/live").addNode("classic").addNode("ApplicationData")
                                                    .addNode("NewsletterApplication");
    categoriesNode = newsletterApplicationNode.addNode("Categories");
    userHomeNode  = newsletterApplicationNode.addNode("Users");
    session.save();

    newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    newsletterPublicUserHandler = newsletterManagerService.getPublicUserHandler();

    sessionProvider = WCMCoreUtils.getSystemSessionProvider();

    newsletterCategoryConfig = new NewsletterCategoryConfig();
    newsletterCategoryConfig.setName("CatNameNewsletters");
    newsletterCategoryConfig.setTitle("TitleNewsletters");
    newsletterCategoryConfig.setDescription("DescriptionNewsletter");
    newsletterCategoryConfig.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, "classic", newsletterCategoryConfig);

    listSubs = new ArrayList<String>();
    for(int i = 0; i < 5; i++) {
      newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();
      newsletterSubscriptionConfig.setCategoryName("CatNameNewsletters");
      newsletterSubscriptionConfig.setName("SubNameSubscriptions_"+i);;
      newsletterSubscriptionConfig.setTitle("TitleNewletter_"+i);
      newsletterSubscriptionConfig.setDescription("DescriptionNewsletter_"+i);
      newsletterSubscriptionHandler.add(sessionProvider, "classic", newsletterSubscriptionConfig);
      listSubs.add(newsletterSubscriptionConfig.getCategoryName()+"#"+newsletterSubscriptionConfig.getName());
    }
  }

  /**
   * Test subscribe.
   *
   * @throws Exception the exception
   */
  public void testSubscribe() throws Exception {
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail, listSubs, "http://test.com", new String[]{"test", "adjasd", "asdasd"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't send mail ");
      }
    }
    List<NewsletterSubscriptionConfig> listSubscriptions = newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, "classic", userEmail);
    assertEquals(5, listSubscriptions.size());

    // Subscribe with wrong protal's name
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, "classicWrong", userEmail, listSubs, "http://test.com", new String[]{"test", "adjasd", "asdasd"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Portal's name is wrong");
      }
    }
    listSubscriptions = newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, "classic", userEmail);
    assertEquals(5, listSubscriptions.size());
  }

  public void testSubscribeEmails() throws Exception {
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail,
                                            listSubs, "http://test.com",
                                            new String[]{"test", "adjasd", "asdasd"});
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail2,
                                            listSubs, "http://test2.com",
                                            new String[]{"test", "adjasd", "asdasd"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't send mail ");
      }
    }
    List<NewsletterSubscriptionConfig> listSubscriptions = newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, "classic", userEmail);
    assertEquals(5, listSubscriptions.size());
  }

  /**
   * Test update subscriptions.
   *
   * @throws Exception the exception
   */
  public void testUpdateSubscriptions() throws Exception {
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, classicPortal, userEmail, listSubs, "http://test.com",
                          new String[]{"as","dsd", "asdasd"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't send mail");
      }
    }
    List<NewsletterSubscriptionConfig> listSubscriptions = newsletterSubscriptionHandler.
                                  getSubscriptionIdsByPublicUser(sessionProvider, classicPortal, userEmail);
    assertEquals(5, listSubscriptions.size());

    List<String> listCategoryAndSubs = new ArrayList<String>();
    for(int j = 0; j < 3; j++) {
      listCategoryAndSubs.add(listSubs.get(j));
    }
    newsletterPublicUserHandler.updateSubscriptions(sessionProvider, this.classicPortal, userEmail, listCategoryAndSubs);
    listSubscriptions = newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, classicPortal, userEmail);
    assertEquals(3, listSubscriptions.size());
  }

  /**
   * Test clear email in subscription.
   *
   * @throws Exception the exception
   */
  public void testClearEmailInSubscription() throws Exception {
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail, listSubs, "http://test.com", new String[]{"test", "sdasd", "asdasd"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't send mail");
      }
    }
    newsletterPublicUserHandler.clearEmailInSubscription(sessionProvider, userEmail);
    List<NewsletterSubscriptionConfig> listSubscriptions =  newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, "classic", userEmail);
    assertEquals(0, listSubscriptions.size());
  }

  /**
   * Test confirm public user.
   *
   * @throws Exception the exception
   */
  public void testConfirmPublicUser() throws Exception {
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail, listSubs, "http://test.com", new String[]{"test", "asdas", "ssss"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't send mail");
      }
    }
    String userPath = NewsletterConstant.generateUserPath("classic");
    Node userFolderNode = (Node)session.getItem(userPath);
    Node node =  userFolderNode.getNode(userEmail);
    boolean isPublicUser = newsletterPublicUserHandler.confirmPublicUser(sessionProvider, userEmail, node.getProperty(NewsletterConstant.USER_PROPERTY_VALIDATION_CODE).getString(), "classic");
    assertEquals(true, isPublicUser);
    isPublicUser = newsletterPublicUserHandler.confirmPublicUser(sessionProvider, userEmail2, node.getProperty(NewsletterConstant.USER_PROPERTY_VALIDATION_CODE).getString(), "classic");
    assertEquals(false, isPublicUser);
  }

  /**
   * Test forget email.
   *
   * @throws Exception the exception
   */
  public void testForgetEmail() throws Exception {
    try{
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail, listSubs, "http://test.com", new String[]{"test","fgfg", "wesad"});
      newsletterPublicUserHandler.subscribe(sessionProvider, "classic", userEmail2, listSubs, "http://test.com", new String[]{"test","fgfg", "wesad"});
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't send mail ");
      }
    }
    newsletterPublicUserHandler.forgetEmail(sessionProvider, "classic", userEmail);
    List<NewsletterSubscriptionConfig> listSubcriptions = newsletterSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, "classic", userEmail);
    assertEquals(0, listSubcriptions.size());
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() {
    try {
      super.tearDown();
      session.getRootNode().getNode("sites content/live").getNode("classic").remove();
      session.save();
    } catch (Exception e) {}
  }
}
