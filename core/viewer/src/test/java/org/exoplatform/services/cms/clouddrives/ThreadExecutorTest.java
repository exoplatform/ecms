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
package org.exoplatform.services.cms.clouddrives;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ThreadExecutorTest {

  @Test
  public void testTheCommandsRunOnDaemonThreads() throws Exception {
    ThreadExecutor executor = ThreadExecutor.createInstance("clouddrive-test-thread-", 1, 1);
    try {
      assertTrue(executor.submit(() -> Thread.currentThread().isDaemon()).get(5, TimeUnit.SECONDS));
    } finally {
      executor.stop();
    }
  }
}
