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
package org.exoplatform.services.cms.timeline.impl;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.timeline.TimelineService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 22, 2009
 * 8:19:51 AM
 */
public class TimelineServiceImpl implements TimelineService {

  private static final Log LOG = ExoLogger.getLogger(TimelineServiceImpl.class.getName());
  private static final String EXO_DATETIME = "exo:datetime";
  private static final String EXO_MODIFIED_DATE = "exo:dateModified";
  private static final String EXO_OWNER = "exo:owner";
  private static final String SELECT_QUERY = "SELECT * FROM " + EXO_DATETIME + " WHERE ";
  private static final String TIME_FORMAT_TAIL = "T00:00:00.000";
  private static final DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private RepositoryService repositoryService_;
  private TemplateService templateService_;
  private int itemPerTimeline = 5;

  public TimelineServiceImpl(RepositoryService repoService, TemplateService templateService,
      InitParams initParams) throws Exception {
    repositoryService_ = repoService;
    templateService_ = templateService;
    itemPerTimeline = Integer.parseInt(initParams.getValueParam("itemPerTimeline").getValue());
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception {
    return getDocumentsOfEarlierThisYear(nodePath, workspace, sessionProvider, userName, byUser, true);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfEarlierThisYear(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception {

    List<Node> documentsOfYear = new ArrayList<Node>();
    Session session = getSession(sessionProvider, workspace);
    Calendar currentTime = new GregorianCalendar();
    String strBeginningOfThisMonthTime = getStrBeginningOfThisMonthTime(currentTime);
    String strBeginningOfThisYearTime = getStrBeginningOfThisYearTime(currentTime);
    StringBuilder sb = new StringBuilder();
    String pathPattern = buildPathPattern(nodePath);
    sb.append(SELECT_QUERY);
    if (pathPattern.length() > 0) {
      sb.append(pathPattern).append(" AND ");
    }
    sb.append("((" + buildDocumentTypePattern() + ")")
      .append(" OR (jcr:primaryType='exo:symlink' AND (" + buildSymlinkDocumentTypePattern() + ")))")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " >= TIMESTAMP '" + strBeginningOfThisYearTime + "')")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " < TIMESTAMP '" + strBeginningOfThisMonthTime + "')");

    if (byUser) {
      sb.append(" AND ").append(" (" + EXO_OWNER + " = '" + userName + "')");
    }
    sb.append(" ORDER BY ").append(EXO_MODIFIED_DATE);

    QueryResult result = executeQuery(session, sb.toString(), Query.SQL, isLimit);
    NodeIterator nodeIter = result.getNodes();
    while (nodeIter.hasNext()) {
      documentsOfYear.add(nodeIter.nextNode());
    }
    return documentsOfYear;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser) throws Exception {
    return getDocumentsOfEarlierThisMonth(nodePath, workspace, sessionProvider, userName, byUser, true);
  }
  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfEarlierThisMonth(String nodePath,
                                                   String workspace,
                                                   SessionProvider sessionProvider,
                                                   String userName,
                                                   boolean byUser,
                                                   boolean isLimit) throws Exception {

    List<Node> documentsOfMonth = new ArrayList<Node>();
    Session session = getSession(sessionProvider, workspace);
    Calendar currentTime = new GregorianCalendar();
    String strBeginningOfThisWeekTime = getStrBeginningOfThisWeekTime(currentTime);
    String strBeginningOfThisMonthTime = getStrBeginningOfThisMonthTime(currentTime);
    StringBuilder sb = new StringBuilder();
    String pathPattern = buildPathPattern(nodePath);
    sb.append(SELECT_QUERY);
    if (pathPattern.length() > 0) {
      sb.append(pathPattern).append(" AND ");
    }
    sb.append("((" + buildDocumentTypePattern() + ")")
      .append(" OR (jcr:primaryType='exo:symlink' AND (" + buildSymlinkDocumentTypePattern() + ")))")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " >= TIMESTAMP '" + strBeginningOfThisMonthTime + "')")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " < TIMESTAMP '" + strBeginningOfThisWeekTime + "')");

    if (byUser) {
      sb.append(" AND ").append(" (" + EXO_OWNER + " = '" + userName + "')");
    }
    sb.append(" ORDER BY ").append(EXO_MODIFIED_DATE);

    QueryResult result = executeQuery(session, sb.toString(), Query.SQL, isLimit);
    NodeIterator nodeIter = result.getNodes();
    while (nodeIter.hasNext()) {
      documentsOfMonth.add(nodeIter.nextNode());
    }
    return documentsOfMonth;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser) throws Exception {
    return getDocumentsOfEarlierThisWeek(nodePath, workspace, sessionProvider, userName, byUser, true);
  }
  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfEarlierThisWeek(String nodePath,
                                                  String workspace,
                                                  SessionProvider sessionProvider,
                                                  String userName,
                                                  boolean byUser,
                                                  boolean isLimit) throws Exception {

    List<Node> documentsOfWeek = new ArrayList<Node>();
    Session session = getSession(sessionProvider, workspace);
    Calendar currentTime = new GregorianCalendar();
    String strYesterdayTime = getStrYesterdayTime(currentTime);
    String strBeginningOfThisWeekTime = getStrBeginningOfThisWeekTime(currentTime);
    StringBuilder sb = new StringBuilder();
    String pathPattern = buildPathPattern(nodePath);
    sb.append(SELECT_QUERY);
    if(pathPattern.length() > 0) {
      sb.append(pathPattern).append(" AND ");
    }
    sb.append("((" + buildDocumentTypePattern() + ")")
      .append(" OR (jcr:primaryType='exo:symlink' AND (" + buildSymlinkDocumentTypePattern() + ")))")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " >= TIMESTAMP '" + strBeginningOfThisWeekTime + "')")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " < TIMESTAMP '" + strYesterdayTime + "')");

    if (byUser) {
      sb.append(" AND ").append(" (" + EXO_OWNER + " = '" + userName + "')");
    }
    sb.append(" ORDER BY ").append(EXO_MODIFIED_DATE);

    QueryResult result = executeQuery(session, sb.toString(), Query.SQL, isLimit);
    NodeIterator nodeIter = result.getNodes();
    while(nodeIter.hasNext()) {
      documentsOfWeek.add(nodeIter.nextNode());
    }
    return documentsOfWeek;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser) throws Exception {
    return getDocumentsOfYesterday(nodePath, workspace, sessionProvider, userName, byUser, true);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfYesterday(String nodePath,
                                            String workspace,
                                            SessionProvider sessionProvider,
                                            String userName,
                                            boolean byUser,
                                            boolean isLimit) throws Exception {

    List<Node> documentsOfYesterday = new ArrayList<Node>();
    Session session = getSession(sessionProvider, workspace);
    Calendar currentTime = new GregorianCalendar();
    String strTodayTime = getStrTodayTime(currentTime);
    String strYesterdayTime = getStrYesterdayTime(currentTime);
    StringBuilder sb = new StringBuilder();
    String pathPattern = buildPathPattern(nodePath);
    sb.append(SELECT_QUERY);
    if(pathPattern.length() > 0) {
      sb.append(pathPattern).append(" AND ");
    }
    sb.append("((" + buildDocumentTypePattern() + ")")
      .append(" OR (jcr:primaryType='exo:symlink' AND (" + buildSymlinkDocumentTypePattern() + ")))")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " >= TIMESTAMP '" + strYesterdayTime + "')")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " < TIMESTAMP '" + strTodayTime + "')");

    if (byUser) {
      sb.append(" AND ").append(" (" + EXO_OWNER + " = '" + userName + "')");
    }
    sb.append(" ORDER BY ").append(EXO_MODIFIED_DATE);

    QueryResult result = executeQuery(session, sb.toString(), Query.SQL, isLimit);
    NodeIterator nodeIter = result.getNodes();
    while(nodeIter.hasNext()) {
      documentsOfYesterday.add(nodeIter.nextNode());
    }
    return documentsOfYesterday;
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser) throws Exception {
    return getDocumentsOfToday(nodePath, workspace, sessionProvider, userName, byUser, true);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDocumentsOfToday(String nodePath,
                                        String workspace,
                                        SessionProvider sessionProvider,
                                        String userName,
                                        boolean byUser,
                                        boolean isLimit) throws Exception {
    List<Node> documentsOfToday = new ArrayList<Node>();
    Session session = getSession(sessionProvider, workspace);
    Calendar currentTime = new GregorianCalendar();
    String strTodayTime = getStrTodayTime(currentTime);
    StringBuilder sb = new StringBuilder();
    String pathPattern = buildPathPattern(nodePath);
    sb.append(SELECT_QUERY);
    if(pathPattern.length() > 0) {
      sb.append(pathPattern).append(" AND ");
    }
    sb.append("((" + buildDocumentTypePattern() + ")")
      .append(" OR (jcr:primaryType='exo:symlink' AND (" + buildSymlinkDocumentTypePattern() + ")))")
      .append(" AND ")
      .append(" (" + EXO_MODIFIED_DATE + " >= TIMESTAMP '" + strTodayTime + "')");
    if (byUser) {
      sb.append(" AND ").append(" (" + EXO_OWNER + " = '" + userName + "')");
    }
    sb.append(" ORDER BY ").append(EXO_MODIFIED_DATE).append(" DESC");

    QueryResult result = executeQuery(session, sb.toString(), Query.SQL, isLimit);
    NodeIterator nodeIter = result.getNodes();
    while(nodeIter.hasNext()) {
      documentsOfToday.add(nodeIter.nextNode());
    }
    return documentsOfToday;
  }

  private Session getSession(SessionProvider sessionProvider, String workspace) throws RepositoryException {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    return sessionProvider.getSession(workspace, manageableRepository);
  }

  private QueryResult executeQuery(Session session,
                                   String statement,
                                   String language,
                                   boolean isLimt) throws Exception {
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      QueryImpl query = (QueryImpl) queryManager.createQuery(statement, language);
      if (isLimt) {
        query.setLimit(itemPerTimeline);
      }
      return query.execute();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Can not execute query", e);
      }
      return null;
    }
  }

  private String buildDocumentTypePattern() throws Exception {
    List<String> documentFileTypes = templateService_.getAllDocumentNodeTypes();
    StringBuilder sb = new StringBuilder();
    for(String documentType : documentFileTypes) {
      if(sb.length() > 0) sb.append(" OR ");
      sb.append("jcr:primaryType='"+documentType+"'");
    }
    return sb.toString();
  }

  private String buildSymlinkDocumentTypePattern() throws Exception {
    List<String> documentFileTypes = templateService_.getAllDocumentNodeTypes();
    StringBuilder sb = new StringBuilder();
    for(String documentType : documentFileTypes) {
      if(sb.length() > 0) sb.append(" OR ");
      sb.append("exo:primaryType='" + documentType + "'");
    }
    return sb.toString();
  }

  private String getStrTodayTime(Calendar calendar) {
    String currentDate = LocalDateTime.ofInstant(calendar.getTime().toInstant(), calendar.getTimeZone().toZoneId()).format(formatDateTime);
    return currentDate + TIME_FORMAT_TAIL;
  }

  private String buildPathPattern(String nodePath) {
    if(nodePath.equals("/")) return "";
    return "jcr:path LIKE '" + nodePath + "/%" + "'";
  }

  private String getStrYesterdayTime(Calendar calendar) {
    Calendar yesterday = (Calendar)calendar.clone();
    yesterday.add(Calendar.DATE, -1);
    String yesterdayDate = LocalDateTime.ofInstant(yesterday.getTime().toInstant(), yesterday.getTimeZone().toZoneId()).format(formatDateTime);
    return yesterdayDate + TIME_FORMAT_TAIL;
  }

  private String getStrBeginningOfThisWeekTime(Calendar calendar) {
    Calendar monday = (Calendar)calendar.clone();
    while (monday.get(Calendar.WEEK_OF_YEAR) == calendar.get(Calendar.WEEK_OF_YEAR)) {
      monday.add(Calendar.DATE, -1);
    }
    monday.add(Calendar.DATE, 1);
    String mondayDate = LocalDateTime.ofInstant(monday.getTime().toInstant(), monday.getTimeZone().toZoneId()).format(formatDateTime);
    return mondayDate + TIME_FORMAT_TAIL;
  }

  private String getStrBeginningOfThisMonthTime(Calendar calendar) {
    Calendar theFirst = (Calendar)calendar.clone();
    theFirst.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1, 0, 0, 0);
    String theFirstDate = LocalDateTime.ofInstant(theFirst.getTime().toInstant(), theFirst.getTimeZone().toZoneId()).format(formatDateTime);
    return theFirstDate + TIME_FORMAT_TAIL;
  }

  private String getStrBeginningOfThisYearTime(Calendar calendar) {
    Calendar theFirst = (Calendar)calendar.clone();
    theFirst.set(calendar.get(Calendar.YEAR), 0, 1, 0, 0, 0);
    String theFirstDate = LocalDateTime.ofInstant(theFirst.getTime().toInstant(), theFirst.getTimeZone().toZoneId()).format(formatDateTime);
    return theFirstDate + TIME_FORMAT_TAIL;
  }

  /**
   * Get the number of items per category displayed in Timeline view. this is
   * get from initialize parameter of Timeline Service class If less than or
   * equal zero then use default value 5 items per category.
   *
   * @return
   */
  public int getItemPerTimeline() {
    if (itemPerTimeline <= 0) {
      return 5;
    }
    return itemPerTimeline;
  }
}
