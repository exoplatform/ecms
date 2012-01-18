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
package org.exoplatform.services.ecm.dms.timeline;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.cms.timeline.TimelineService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 22, 2009
 * 10:50:05 AM
 */
public class TestTimelineService extends BaseDMSTestCase {

  private TimelineService timelineService;
  final private static String EXO_MODIFIED_DATE = "exo:dateModified";

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    super.setUp();
    timelineService = (TimelineService)container.getComponentInstanceOfType(TimelineService.class);
  }

  /**
   * test method getDocumentsOfToday
   * input:    /testNode/today1(dateModified is today)
   *       /testNode/today2(dateModified is today)
   *       /testNode/yesterday1(dateModified is yesterday)
   * action:  getDocumentsOfToday
   * expectedValue:  2(today1 and today2);
   * @throws Exception
   */
  public void testGetDocumentsOfToday() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();

    Node today1 = testNode.addNode("today1", "exo:sample");
    today1.setProperty("exo:title", "sample");
    if(today1.canAddMixin("exo:datetime")) today1.addMixin("exo:datetime");
    today1.setProperty(EXO_MODIFIED_DATE, currentTime);

    Node today2 = testNode.addNode("today2", "exo:sample");
    today2.setProperty("exo:title", "sample");
    if(today2.canAddMixin("exo:datetime")) today2.addMixin("exo:datetime");
    today2.setProperty(EXO_MODIFIED_DATE, currentTime);

    Calendar yesterdayTime = (Calendar)currentTime.clone();
    yesterdayTime.add(Calendar.DATE, -1);

    Node yesterday1 = testNode.addNode("yesterday1", "exo:sample");
    yesterday1.setProperty("exo:title", "sample");
    if(yesterday1.canAddMixin("exo:datetime")) yesterday1.addMixin("exo:datetime");
    yesterday1.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    session.save();

    List<Node> res = timelineService.getDocumentsOfToday(testNode.getPath(),
                                                         COLLABORATION_WS,
                                                         createSessionProvider(),
                                                         "root",
                                                         true);
    assertEquals("testGetDocumentsOfToday failed! ", 2, res.size());
  }
  
  /**
   * test method getDocumentsOfToday
   * input:    /testNode/today1(dateModified is today)
   *       /testNode/today2(dateModified is today)
   *       /testNode/today3(dateModified is today)
   *       /testNode/today4(dateModified is today)
   *       /testNode/today5(dateModified is today)
   *       /testNode/today6(dateModified is today)
   *       /testNode/yesterday1(dateModified is yesterday)
   * action:  getDocumentsOfToday
   * expectedValue:  6(today1, today2, today3, today4, today5 and today6);
   * @throws Exception
   */
  public void testGetDocumentsOfTodayUnLimited() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();

    Node today1 = testNode.addNode("today1", "exo:sample");
    today1.setProperty("exo:title", "sample");
    if(today1.canAddMixin("exo:datetime")) today1.addMixin("exo:datetime");
    today1.setProperty(EXO_MODIFIED_DATE, currentTime);

    Node today2 = testNode.addNode("today2", "exo:sample");
    today2.setProperty("exo:title", "sample");
    if(today2.canAddMixin("exo:datetime")) today2.addMixin("exo:datetime");
    today2.setProperty(EXO_MODIFIED_DATE, currentTime);
    
    Node today3 = testNode.addNode("today3", "exo:sample");
    today3.setProperty("exo:title", "sample");
    if(today3.canAddMixin("exo:datetime")) today3.addMixin("exo:datetime");
    today3.setProperty(EXO_MODIFIED_DATE, currentTime);
    
    Node today4 = testNode.addNode("today4", "exo:sample");
    today4.setProperty("exo:title", "sample");
    if(today4.canAddMixin("exo:datetime")) today4.addMixin("exo:datetime");
    today4.setProperty(EXO_MODIFIED_DATE, currentTime);
    
    Node today5 = testNode.addNode("today5", "exo:sample");
    today5.setProperty("exo:title", "sample");
    if(today5.canAddMixin("exo:datetime")) today5.addMixin("exo:datetime");
    today5.setProperty(EXO_MODIFIED_DATE, currentTime);
    
    Node today6 = testNode.addNode("today6", "exo:sample");
    today6.setProperty("exo:title", "sample");
    if(today6.canAddMixin("exo:datetime")) today6.addMixin("exo:datetime");
    today6.setProperty(EXO_MODIFIED_DATE, currentTime);    

    Calendar yesterdayTime = (Calendar)currentTime.clone();
    yesterdayTime.add(Calendar.DATE, -1);

    Node yesterday1 = testNode.addNode("yesterday1", "exo:sample");
    yesterday1.setProperty("exo:title", "sample");
    if(yesterday1.canAddMixin("exo:datetime")) yesterday1.addMixin("exo:datetime");
    yesterday1.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    session.save();

    List<Node> res = timelineService.getDocumentsOfToday(testNode.getPath(),
                                                         COLLABORATION_WS,
                                                         createSessionProvider(),
                                                         "root",
                                                         true, 
                                                         false);
    assertEquals("testGetDocumentsOfToday failed! ", 6, res.size());
  }  

  /**
   * test method getDocumentsOfYesterday
   * input:    /testNode/today1(dateModified is today)
   *       /testNode/today2(dateModified is today)
   *       /testNode/yesterday1(dateModified is yesterday)
   * action:  getDocumentsOfYesterday
   * expectedValue:  1(yesterday1)
   * @throws Exception
   */
  public void testGetDocumentsOfYesterday() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();

    Node today1 = testNode.addNode("today1", "exo:sample");
    today1.setProperty("exo:title", "sample");
    if(today1.canAddMixin("exo:datetime")) {
      today1.addMixin("exo:datetime");
    }
    today1.setProperty(EXO_MODIFIED_DATE, currentTime);

    Node today2 = testNode.addNode("today2", "exo:sample");
    today2.setProperty("exo:title", "sample");
    if(today2.canAddMixin("exo:datetime")) {
      today2.addMixin("exo:datetime");
    }
    today2.setProperty(EXO_MODIFIED_DATE, currentTime);

    Calendar yesterdayTime = (Calendar)currentTime.clone();
    yesterdayTime.add(Calendar.DATE, -1);

    Node yesterday1 = testNode.addNode("yesterday1", "exo:sample");
    yesterday1.setProperty("exo:title", "sample");
    if(yesterday1.canAddMixin("exo:datetime")) {
      yesterday1.addMixin("exo:datetime");
    }
    yesterday1.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    session.save();

    List<Node> res = timelineService.getDocumentsOfYesterday(rootNode.getPath(),
                                                             COLLABORATION_WS,
                                                             createSessionProvider(),
                                                             "root",
                                                             true);
    assertEquals("testGetDocumentsOfYesterday failed! ", 1, res.size());
  }
  
  /**
   * test method getDocumentsOfYesterday
   * input:    /testNode/today1(dateModified is today)
   *       /testNode/today2(dateModified is today)
   *       /testNode/yesterday1(dateModified is yesterday)
   *       /testNode/yesterday2(dateModified is yesterday)
   *       /testNode/yesterday3(dateModified is yesterday)
   *       /testNode/yesterday4(dateModified is yesterday)
   *       /testNode/yesterday5(dateModified is yesterday)
   *       /testNode/yesterday6(dateModified is yesterday)
   *       /testNode/yesterday7(dateModified is yesterday)
   * action:  getDocumentsOfYesterday
   * expectedValue:  7(yesterday1, yesterday2, yesterday3, yesterday4, yesterday5, yesterday6, yesterday6));
   * @throws Exception
   */
  public void testGetDocumentsOfYesterdayUnLimited() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();

    Node today1 = testNode.addNode("today1", "exo:sample");
    today1.setProperty("exo:title", "sample");
    if(today1.canAddMixin("exo:datetime")) {
      today1.addMixin("exo:datetime");
    }
    today1.setProperty(EXO_MODIFIED_DATE, currentTime);

    Node today2 = testNode.addNode("today2", "exo:sample");
    today2.setProperty("exo:title", "sample");
    if(today2.canAddMixin("exo:datetime")) {
      today2.addMixin("exo:datetime");
    }
    today2.setProperty(EXO_MODIFIED_DATE, currentTime);

    Calendar yesterdayTime = (Calendar)currentTime.clone();
    yesterdayTime.add(Calendar.DATE, -1);

    Node yesterday1 = testNode.addNode("yesterday1", "exo:sample");
    yesterday1.setProperty("exo:title", "sample");
    if(yesterday1.canAddMixin("exo:datetime")) {
      yesterday1.addMixin("exo:datetime");
    }
    yesterday1.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    
    Node yesterday2 = testNode.addNode("yesterday2", "exo:sample");
    yesterday2.setProperty("exo:title", "sample");
    if(yesterday2.canAddMixin("exo:datetime")) {
      yesterday2.addMixin("exo:datetime");
    }
    yesterday2.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    
    Node yesterday3 = testNode.addNode("yesterday3", "exo:sample");
    yesterday3.setProperty("exo:title", "sample");
    if(yesterday3.canAddMixin("exo:datetime")) {
      yesterday3.addMixin("exo:datetime");
    }
    yesterday3.setProperty(EXO_MODIFIED_DATE, yesterdayTime);

    Node yesterday4 = testNode.addNode("yesterday4", "exo:sample");
    yesterday4.setProperty("exo:title", "sample");
    if(yesterday4.canAddMixin("exo:datetime")) {
      yesterday4.addMixin("exo:datetime");
    }
    yesterday4.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    
    Node yesterday5 = testNode.addNode("yesterday5", "exo:sample");
    yesterday5.setProperty("exo:title", "sample");
    if(yesterday5.canAddMixin("exo:datetime")) {
      yesterday5.addMixin("exo:datetime");
    }
    yesterday5.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    
    Node yesterday6 = testNode.addNode("yesterday6", "exo:sample");
    yesterday6.setProperty("exo:title", "sample");
    if(yesterday6.canAddMixin("exo:datetime")) {
      yesterday6.addMixin("exo:datetime");
    }
    yesterday6.setProperty(EXO_MODIFIED_DATE, yesterdayTime);
    
    Node yesterday7 = testNode.addNode("yesterday7", "exo:sample");
    yesterday7.setProperty("exo:title", "sample");
    if(yesterday7.canAddMixin("exo:datetime")) {
      yesterday7.addMixin("exo:datetime");
    }
    yesterday7.setProperty(EXO_MODIFIED_DATE, yesterdayTime);        
    
    session.save();

    List<Node> resLimit = timelineService.getDocumentsOfYesterday(rootNode.getPath(),
                                                             COLLABORATION_WS,
                                                             createSessionProvider(),
                                                             "root",
                                                             true);
    
    List<Node> resUnLimit = timelineService.getDocumentsOfYesterday(rootNode.getPath(),
                                                             COLLABORATION_WS,
                                                             createSessionProvider(),
                                                             "root",
                                                             true,
                                                             false);
    
    assertEquals("testGetDocumentsOfYesterday failed! ", 7, resUnLimit.size());
    assertEquals("testGetDocumentsOfYesterday failed! ", 5, resLimit.size());
  }

  /**
   * test method getDocumentsOfEarlierThisWeek
   * input:    /testNode/Sunday
   *       /testNode/Monday
   *       /testNode/Tuesday
   *       ...
   *       /testNode/${today} (depends on current date time)
   * action:  getDocumentsOfEarlierThisWeek
   * expectedValue:  (depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisWeek() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();
    Calendar time = (Calendar)currentTime.clone();
    int count = 0;
    int index = 0;
    while (currentTime.get(Calendar.WEEK_OF_YEAR) == time.get(Calendar.WEEK_OF_YEAR)) {
      if (currentTime.get(Calendar.WEEK_OF_YEAR) == time.get(Calendar.WEEK_OF_YEAR)) {
        if (time.get(Calendar.DAY_OF_YEAR) < currentTime.get(Calendar.DAY_OF_YEAR)-1)
          count++;
      }
      Node dayNode = testNode.addNode("dayNode" + index++, "exo:sample");
      dayNode.setProperty("exo:title", "sample");
      if(dayNode.canAddMixin("exo:datetime")) {
        dayNode.addMixin("exo:datetime");
      }
      dayNode.setProperty(EXO_MODIFIED_DATE, time);
      time.add(Calendar.DATE, -1);
    }

    session.save();
    List<Node> res = timelineService.getDocumentsOfEarlierThisWeek(rootNode.getPath(),
                                                                   COLLABORATION_WS,
                                                                   createSessionProvider(),
                                                                   "root",
                                                                   true);
    assertEquals("testGetDocumentsOfEarlierThisWeek failed! ", Math.min(5, count), res.size());
  }
  
  /**
   * test method getDocumentsOfEarlierThisWeek
   * input:    /testNode/Sunday
   *       /testNode/Monday
   *       /testNode/Tuesday
   *       ...
   *       /testNode/${today} (depends on current date time)
   * action:  getDocumentsOfEarlierThisWeek
   * expectedValue:  (depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisWeek2() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();
    Calendar time = (Calendar)currentTime.clone();
    int count = 0;
    int index = 0;
    while (currentTime.get(Calendar.WEEK_OF_YEAR) == time.get(Calendar.WEEK_OF_YEAR)) {
      if (currentTime.get(Calendar.WEEK_OF_YEAR) == time.get(Calendar.WEEK_OF_YEAR)) {
        if (time.get(Calendar.DAY_OF_YEAR) < currentTime.get(Calendar.DAY_OF_YEAR)-1)
          count++;
      }
      Node dayNode = testNode.addNode("dayNode" + index++, "exo:sample");
      dayNode.setProperty("exo:title", "sample");
      if(dayNode.canAddMixin("exo:datetime")) {
        dayNode.addMixin("exo:datetime");
      }
      dayNode.setProperty(EXO_MODIFIED_DATE, time);
      time.add(Calendar.DATE, -1);
    }

    session.save();
    List<Node> res = timelineService.getDocumentsOfEarlierThisWeek(rootNode.getPath(),
                                                                   COLLABORATION_WS,
                                                                   createSessionProvider(),
                                                                   "root",
                                                                   true,
                                                                   false);
    assertEquals("testGetDocumentsOfEarlierThisWeek failed! ", count, res.size());
  }  

  /**
   * test method getDocumentsOfEarlierThisMonth
   * input:    /testNode/1stOfThisMonth
   *       /testNode/2ndOfThisMonth
   *       /testNode/3rdOfThisMonth
   *       ...
   *       /testNode/${today} (depends on current date time)
   * action:  getDocumentsOfEarlierThisMonth
   * expectedValue:  (depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisMonth() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();
    Calendar time = (Calendar)currentTime.clone();
    int count = 0;
    int index = 0;
    while (currentTime.get(Calendar.MONTH) == time.get(Calendar.MONTH)) {
      if (currentTime.get(Calendar.MONTH) == time.get(Calendar.MONTH)) {
        if (time.get(Calendar.WEEK_OF_YEAR) < currentTime.get(Calendar.WEEK_OF_YEAR))
          count++;
      }
      Node dayNode = testNode.addNode("dayNode" + index++, "exo:sample");
      dayNode.setProperty("exo:title", "sample");
      if(dayNode.canAddMixin("exo:datetime")) {
        dayNode.addMixin("exo:datetime");
      }
      dayNode.setProperty(EXO_MODIFIED_DATE, time);
      time.add(Calendar.DATE, -1);
    }

    session.save();
    List<Node> res = timelineService.getDocumentsOfEarlierThisMonth(rootNode.getPath(),
                                                                    COLLABORATION_WS,
                                                                    createSessionProvider(),
                                                                    "root",
                                                                    true);
    assertEquals("testGetDocumentsOfEarlierThisMonth failed! ", Math.min(5, count), res.size());
  }
  
  /**
   * test method getDocumentsOfEarlierThisMonth
   * input:    /testNode/1stOfThisMonth
   *       /testNode/2ndOfThisMonth
   *       /testNode/3rdOfThisMonth
   *       ...
   *       /testNode/${today} (depends on current date time)
   * action:  getDocumentsOfEarlierThisMonth
   * expectedValue:  (depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisMonth2() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();
    Calendar time = (Calendar)currentTime.clone();
    int count = 0;
    int index = 0;
    while (currentTime.get(Calendar.MONTH) == time.get(Calendar.MONTH)) {
      if (currentTime.get(Calendar.MONTH) == time.get(Calendar.MONTH)) {
        if (time.get(Calendar.WEEK_OF_YEAR) < currentTime.get(Calendar.WEEK_OF_YEAR))
          count++;
      }
      Node dayNode = testNode.addNode("dayNode" + index++, "exo:sample");
      dayNode.setProperty("exo:title", "sample");
      if(dayNode.canAddMixin("exo:datetime")) {
        dayNode.addMixin("exo:datetime");
      }
      dayNode.setProperty(EXO_MODIFIED_DATE, time);
      time.add(Calendar.DATE, -1);
    }

    session.save();
    List<Node> res = timelineService.getDocumentsOfEarlierThisMonth(rootNode.getPath(),
                                                                    COLLABORATION_WS,
                                                                    createSessionProvider(),
                                                                    "root",
                                                                    true,
                                                                    false);
    assertEquals("testGetDocumentsOfEarlierThisMonth failed! ", count, res.size());
  }  

  /**
   * test method getDocumentsOfEarlierThisYear
   * input:    /testNode/1stOfThisYear
   *       /testNode/2ndOfThisYear
   *       /testNode/3rdOfThisYear
   *       ...
   *       /testNode/${today} (depends on current date time)
   * action:  getDocumentsOfEarlierThisYear
   * expectedValue:  (depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisYear() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();
    Calendar time = (Calendar)currentTime.clone();
    int count = 0;
    int index = 0;
    while (currentTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
      if (currentTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
        if (time.get(Calendar.MONTH) < currentTime.get(Calendar.MONTH))
          count++;
      }
      Node dayNode = testNode.addNode("dayNode" + index++, "exo:sample");
      dayNode.setProperty("exo:title", "sample");
      if(dayNode.canAddMixin("exo:datetime")) {
        dayNode.addMixin("exo:datetime");
      }
      dayNode.setProperty(EXO_MODIFIED_DATE, time);
      time.add(Calendar.DATE, -1);
    }

    session.save();
    List<Node> res = timelineService.getDocumentsOfEarlierThisYear(rootNode.getPath(),
                                                                   COLLABORATION_WS,
                                                                   createSessionProvider(),
                                                                   "root",
                                                                   true);
    assertEquals("testGetDocumentsOfEarlierThisYear failed! ", Math.min(5, count), res.size());
  }
  
  /**
   * test method getDocumentsOfEarlierThisYear
   * input:    /testNode/1stOfThisYear
   *       /testNode/2ndOfThisYear
   *       /testNode/3rdOfThisYear
   *       ...
   *       /testNode/${today} (depends on current date time)
   * action:  getDocumentsOfEarlierThisYear
   * expectedValue:  (depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisYear2() throws Exception {
    applyUserSession("root", "exo");
    Node rootNode = session.getRootNode();
    Node testNode = rootNode.addNode("testNode");

    Calendar currentTime = new GregorianCalendar();
    Calendar time = (Calendar)currentTime.clone();
    int count = 0;
    int index = 0;
    while (currentTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
      if (currentTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
        if (time.get(Calendar.MONTH) < currentTime.get(Calendar.MONTH))
          count++;
      }
      Node dayNode = testNode.addNode("dayNode" + index++, "exo:sample");
      dayNode.setProperty("exo:title", "sample");
      if(dayNode.canAddMixin("exo:datetime")) {
        dayNode.addMixin("exo:datetime");
      }
      dayNode.setProperty(EXO_MODIFIED_DATE, time);
      time.add(Calendar.DATE, -1);
    }

    session.save();
    List<Node> res = timelineService.getDocumentsOfEarlierThisYear(rootNode.getPath(),
                                                                   COLLABORATION_WS,
                                                                   createSessionProvider(),
                                                                   "root",
                                                                   true,
                                                                   false);
    assertEquals("testGetDocumentsOfEarlierThisYear failed! ", count, res.size());
  }  

  /**
   * private method create sessionProvider instance.
   * @return SessionProvider
   */
  private SessionProvider createSessionProvider() {
    SessionProviderService sessionProviderService = (SessionProviderService) container
        .getComponentInstanceOfType(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }

  public void tearDown() throws Exception {
  Node rootNode = session.getRootNode();
  Node testNode = rootNode.getNode("testNode");
  if (testNode != null)
    testNode.remove();
    session.save();
    super.tearDown();
  }
}
