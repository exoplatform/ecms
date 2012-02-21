package org.exoplatform.services.wcm.newsletter.handler;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.newsletter.config.NewsletterUserConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class TestNewsletterManageUserHandler.
 */
public class TestNewsletterManageUserHandler extends BaseWCMTestCase {

  /** The categories node. */
  @SuppressWarnings("unused")
  private Node categoriesNode;

  /** The user home node. */
  @SuppressWarnings("unused")
  private Node userHomeNode;

  /** The newsletter category config. */
  private NewsletterCategoryConfig newsletterCategoryConfig;

  /** The newsletter category handler. */
  private NewsletterCategoryHandler newsletterCategoryHandler;

  /** The newsletter subscription config. */
  private NewsletterSubscriptionConfig newsletterSubscriptionConfig;

  /** The news subscription handler. */
  private NewsletterSubscriptionHandler newsSubscriptionHandler;

  /** The list subs. */
  private List<String> listSubs = new ArrayList<String>();

  /** The user email. */
  private String userEmail = "test@local.com";

  /** The newsletter public user handler. */
  private NewsletterPublicUserHandler newsletterPublicUserHandler;

  /** The newsletter manage user handler. */
  private NewsletterManageUserHandler newsletterManageUserHandler;

  /** The user node. */
  private Node userNode;

  /** The newsletter manager service. */
  private NewsletterManagerService newsletterManagerService;

  /** The session provider. */
  private SessionProvider sessionProvider;

  private static Log log = ExoLogger.getLogger(TestNewsletterManageUserHandler.class);

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    Node newsletterApplicationNode = null;
    try {
      newsletterApplicationNode = (Node) session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication");
    } catch(ItemNotFoundException e) {
      e.printStackTrace();
      fail();
    }
    categoriesNode = newsletterApplicationNode.addNode("Categories");
    userHomeNode   = newsletterApplicationNode.addNode("Users");

    session.save();

    newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();
    newsSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    newsletterPublicUserHandler = newsletterManagerService.getPublicUserHandler();
    newsletterManageUserHandler = newsletterManagerService.getManageUserHandler();

    sessionProvider = WCMCoreUtils.getSystemSessionProvider();

    newsletterCategoryConfig = new NewsletterCategoryConfig();
    newsletterCategoryConfig.setName("CategoryName");
    newsletterCategoryConfig.setTitle("CategoryTitle");
    newsletterCategoryConfig.setDescription("CategoryDescription");
    newsletterCategoryConfig.setModerator("root");
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);

    newsletterSubscriptionConfig = new NewsletterSubscriptionConfig();
    for(int i = 0; i < 5; i++) {
      newsletterSubscriptionConfig.setCategoryName("CategoryName");
      newsletterSubscriptionConfig.setName("SubcriptionName_"+i);
      newsletterSubscriptionConfig.setTitle("SubscriptionTitle_"+i);
      newsletterSubscriptionConfig.setDescription("SubscriptionDescription_"+i);
      newsSubscriptionHandler.add(sessionProvider, classicPortal, newsletterSubscriptionConfig);
      listSubs.add(newsletterSubscriptionConfig.getCategoryName()+"#"+newsletterSubscriptionConfig.getName());
    }
    newsletterPublicUserHandler.subscribe(sessionProvider, classicPortal, userEmail, listSubs, "http://test.com", new String[]{"a", "b", "c"});

    String userPath = NewsletterConstant.generateUserPath(classicPortal);
      Node userFolderNode = (Node)session.getItem(userPath);
      userNode =  userFolderNode.getNode(userEmail);
  }

  /**
   * Test add administrator.
   *
   * @throws Exception the exception
   */
  public void testAddAdministrator() throws Exception {
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "test01");
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "test02");
    List<String> listUser = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal);
    assertEquals(2, listUser.size());
    // Add administrator with wrong portal's name
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal + "Wrong", "test02");
    listUser = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal);
    assertEquals(2, listUser.size());
    // Add administrator with username which is exist in administrators list
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "test02");
    listUser = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal);
    assertEquals(2, listUser.size());
  }

  /**
   * Test delete user addministrator.
   *
   * @throws Exception the exception
   */
  public void testDeleteUserAddministrator() throws Exception {
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "userId01");
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "userId02");
    List<String> listUserAdd = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal);
    assertEquals(2,	listUserAdd.size());
    newsletterManageUserHandler.deleteUserAddministrator(sessionProvider, classicPortal, "userId02");
    List<String> listUserDelete = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal);
    assertEquals(1, listUserDelete.size());
  }

  /**
   * Test get all administrator.
   *
   * @throws Exception the exception
   */
  public void testGetAllAdministrator() throws Exception {
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "user01");
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "user02");
    newsletterManageUserHandler.addAdministrator(sessionProvider, classicPortal, "user03");
    List<String> listUser = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal);
    assertEquals(3, listUser.size());
    // test get all administrator with wrong portal's name
    listUser = newsletterManageUserHandler.getAllAdministrator(sessionProvider, classicPortal + "Wrong");
    assertEquals(0, listUser.size());
  }


  /**
   * Test add.
   *
   * @throws Exception the exception
   */
  public void testAdd() throws Exception {
    newsletterManageUserHandler.add(sessionProvider, classicPortal, userEmail);
    boolean isCorrectUser = newsletterPublicUserHandler.confirmPublicUser(sessionProvider, userEmail, userNode.getProperty(NewsletterConstant.USER_PROPERTY_VALIDATION_CODE).getString(), classicPortal);
    assertEquals(true, isCorrectUser);

    // Add email with wrong portal's name
    try{
      newsletterManageUserHandler.add(sessionProvider, classicPortal + "Wrong", "test" + userEmail);
    }catch(Exception ex){
      if (log.isWarnEnabled()) {
        log.warn("Can't add user because portal's name is not exist");
      }
    }
    isCorrectUser = newsletterPublicUserHandler.confirmPublicUser(sessionProvider, "test" + userEmail, userNode.getProperty(NewsletterConstant.USER_PROPERTY_VALIDATION_CODE).getString(), classicPortal);
    assertEquals(false, isCorrectUser);
  }

  /**
   * Test change ban statusl.
   *
   * @throws Exception the exception
   */
  public void testChangeBanStatusl() throws Exception {
    userNode.setProperty(NewsletterConstant.USER_PROPERTY_BANNED, false);
    session.save();
    boolean isBanned = userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean();

    // change banned status with wrong portal's name
    newsletterManageUserHandler.changeBanStatus(sessionProvider, classicPortal + "Wrong", userEmail, true);
    isBanned = userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean();
    assertFalse(isBanned);

    // change banned status for user who is not exist in system
    newsletterManageUserHandler.changeBanStatus(sessionProvider, classicPortal, userEmail + "NotExist", true);
    isBanned = userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean();
    assertFalse(isBanned);

    // change banned status
    newsletterManageUserHandler.changeBanStatus(sessionProvider, classicPortal, userEmail, true);
    isBanned = userNode.getProperty(NewsletterConstant.USER_PROPERTY_BANNED).getBoolean();
    assertTrue(isBanned);
  }

  /**
   * Test delete.
   *
   * @throws Exception the exception
   */
  public void testDelete() throws Exception {
    // Delete user with wrong portal's name
    newsletterManageUserHandler.delete(sessionProvider, classicPortal + "Wrong", userEmail);
    List<NewsletterSubscriptionConfig> listSubscription = newsSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, classicPortal, userEmail);
    assertEquals(5, listSubscription.size());

    // Delete user who is not exist in system
    newsletterManageUserHandler.delete(sessionProvider, classicPortal, userEmail + "NotExist");
    listSubscription = newsSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, classicPortal, userEmail);
    assertEquals(5, listSubscription.size());

    // Delete user
    newsletterManageUserHandler.delete(sessionProvider, classicPortal, userEmail);
    listSubscription = newsSubscriptionHandler.getSubscriptionIdsByPublicUser(sessionProvider, classicPortal, userEmail);
    assertEquals(0, listSubscription.size());
  }

  /**
   * Test get users.
   *
   * @throws Exception the exception
   */
  public void testGetUsers() throws Exception {
    // Get user by subscription
    List<NewsletterUserConfig> listUser = newsletterManageUserHandler
      .getUsers(sessionProvider, classicPortal, newsletterCategoryConfig.getName(), newsletterSubscriptionConfig.getName());
    assertEquals(1, listUser.size());

    // Get all user
    listUser = newsletterManageUserHandler.getUsers(sessionProvider, classicPortal, null, null);
    assertEquals(1, listUser.size());

    // get user in category
    listUser = newsletterManageUserHandler
                .getUsers(sessionProvider, classicPortal, newsletterCategoryConfig.getName(), null);
    assertEquals(1, listUser.size());
  }

  /**
   * Test get quantity user by subscription.
   *
   * @throws Exception the exception
   */
  public void testGetQuantityUserBySubscription() throws Exception {
    int countUser = newsletterManageUserHandler
      .getQuantityUserBySubscription(sessionProvider, classicPortal, newsletterCategoryConfig.getName(), newsletterSubscriptionConfig.getName());
    assertEquals(1, countUser);

    countUser = newsletterManageUserHandler
    .getQuantityUserBySubscription(sessionProvider, classicPortal, newsletterCategoryConfig.getName(), newsletterSubscriptionConfig.getName() + "wrong");
    assertEquals(0, countUser);
  }

  /**
   * Test check existed email.
   *
   * @throws Exception the exception
   */
  public void testCheckExistedEmail() throws Exception {
    boolean existEmail = newsletterManageUserHandler.checkExistedEmail(sessionProvider, classicPortal, userEmail);
    assertEquals(true, existEmail);

    existEmail = newsletterManageUserHandler.checkExistedEmail(sessionProvider, classicPortal, "donothave@yahoo.com");
    assertEquals(false, existEmail);

    existEmail = newsletterManageUserHandler.checkExistedEmail(sessionProvider, "classicPortal", "donothave@yahoo.com");
    assertEquals(false, existEmail);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() {
    try {
      super.tearDown();
      try {
        session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication/Categories").remove();
        session.getItem("/sites content/live/classic/ApplicationData/NewsletterApplication/Users").remove();
      } catch(ItemNotFoundException e) {
        e.printStackTrace();
        fail();
      }
      session.save();
    } catch (Exception e) {}
  }
}
