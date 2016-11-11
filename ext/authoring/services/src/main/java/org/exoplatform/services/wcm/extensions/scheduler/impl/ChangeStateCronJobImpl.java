package org.exoplatform.services.wcm.extensions.scheduler.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class ChangeStateCronJobImpl implements Job {
  private static final Log LOG                 = ExoLogger.getLogger(ChangeStateCronJobImpl.class.getName());

  private static final String START_TIME_PROPERTY = "publication:startPublishedDate";

  private static final String END_TIME_PROPERTY   = "publication:endPublishedDate";
  
  private static final int NORMAL_NODE = 0;
  
  private static final int STAGED_NODE = 1;
  
  private String              fromState           = null;

  private String              toState             = null;

  private String              predefinedPath      = null;

  private String              workspace           = null;
  private String              contentPath         = null;
  

  public void execute(JobExecutionContext context) throws JobExecutionException {
    SessionProvider sessionProvider = null;
    try {
      RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
      if (mx.getUptime()>120000) {
        if (LOG.isDebugEnabled()) LOG.debug("Start Execute ChangeStateCronJob");
        if (fromState == null) {

          JobDataMap jdatamap = context.getJobDetail().getJobDataMap();

          fromState = jdatamap.getString("fromState");
          toState = jdatamap.getString("toState");
          predefinedPath = jdatamap.getString("predefinedPath");
          String[] pathTab = predefinedPath.split(":");
          workspace = pathTab[0];
          contentPath = pathTab[1];
        }
        if (LOG.isDebugEnabled()) LOG.debug("Start Execute ChangeStateCronJob: change the State from " + fromState + " to "
            + toState);
        
        sessionProvider = SessionProvider.createSystemProvider();
        
        String property = null;
        if ("staged".equals(fromState) && "published".equals(toState)) {
          property = START_TIME_PROPERTY;
        } else if ("published".equals(fromState) && "unpublished".equals(toState)) {
          property = END_TIME_PROPERTY;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Date now = Calendar.getInstance().getTime();
        String currentTime = format.format(now);

        if (property != null) {
          // appends trailing / if missing
          if (contentPath != null) {
            if (!contentPath.endsWith("/")) {
              contentPath += "/";
            }
          }
          
          StringBuilder normalNodesStatement 
               = new StringBuilder().append("select * from nt:base where ").
                                     append("(publication:currentState='").append(fromState).append("') ").
                                     append(" and (").append(property).append(" IS NOT NULL )").
                                     append(" and (").append(property).append(" < TIMESTAMP '").append(currentTime).append("') ").
                                     append(" and (jcr:path like '").append(contentPath).append("%' )");
          
          StringBuilder stagedNodesStatement
               = new StringBuilder().append("select * from nt:base where ").
                                     append("(publication:currentState='").append(fromState).append("') ").
                                     append(" and (").append(property).append(" IS NULL ) "). 
                                     append(" and (jcr:path like '").append(contentPath).append("%' )");
                                     
          long normalCount = changeStateForNodes(sessionProvider, property, NORMAL_NODE, normalNodesStatement.toString());
          long stagedCount = (START_TIME_PROPERTY.equals(property)) ? 
                              changeStateForNodes(sessionProvider, property, STAGED_NODE, stagedNodesStatement.toString()) : 0;
                              
          long numberOfItemsToChange = normalCount + stagedCount;
          
          if (numberOfItemsToChange > 0) {
            if (LOG.isDebugEnabled()) { 
              LOG.debug(numberOfItemsToChange + " '" + fromState + "' candidates for state '" + toState
                + "' found in " + predefinedPath);
            }
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("no '" + fromState + "' content found in " + predefinedPath);
            }
          }
          
        }
        if (LOG.isDebugEnabled()) LOG.debug("End Execute ChangeStateCronJob");
      }

    } catch (RepositoryException ex) {
      if (LOG.isErrorEnabled()) LOG.error("Repository not found. Ignoring :  " + ex.getMessage(), ex);
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) LOG.error("error when changing the state of the content : " + ex.getMessage(), ex);
    } finally {
      if (sessionProvider != null)
        sessionProvider.close();
    }
  }
  
  private long changeStateForNodes(SessionProvider sessionProvider, String property, int nodeType, String statement) 
  throws Exception {
    long ret = 0;
    RepositoryService repositoryService_ = WCMCoreUtils.getService(RepositoryService.class);
    PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class);
    PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins()
    .get(AuthoringPublicationConstant.LIFECYCLE_NAME);
    HashMap<String, String> context_ = new HashMap<String, String>();
    
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    if (manageableRepository == null) {
      if (LOG.isDebugEnabled()) LOG.debug("Repository not found. Ignoring");
      return 0;
    }

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    
    Query query = queryManager.createQuery(statement, Query.SQL);
    QueryResult queryResult = query.execute();
    
    for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
      Node node_ = iter.nextNode();
      
      String path = node_.getPath();
      if (!path.startsWith("/jcr:system")) {
        if (NORMAL_NODE == nodeType) {
          Date nodeDate = node_.getProperty(property).getDate().getTime();
          if (LOG.isInfoEnabled()) LOG.info("'" + toState + "' " + node_.getPath() + " (" + property + "="
              + format.format(nodeDate) + ")");
  
          if (PublicationDefaultStates.UNPUBLISHED.equals(toState)) {
            if (node_.hasProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP)) {
              String liveRevisionProperty = node_.getProperty(AuthoringPublicationConstant.LIVE_REVISION_PROP)
              .getString();
              if (!"".equals(liveRevisionProperty)) {
                Node liveRevision = session.getNodeByUUID(liveRevisionProperty);
                if (liveRevision != null) {
                  context_.put(AuthoringPublicationConstant.CURRENT_REVISION_NAME,
                      liveRevision.getName());
                }
              }
            }
  
          }
          publicationPlugin.changeState(node_, toState, context_);
          ret ++;
        } else {
          if (LOG.isInfoEnabled()) LOG.info("'" + toState + "' " + node_.getPath());
          publicationPlugin.changeState(node_, toState, context_);
        }
        if(START_TIME_PROPERTY.equals(property) && node_.hasProperty(START_TIME_PROPERTY)){
          node_.getProperty(START_TIME_PROPERTY).remove();
          node_.save();
        }
        if(END_TIME_PROPERTY.equals(property) && node_.hasProperty(END_TIME_PROPERTY)){
          node_.getProperty(END_TIME_PROPERTY).remove();
          node_.save();
        }
      }
    }
    
    return ret;
  }
}
