/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.comments.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class CommentsServiceImpl implements CommentsService {

  private static final Log LOG = ExoLogger.getLogger(CommentsServiceImpl.class.getName());
  private static final String CACHE_NAME = "ecms.CommentsService" ;

  private final static String COMMENTS = "comments" ;
  private final static String COMMENTABLE = "mix:commentable" ;
  private final static String EXO_COMMENTS = "exo:comments" ;
  private final static String NT_UNSTRUCTURE = "nt:unstructured" ;
  private final static String MESSAGE = "exo:commentContent" ;
  private final static String COMMENTOR = "exo:commentor" ;
  private final static String COMMENTOR_FULLNAME = "exo:commentorFullName" ;
  private final static String COMMENTOR_EMAIL = "exo:commentorEmail" ;
  private final static String COMMENTOR_SITE = "exo:commentorSite" ;
  private final static String CREATED_DATE = "exo:commentDate" ;
  private static final String LANGUAGES = "languages" ;
  private static final String ANONYMOUS = "anonymous" ;

  private ExoCache<String, List<Node>> commentsCache_ ;
  private MultiLanguageService         multiLangService_ ;
  private ListenerService              listenerService;
  private ActivityCommonService        activityService;
  /**
   * Constructor Method
   * @param cacheService        CacheService Object
   * @param multiLangService    MultiLanguageService Object
   */
  public CommentsServiceImpl(CacheService cacheService,
                             MultiLanguageService multiLangService) throws Exception {
    commentsCache_ = cacheService.getCacheInstance(CACHE_NAME) ;
    multiLangService_ = multiLangService ;
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
  }

  /**
   * {@inheritDoc}
   */
  public void addComment(Node node, String commentor,String email, String site, String comment,String language)
      throws Exception {
    if (listenerService==null) {
      listenerService = WCMCoreUtils.getService(ListenerService.class);
    }
    Session session = node.getSession();
    try {
      Node document = (Node)session.getItem(node.getPath());
      if(!document.isNodeType(COMMENTABLE)) {
        if(document.canAddMixin(COMMENTABLE)) document.addMixin(COMMENTABLE) ;
        else throw new Exception("This node does not support comments.") ;
      }
      Node multiLanguages =null, languageNode= null, commentNode = null ;

      if(!document.hasNode(LANGUAGES) || language.equals(multiLangService_.getDefault(document))) {
        if(document.hasNode(COMMENTS)) commentNode = document.getNode(COMMENTS) ;
        else {
          commentNode = document.addNode(COMMENTS,NT_UNSTRUCTURE) ;          
          commentNode.addMixin("exo:hiddenable");
        }
      } else {
        multiLanguages = document.getNode(LANGUAGES) ;
        if(multiLanguages.hasNode(language)) {
          languageNode = multiLanguages.getNode(language) ;
        } else {
          languageNode = multiLanguages.addNode(language) ;
        }
        if(languageNode.hasNode(COMMENTS)) {
          commentNode = languageNode.getNode(COMMENTS) ;
        } else{
          commentNode = languageNode.addNode(COMMENTS,NT_UNSTRUCTURE) ;
          commentNode.addMixin("exo:hiddenable");
        }
      }

      if(commentor == null || commentor.length() == 0) {
        commentor = ANONYMOUS ;
      }      

      Calendar commentDate = new GregorianCalendar() ;
      String name = Long.toString(commentDate.getTimeInMillis()) ;
      Node newComment = commentNode.addNode(name,EXO_COMMENTS) ;
      newComment.setProperty(COMMENTOR,commentor) ;

      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(commentor);
     
      if(user == null)
        newComment.setProperty(COMMENTOR_FULLNAME,"ANONYMOUS") ;
      else {
        String fullName = user.getDisplayName();
        if(fullName == null) fullName = user.getUserName();
        newComment.setProperty(COMMENTOR_FULLNAME,fullName) ; 
      }
      newComment.setProperty(CREATED_DATE,commentDate) ;
      newComment.setProperty(MESSAGE,comment) ;
      if(email!=null && email.length()>0) {
        newComment.setProperty(COMMENTOR_EMAIL,email) ;
      }
      if(site !=null && site.length()>0) {
        newComment.setProperty(COMMENTOR_SITE,site) ;
      }
      document.save();
      session.save();
      if (listenerService!=null) {
        try {
          if (activityService.isAcceptedNode(document) 
              || (document.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) 
                  && activityService.isBroadcastNTFileEvents(document))) {
            listenerService.broadcast(ActivityCommonService.COMMENT_ADDED_ACTIVITY, document, newComment);
          }
        } catch (Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Can not notify CommentAddedActivity because of: " + e.getMessage());
          }
        }
      }
      commentsCache_.remove(commentNode.getPath()) ;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem happen when try to add comment", e);
      }
    }

  }

  /**
   * {@inheritDoc}
   */
  public void updateComment(Node commentNode, String newComment) throws Exception {
    Calendar commentDate = new GregorianCalendar() ;
    commentNode.setProperty(CREATED_DATE, commentDate);
    commentNode.setProperty(MESSAGE, newComment);
    commentNode.save();
    Node documentNode = commentNode.getParent().getParent();
    if (listenerService!=null && activityService!=null) {
      try {
        if (activityService.isAcceptedNode(documentNode) || 
            (documentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) && 
                activityService.isBroadcastNTFileEvents(documentNode))) {
          listenerService.broadcast(ActivityCommonService.COMMENT_UPDATED_ACTIVITY, documentNode, commentNode);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not notify CommentModifiedActivity because of: " + e.getMessage());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void deleteComment(Node commentNode) throws Exception {
    Node document = commentNode.getParent();
    String activityID;
    try {
      activityID = ActivityTypeUtils.getActivityId(commentNode);
    }catch (Exception e) {
      activityID = null;
    }
    commentNode.remove();
    document.save();    
    if (listenerService!=null && activityID !=null && activityService !=null) {
      Node parentNode = document.getParent();
      try {
        if (activityService.isAcceptedNode(parentNode) || 
            (parentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE) && 
                activityService.isBroadcastNTFileEvents(parentNode))) {
          listenerService.broadcast(ActivityCommonService.COMMENT_REMOVED_ACTIVITY, parentNode, activityID);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can not notify CommentRemovedActivity because of: " + e.getMessage());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public List<Node> getComments(Node document,String language) throws Exception {
    Node commentsNode = null ;
    Node languagesNode = null ;
    Node languageNode = null ;
    if(!isSupportedLocalize(document,language)) {
      if(document.hasProperty("exo:language")) language = document.getProperty("exo:language").getString() ;
    }
    if(document.hasNode(LANGUAGES)) {
      languagesNode = document.getNode(LANGUAGES) ;
      if(languagesNode.hasNode(language)) {
        languageNode = languagesNode.getNode(language) ;
        if(languageNode.hasNode(COMMENTS)) commentsNode = languageNode.getNode(COMMENTS) ;
      } else if(language.equals(multiLangService_.getDefault(document))) {
        languageNode = document ;
      }
    } else {
      languageNode = document ;
    }
    if(!languageNode.hasNode(COMMENTS)) return new ArrayList<Node>() ;
    Session session = document.getSession();
    //TODO check if really need delegate to system session
    Session systemSession = WCMCoreUtils.getSystemSessionProvider().getSession(session.getWorkspace().getName(),
                                                                               WCMCoreUtils.getRepository()) ;
    List<Node> list = new ArrayList<Node>() ;
    try {
      commentsNode = (Node)systemSession.getItem(languageNode.getPath() + "/" + COMMENTS) ;
      String cacheKey = document.getPath().concat(commentsNode.getPath());
      Object comments = commentsCache_.get(cacheKey) ;
      if(comments !=null) return (List<Node>)comments ;
      for(NodeIterator iter = commentsNode.getNodes(); iter.hasNext();) {
        list.add(iter.nextNode()) ;
      }
      Collections.sort(list,new DateComparator()) ;
      commentsCache_.put(commentsNode.getPath(),list) ;
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem happen when try to get comments", e);
      }
    }
    return list;
  }


  /**
   * This Class implements Comparator<Node> to compare the created date of nodes.
   */
  private class DateComparator implements Comparator<Node> {

    /**
     * Compare the created date of nodes
     * @param node1     node is used to compare
     * @param node2     node is used to compare
     */
    public int compare(Node node1, Node node2) {
      try{
        Date date1 = node1.getProperty(CREATED_DATE).getDate().getTime() ;
        Date date2 = node2.getProperty(CREATED_DATE).getDate().getTime() ;
        return date2.compareTo(date1) ;
      }catch (Exception e) {
        return 0;
      }
    }
  }

  /**
   * Check language of comment is supported in a document
   * @param  document    The document node is commented
   * @param  language    The language of comment node
   * @throws Exception
   */
  private boolean isSupportedLocalize(Node document,String language)throws Exception {
    List<String> locales= multiLangService_.getSupportedLanguages(document) ;
    if(Collections.frequency(locales,language) >0) return true ;
    return false ;
  }

}
