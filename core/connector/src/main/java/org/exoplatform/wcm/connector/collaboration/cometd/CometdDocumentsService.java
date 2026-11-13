package org.exoplatform.wcm.connector.collaboration.cometd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import javax.jcr.RepositoryException;

import org.cometd.annotation.Param;
import org.cometd.annotation.server.ServerAnnotationProcessor;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.annotation.Listener;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.BayeuxServer.ChannelListener;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerChannel.SubscriptionListener;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.oort.Oort;
import org.eclipse.jetty.util.component.LifeCycle;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class CometdDocumentsService.
 */
public class CometdDocumentsService implements Startable {

  /**
   * Command thread factory adapted from {@link Executors#DefaultThreadFactory}.
   */
  static class CommandThreadFactory implements ThreadFactory {

    /** The group. */
    final ThreadGroup   group;

    /** The thread number. */
    final AtomicInteger threadNumber = new AtomicInteger(1);

    /** The name prefix. */
    final String        namePrefix;

    /**
     * Instantiates a new command thread factory.
     *
     * @param namePrefix the name prefix
     */
    CommandThreadFactory(String namePrefix) {
      SecurityManager s = System.getSecurityManager();
      this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      this.namePrefix = namePrefix;
    }

    /**
     * New thread.
     *
     * @param r the r
     * @return the thread
     */
    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0) {

        /**
         * {@inheritDoc}
         */
        @Override
        protected void finalize() throws Throwable {
          super.finalize();
          threadNumber.decrementAndGet();
        }

      };
      if (t.isDaemon()) {
        t.setDaemon(false);
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }

  /**
   * The Class ContainerCommand.
   */
  abstract class ContainerCommand implements Runnable {

    /** The container name. */
    final String containerName;

    /**
     * Instantiates a new container command.
     *
     * @param containerName the container name
     */
    ContainerCommand(String containerName) {
      this.containerName = containerName;
    }

    /**
     * Execute actual work of the commend (in extending class).
     *
     * @param exoContainer the exo container
     */
    abstract void execute(ExoContainer exoContainer);

    /**
     * Callback to execute on container error.
     *
     * @param error the error
     */
    abstract void onContainerError(String error);

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      // Do the work under eXo container context (for proper work of eXo apps
      // and JPA storage)
      ExoContainer exoContainer = ExoContainerContext.getContainerByName(containerName);
      if (exoContainer != null) {
        ExoContainer contextContainer = ExoContainerContext.getCurrentContainerIfPresent();
        try {
          // Container context
          ExoContainerContext.setCurrentContainer(exoContainer);
          RequestLifeCycle.begin(exoContainer);
          // do the work here
          execute(exoContainer);
        } finally {
          // Restore context
          RequestLifeCycle.end();
          ExoContainerContext.setCurrentContainer(contextContainer);
        }
      } else {
        onContainerError("Container not found");
      }

    }
  }

  /**
   * The Class EditorsContext holds information about current opened editons (cometd subsriptions).
   * Keeps closed editor timers (created when the last editor closed) that will be executed after the timeout.
   */
  static class EditorsContext {

    /** The editors open on this node. The key is session id, the value - client info. */
    private ConcurrentHashMap<String, ClientInfo>           clients            = new ConcurrentHashMap<>();

    /**
     * The editors open on every node. The key is session id, the value - the
     * node, the session id, the file id and the provider, as
     * {@link #editorValue(String, String, String)} joins them.
     */
    private final ExoCache<String, String>                  editors;

    /** The separator of the parts of an open editor's value. */
    private static final String                             SEPARATOR          = "\n";

    /** The URL that names this node in the cluster, once known. */
    private volatile String                                 node               = "";

    /** The closed editors. The key is client info, value - timer with task (setCurrentProvider to null) */
    private ConcurrentHashMap<ClientInfo, Timer>            closedEditorTimers = new ConcurrentHashMap<>();

    /**
     * @param editors the cache of the editors open on every node
     */
    EditorsContext(ExoCache<String, String> editors) {
      this.editors = editors;
    }

    /**
     * Adds the client.
     *
     * @param sessionId the session id
     * @param fileId the file id
     * @param provider the provider
     * @param workspace the workspace
     */
    public void addClient(String sessionId, String fileId, String provider, String workspace) {
      clients.put(sessionId, new ClientInfo(fileId, workspace, provider));
      editors.put(sessionId, editorValue(sessionId, fileId, provider));
    }

    /**
     * Removes the client.
     *
     * @param sessionId the session id
     * @return the client info
     */
    public ClientInfo removeClient(String sessionId) {
      ClientInfo clientInfo = clients.remove(sessionId);
      if (clientInfo != null) {
        editors.remove(sessionId);
      }
      return clientInfo;
    }

    /**
     * Removes the editors open on this node from the editors of every node, as
     * the node stops
     */
    public void removeClients() {
      for (String sessionId : clients.keySet()) {
        removeClient(sessionId);
      }
    }

    /**
     * Names this node, and removes the editors a previous run of it left. The
     * cluster's cache template does not transfer a cache's state to a node
     * that joins: the node starts with no editor, and learns those of the
     * other nodes as each of them puts its editors again when it sees the
     * node join. A previous run's editors are removed by the other nodes as
     * they see that run leave; the removal here covers a cache configured to
     * transfer its state.
     *
     * @param node the URL that names this node in the cluster
     */
    public void joinCluster(String node) {
      this.node = node;
      removeNode(node);
      putClients();
    }

    /**
     * Puts the editors open on this node again, for a node that joins: it
     * starts with no editor, or removed them when it saw this node leave
     */
    public void putClients() {
      clients.forEach((sessionId, client) -> editors.put(sessionId,
                                                         editorValue(sessionId, client.getFileId(), client.getProvider())));
    }

    /**
     * Puts again the editors open on this node that the cache no longer
     * holds: another node removed them, seeing this node leave while it was
     * up, or seeing a previous run of it leave once this run had put them
     */
    public void restoreClients() {
      clients.forEach((sessionId, client) -> {
        if (editors.get(sessionId) == null) {
          editors.put(sessionId, editorValue(sessionId, client.getFileId(), client.getProvider()));
        }
      });
    }

    /**
     * Removes the editors open on a node that left the cluster, or that a
     * previous run of a node left
     *
     * @param leftNode the URL that names the node
     */
    public void removeNode(String leftNode) {
      for (String value : cachedValues()) {
        String[] parts = value.split(SEPARATOR, 3);
        if (parts.length == 3 && parts[0].equals(leftNode) && !clients.containsKey(parts[1])) {
          editors.remove(parts[1]);
        }
      }
    }

    /**
     * Gets the opened editors count.
     *
     * @param fileId the file id
     * @param provider the provider
     * @return the number of editors of the file with the provider open on
     *         every node
     */
    public int getOpenedEditorsCount(String fileId, String provider) {
      String editor = fileId + "/" + provider;
      try {
        return (int) editors.getCachedObjects().stream().filter(value -> {
          String[] parts = value.split(SEPARATOR, 3);
          return parts.length == 3 && parts[2].equals(editor);
        }).count();
      } catch (Exception e) {
        // Counted as open: the current editor stays, and the next close resets it
        LOG.warn("Cannot count the editors open on every node, counting the document as still open", e);
        return 1;
      }
    }

    private List<String> cachedValues() {
      try {
        return new ArrayList<>(editors.getCachedObjects());
      } catch (Exception e) {
        LOG.warn("Cannot read the editors open on every node", e);
        return Collections.emptyList();
      }
    }

    private String editorValue(String sessionId, String fileId, String provider) {
      return node + SEPARATOR + sessionId + SEPARATOR + fileId + "/" + provider;
    }

    /**
     * Cancel closed editor timer.
     *
     * @param fileId the file id
     * @param workspace the workspace
     * @param provider the provider
     */
    public void cancelClosedEditorTimer(String fileId, String workspace, String provider) {
      ClientInfo clientInfo = new ClientInfo(fileId, workspace, provider);
      Timer timer = closedEditorTimers.get(clientInfo);
      if (timer != null) {
        timer.cancel();
        closedEditorTimers.remove(clientInfo);
      }
    }

    /**
     * Sets the closed editor timer.
     *
     * @param fileId the file id
     * @param workspace the workspace
     * @param provider the provider
     * @param timer the timer
     */
    public void saveClosedEditorTimer(String fileId, String workspace, String provider, Timer timer) {
      ClientInfo clientInfo = new ClientInfo(fileId, workspace, provider);
      closedEditorTimers.put(clientInfo, timer);
    }
  }

  /**
   * The Class ClientInfo.
   */
  static class ClientInfo {

    /** The workspace. */
    private final String workspace;

    /** The provider. */
    private final String provider;

    /** The file id. */
    private final String fileId;

    /**
     * Instantiates a new client info.
     *
     * @param fileId the fileId
     * @param workspace the workspace
     * @param provider the provider
     */
    public ClientInfo(String fileId, String workspace, String provider) {
      this.fileId = fileId;
      this.workspace = workspace;
      this.provider = provider;
    }

    /**
     * Gets the workspace.
     *
     * @return the workspace
     */
    public String getWorkspace() {
      return workspace;
    }

    /**
     * Gets the provider.
     *
     * @return the provider
     */
    public String getProvider() {
      return provider;
    }

    /**
     * Gets the fileId.
     *
     * @return the fileId
     */
    public String getFileId() {
      return fileId;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
      result = prime * result + ((provider == null) ? 0 : provider.hashCode());
      result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
      return result;
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ClientInfo other = (ClientInfo) obj;
      if (fileId == null) {
        if (other.fileId != null)
          return false;
      } else if (!fileId.equals(other.fileId))
        return false;
      if (provider == null) {
        if (other.provider != null)
          return false;
      } else if (!provider.equals(other.provider))
        return false;
      if (workspace == null) {
        if (other.workspace != null)
          return false;
      } else if (!workspace.equals(other.workspace))
        return false;
      return true;
    }

  }

  /**
   * The listener interface for receiving channelSubscription events.
   * The class that is interested in processing a channelSubscription
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChannelSubscriptionListener<code> method. When
   * the channelSubscription event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChannelSubscriptionEvent
   */
  class ChannelSubscriptionListener implements SubscriptionListener, ConfigurableServerChannel.ServerChannelListener.Weak {

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribed(ServerSession remote, ServerChannel channel, ServerMessage message) {
      String channelId = channel.getId();
      if (channelId.startsWith(CHANNEL_NAME)) {
        String sessionId = remote.getId();
        String exoClientId = asString(message.get("exoClientId"));
        String exoContainerName = asString(message.get("exoContainerName"));
        String provider = asString(message.get("provider"));
        String workspace = asString(message.get("workspace"));

        if (provider != null) {
          String fileId = channelId.substring(channelId.lastIndexOf("/") + 1);
          editorsContext.addClient(sessionId, fileId, provider, workspace);
          editorsContext.cancelClosedEditorTimer(fileId, workspace, provider);
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Subscribed: provider: " + provider + ", session:" + sessionId + " (" + exoContainerName + "@"
              + exoClientId + "), channel:" + channelId);
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribed(ServerSession session, ServerChannel channel, ServerMessage message) {
      String channelId = channel.getId();
      if (channelId.startsWith(CHANNEL_NAME)) {
        String sessionId = session.getId();
        String exoClientId = null;
        String exoContainerName = null;

        ClientInfo removedClient = editorsContext.removeClient(sessionId);
        if (removedClient != null) {
          String fileId = removedClient.getFileId();
          String provider = removedClient.getProvider();
          String workspace = removedClient.getWorkspace();
          if (editorsContext.getOpenedEditorsCount(fileId, provider) == 0) {
            try {
              DocumentEditorProvider editorProvider = documentService.getEditorProvider(provider);
              // Postpone reseting current editor provider for specified delay
              Timer timer = new Timer();
              timer.schedule(new TimerTask() {
                @Override
                public void run() {
                  resetCurrentProviderIfClosed(fileId, workspace, provider);
                }
              }, editorProvider.getEditingFinishedDelay());
              editorsContext.saveClosedEditorTimer(fileId, workspace, provider, timer);
            } catch (DocumentEditorProviderNotFoundException e) {
              LOG.error("Cannot find {} editor provider. {}", provider, e.getMessage());
            }
          }
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Unsubscribed: session:" + sessionId + " (" + exoContainerName + "@" + exoClientId + "), channel:"
              + channelId);
        }
      }
    }

  }

  /**
   * The listener interface for receiving client channel events.
   *
   * @see ClientChannelEvent
   */
  class ClientChannelListener implements ChannelListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureChannel(ConfigurableServerChannel channel) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void channelAdded(ServerChannel channel) {
      // Add sub/unsub listener to WebConferencing channel
      final String channelId = channel.getId();
      if (channelId.startsWith(CHANNEL_NAME)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("> Channel added: " + channelId);
        }
        channel.addListener(subscriptionListener);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void channelRemoved(String channelId) {
      if (channelId.startsWith(CHANNEL_NAME) && LOG.isDebugEnabled()) {
        LOG.debug("< Channel removed: " + channelId);
      }
    }
  }

  /** The Constant LOG. */
  private static final Log                    LOG                      = ExoLogger.getLogger(CometdDocumentsService.class);

  /** The channel name. */
  public static final String                  CHANNEL_NAME             = "/eXo/Application/documents/editor/";

  /** The cache of the editors open on every node. */
  public static final String                  EDITORS_CACHE_NAME       = "ecms.DocumentEditors";

  /**
   * Period, in seconds, at which a node puts again those of its editors
   * another node removed.
   */
  public static final long                    RESTORE_PERIOD           = 60;

  /** The channel name. */
  public static final String                  CHANNEL_NAME_PARAMS      = CHANNEL_NAME + "{fileId}";

  /** The document opened event. */
  public static final String                  DOCUMENT_OPENED_EVENT    = "DOCUMENT_OPENED";

  /** The Constant LAST_EDITOR_CLOSED_EVENT. */
  public static final String                  LAST_EDITOR_CLOSED_EVENT = "LAST_EDITOR_CLOSED";

  /** The Constant CURRENT_PROVIDER_INFO. */
  public static final String                  CURRENT_PROVIDER_INFO    = "CURRENT_PROVIDER_INFO";

  /**
   * Base minimum number of threads for document updates thread executors.
   */
  public static final int                     MIN_THREADS              = 2;

  /**
   * Minimal number of threads maximum possible for document updates thread
   * executors.
   */
  public static final int                     MIN_MAX_THREADS          = 4;

  /** Thread idle time for thread executors (in seconds). */
  public static final int                     THREAD_IDLE_TIME         = 120;

  /**
   * Maximum threads per CPU for thread executors of document changes channel.
   */
  public static final int                     MAX_FACTOR               = 20;

  /**
   * Queue size per CPU for thread executors of document updates channel.
   */
  public static final int                     QUEUE_FACTOR             = MAX_FACTOR * 2;

  /**
   * Thread name used for the executor.
   */
  public static final String                  THREAD_PREFIX            = "documents-comet-thread-";

  /** The exo bayeux. */
  protected final EXoContinuationBayeux       exoBayeux;

  /** The service. */
  protected final CometdService               service;

  /** The call handlers. */
  protected final ExecutorService             eventsHandlers;

  /** The document service. */
  protected final DocumentService             documentService;

  /** The identity registry. */
  protected final IdentityRegistry            identityRegistry;

  /** The authenticator. */
  protected final Authenticator               authenticator;

  /** The subscription listener. */
  protected final ChannelSubscriptionListener subscriptionListener     = new ChannelSubscriptionListener();

  /** The channel listener. */
  protected final ClientChannelListener       channelListener          = new ClientChannelListener();

  /** The editors context. */
  protected final EditorsContext              editorsContext;

  /** Puts again the editors of this node another node removed. */
  ScheduledThreadPoolExecutor                 restoreExecutor;

  /**
   * Instantiates the CometdDocumentsService.
   *
   * @param exoBayeux the exoBayeux
   * @param documentService the document service
   * @param identityRegistry the identity registry
   * @param authenticator the authenticator
   * @param cacheService the cache service
   */
  public CometdDocumentsService(EXoContinuationBayeux exoBayeux,
                                DocumentService documentService,
                                IdentityRegistry identityRegistry,
                                Authenticator authenticator,
                                CacheService cacheService) {
    this.exoBayeux = exoBayeux;
    this.editorsContext = new EditorsContext(cacheService.getCacheInstance(EDITORS_CACHE_NAME));
    this.documentService = documentService;
    this.service = new CometdService();
    this.eventsHandlers = createThreadExecutor(THREAD_PREFIX, MAX_FACTOR, QUEUE_FACTOR);
    this.identityRegistry = identityRegistry;
    this.authenticator = authenticator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // A document's channel reaches its editors on every node
    exoBayeux.observeChannel(CHANNEL_NAME + "**");
    // The editors open on a node that leaves are no longer counted
    exoBayeux.onOort(this::joinCluster);
    restoreExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
      Thread thread = new Thread(runnable, "documents-editors-restore");
      thread.setDaemon(true);
      return thread;
    });
    restoreExecutor.scheduleWithFixedDelay(this::restoreEditors, RESTORE_PERIOD, RESTORE_PERIOD, TimeUnit.SECONDS);
    // instantiate processor after the eXo container start, to let
    // start-dependent logic worked before us
    final AtomicReference<ServerAnnotationProcessor> processor = new AtomicReference<>();
    // need initiate process after Bayeux server starts
    exoBayeux.addEventListener(new LifeCycle.Listener() {
      @Override
      public void lifeCycleStarted(LifeCycle event) {
        ServerAnnotationProcessor p = new ServerAnnotationProcessor(exoBayeux);
        processor.set(p);
        p.process(service);
      }

      @Override
      public void lifeCycleStopped(LifeCycle event) {
        ServerAnnotationProcessor p = processor.get();
        if (p != null) {
          p.deprocess(service);
        }
      }

      @Override
      public void lifeCycleStarting(LifeCycle event) {
        // Nothing
      }

      @Override
      public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        // Nothing
      }

      @Override
      public void lifeCycleStopping(LifeCycle event) {
        // Nothing
      }
    });

    if (PropertyManager.isDevelopping()) {
      // This listener not required for work, just for info during development
      exoBayeux.addListener(new BayeuxServer.SessionListener() {
        @Override
        public void sessionRemoved(ServerSession session, ServerMessage message, boolean timedout) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("sessionRemoved: " + session.getId() + " timedout:" + timedout + " channels: "
                + channelsAsString(session.getSubscriptions()));
          }
        }

        @Override
        public void sessionAdded(ServerSession session, ServerMessage message) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("sessionAdded: " + session.getId() + " channels: " + channelsAsString(session.getSubscriptions()));
          }
        }
      });
    }
  }

  /**
   * The CometService is responsible for sending messages to Cometd channels
   * when a document is saved.
   */
  @Service("documents")
  public class CometdService {

    /** The bayeux. */
    @Inject
    private BayeuxServer  bayeux;

    /** The local session. */
    @Session
    private LocalSession  localSession;

    /** The server session. */
    @Session
    private ServerSession serverSession;

    /**
     * Post construct.
     */
    @PostConstruct
    public void postConstruct() {
      bayeux.addListener(channelListener);
    }

    /**
     * Pre destroy.
     */
    @PreDestroy
    public void preDestroy() {
      bayeux.removeListener(channelListener);
    }

    /**
     * Subscribe documents.
     *
     * @param sender the session that published the message
     * @param message the message
     * @param fileId the file id
     * @throws RepositoryException the repository exception
     */
    @Listener(CHANNEL_NAME_PARAMS)
    public void subscribeDocuments(ServerSession sender,
                                   ServerMessage.Mutable message,
                                   @Param("fileId") String fileId) throws RepositoryException {
      if (exoBayeux.isRelayed(sender)) {
        // Handled on the node the client published it to
        return;
      }
      Object objData = message.getData();
      if (!Map.class.isInstance(objData)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Couldn't get data as a map from event");
        }
        return;
      }

      Map<String, Object> data = message.getDataAsMap();
      String type = (String) data.get("type");

      if (type.equals(DOCUMENT_OPENED_EVENT)) {
        String userId = (String) data.get("userId");
        String workspace = (String) data.get("workspace");
        String provider = (String) data.get("provider");
        eventsHandlers.submit(new ContainerCommand(PortalContainer.getCurrentPortalContainerName()) {
          @Override
          void onContainerError(String error) {
            LOG.error("An error has occured in container: {}", containerName);
          }

          @Override
          void execute(ExoContainer exoContainer) {
            try {
              DocumentEditorProvider editorProvider = documentService.getEditorProvider(provider);
              Identity identity = userIdentity(userId);
              boolean allowed = editorProvider.isAvailableForUser(identity);
              List<String> availableProviders = service.getAllAvailableProviders(identity);
              String currentProvider = documentService.getCurrentDocumentProvider(fileId, workspace);
              boolean available = allowed && (currentProvider == null || provider.equals(currentProvider));
              service.sendCurrentProviderInfo(sender, fileId, available, availableProviders);
              if (currentProvider == null) {
                setCurrentDocumentProvider(fileId, workspace, provider);
              }
            } catch (DocumentEditorProviderNotFoundException | RepositoryException e) {
              LOG.error("Cannot send current provider info for fileId: " + fileId + ", workspace: " + workspace, e);
            }
          }
        });
      }
    }

    protected List<String> getAllAvailableProviders(Identity identity) {
      return documentService.getDocumentEditorProviders()
                            .stream()
                            .filter(provider -> provider.isAvailableForUser(identity))
                            .map(DocumentEditorProvider::getProviderName)
                            .collect(Collectors.toList());
    }

    /**
     * Sets the current document provider.
     *
     * @param fileId the file id
     * @param workspace the workspace
     * @param provider the provider
     */
    protected void setCurrentDocumentProvider(String fileId, String workspace, String provider) {
      eventsHandlers.submit(new ContainerCommand(PortalContainer.getCurrentPortalContainerName()) {
        @Override
        void onContainerError(String error) {
          LOG.error("An error has occured in container: {}", containerName);
        }

        @Override
        void execute(ExoContainer exoContainer) {
          try {
            documentService.saveCurrentDocumentProvider(fileId, workspace, provider);
          } catch (RepositoryException e) {
            LOG.error("Cannot set current document provider for fileId: " + fileId + ", workspace: " + workspace, e);
          }
        }
      });
    }

    /**
     * Find or create user identity.
     *
     * @param userId the user id
     * @return the identity can be null if not found and cannot be created via
     *         current authenticator
     */
    protected Identity userIdentity(String userId) {
      Identity userIdentity = identityRegistry.getIdentity(userId);
      if (userIdentity == null) {
        // We create user identity by authenticator, but not register it in the
        // registry
        try {
          if (LOG.isDebugEnabled()) {
            LOG.debug("User identity not registered, trying to create it for: " + userId);
          }
          userIdentity = authenticator.createIdentity(userId);
        } catch (Exception e) {
          LOG.warn("Failed to create user identity: " + userId, e);
        }
      }
      return userIdentity;
    }

    /**
     * Send last editor closed event.
     *
     * @param fileId the file id
     * @param provider the provider
     */
    protected void sendLastEditorClosedEvent(String fileId, String provider) {
      // Created when no session of this node subscribes to it: Oort relays it to
      // the editors of the other nodes
      ServerChannel channel = bayeux.createChannelIfAbsent(CHANNEL_NAME + fileId).getReference();
      if (channel != null) {
        StringBuilder data = new StringBuilder();
        data.append('{');
        data.append("\"type\": \"");
        data.append(LAST_EDITOR_CLOSED_EVENT);
        data.append("\", ");
        data.append("\"fileId\": \"");
        data.append(fileId);
        data.append("\", ");
        data.append("\"provider\": \"");
        data.append(provider);
        data.append("\"}");
        channel.publish(localSession, data.toString(), Promise.noop());
      }
    }

    /**
     * Sends the current provider information to the user who opened the
     * document: it says which providers are available to that user.
     *
     * @param opener the session of the user who opened the document
     * @param fileId the file id
     * @param available whether the opener can open the document with the
     *          provider
     * @param availableProviders the providers available to the opener
     */
    protected void sendCurrentProviderInfo(ServerSession opener,
                                           String fileId,
                                           boolean available,
                                           List<String> availableProviders) {
      String allProviders = null;
      try {
        allProviders = new JsonGeneratorImpl().createJsonArray(availableProviders).toString();
      } catch (JsonException e) {
        LOG.warn("Cannot serialize all available providers to JSON", e);
      }

      StringBuilder data = new StringBuilder();
      data.append('{');
      data.append("\"type\": \"");
      data.append(CURRENT_PROVIDER_INFO);
      data.append("\", ");
      data.append("\"fileId\": \"");
      data.append(fileId);
      data.append("\", ");
      data.append("\"available\": \"");
      data.append(available);
      data.append("\", ");
      data.append("\"allProviders\":");
      data.append(allProviders);
      data.append("}");
      opener.deliver(localSession, CHANNEL_NAME + fileId, data.toString(), Promise.noop());
    }

  }

  /**
   * Channels as string.
   *
   * @param channels the channels
   * @return the string
   */
  protected String channelsAsString(Set<ServerChannel> channels) {
    return channels.stream().map(c -> c.getId()).collect(Collectors.joining(", "));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    if (restoreExecutor != null) {
      restoreExecutor.shutdownNow();
    }
    editorsContext.removeClients();
  }

  /**
   * Puts again the editors of this node that another node removed
   */
  void restoreEditors() {
    try {
      editorsContext.restoreClients();
    } catch (RuntimeException e) {
      LOG.warn("Error while restoring the editors open on this node", e);
    }
  }

  /**
   * Names this node in the counts of open editors, and follows the nodes that
   * leave the cluster and join it
   *
   * @param oort this node's Oort
   */
  void joinCluster(Oort oort) {
    editorsContext.joinCluster(oort.getURL());
    oort.addCometListener(new Oort.CometListener() {
      @Override
      public void cometJoined(Event event) {
        // A node that saw this one leave removed its editors
        editorsContext.putClients();
      }

      @Override
      public void cometLeft(Event event) {
        editorsContext.removeNode(event.getCometURL());
      }
    });
  }

  /**
   * Resets the current provider of a document whose last editor of the
   * provider closed, unless an editor of it opened since, on any node
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @param provider the provider
   */
  void resetCurrentProviderIfClosed(String fileId, String workspace, String provider) {
    if (editorsContext.getOpenedEditorsCount(fileId, provider) > 0) {
      return;
    }
    try {
      String currentProvider = documentService.getCurrentDocumentProvider(fileId, workspace);
      if (currentProvider != null) {
        service.setCurrentDocumentProvider(fileId, workspace, null);
        service.sendLastEditorClosedEvent(fileId, provider);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Last editor closed. Provider" + provider + ", workspace: " + workspace + ", fileId:" + fileId);
        }
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot reset current editor provider for fileId: " + fileId + ", workspace: " + workspace, e);
    }
  }

  /**
   * Gets the cometd server path.
   *
   * @return the cometd server path
   */
  public String getCometdServerPath() {
    return new StringBuilder("/").append(exoBayeux.getCometdContextName()).append("/cometd").toString();
  }

  /**
   * Gets the user token.
   *
   * @param userId the userId
   * @return the token
   */
  public String getUserToken(String userId) {
    return exoBayeux.getUserToken(userId);
  }

  /**
   * Return object if it's String instance or null if it is not.
   *
   * @param obj the obj
   * @return the string or null
   */
  protected String asString(Object obj) {
    if (obj != null && String.class.isAssignableFrom(obj.getClass())) {
      return String.class.cast(obj);
    }
    return null;
  }

  /**
   * Create a new thread executor service.
   *
   * @param threadNamePrefix the thread name prefix
   * @param maxFactor - max processes per CPU core
   * @param queueFactor - queue size per CPU core
   * @return the executor service
   */
  protected ExecutorService createThreadExecutor(String threadNamePrefix, int maxFactor, int queueFactor) {
    // Executor will queue all commands and run them in maximum set of threads.
    // Minimum set of threads will be
    // maintained online even idle, other inactive will be stopped in two
    // minutes.
    final int cpus = Runtime.getRuntime().availableProcessors();
    int poolThreads = cpus / 4;
    poolThreads = poolThreads < MIN_THREADS ? MIN_THREADS : poolThreads;
    int maxThreads = Math.round(cpus * 1f * maxFactor);
    maxThreads = maxThreads > 0 ? maxThreads : 1;
    maxThreads = maxThreads < MIN_MAX_THREADS ? MIN_MAX_THREADS : maxThreads;
    int queueSize = cpus * queueFactor;
    queueSize = queueSize < queueFactor ? queueFactor : queueSize;
    if (LOG.isDebugEnabled()) {
      LOG.debug("Creating thread executor " + threadNamePrefix + "* for " + poolThreads + ".." + maxThreads
          + " threads, queue size " + queueSize);
    }
    return new ThreadPoolExecutor(poolThreads,
                                  maxThreads,
                                  THREAD_IDLE_TIME,
                                  TimeUnit.SECONDS,
                                  new LinkedBlockingQueue<Runnable>(queueSize),
                                  new CommandThreadFactory(threadNamePrefix),
                                  new ThreadPoolExecutor.CallerRunsPolicy());
  }

}
