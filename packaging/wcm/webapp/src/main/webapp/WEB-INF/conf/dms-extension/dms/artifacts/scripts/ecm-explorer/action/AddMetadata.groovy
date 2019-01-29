/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

import java.util.Map;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Item;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 29, 2007 10:01:09 AM
 */
public class AddMetadataScript implements CmsScript {
  
  private RepositoryService repositoryService_ ;
  private SessionProviderService seProviderService_;
  
  private static final Log LOG  = ExoLogger.getLogger(AddMetadataScript.class);  
  
  public AddMetadataScript(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    repositoryService_ = repositoryService ;
    seProviderService_ = sessionProviderService;
  }

  public void execute(Object context) {
    String metadataName = (String) context.get("exo:mixinMetadata");
    String srcWorkspace = (String) context.get("srcWorkspace");
    String nodePath = (String) context.get("nodePath");
    String srcPath = (String) context.get("srcPath");
    try {
      ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
      SessionProvider sessionProvider = seProviderService_.getSessionProvider(null);
      if (sessionProvider == null) {
        sessionProvider = seProviderService_.getSystemSessionProvider(null);
      }
      Session session = sessionProvider.getSession(srcWorkspace, manageableRepository);
      Item item = session.getItem(nodePath);
      if (!(item instanceof Node)) {
        item = session.getItem(srcPath);
      }
      if (item instanceof Node) {
        Node currentNode = (Node) item;
        if (currentNode.canAddMixin(metadataName)) {
          currentNode.addMixin(metadataName);
          currentNode.save();
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error occurs while executing add meta data script: ", e);
      }
    }
  }

  public void setParams(String[] params) {}

}