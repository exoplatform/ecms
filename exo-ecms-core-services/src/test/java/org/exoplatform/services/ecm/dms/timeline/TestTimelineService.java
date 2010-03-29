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
   * input:		/testNode/today1(dateModified is today)
   * 			/testNode/today2(dateModified is today)
   * 			/testNode/yesterday1(dateModified is yesterday)
   * action:	getDocumentsOfToday
   * expectedValue:	2(today1 and today2);
   * @throws Exception
   */
  public void testGetDocumentsOfToday() throws Exception {
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
    
    List<Node> res = timelineService.getDocumentsOfToday(testNode.getPath(), REPO_NAME, COLLABORATION_WS, createSessionProvider(), "root", true);
//    assertEquals("testGetDocumentsOfToday failed! ", 2, res.size());
  }
  
  /**
   * test method getDocumentsOfYesterday
   * input:		/testNode/today1(dateModified is today)
   * 			/testNode/today2(dateModified is today)
   * 			/testNode/yesterday1(dateModified is yesterday)
   * action:	getDocumentsOfYesterday
   * expectedValue:	1(yesterday1);
   * @throws Exception
   */
  public void testGetDocumentsOfYesterday() throws Exception {
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
	  
    List<Node> res = timelineService.getDocumentsOfYesterday(rootNode.getPath(), REPO_NAME, COLLABORATION_WS, createSessionProvider(), "root", true);
//    assertEquals("testGetDocumentsOfYesterday failed! ", 1, res.size());
  }
  
  /**
   * test method getDocumentsOfEarlierThisWeek
   * input:		/testNode/Sunday
   * 			/testNode/Monday
   * 			/testNode/Tuesday
   * 			...
   * 			/testNode/${today} (depends on current date time)
   * action:	getDocumentsOfEarlierThisWeek
   * expectedValue:	(depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisWeek() throws Exception {
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
	List<Node> res = timelineService.getDocumentsOfEarlierThisWeek(rootNode.getPath(), REPO_NAME, COLLABORATION_WS, createSessionProvider(), "root", true);
//	assertEquals("testGetDocumentsOfEarlierThisWeek falied! ", Math.min(5, count), res.size());
	System.out.println("Expected: " + count);
	System.out.println("actual: " + res.size());
  }
  
  /**
   * test method getDocumentsOfEarlierThisMonth
   * input:		/testNode/1stOfThisMonth
   * 			/testNode/2ndOfThisMonth
   * 			/testNode/3rdOfThisMonth
   * 			...
   * 			/testNode/${today} (depends on current date time)
   * action:	getDocumentsOfEarlierThisMonth
   * expectedValue:	(depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisMonth() throws Exception {
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
	List<Node> res = timelineService.getDocumentsOfEarlierThisMonth(rootNode.getPath(), REPO_NAME, COLLABORATION_WS, createSessionProvider(), "root", true);
//	assertEquals("testGetDocumentsOfEarlierThisMonth falied! ", Math.min(5, count), res.size());
	System.out.println("Expected: " + count);
	System.out.println("actual: " + res.size());
  }

  /**
   * test method getDocumentsOfEarlierThisYear
   * input:		/testNode/1stOfThisYear
   * 			/testNode/2ndOfThisYear
   * 			/testNode/3rdOfThisYear
   * 			...
   * 			/testNode/${today} (depends on current date time)
   * action:	getDocumentsOfEarlierThisYear
   * expectedValue:	(depends on current date time, must calculate yourself);
   * @throws Exception
   */
  public void testGetDocumentsOfEarlierThisYear() throws Exception {
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
    List<Node> res = timelineService.getDocumentsOfEarlierThisYear(rootNode.getPath(), REPO_NAME, COLLABORATION_WS, createSessionProvider(), "root", true);
    //assertEquals("testGetDocumentsOfEarlierThisYear falied! ", Math.min(5, count), res.size());
    System.out.println("Expected: " + count);
    System.out.println("actual: " + res.size());
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
