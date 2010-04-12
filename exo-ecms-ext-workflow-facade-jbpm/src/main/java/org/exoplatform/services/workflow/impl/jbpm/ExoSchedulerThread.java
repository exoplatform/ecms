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
package org.exoplatform.services.workflow.impl.jbpm;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.db.JbpmSession;
import org.jbpm.db.SchedulerSession;
import org.jbpm.scheduler.exe.Timer;
import org.jbpm.scheduler.impl.SchedulerListener;

public class ExoSchedulerThread extends Thread {

  static BusinessCalendar businessCalendar = new BusinessCalendar();
  
  List listeners = new ArrayList();
  boolean keepRunning = true;
  long interval = 5000;

  private String containerName;

  public ExoSchedulerThread(String containerName) {
    super("exo workflow scheduler");
    this.containerName = containerName;
  }
  
  public void run() {
    while (keepRunning) {
      long millisToWait = interval;
      try {
        millisToWait = executeTimers(); 
        // calculate the milliseconds to wait...
        if (millisToWait < 0) {
          millisToWait = interval;
        }
        millisToWait = Math.min(millisToWait, interval);
      } catch (RuntimeException e) {        
        //runtime exception while executing timers     
        e.printStackTrace();
      } finally {
        try {
          Thread.sleep(millisToWait);
        } catch (InterruptedException e) {
          //waiting for timers got interuppted
        }
      }
    }
  }

  public long executeTimers() {
    long millisTillNextTimerIsDue = -1;
    boolean isDueDateInPast = true;

    boolean exceptionOccured = true;
    
    PortalContainer container = RootContainer.getInstance().getPortalContainer(containerName);
    PortalContainer.setInstance(container);
    
    WorkflowServiceContainerImpl workflowContainer = (WorkflowServiceContainerImpl) container.getComponentInstanceOfType(
        WorkflowServiceContainer.class);
    JbpmSession jbpmSession = workflowContainer.openSession();
    try {
      SchedulerSession schedulerSession = new SchedulerSession(jbpmSession);
      Iterator iter = schedulerSession.findTimersByDueDate();
      
      while( (iter.hasNext()) && (isDueDateInPast)) {
        Timer timer = (Timer) iter.next();
        // if this timer is due
        if (timer.isDue()) {
          // execute
          timer.execute();
          // notify the listeners (e.g. the scheduler servlet)
          notifyListeners(timer);
          // if there was an exception, just save the timer
          if (timer.getException()!=null) {
            schedulerSession.saveTimer(timer);
          // if repeat is specified
          } else if (timer.getRepeat()!=null) {
            // update timer by adding the repeat duration
            Date dueDate = timer.getDueDate();
            // suppose that it took the timer runner thread a 
            // very long time to execute the timers.
            // then the repeat action dueDate could already have passed.
            while (dueDate.getTime()<=System.currentTimeMillis()) {
              dueDate = businessCalendar
                    .add(dueDate, 
                      new Duration(timer.getRepeat()));
            }
            timer.setDueDate( dueDate );
            // save the updated timer in the database
            schedulerSession.saveTimer(timer);
          } else {
            // delete this timer
            schedulerSession.deleteTimer(timer);
          }
        } else { // this is the first timer that is not yet due
          isDueDateInPast = false;
          millisTillNextTimerIsDue = timer.getDueDate().getTime() - System.currentTimeMillis();
        }
      }
      exceptionOccured = false;
    } finally {
      if(exceptionOccured)
        workflowContainer.rollback(jbpmSession);        
      else  
        workflowContainer.closeSession(jbpmSession); 
      PortalContainer.setInstance(null);
    }
    return millisTillNextTimerIsDue;
  }

  // listeners ////////////////////////////////////////////////////////////////
  public void addListener(SchedulerListener listener) {
    if (listeners==null) listeners = new ArrayList();
    listeners.add(listener);
  }

  public void removeListener(SchedulerListener listener) {
    listeners.remove(listener);
    if (listeners.isEmpty()) {
      listeners = null;
    }
  }

  private void notifyListeners(Timer timer) {
    if (listeners!=null) {
      Date now = new Date();
      Iterator iter = new ArrayList(listeners).iterator();
      while (iter.hasNext()) {
        SchedulerListener timerRunnerListener = (SchedulerListener) iter.next();
        timerRunnerListener.timerExecuted(now, timer);
      }
    }
  }

  public void setInterval(long interval) {
    this.interval = interval;
  }

}
