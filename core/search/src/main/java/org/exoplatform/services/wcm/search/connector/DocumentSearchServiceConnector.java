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

import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 5, 2013  
 */
public class DocumentSearchServiceConnector extends BaseContentSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(DocumentSearchServiceConnector.class.getName());
  
  public DocumentSearchServiceConnector(InitParams initParams) throws Exception {
    super(initParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String[] getSearchedDocTypes() {
    List<String> docTypes = null;
    try {
       docTypes = WCMCoreUtils.getService(TemplateService.class).getDocumentTemplates();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    docTypes.remove(NodetypeConstant.NT_FILE);
    return docTypes.toArray(new String[]{});
  }
  
  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   */
  @Override
  protected ResultNode filterNode(ResultNode node) throws RepositoryException {
    //do not accept nt:file
    return node.isNodeType(NodetypeConstant.NT_FILE) ? null : node;
  }
}
