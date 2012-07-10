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
package org.exoplatform.ecms.test;

import static org.testng.AssertJUnit.fail;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.testing.ContainerBuilder;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.testng.annotations.BeforeClass;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Jun 14, 2012  
 */
public abstract class AbstractECMSTestCase {

  private static ExoContainer container;

  @BeforeClass
  public void startContainer() {
    beforeContainerStart();
    initExoContainer();
    afterContainerStart();
  }

  protected void afterContainerStart() {
  }

  protected void beforeContainerStart() {
  }

  private void initExoContainer() {
    if (container == null) {
      Set<String> rootConfigPaths = new HashSet<String>();
      rootConfigPaths.add("conf/test-root-configuration.xml");

      Set<String> portalConfigPaths = new HashSet<String>();
      portalConfigPaths.add("conf/test-portal-configuration.xml");

      portalConfigPaths.add("conf/" + getClass().getSimpleName() + ".xml");

      EnumMap<ContainerScope, Set<String>> configs = new EnumMap<ContainerScope, Set<String>>(ContainerScope.class);
      configs.put(ContainerScope.ROOT, rootConfigPaths);
      configs.put(ContainerScope.PORTAL, portalConfigPaths);

      ConfiguredBy cfBy = getClass().getAnnotation(ConfiguredBy.class);

      if (cfBy != null) {
        for (ConfigurationUnit src : cfBy.value()) {
          configs.get(src.scope()).add(src.path());
        }
      }

      ContainerBuilder builder = new ContainerBuilder();
      Set<String> rootConfs = configs.get(ContainerScope.ROOT);
      for (String rootConf : rootConfs) {
        builder.withRoot(rootConf);
      }

      try {
        Set<String> portalConfs = configs.get(ContainerScope.PORTAL);
        for (String portalConf : portalConfs) {
          builder.withPortal(portalConf);
        }

        builder.build();
        container = ExoContainerContext.getCurrentContainer();
      } catch (Exception e) {
        fail("Failed to initialized container");
      }
    }
  }
}
