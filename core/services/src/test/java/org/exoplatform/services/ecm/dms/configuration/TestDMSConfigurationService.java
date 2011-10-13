/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.ecm.dms.configuration;

import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;

/**
 * Created by eXo Platform
 * Author : Nguyen Manh Cuong
 *          manhcuongpt@gmail.com
 * Jun 24, 2009
 */

/**
 * Unit test for DMSConfiguration
 * Methods need to test
 * 1. getConfig() method
 * 2. addPlugin() method
 * 3. initNewRepo() method
 */

public class TestDMSConfigurationService extends BaseDMSTestCase {

  private DMSConfiguration dmsConfiguration = null;

  private final static String TEST_WS = "workspace-test";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    dmsConfiguration = (DMSConfiguration)container.getComponentInstanceOfType(DMSConfiguration.class);
  }

  /**
   * Test Method: getConfig()
   * Expected:
   *     get configuration contains:
   *     Workspace Name:  DMSSYSTEM_WS
   *     Repository Name: REPO_NAME
   */
  public void testGetConfig() throws Exception {
    DMSRepositoryConfiguration oldDmsRepoConf = dmsConfiguration.getConfig();
    try {
      DMSRepositoryConfiguration dmsRepoConf = dmsConfiguration.getConfig();
      assertEquals(DMSSYSTEM_WS, dmsRepoConf.getSystemWorkspace());
    }
    finally {
      dmsConfiguration.addPlugin(oldDmsRepoConf);
    }
  }

  /**
   * Test Method: initNewRepo()
   * Input: DMSRepositoryConfiguration with new repository name and new workspace name
   * Expected:
   *        New repository is initialized
   */
  public void testInitNewRepo() throws Exception {
    DMSRepositoryConfiguration oldDmsRepoConf = dmsConfiguration.getConfig();
    try {
      DMSRepositoryConfiguration dmsRepoConfig = new DMSRepositoryConfiguration();
      dmsRepoConfig.setSystemWorkspace(TEST_WS);
      dmsConfiguration.initNewRepo(dmsRepoConfig);
      DMSRepositoryConfiguration dmsRepoConf = dmsConfiguration.getConfig();
      assertEquals(TEST_WS, dmsRepoConf.getSystemWorkspace());
    }
    finally {
      dmsConfiguration.addPlugin(oldDmsRepoConf);
    }
  }

  /**
   * Test Method: addPlugin()
   * Input: plugin is an instance of DMSRespositoryConfig
   * Expected:
   *        plugin is added to repository
   */
  public void testAddPlugin() throws Exception {
    DMSRepositoryConfiguration oldDmsRepoConf = dmsConfiguration.getConfig();
    try {
      DMSRepositoryConfiguration dmsRepoConfig = new DMSRepositoryConfiguration();
      dmsRepoConfig.setSystemWorkspace(TEST_WS);
      dmsConfiguration.addPlugin(dmsRepoConfig);
      DMSRepositoryConfiguration dmsRepoConf = dmsConfiguration.getConfig();
      assertEquals(TEST_WS, dmsRepoConf.getSystemWorkspace());
    }
    finally {
      dmsConfiguration.addPlugin(oldDmsRepoConf);
    }
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
}
