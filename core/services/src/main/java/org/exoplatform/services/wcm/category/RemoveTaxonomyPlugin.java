/*
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
 */
package org.exoplatform.services.wcm.category;

import javax.jcr.Node;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.portal.artifacts.RemovePortalPlugin;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Aug 11, 2009
 */
public class RemoveTaxonomyPlugin extends RemovePortalPlugin {

  /** The taxonomy service_. */
  private TaxonomyService         taxonomyService;

  private ActionServiceContainer actionServiceContainer;

  private RepositoryService repositoryService;

  /**
   * Instantiates a new initial taxonomy plugin.
   *
   * @param params the params
   * @param configurationManager the configuration manager
   * @param repositoryService the repository service
   * @param taxonomyService the taxonomy service
   * @param actionServiceContainer the action service container
   *
   * @throws Exception the exception
   */
  public RemoveTaxonomyPlugin(InitParams params,
                               ConfigurationManager configurationManager,
                               RepositoryService repositoryService,
                               TaxonomyService taxonomyService,
                               ActionServiceContainer actionServiceContainer) throws Exception {
    super(params, configurationManager, repositoryService);
    this.repositoryService = repositoryService;
    this.taxonomyService = taxonomyService;
    this.actionServiceContainer = actionServiceContainer;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.portal.artifacts.BasePortalArtifactsPlugin
   * #deployToPortal(java.lang.String,
   * org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public void invalidateFromPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    String repository = repositoryService.getCurrentRepository().getConfiguration().getName();
    if (taxonomyService.hasTaxonomyTree(portalName)) {
      Node taxonomyTreeNode = taxonomyService.getTaxonomyTree(portalName, true);
      if (taxonomyTreeNode!=null) {
        actionServiceContainer.removeAction(taxonomyTreeNode, repository);
      }
      taxonomyService.removeTaxonomyTree(portalName);
    }
  }
}
