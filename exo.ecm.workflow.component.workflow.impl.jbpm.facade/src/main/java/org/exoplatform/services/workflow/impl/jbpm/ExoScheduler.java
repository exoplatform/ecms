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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jbpm.db.JbpmSessionFactory;
import org.jbpm.scheduler.exe.Timer;
import org.jbpm.scheduler.impl.SchedulerHistoryLog;
import org.jbpm.scheduler.impl.SchedulerListener;

public class ExoScheduler {

  ExoSchedulerThread schedulerThread = null;
  LinkedList historyLogs = new LinkedList();
  int interval = 5000;
  int historyMaxSize = 30;
  private JbpmSessionFactory jbpmSessionFactory;
  private String containerName;

  public ExoScheduler(String containerName) {
    this.containerName = containerName;
  }
  
  public void start() {
    schedulerThread = new ExoSchedulerThread(containerName);
    schedulerThread.setInterval(interval);
    schedulerThread.addListener(new HistoryListener());
    schedulerThread.start();
  }

  public void stop() {
    if (isRunning()) {
      schedulerThread.keepRunning = false;
      schedulerThread.interrupt();
      schedulerThread = null;
    }
  }

  public boolean isRunning() {
    return ( (schedulerThread!=null)
             && (schedulerThread.isAlive()) );
  }

  public List getSchedulerHistoryLogs() {
    return historyLogs;
  }

  public void clearSchedulerHistoryLogs() {
    historyLogs.clear();
  }

  class HistoryListener implements SchedulerListener {
    public void timerExecuted(Date date, Timer timer) {
      historyLogs.add(new SchedulerHistoryLog(date, timer));
      if (historyLogs.size()>historyMaxSize) {
        historyLogs.removeLast();
      }
    }
  }

  public int getHistoryMaxSize() {
    return historyMaxSize;
  }

  public void setHistoryMaxSize(int historyMaxSize) {
    this.historyMaxSize = historyMaxSize;
  }

  public int getInterval() {
    return interval;
  }

  public void setInterval(int interval) {
    this.interval = interval;
  }

}
