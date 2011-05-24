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
package org.exoplatform.services.ecm.dms.cms;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.impl.CmsServiceImpl;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.listener.ListenerService;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Mar 16, 2011
 * 5:45:40 PM  
 */
public class MockCmsServiceImpl extends CmsServiceImpl {

  private RepositoryService jcrService;
  
  public MockCmsServiceImpl(RepositoryService jcrService, IDGeneratorService idGeneratorService,
      ListenerService listenerService) {
    super(jcrService, idGeneratorService, listenerService);
    this.jcrService = jcrService;
  }
  
  public String storeNode(String workspace, String nodeTypeName, String storePath,
      Map mappings) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionProviderService service =
      (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);
    Session session = 
      service.getSystemSessionProvider(null).getSession(workspace, jcrService.getCurrentRepository());
    Node storeHomeNode = (Node) session.getItem(storePath);
    String path = storeNode(nodeTypeName, storeHomeNode, mappings, true);
    storeHomeNode.save();
    session.save();
    return path;
  }

}
