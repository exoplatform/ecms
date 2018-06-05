/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms.watch;

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

import org.picocontainer.Startable;

import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.cms.watch.impl.MessageConfig;
import org.exoplatform.services.cms.watch.impl.MessageConfigPlugin;
import org.exoplatform.services.cms.watch.impl.WatchDocumentServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * This is a COPY of ECMS's {@link WatchDocumentServiceImpl} with proposed fix
 * of https://jira.exoplatform.org/browse/ECMS-5973.
 */
public class WatchCloudDocumentServiceImpl implements WatchDocumentService, Startable {

  /** The Constant EXO_WATCHABLE_MIXIN. */
  final public static String  EXO_WATCHABLE_MIXIN   = "exo:watchable";

  /** The Constant EMAIL_WATCHERS_PROP. */
  final public static String  EMAIL_WATCHERS_PROP   = "exo:emailWatcher";

  /** The Constant RSS_WATCHERS_PROP. */
  final public static String  RSS_WATCHERS_PROP     = "exo:rssWatcher";

  /** The Constant WATCHABLE_MIXIN_QUERY. */
  final private static String WATCHABLE_MIXIN_QUERY = "//element(*,exo:watchable)";

  /** The Constant CLOUDDRIVE_WATCH_LINK. */
  final public static String  CLOUDDRIVE_WATCH_LINK = "ecd:watchLink";

  /** The repo service. */
  private RepositoryService   repoService_;

  /** The message config. */
  private MessageConfig       messageConfig_;

  /** The template service. */
  private TemplateService     templateService_;

  /** The Constant LOG. */
  private static final Log    LOG                   = ExoLogger.getLogger(WatchCloudDocumentServiceImpl.class.getName());

  /**
   * Constructor Method.
   *
   * @param params the params
   * @param repoService the repo service
   * @param templateService the template service
   */
  public WatchCloudDocumentServiceImpl(InitParams params, RepositoryService repoService, TemplateService templateService) {
    repoService_ = repoService;
    templateService_ = templateService;
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
    NodeType[] mixinTypes = documentNode.getMixinNodeTypes();
    NodeType watchableMixin = null;
    if (mixinTypes.length > 0) {
      for (NodeType nodeType : mixinTypes) {
        if (nodeType.getName().equalsIgnoreCase(EXO_WATCHABLE_MIXIN)) {
          watchableMixin = nodeType;
          break;
        }
      }
    }
    if (watchableMixin == null)
      return -1;
    boolean notifyByEmail = checkNotifyTypeOfWatcher(documentNode, userName, EMAIL_WATCHERS_PROP);
    boolean notifyByRss = checkNotifyTypeOfWatcher(documentNode, userName, RSS_WATCHERS_PROP);
    if (notifyByEmail && notifyByRss)
      return FULL_NOTIFICATION;
    if (notifyByEmail)
      return NOTIFICATION_BY_EMAIL;
    if (notifyByRss)
      return NOTIFICATION_BY_RSS;
    return -1;
  }

  /**
   * {@inheritDoc}
   */
  public void watchDocument(Node documentNode, String userName, int notifyType) throws Exception {
    Value newWatcher = documentNode.getSession().getValueFactory().createValue(userName);
    EmailNotifyCloudDocumentListener listener = null;
    if (!documentNode.isNodeType(EXO_WATCHABLE_MIXIN)) {
      documentNode.addMixin(EXO_WATCHABLE_MIXIN);
      if (notifyType == NOTIFICATION_BY_EMAIL) {
        documentNode.setProperty(EMAIL_WATCHERS_PROP, new Value[] { newWatcher });
        listener = new EmailNotifyCloudDocumentListener(documentNode);
        observeNode(documentNode, listener);
      }
    } else {
      List<Value> watcherList = new ArrayList<Value>();
      if (notifyType == NOTIFICATION_BY_EMAIL) {
        listener = new EmailNotifyCloudDocumentListener(documentNode);
        if (documentNode.hasProperty(EMAIL_WATCHERS_PROP)) {
          for (Value watcher : documentNode.getProperty(EMAIL_WATCHERS_PROP).getValues()) {
            watcherList.add(watcher);
          }
          watcherList.add(newWatcher);
        }

        documentNode.setProperty(EMAIL_WATCHERS_PROP, watcherList.toArray(new Value[watcherList.size()]));
      }
    }

    if (listener != null && documentNode.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)
        && !documentNode.hasProperty(CLOUDDRIVE_WATCH_LINK)) {
      // init Cloud Drive document with document URI in portal and store this
      // URI
      documentNode.setProperty(CLOUDDRIVE_WATCH_LINK, listener.getViewableLink());
    }

    documentNode.save();
  }

  /**
   * {@inheritDoc}
   */
  public void unwatchDocument(Node documentNode, String userName, int notificationType) throws Exception {
    if (!documentNode.isNodeType(EXO_WATCHABLE_MIXIN))
      return;
    if (notificationType == NOTIFICATION_BY_EMAIL) {
      Value[] watchers = documentNode.getProperty(EMAIL_WATCHERS_PROP).getValues();
      List<Value> watcherList = new ArrayList<Value>();
      for (Value watcher : watchers) {
        if (!watcher.getString().equals(userName)) {
          watcherList.add(watcher);
        }
      }
      documentNode.setProperty(EMAIL_WATCHERS_PROP, watcherList.toArray(new Value[watcherList.size()]));
    }

    if (documentNode.isNodeType(JCRLocalCloudDrive.ECD_CLOUDFILE)) {
      documentNode.setProperty(CLOUDDRIVE_WATCH_LINK, (String) null);
    }

    documentNode.save();
  }

  /**
   * This method will observes the specification node by giving the following
   * param : listener, node Its add an event listener to this node to observes
   * anything that changes to this.
   *
   * @param node Specify the node to observe
   * @param listener The object of EventListener
   * @throws Exception the exception
   * @see EventListener
   * @see Node
   */
  private void observeNode(Node node, EventListener listener) throws Exception {
    String workspace = node.getSession().getWorkspace().getName();
    Session systemSession = repoService_.getCurrentRepository().getSystemSession(workspace);
    List<String> list = getDocumentNodeTypes(node);
    String[] observedNodeTypeNames = list.toArray(new String[list.size()]);
    ObservationManager observationManager = systemSession.getWorkspace().getObservationManager();
    observationManager.addEventListener(listener,
                                        Event.PROPERTY_CHANGED,
                                        node.getPath(),
                                        true,
                                        null,
                                        observedNodeTypeNames,
                                        false);
    systemSession.logout();
  }

  /**
   * This method will check notify type of watcher, userName is equal value of
   * property with notification type.
   *
   * @param documentNode specify a node to watch
   * @param userName userName to watch a document
   * @param notificationType the notification type
   * @return boolean
   * @throws Exception the exception
   */
  private boolean checkNotifyTypeOfWatcher(Node documentNode, String userName, String notificationType) throws Exception {
    if (documentNode.hasProperty(notificationType)) {
      Value[] watchers = documentNode.getProperty(notificationType).getValues();
      for (Value value : watchers) {
        if (userName.equalsIgnoreCase(value.getString()))
          return true;
      }
    }
    return false;
  }

  /**
   * This method will get all node types of node.
   *
   * @param node the node
   * @return the document node types
   * @throws Exception the exception
   */
  private List<String> getDocumentNodeTypes(Node node) throws Exception {
    List<String> nodeTypeNameList = new ArrayList<String>();
    NodeType primaryType = node.getPrimaryNodeType();
    if (templateService_.isManagedNodeType(primaryType.getName())) {
      nodeTypeNameList.add(primaryType.getName());
    }
    for (NodeType nodeType : node.getMixinNodeTypes()) {
      if (templateService_.isManagedNodeType(nodeType.getName())) {
        nodeTypeNameList.add(nodeType.getName());
      }
    }
    return nodeTypeNameList;
  }

  /**
   * This method will re-observer all nodes that have been ever observed with
   * all repositories.
   *
   * @throws Exception the exception
   */
  private void reInitObserver() throws Exception {
    RepositoryEntry repo = repoService_.getCurrentRepository().getConfiguration();
    ManageableRepository repository = repoService_.getCurrentRepository();
    String[] workspaceNames = repository.getWorkspaceNames();
    for (String workspace : workspaceNames) {
      Session session = repository.getSystemSession(workspace);
      QueryManager queryManager = null;
      try {
        queryManager = session.getWorkspace().getQueryManager();
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
      if (queryManager == null) {
        session.logout();
        continue;
      }
      try {
        Query query = queryManager.createQuery(WATCHABLE_MIXIN_QUERY, Query.XPATH);
        QueryResult queryResult = query.execute();
        for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
          Node observedNode = iter.nextNode();
          EmailNotifyCloudDocumentListener emailNotifyListener = new EmailNotifyCloudDocumentListener(observedNode);
          ObservationManager manager = session.getWorkspace().getObservationManager();
          List<String> list = getDocumentNodeTypes(observedNode);
          String[] observedNodeTypeNames = list.toArray(new String[list.size()]);
          manager.addEventListener(emailNotifyListener,
                                   Event.PROPERTY_CHANGED,
                                   observedNode.getPath(),
                                   true,
                                   null,
                                   observedNodeTypeNames,
                                   false);
        }
        session.logout();
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("==>>> Cannot init observer for node: " + e.getLocalizedMessage() + " in '" + repo.getName() + "' repository");
        }
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
    }
  }

  /**
   * This method will get message configuration when a node is observing and
   * there is some changes with it's properties.
   * 
   * @return MessageCongig
   */
  protected MessageConfig getMessageConfig() {
    return messageConfig_;
  }

  /**
   * using for re-observer.
   */
  public void start() {
    try {
      reInitObserver();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("==>>> Exeption when startd WatchDocumentSerice!!!!");
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }
}
