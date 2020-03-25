package org.exoplatform.services.cms.documents.cometd;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.cometd.annotation.Param;
import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.annotation.Subscription;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.eclipse.jetty.util.component.LifeCycle;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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
   * The Class ProvidersInfo.
   */
  static class ProvidersInfo {

    /** The active providers. */
    private ConcurrentHashMap<String, AtomicInteger> activeProviders = new ConcurrentHashMap<>();

    /**
     * Gets the opened editors.
     *
     * @param provider the provider
     * @return the opened editors
     */
    public int getOpenedEditorsCount(String provider) {
      AtomicInteger count = activeProviders.get(provider);
      return count != null ? count.get() : 0;
    }

    /**
     * Adds the opened editor.
     *
     * @param provider the provider
     */
    public void addOpenedEditor(String provider) {
      activeProviders.compute(provider, (key, count) -> {
        if (count == null) {
          return new AtomicInteger(1);
        } else {
          count.incrementAndGet();
          return count;
        }
      });
    }

    /**
     * Adds the closed editor.
     *
     * @param provider the provider
     */
    public void addClosedEditor(String provider) {
      activeProviders.compute(provider, (key, count) -> {
        if (count == null) {
          return new AtomicInteger(0);
        } else {
          count.decrementAndGet();
          return count;
        }
      });
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
        // LOG.warn("Container not found " + containerName + " for remote call "
        // + contextName);
        onContainerError("Container not found");
      }

    }

  }

  /** The Constant LOG. */
  private static final Log                                 LOG                      =
                                                               ExoLogger.getLogger(CometdDocumentsService.class);

  /** The channel name. */
  public static final String                               CHANNEL_NAME             = "/eXo/Application/documents/";

  /** The channel name. */
  public static final String                               CHANNEL_NAME_PARAMS      = CHANNEL_NAME + "{fileId}";

  /** The document saved event. */
  public static final String                               DOCUMENT_OPENED_EVENT    = "DOCUMENT_OPENED";

  /** The document deleted event. */
  public static final String                               DOCUMENT_CLOSED_EVENT    = "DOCUMENT_CLOSED";

  
  /** The Constant LAST_EDITOR_CLOSED_EVENT. */
  public static final String                               LAST_EDITOR_CLOSED_EVENT = "LAST_EDITOR_CLOSED";

  /**
   * Base minimum number of threads for document updates thread executors.
   */
  public static final int                                  MIN_THREADS              = 2;

  /**
   * Minimal number of threads maximum possible for document updates thread
   * executors.
   */
  public static final int                                  MIN_MAX_THREADS          = 4;

  /** Thread idle time for thread executors (in seconds). */
  public static final int                                  THREAD_IDLE_TIME         = 120;

  /**
   * Maximum threads per CPU for thread executors of document changes channel.
   */
  public static final int                                  MAX_FACTOR               = 20;

  /**
   * Queue size per CPU for thread executors of document updates channel.
   */
  public static final int                                  QUEUE_FACTOR             = MAX_FACTOR * 2;

  /**
   * Thread name used for the executor.
   */
  public static final String                               THREAD_PREFIX            = "documents-comet-thread-";

  /** The exo bayeux. */
  protected final EXoContinuationBayeux                    exoBayeux;

  /** The service. */
  protected final CometdService                            service;

  /** The call handlers. */
  protected final ExecutorService                          eventsHandlers;

  /** The document service. */
  protected final DocumentService                          documentService;

  /** The active providers. */
  protected final ConcurrentHashMap<String, ProvidersInfo> activeProviders          =
                                                                           new ConcurrentHashMap<String, ProvidersInfo>();

  /**
   * Instantiates the CometdDocumentsService.
   *
   * @param exoBayeux the exoBayeux
   * @param documentService the document service
   */
  public CometdDocumentsService(EXoContinuationBayeux exoBayeux, DocumentService documentService) {
    this.exoBayeux = exoBayeux;
    this.documentService = documentService;
    this.service = new CometdService();
    this.eventsHandlers = createThreadExecutor(THREAD_PREFIX, MAX_FACTOR, QUEUE_FACTOR);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // instantiate processor after the eXo container start, to let
    // start-dependent logic worked before us
    final AtomicReference<ServerAnnotationProcessor> processor = new AtomicReference<>();
    // need initiate process after Bayeux server starts
    exoBayeux.addLifeCycleListener(new LifeCycle.Listener() {
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
        public void sessionRemoved(ServerSession session, boolean timedout) {
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

    }

    /**
     * Subscribe document events.
     *
     * @param message the message.
     * @param fileId the fileId.
     */
    @Subscription(CHANNEL_NAME_PARAMS)
    public void subscribeDocuments(Message message, @Param("fileId") String fileId) {
      Object objData = message.getData();
      if (!Map.class.isInstance(objData)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Couldn't get data as a map from event");
        }
        return;
      }

      Map<String, Object> data = message.getDataAsMap();
      String type = (String) data.get("type");
      String workspace = (String) data.get("workspace");
      String provider = (String) data.get("provider");
      
      eventsHandlers.submit(new ContainerCommand(PortalContainer.getCurrentPortalContainerName()) {
        @Override
        void onContainerError(String error) {
          LOG.error("An error has occured in container: {}", containerName);
        }

        @Override
        void execute(ExoContainer exoContainer) {
          switch (type) {
          case DOCUMENT_OPENED_EVENT: {
            try {
              addActiveProvider(fileId, provider);
              if (getActiveProvidersCount(fileId, provider) == 1) {
                documentService.setCurrentDocumentEditor(fileId, workspace, provider);
              }
            } catch (RepositoryException e) {
              LOG.error("Cannot set current document editor provider", e);
            }
            break;
          }
          case DOCUMENT_CLOSED_EVENT:
            try {
              removeActiveProvider(fileId, provider);
              if (getActiveProvidersCount(fileId, provider) == 0) {
                sendLastEditorClosedEvent(fileId, provider);
                documentService.setCurrentDocumentEditor(fileId, workspace, null);
              }
            } catch (RepositoryException e) {
              LOG.error("Cannot remove current document editor provider", e);
            }
            break;
          }
        }
      });
      if (LOG.isDebugEnabled()) {
        LOG.debug("Event published in " + message.getChannel() + ", fileId: " + fileId + ", data: " + message.getJSON());
      }
    }

    /**
     * Adds the active provider.
     *
     * @param fileId the doc id
     * @param provider the provider
     */
    protected void addActiveProvider(String fileId, String provider) {
      activeProviders.putIfAbsent(fileId, new ProvidersInfo());
      activeProviders.get(fileId).addOpenedEditor(provider);
    }

    /**
     * Removes the active provider.
     *
     * @param fileId the doc id
     * @param provider the provider
     */
    protected void removeActiveProvider(String fileId, String provider) {
      activeProviders.putIfAbsent(fileId, new ProvidersInfo());
      activeProviders.get(fileId).addClosedEditor(provider);
    }

    /**
     * Gets the active providers count.
     *
     * @param fileId the file id
     * @param provider the provider
     * @return the active providers count
     */
    protected int getActiveProvidersCount(String fileId, String provider) {
      ProvidersInfo providersInfo = activeProviders.get(fileId);
      return providersInfo != null ? providersInfo.getOpenedEditorsCount(provider) : 0;
    }

    /**
     * Send last editor closed event.
     *
     * @param fileId the file id
     * @param provider the provider
     */
    protected void sendLastEditorClosedEvent(String fileId, String provider) {
      ServerChannel channel = bayeux.getChannel(CHANNEL_NAME + fileId);
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
        channel.publish(localSession, data.toString());
      }
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
    // Nothing
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
