/*
 * Copyright (C) 2026 eXo Platform SAS.
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
package org.exoplatform.services.cms.lock.impl;

import java.io.Serializable;

import org.exoplatform.services.rpc.RemoteCommand;

/**
 * Command that every cluster member answers: the number of answers received
 * by the node that casts it is the number of members of the cluster, itself
 * included.
 */
class LockServiceClusterProbe implements RemoteCommand {

  static final String COMMAND_ID = "ecms.LockService.clusterProbe";

  /**
   * {@inheritDoc}
   */
  @Override
  public Serializable execute(Serializable[] args) {
    return Boolean.TRUE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return COMMAND_ID;
  }

}
