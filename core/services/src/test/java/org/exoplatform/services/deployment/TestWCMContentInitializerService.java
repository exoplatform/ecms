/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.deployment;

import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 3 Aug 2012  
 */
public class TestWCMContentInitializerService extends BaseWCMTestCase {
  WCMContentInitializerService WCIService;
  public void testRemoveGroupsOrUsersForLock() throws Exception {
    //For recovering sonar
    WCIService.addPlugin(null);
  }
  public void setUp() throws Exception {
    super.setUp();
    WCIService = (WCMContentInitializerService)container.getComponentInstanceOfType(WCMContentInitializerService.class);
    WCIService.start();
    applySystemSession();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
