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
package org.exoplatform.services.cms.clouddrives;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Manages fixed pool of threads for Cloud Drive tasks.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Synchronizer.java 00000 Nov 8, 2013 pnedonosko $
 */
public class ThreadExecutor {

  /**
   * Wait period for synchronization process in milliseconds.
   */
  public static final long        SYNC_PERIOD             = 10000;

  /**
   * A timeout to wait for scheduler stop in milliseconds. It is four times
   * bigger of SYNC_PERIOD.
   */
  public static final long        STOP_TIMEOUT            = 4 * SYNC_PERIOD;

  /**
   * Minimum number of threads to start.
   */
  public static final int         MIN_THREADS             = 2;

  /**
   * Minimum threads per CPU.
   */
  public static final int         MIN_FACTOR              = 4;

  /**
   * Default maximum threads per CPU.
   */
  public static final int         MAX_FACTOR              = 50;

  /**
   * Default queue size per CPU.
   */
  public static final int         QUEUE_FACTOR            = MAX_FACTOR * 2;

  /**
   * Thread name used for singleton executor.
   */
  public static final String      SINGLETON_THREAD_PREFIX = "clouddrive-thread-";

  /** The Constant LOG. */
  protected static final Log      LOG                     = ExoLogger.getLogger(ThreadExecutor.class);

  /** The singleton. */
  protected static ThreadExecutor singleton;

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
      if (t.isDaemon())
        t.setDaemon(false);
      if (t.getPriority() != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY);
      return t;
    }
  }

  /** The drives. */
  private final ConcurrentHashMap<CloudDrive, Object> drives = new ConcurrentHashMap<CloudDrive, Object>();

  /** The max factor. */
  private final int                                   maxFactor;

  /** The queue factor. */
  private final int                                   queueFactor;

  /** The thread name prefix. */
  private final String                                threadNamePrefix;

  /** The executor. */
  private final ExecutorService                       executor;

  /**
   * Singleton of {@link ThreadExecutor}.
   * 
   * @return {@link ThreadExecutor} instance
   */
  public static ThreadExecutor getInstance() {
    if (singleton == null) {
      singleton = new ThreadExecutor();
    }
    return singleton;
  }

  /**
   * Create new instance of {@link ThreadExecutor}.
   * 
   * @param threadNamePrefix {@link String} thread name prefix for this pool
   * @param maxThreadsPerCPU int maximum threads allowed per single CPU core
   * @param queuePerCPU int queue size per CPU core
   * @return {@link ThreadExecutor} instance
   */
  public static ThreadExecutor createInstance(String threadNamePrefix, int maxThreadsPerCPU, int queuePerCPU) {
    return new ThreadExecutor(threadNamePrefix, maxThreadsPerCPU, queuePerCPU);
  }

  /**
   * Instantiates a new thread executor.
   */
  private ThreadExecutor() {
    this(SINGLETON_THREAD_PREFIX, MAX_FACTOR, QUEUE_FACTOR);
  }

  /**
   * Instantiates a new thread executor.
   *
   * @param threadNamePrefix the thread name prefix
   * @param maxFactor the max factor
   * @param queueFactor the queue factor
   */
  private ThreadExecutor(String threadNamePrefix, int maxFactor, int queueFactor) {
    this.maxFactor = maxFactor;
    this.queueFactor = queueFactor;
    this.threadNamePrefix = threadNamePrefix;

    // Executor will queue all commands and run them in maximum ten threads. Two
    // threads will be maintained
    // online even idle, other inactive will be stopped in two minutes.
    int cpus = Runtime.getRuntime().availableProcessors();
    int poolThreads = cpus; // Math.round(cpus * 1f * MIN_FACTOR);
    // use scale factor 25... we know our threads will not create high CPU load,
    // as they are HTTP callers
    // mainly and we want good parallelization
    int maxThreads = Math.round(cpus * 1f * maxFactor);
    maxThreads = maxThreads > 0 ? maxThreads : 1;
    maxThreads = maxThreads < MIN_THREADS ? MIN_THREADS : maxThreads;
    int queueSize = cpus * queueFactor;
    queueSize = queueSize < queueFactor ? queueFactor : queueSize;
    LOG.info("Initializing command executor for max " + maxThreads + " threads, queue size " + queueSize);
    executor = new ThreadPoolExecutor(poolThreads,
                                      maxThreads,
                                      120,
                                      TimeUnit.SECONDS,
                                      new LinkedBlockingQueue<Runnable>(queueSize),
                                      new CommandThreadFactory(threadNamePrefix),
                                      new ThreadPoolExecutor.CallerRunsPolicy());
  }

  /**
   * Submit.
   *
   * @param <P> the generic type
   * @param command the command
   * @return the future
   */
  public synchronized <P> Future<P> submit(Callable<P> command) {
    return executor.submit(command);
  }

  /**
   * Submit.
   *
   * @param worker the worker
   * @return the future
   */
  public synchronized Future<?> submit(Runnable worker) {
    return executor.submit(worker);
  }

  /**
   * Stop.
   */
  public void stop() {
    stopSheduller();
  }

  // internals

  /**
   * Stop sheduller.
   */
  private void stopSheduller() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

}
