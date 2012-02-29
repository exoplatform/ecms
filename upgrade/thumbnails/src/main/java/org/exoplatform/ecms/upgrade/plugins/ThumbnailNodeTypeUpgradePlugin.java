/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.ecms.upgrade.plugins;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : hai_lethanh hailt@exoplatform.com
 * Sep 27, 2011
 */
public class ThumbnailNodeTypeUpgradePlugin extends UpgradeProductPlugin {
  private static final String SUPER_TYPE_NT_UNSTRUCTURED   = "nt:unstructured";

  private static final String SUPER_TYPE_NT_HIERARCHY_NODE = "nt:hierarchyNode";

  private static final String SUPER_TYPE_NT_FOLDER         = "nt:folder";

  private static final String EXO_THUMBNAIL                = "exo:thumbnail";

  private static final String EXO_THUMBNAILS               = "exo:thumbnails";

  private Log                 log                          = ExoLogger.getLogger(this.getClass());

  private DMSConfiguration    dmsConfiguration_;

  private RepositoryService   repoService_;

  public ThumbnailNodeTypeUpgradePlugin(DMSConfiguration dmsConfiguration,
                                        RepositoryService repoService,
                                        InitParams initParams) {
    super(initParams);
    this.dmsConfiguration_ = dmsConfiguration;
    this.repoService_ = repoService;
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      if (log.isInfoEnabled()) {
        log.info("Start " + this.getClass().getName() + ".............");
      }
      Session session = sessionProvider.getSession(dmsConfiguration_.getConfig()
                                                                    .getSystemWorkspace(),
                                                   repoService_.getCurrentRepository());
      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager) session.getWorkspace().getNodeTypeManager();

      // update exo:thumbnails nodetype
      NodeTypeValue exoThumbnailsNodeTypeValue = nodeTypeManager.getNodeTypeValue(EXO_THUMBNAILS);
      updateSuperType(nodeTypeManager, exoThumbnailsNodeTypeValue);

      // update exo:thumbnail nodetype
      NodeTypeValue exoThumbnailNodeTypeValue = nodeTypeManager.getNodeTypeValue(EXO_THUMBNAIL);
      updateSuperType(nodeTypeManager, exoThumbnailNodeTypeValue);

      if (log.isInfoEnabled()) {
        log.info("End " + this.getClass().getName() + ".............");
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(this.getClass().getName() + " failed:", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * update super type of node type
   * @param nodeTypeManager
   * @param exoThumbnailNodeTypeValue
   * @throws RepositoryException
   */
  private void updateSuperType(ExtendedNodeTypeManager nodeTypeManager,
                               NodeTypeValue exoThumbnailNodeTypeValue) throws RepositoryException {
    List<String> lstDeclaredSupertype = exoThumbnailNodeTypeValue.getDeclaredSupertypeNames();

    // remove nt:folder super type
    if (lstDeclaredSupertype.contains(SUPER_TYPE_NT_FOLDER)) {
      lstDeclaredSupertype.remove(SUPER_TYPE_NT_FOLDER);
    }

    // add nt:hierarchyNode super type
    if (!lstDeclaredSupertype.contains(SUPER_TYPE_NT_HIERARCHY_NODE)) {
      lstDeclaredSupertype.add(SUPER_TYPE_NT_HIERARCHY_NODE);
    }

    // add nt:unstructured super type
    if (!lstDeclaredSupertype.contains(SUPER_TYPE_NT_UNSTRUCTURED)) {
      lstDeclaredSupertype.add(SUPER_TYPE_NT_UNSTRUCTURED);
    }

    // save changes
    exoThumbnailNodeTypeValue.setDeclaredSupertypeNames(lstDeclaredSupertype);
    nodeTypeManager.registerNodeType(exoThumbnailNodeTypeValue, ExtendedNodeTypeManager.REPLACE_IF_EXISTS);
  }
}
