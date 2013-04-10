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
package org.exoplatform.services.cms.timeline;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * The TimelineService component allows documents to be displayed by days, months and years.
 * The configuration of this component is found in /core/core-configuration/src/main/webapp/WEB-INF/conf/wcm-core/core-services-configuration.xml.
 *
 * {@code
 * <component>
 *     <key>org.exoplatform.services.cms.timeline.TimelineService</key>
 *     <type>org.exoplatform.services.cms.timeline.impl.TimelineServiceImpl</type>
 *     <init-params>
 *         <value-param>
 *             <name>itemPerTimeline</name>
 *             <value>5</value>
 *         </value-param>
 *     </init-params>
 * </component>
 * }
 *
 * Support to get all documents by time frame.
 *
 * @LevelAPI Experimental
 */
public interface TimelineService {

  /**
   * Get all documents of Today
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser) throws Exception;
  
  /**
   * Get all documents of Today
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @param isLimit Indicate that the limitation of query is enabled or not
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser,
                                        boolean isLimit) throws Exception;  

  /**
   * Get all documents of Yesterday.
   * 
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser) throws Exception;
  
  /**
   * Get all documents of Yesterday.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @param isLimit Indicate that the limitation of query is enabled or not.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser,
                                            boolean isLimit) throws Exception;   

  /**
   * Get all documents earlier this week.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this week.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @param isLimit Indicate that the limitation of query is enabled or not.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception;

  /**
   * Get all documents earlier this month.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this month.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @param isLimit Indicate that the limitation of query is enabled or not.
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser,
                                                   boolean isLimit) throws Exception;  

  /**
   * Get all documents earlier this year.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this year.
   *
   * @param nodePath The Path of current node
   * @param workspace The Workspace name.
   * @param sessionProvider The Session Provider.
   * @param userName Logged in user.
   * @param byUser Show documents by current user or by all users
   * @param isLimit Indicate that the limitation of query is enabled or not
   * @return List<Node>
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception;
  
  /**
   * Get the number of items per category displayed in Timeline view. This is
   * get from initialize parameter of Timeline Service class If less than or
   * equal zero then use default value 5 items per category.
   * 
   * @return ItemPerTimeline as int
   */
  public int getItemPerTimeline();
}
