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
 * Supports to get all documents by time frame (days, months and years).
 * The configuration of this component is found in /core/core-configuration/src/main/webapp/WEB-INF/conf/wcm-core/core-services-configuration.xml.
 * <p></p>
 * <pre>
 * &lt;component&gt;
 *     &lt;key&gt;org.exoplatform.services.cms.timeline.TimelineService&lt;/key&gt;
 *     &lt;type&gt;org.exoplatform.services.cms.timeline.impl.TimelineServiceImpl&lt;/type&gt;
 *     &lt;init-params&gt;
 *         &lt;value-param&gt;
 *             &lt;name&gt;itemPerTimeline&lt;/name&gt;
 *             &lt;value&gt;5&lt;/value&gt;
 *         &lt;/value-param&gt;
 *     &lt;/init-params&gt;
 * &lt;/component&gt;
 * </pre>
 *
 * @LevelAPI Experimental
 */
public interface TimelineService {

  /**
   * Gets all documents of today.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser) throws Exception;
  
  /**
   * Gets all documents of today.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @param isLimit Indicates the limitation of query is enabled or not.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser,
                                        boolean isLimit) throws Exception;  

  /**
   * Gets all documents of yesterday.
   * 
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser) throws Exception;
  
  /**
   * Gets all documents of yesterday.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users
   * @param isLimit Indicates the limitation of query is enabled or not.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser,
                                            boolean isLimit) throws Exception;   

  /**
   * Gets all documents earlier this week.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Gets all documents earlier this week.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users
   * @param isLimit Indicates the limitation of query is enabled or not.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception;

  /**
   * Gets all documents earlier this month.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser) throws Exception;
  
  /**
   * Gets all documents earlier this month.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @param isLimit Indicates the limitation of query is enabled or not.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser,
                                                   boolean isLimit) throws Exception;  

  /**
   * Gets all documents earlier this year.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Gets all documents earlier this year.
   *
   * @param nodePath Path of the current node.
   * @param workspace Name of the workspace.
   * @param sessionProvider The session provider.
   * @param userName The logged-in user.
   * @param byUser Shows documents by current user or by all users
   * @param isLimit Indicates the limitation of query is enabled or not.
   * @return The list of documents.
   * @throws Exception The exception
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception;
  
  /**
   * Gets the number of items per category displayed in the Timeline view.
   * 
   * @return The number of items.
   */
  public int getItemPerTimeline();
}
