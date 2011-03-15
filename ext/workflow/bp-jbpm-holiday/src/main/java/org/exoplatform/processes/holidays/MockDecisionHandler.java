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

package org.exoplatform.processes.holidays;

import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;

/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 12 mai 2004
 */
public class MockDecisionHandler implements DecisionHandler {

  private static Logger log = Logger.getLogger(MockDecisionHandler.class.getName());

  public String decide(ExecutionContext executionContext) {
    log.info("Start Date : " + executionContext.getVariable("start"));
    log.info("decide in MockDecisionHandler : " + executionContext.getNode().getName());
    return "enough holidays";
  }

}
