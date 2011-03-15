/*
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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.newsletter.handler;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterConstant;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.NewsletterSubscriptionConfig;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com Jul 14, 2009
 */
public class TestNewsletterCategoryHandler extends BaseWCMTestCase {

  /** The session provider. */
  private SessionProvider sessionProvider;

  /** The newsletter category config. */
  private NewsletterCategoryConfig newsletterCategoryConfig;

  /** The categories node. */
  private Node categoriesNode;

  /** The newsletter category handler. */
  private NewsletterCategoryHandler newsletterCategoryHandler;

  private Node newsletterApplicationNode;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    try {
      newsletterApplicationNode = session.getRootNode().getNode("sites content/live/classic/ApplicationData/NewsletterApplication");
      categoriesNode = newsletterApplicationNode.getNode("Categories");
    } catch(ItemNotFoundException e) {
      e.printStackTrace();
      fail();
    }
    categoriesNode.setProperty(NewsletterConstant.CATEGORIES_PROPERTY_ADDMINISTRATOR, new String[]{this.userRoot});
    session.save();

    NewsletterManagerService  newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    newsletterCategoryHandler = newsletterManagerService.getCategoryHandler();

    newsletterCategoryConfig = new NewsletterCategoryConfig();
    newsletterCategoryConfig.setName("newsletter01");
    newsletterCategoryConfig.setTitle("title01");
    newsletterCategoryConfig.setDescription("description01");
    newsletterCategoryConfig.setModerator("root");
  }

  /**
   * Test add category.
   *
   * @throws Exception the exception
   */
  public void testAddCategory() throws Exception {
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);

    Node node = categoriesNode.getNode(newsletterCategoryConfig.getName());
    assertNotNull(node);
    assertEquals(node.getName(), newsletterCategoryConfig.getName());
    assertEquals(node.getProperty(NewsletterConstant.CATEGORY_PROPERTY_DESCRIPTION).getString(), newsletterCategoryConfig.getDescription());
    assertEquals(node.getProperty(NewsletterConstant.CATEGORY_PROPERTY_TITLE).getString(), newsletterCategoryConfig.getTitle());

    newsletterCategoryConfig.setName("TestWrong");
    newsletterCategoryHandler.add(sessionProvider, "classicPortal", newsletterCategoryConfig);
    node = null;
    try{
      node = categoriesNode.getNode(newsletterCategoryConfig.getName());
    }catch(Exception ex){}
    assertNull(node);
  }

  /**
   * Test edit category.
   *
   * @throws Exception the exception
   */
  public void testEditCategory()  throws Exception {
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);

    newsletterCategoryConfig.setTitle("Sport News");
    newsletterCategoryConfig.setDescription("Soccer,tennis,...");
    newsletterCategoryConfig.setModerator("john");
    newsletterCategoryHandler.edit(sessionProvider, classicPortal, newsletterCategoryConfig);

    // get node and edit
    Node node = categoriesNode.getNode(newsletterCategoryConfig.getName());
    assertNotNull(node);
    assertEquals(node.getProperty(NewsletterConstant.CATEGORY_PROPERTY_TITLE).getString(), newsletterCategoryConfig.getTitle());
    assertEquals(node.getProperty(NewsletterConstant.CATEGORY_PROPERTY_DESCRIPTION).getString(), newsletterCategoryConfig.getDescription());

    // Edit category with wrong portal's name
    newsletterCategoryHandler.edit(sessionProvider, classicPortal + "Wrong", newsletterCategoryConfig);
    node = categoriesNode.getNode(newsletterCategoryConfig.getName());
    newsletterCategoryConfig.setDescription("I have just modified");
    assertNotSame(node.getProperty(NewsletterConstant.CATEGORY_PROPERTY_DESCRIPTION).getString(), newsletterCategoryConfig.getDescription());

  }

  /**
   * Test delete category.
   *
   * @throws Exception the exception
   */
  public void testDeleteCategory() throws Exception {
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);
    assertEquals(1, categoriesNode.getNodes().getSize());
    newsletterCategoryHandler.delete(sessionProvider, classicPortal, newsletterCategoryConfig.getName()+"Wrong");
    assertEquals(1, categoriesNode.getNodes().getSize());
    newsletterCategoryHandler.delete(sessionProvider, classicPortal, newsletterCategoryConfig.getName());
    assertEquals(0, categoriesNode.getNodes().getSize());

  }

  /**
   * Test get category by name.
   *
   * @throws Exception the exception
   */
  public void testGetCategoryByName() throws Exception {
    newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);
    NewsletterCategoryConfig cat = newsletterCategoryHandler.getCategoryByName(sessionProvider, classicPortal, newsletterCategoryConfig.getName());
    assertEquals("newsletter01", cat.getName());

    cat = newsletterCategoryHandler.getCategoryByName(sessionProvider, classicPortal, newsletterCategoryConfig.getName() + "wrong");
    assertEquals(null, cat);
  }

  /**
   * Test get list categories by name.
   *
   * @throws Exception the exception
   */
  public void testGetListCategoriesByName() throws Exception {
    for(int i =0; i < 5; i ++) {
      NewsletterCategoryConfig newsletterCategoryConfig = new NewsletterCategoryConfig();
      newsletterCategoryConfig.setName("cat_" + i);
      newsletterCategoryConfig.setTitle("title_" + i);
      newsletterCategoryConfig.setDescription("description_" + i);
      newsletterCategoryConfig.setModerator(this.userRoot);
      newsletterCategoryHandler.add(sessionProvider, this.classicPortal, newsletterCategoryConfig);
    }
    assertEquals(5, newsletterCategoryHandler.getListCategories(this.classicPortal, sessionProvider).size());
    //assertEquals(0, newsletterCategoryHandler.getListCategories(this.classicPortal + "Wrong", sessionProvider).size());
  }

  public void testGetListCategoryCanView() throws Exception {
    NewsletterCategoryConfig newsletterCategoryConfig;
    NewsletterSubscriptionConfig subscriptionConfig;
    NewsletterManagerService  newsletterManagerService = WCMCoreUtils.getService(NewsletterManagerService.class);
    NewsletterSubscriptionHandler newsletterSubscriptionHandler = newsletterManagerService.getSubscriptionHandler();
    for(int i =0; i < 5; i ++) {
      newsletterCategoryConfig = new NewsletterCategoryConfig();
      newsletterCategoryConfig.setName("cat_" + i);
      newsletterCategoryConfig.setTitle("title_" + i);
      newsletterCategoryConfig.setDescription("description_" + i);
      newsletterCategoryConfig.setModerator(this.userRoot);
      newsletterCategoryHandler.add(sessionProvider, classicPortal, newsletterCategoryConfig);
      for(int j = 0; j < 2; j ++){
        subscriptionConfig = new NewsletterSubscriptionConfig();
        subscriptionConfig.setName(newsletterCategoryConfig.getName() + "subscription" + j);
        subscriptionConfig.setCategoryName(newsletterCategoryConfig.getName());
        subscriptionConfig.setDescription("Description");
        subscriptionConfig.setRedactor("demo");
        subscriptionConfig.setTitle("Title");
        newsletterSubscriptionHandler.add(sessionProvider, classicPortal, subscriptionConfig);
      }
    }
    assertEquals(5, newsletterCategoryHandler.getListCategories(this.classicPortal, sessionProvider).size());

    assertEquals(0, newsletterCategoryHandler.getListCategoriesCanView(this.classicPortal,
                                                                       "demo",
                                                                       sessionProvider).size());
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
    NodeIterator nodeIterator = categoriesNode.getNodes();
    while(nodeIterator.hasNext()) {
      nodeIterator.nextNode().remove();
    }
    session.save();
  }
}
