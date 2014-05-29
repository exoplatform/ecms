/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.clouddrive;

import org.exoplatform.clouddrive.CloudDrive.Command;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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

/**
 * Manages fixed pool of threads for synchronization commands.
 * 
 * TODO not used.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Synchronizer.java 00000 Nov 8, 2013 pnedonosko $
 * 
 */
public class CommandPoolExecutor {

  /**
   * Wait period for synchronization process in milliseconds.
   */
  public static final long             SYNC_PERIOD  = 10000;

  /**
   * A timeout to wait for scheduler stop in milliseconds. It is four times bigger of SYNC_PERIOD.
   */
  public static final long             STOP_TIMEOUT = 4 * SYNC_PERIOD;

  /**
   * Minimum number of threads to start.
   */
  public static final int              MIN_THREADS  = 2;

  protected static final Log           LOG          = ExoLogger.getLogger(CommandPoolExecutor.class);

  protected static CommandPoolExecutor singleton;

  /**
   * Command thread factory adapted from {@link Executors#DefaultThreadFactory}.
   */
  static class CommandThreadFactory implements ThreadFactory {
    final ThreadGroup   group;

    final AtomicInteger threadNumber = new AtomicInteger(1);

    final String        namePrefix;

    CommandThreadFactory() {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = "clouddrive-command-thread-";
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

  private final ConcurrentHashMap<CloudDrive, Object> drives = new ConcurrentHashMap<CloudDrive, Object>();

  private ExecutorService                             executor;

  /**
   * Singleton of {@link CommandPoolExecutor}.
   * 
   * @return {@link CommandPoolExecutor} instance
   */
  public static CommandPoolExecutor getInstance() {
    if (singleton == null) {
      singleton = new CommandPoolExecutor();
    }
    return singleton;
  }

  /**
   * 
   */
  private CommandPoolExecutor() {

  }

  public synchronized <S> Future<Command> submit(String name, Callable<Command> command) throws InterruptedException {
    init();
    return executor.submit(command);
  }

  public void stop() {
    stopSheduller();
  }

  // internals

  private void init() throws InterruptedException {
    if (executor != null) {
      if (executor.isShutdown()) {
        if (!executor.isTerminated()) {
          stopSheduller();
          if (!executor.awaitTermination(STOP_TIMEOUT, TimeUnit.SECONDS)) {
            LOG.warn("Cloud Drive scheduler (" + drives.size() + ") already shutdown but not yet terminated "
                + executor);
          }
        }
      } else {
        // scheduler already initialized and running - do nothing
        return;
      }
    }

    // Executor will queue all commands and run them in maximum ten threads. Two threads will be maintained
    // online even idle, other inactive will be stopped in two minutes.
    int cpus = Runtime.getRuntime().availableProcessors();
    // use scale factor 25... we know our threads will not create high CPU load, as they are HTTP callers
    // mainly and we want good parallelization
    int maxThreads = Math.round(cpus * 25f);
    maxThreads = maxThreads > 0 ? maxThreads : 1;
    maxThreads = maxThreads < MIN_THREADS ? MIN_THREADS : maxThreads;
    int queueSize = cpus * 4;
    queueSize = queueSize < 4 ? 4 : queueSize;
    LOG.info("Initializing command executor for max " + maxThreads + " threads, queue size " + queueSize);
    executor = new ThreadPoolExecutor(MIN_THREADS,
                                      maxThreads,
                                      120,
                                      TimeUnit.SECONDS,
                                      new LinkedBlockingQueue<Runnable>(queueSize),
                                      new CommandThreadFactory(),
                                      new ThreadPoolExecutor.CallerRunsPolicy());
  }

  private void stopSheduller() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

}
