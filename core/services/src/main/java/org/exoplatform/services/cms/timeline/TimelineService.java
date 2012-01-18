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
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 22, 2009
 * 8:19:26 AM
 */
/**
 * Support to get all documents by time frame
 */
public interface TimelineService {

  /**
   * Get all documents of Today
   * 
   * @param nodePath Path of current node
   * @param repository Repository name
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  @Deprecated
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String repository,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser) throws Exception;

  /**
   * Get all documents of Today
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser) throws Exception;
  
  /**
   * Get all documents of Today
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @param isLimit indicate that the limitation of query is enabled or not
   * @return List<Node>
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser,
                                        boolean isLimit) throws Exception;  

  /**
   * Get all documents of Yesterday
   * 
   * @param nodePath Path of current node
   * @param repository Repository name
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  @Deprecated
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String repository,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser) throws Exception;
  
  /**
   * Get all documents of Yesterday
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser) throws Exception;
  
  /**
   * Get all documents of Yesterday
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @param isLimit indicate that the limitation of query is enabled or not
   * @return List<Node>
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser,
                                            boolean isLimit) throws Exception;   

  /**
   * Get all documents earlier this week
   * 
   * @param nodePath Path of current node
   * @param repository Repository name
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  @Deprecated
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String repository,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this week
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this week
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @param isLimit indicate that the limitation of query is enabled or not
   * @return List<Node>
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception;

  /**
   * Get all documents earlier this month
   * 
   * @param nodePath Path of current node
   * @param repository Repository name
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  @Deprecated
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String repository,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this month
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this month
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @param isLimit indicate that the limitation of query is enabled or not
   * @return List<Node>
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser,
                                                   boolean isLimit) throws Exception;  

  /**
   * Get all documents earlier this year
   * 
   * @param nodePath Path of current node
   * @param repository Repository name
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  @Deprecated
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String repository,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this year
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @return List<Node>
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception;
  
  /**
   * Get all documents earlier this year
   * 
   * @param nodePath Path of current node
   * @param workspace Workspace name
   * @param sessionProvider SessionProvider
   * @param userName Logged in user
   * @param byUser show documents by current user or by all users
   * @param isLimit indicate that the limitation of query is enabled or not
   * @return List<Node>
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception;
  
  /**
   * Get the number of items per category displayed in Timeline view. this is
   * get from initialize parameter of Timeline Service class If less than or
   * equal zero then use default value 5 items per category.
   * 
   * @return
   */
  public int getItemPerTimeline();
}
