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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.lock.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;

public class LockServiceImplStartTest {

  private RPCService rpcService;

  private LockServiceImpl lockService;

  @Before
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void setUp() throws Exception {
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.getCacheInstance(any())).thenReturn(mock(ExoCache.class));
    rpcService = mock(RPCService.class);
    when(rpcService.registerCommand(any(RemoteCommand.class))).thenAnswer(i -> i.getArgument(0));
    lockService = spy(new LockServiceImpl(null, cacheService, rpcService));
    doNothing().when(lockService).removeLocks();
  }

  @Test
  public void startCleansLocksWhenNodeIsAloneInCluster() throws Exception {
    when(rpcService.executeCommandOnAllNodes(any(RemoteCommand.class), anyLong())).thenReturn(List.of(Boolean.TRUE));

    lockService.start();

    verify(lockService).removeLocks();
  }

  @Test
  public void startKeepsLocksWhenOtherClusterMembersAreRunning() throws Exception {
    when(rpcService.executeCommandOnAllNodes(any(RemoteCommand.class), anyLong())).thenReturn(List.of(Boolean.TRUE,
                                                                                                          Boolean.TRUE));

    lockService.start();

    verify(lockService, never()).removeLocks();
  }

  @Test
  public void startKeepsLocksWhenClusterMembersCannotBeCounted() throws Exception {
    when(rpcService.executeCommandOnAllNodes(any(RemoteCommand.class), anyLong())).thenThrow(new RPCException("not started"));

    lockService.start();

    verify(lockService, never()).removeLocks();
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void startCleansLocksWhenNodeIsNotClustered() throws Exception {
    CacheService cacheService = mock(CacheService.class);
    when(cacheService.getCacheInstance(any())).thenReturn(mock(ExoCache.class));
    LockServiceImpl standalone = spy(new LockServiceImpl(null, cacheService));
    doNothing().when(standalone).removeLocks();

    standalone.start();

    verify(standalone).removeLocks();
  }

}
