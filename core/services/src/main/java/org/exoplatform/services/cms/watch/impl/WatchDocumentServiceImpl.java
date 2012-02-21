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
package org.exoplatform.services.cms.watch.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;


/**
 * Created by eXo Platform
 * Author : Pham Xuan Hoa
 *          hoapham@exoplatform.com
 * Nov 30, 2006
 */
public class WatchDocumentServiceImpl implements WatchDocumentService, Startable {

  final public static String EXO_WATCHABLE_MIXIN = "exo:watchable" ;
  final public static String EMAIL_WATCHERS_PROP = "exo:emailWatcher" ;
  final public static String RSS_WATCHERS_PROP = "exo:rssWatcher" ;
  final private static String WATCHABLE_MIXIN_QUERY = "//element(*,exo:watchable)" ;

  private RepositoryService repoService_ ;
  private MessageConfig messageConfig_ ;
  private TemplateService templateService_ ;
  private static final Log LOG  = ExoLogger.getLogger(WatchDocumentServiceImpl.class);

  /**
   * Constructor Method
   * @param params
   * @param repoService
   * @param templateService
   */
  public WatchDocumentServiceImpl(InitParams params,
      RepositoryService repoService, TemplateService templateService) {
    repoService_ = repoService ;
    templateService_ = templateService ;
  }
  
  /**
   * {@inheritDoc}
   */
  public void initializeMessageConfig(MessageConfigPlugin msgConfigPlugin) {
    messageConfig_ = msgConfigPlugin.getMessageConfig();
  }

  /**
   * {@inheritDoc}
   */
  public int getNotificationType(Node documentNode, String userName) throws Exception {
    NodeType[] mixinTypes = documentNode.getMixinNodeTypes() ;
    NodeType watchableMixin = null ;
    if(mixinTypes.length>0) {
      for(NodeType nodeType: mixinTypes) {
        if(nodeType.getName().equalsIgnoreCase(EXO_WATCHABLE_MIXIN)) {
          watchableMixin = nodeType ;
          break ;
        }
      }
    }
    if(watchableMixin == null)  return -1 ;
    boolean notifyByEmail = checkNotifyTypeOfWatcher(documentNode,userName,EMAIL_WATCHERS_PROP) ;
    boolean notifyByRss = checkNotifyTypeOfWatcher(documentNode,userName,RSS_WATCHERS_PROP) ;
    if( notifyByEmail && notifyByRss) return FULL_NOTIFICATION ;
    if(notifyByEmail) return NOTIFICATION_BY_EMAIL ;
    if(notifyByRss) return NOTIFICATION_BY_RSS ;
    return -1 ;
  }

  /**
   * {@inheritDoc}
   */
  public void watchDocument(Node documentNode, String userName, int notifyType) throws Exception {
    Session session = documentNode.getSession() ;
    Value newWatcher = session.getValueFactory().createValue(userName) ;
    if(!documentNode.isNodeType(EXO_WATCHABLE_MIXIN)) {
      documentNode.addMixin(EXO_WATCHABLE_MIXIN) ;
      if(notifyType == NOTIFICATION_BY_EMAIL) {
        documentNode.setProperty(EMAIL_WATCHERS_PROP,new Value[] {newWatcher}) ;
        documentNode.save() ;
        session.save() ;
        EmailNotifyListener listener = new EmailNotifyListener(documentNode) ;
        observeNode(documentNode,listener) ;
      }
      session.save() ;
    } else {
      List<Value>  watcherList = new ArrayList<Value>() ;
      if(notifyType == NOTIFICATION_BY_EMAIL) {
        if(documentNode.hasProperty(EMAIL_WATCHERS_PROP)) {
          for(Value watcher : documentNode.getProperty(EMAIL_WATCHERS_PROP).getValues()) {
            watcherList.add(watcher) ;
          }
          watcherList.add(newWatcher) ;
        }

        documentNode.setProperty(EMAIL_WATCHERS_PROP,watcherList.toArray(new Value[watcherList.size()])) ;
        documentNode.save() ;
      }
      session.save() ;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void unwatchDocument(Node documentNode, String userName, int notificationType) throws Exception {
    if(!documentNode.isNodeType(EXO_WATCHABLE_MIXIN)) return  ;
    Session session = documentNode.getSession() ;
    if(notificationType == NOTIFICATION_BY_EMAIL) {
      Value[] watchers = documentNode.getProperty(EMAIL_WATCHERS_PROP).getValues() ;
      List<Value> watcherList = new ArrayList<Value>() ;
      for(Value watcher: watchers) {
        if(!watcher.getString().equals(userName)) {
          watcherList.add(watcher) ;
        }
      }
      documentNode.setProperty(EMAIL_WATCHERS_PROP,watcherList.toArray(new Value[watcherList.size()])) ;
    }
    documentNode.save() ;
    session.save() ;
  }

  /**
   * This method will observes the specification node by giving the following param : listener, node
   * Its add an event listener to this node to observes anything that changes to this
   * @param node              Specify the node to observe
   * @param listener          The object of EventListener
   * @see                     EventListener
   * @see                     Node
   * @throws Exception
   */
  private void observeNode(Node node, EventListener listener) throws Exception {
    String workspace = node.getSession().getWorkspace().getName() ;
    Session systemSession = repoService_.getCurrentRepository().getSystemSession(workspace) ;
    List<String> list = getDocumentNodeTypes(node) ;
    String[] observedNodeTypeNames = list.toArray(new String[list.size()]) ;
    ObservationManager observationManager = systemSession.getWorkspace().getObservationManager() ;
    observationManager.addEventListener(listener,Event.PROPERTY_CHANGED,
        node.getPath(),true,null,observedNodeTypeNames,false) ;
    systemSession.logout();
  }

  /**
   * This method will check notify type of watcher, userName is equal value of property with notification type
   * @param documentNode    specify a node to watch
   * @param userName        userName to watch a document
   * @param notification    Notification Type
   * @return boolean
   * @throws Exception
   */
  private boolean checkNotifyTypeOfWatcher(Node documentNode, String userName,String notificationType) throws Exception {
    if(documentNode.hasProperty(notificationType)) {
      Value [] watchers = documentNode.getProperty(notificationType).getValues() ;
      for(Value value: watchers) {
        if(userName.equalsIgnoreCase(value.getString())) return true ;
      }
    }
    return false ;
  }

  /**
   * This method will get all node types of node.
   * @param node
   * @return
   * @throws Exception
   */
  private List<String> getDocumentNodeTypes(Node node) throws Exception {
    List<String> nodeTypeNameList = new ArrayList<String>() ;
    NodeType  primaryType = node.getPrimaryNodeType() ;
    if(templateService_.isManagedNodeType(primaryType.getName())) {
      nodeTypeNameList.add(primaryType.getName()) ;
    }
    for(NodeType nodeType: node.getMixinNodeTypes()) {
      if(templateService_.isManagedNodeType(nodeType.getName())) {
        nodeTypeNameList.add(nodeType.getName()) ;
      }
    }
    return nodeTypeNameList ;
  }

  /**
   * This method will re-observer all nodes that have been ever observed with all repositories.
   * @throws Exception
   */
  private void reInitObserver() throws Exception {
    RepositoryEntry repo = repoService_.getCurrentRepository().getConfiguration();
    ManageableRepository repository = repoService_.getCurrentRepository();
    String[] workspaceNames = repository.getWorkspaceNames() ;
    for(String workspace: workspaceNames) {
      Session session = repository.getSystemSession(workspace) ;
      QueryManager queryManager = null ;
      try{
        queryManager = session.getWorkspace().getQueryManager() ;
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
      if(queryManager == null) {
        session.logout();
        continue ;
      }
      try {
        Query query = queryManager.createQuery(WATCHABLE_MIXIN_QUERY,Query.XPATH) ;
        QueryResult queryResult = query.execute() ;
        for(NodeIterator iter = queryResult.getNodes(); iter.hasNext(); ) {
          Node observedNode = iter.nextNode() ;
          EmailNotifyListener emailNotifyListener = new EmailNotifyListener(observedNode) ;
          ObservationManager manager = session.getWorkspace().getObservationManager() ;
          List<String> list = getDocumentNodeTypes(observedNode) ;
          String[] observedNodeTypeNames = list.toArray(new String[list.size()]) ;
          manager.addEventListener(emailNotifyListener,Event.PROPERTY_CHANGED,
              observedNode.getPath(),true,null,observedNodeTypeNames,false) ;
        }
        session.logout();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("==>>> Cannot init observer for node: "
            +e.getLocalizedMessage() + " in '"+repo.getName()+"' repository");
        }
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
    }
  }

  /**
   * This method will get message configuration when a node is observing and there is some changes
   * with it's properties.
   * @return MessageCongig
   */
  protected MessageConfig getMessageConfig() { return messageConfig_ ; }

  /**
   * using for re-observer
   */
  public void start() {
    try {
      reInitObserver() ;
    }catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("==>>> Exeption when startd WatchDocumentSerice!!!!");
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() { }
}
