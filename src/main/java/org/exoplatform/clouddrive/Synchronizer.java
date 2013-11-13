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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public class Synchronizer {

  /**
   * Wait period for synchronization process in milliseconds.
   */
  public static final long SYNC_PERIOD  = 10000;

  /**
   * A timeout to wait for scheduler stop in milliseconds. It is four times bigger of SYNC_PERIOD.
   */
  public static final long STOP_TIMEOUT = 4 * SYNC_PERIOD;

  protected static final Log LOG = ExoLogger.getLogger(Synchronizer.class);
  
  /**
   * Internal interface used for auto-synchronization process.
   */
  protected interface Sync extends Runnable {

    /**
     * Check if sync active.
     * 
     * @return boolean <code>true</code> if sync currently in progress, <code>false</code> otherwise
     */
    boolean isActive();

    /**
     * Invoke synchronization explicitly. This method will wait if sync process already in progress.
     * 
     */
    void submit();
  }

  private final ConcurrentHashMap<CloudDrive, Object> drives = new ConcurrentHashMap<CloudDrive, Object>();

  private ScheduledExecutorService                    scheduler;

  /**
   * 
   */
  public Synchronizer() {

  }

  void submit(CloudDrive drive) {
    initSheduler();
    
    //drives.put(drive, value)
  }

  void stop() {
    stopSheduller();
  }

  // internals

  private void initSheduler() {
    if (scheduler != null) {
      if (scheduler.isShutdown()) {
        if (!scheduler.isTerminated()) {
          stopSheduller();
          try {
            if (!scheduler.awaitTermination(STOP_TIMEOUT, TimeUnit.SECONDS)) {
              LOG.warn("Cloud Drive scheduler (" + drives.size()
                  + ") already shutdown but not yet terminated " + scheduler);
            };
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      } else {
        // scheduler already initialized and running - do nothing
        return;
      }
    }

    scheduler = Executors.newScheduledThreadPool(4);
  }

  private void stopSheduller() {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }

}
