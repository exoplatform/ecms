/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.connector;

import javax.jcr.RepositoryException;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 5, 2013  
 */
public class FileSearchServiceConnector extends BaseContentSearchServiceConnector {

  public FileSearchServiceConnector(InitParams initParams) throws Exception {
    super(initParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String[] getSearchedDocTypes() {
    return new String[]{NodetypeConstant.NT_FILE};
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected ResultNode filterNode(ResultNode node) throws RepositoryException {
    return node.isNodeType(NodetypeConstant.NT_FILE) ? node : null;
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected String getPath(DriveData driveData, ResultNode node, SearchContext context) throws Exception {
    String siteName = WCMCoreUtils.getService(PortalContainerInfo.class).getContainerName();
    String restContextName = WCMCoreUtils.getService(PortalContainerConfig.class).getRestContextName(siteName);
    StringBuffer ret = new StringBuffer();
    ret.append('/').append(siteName).append('/').append(restContextName).append("/jcr/").
        append(WCMCoreUtils.getRepository().getConfiguration().getName()).append('/'). 
        append(node.getSession().getWorkspace().getName()).append(node.getPath());
    return ret.toString();
  }

}
