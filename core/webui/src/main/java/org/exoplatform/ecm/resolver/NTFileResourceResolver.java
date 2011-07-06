/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.resolver;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public class NTFileResourceResolver extends JCRResourceResolver {

  /**
   * Instantiates a new nT file resource resolver to load
   * template that stored as nt:file in jcr
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param propertyName the property name
   */
  @Deprecated
  public NTFileResourceResolver(String repository, String workspace, String propertyName) {
    super(repository, workspace, propertyName);
  }

  /**
   * Instantiates a new nT file resource resolver.
   *
   * @param repository the repository
   * @param workspace the workspace
   */
  @Deprecated
  public NTFileResourceResolver(String repository, String workspace) {
    super(repository,workspace,null);
  }
  
  public NTFileResourceResolver(String workspace) {
    super(workspace);
  }

  /**
   * @param url URL must be like: jcr:uuid with uuid is node uiid of the file
   * @see org.exoplatform.resolver.ResourceResolver#getInputStream(java.lang.String)
   */
  public InputStream getInputStream(String url) throws Exception  {
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    RepositoryService repositoryService =
      (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class) ;
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    SessionProvider provider = WCMCoreUtils.getSystemSessionProvider();
    Session session = provider.getSession(workspace,manageableRepository);
    String fileUUID = removeScheme(url);
    Node node = session.getNodeByUUID(fileUUID);
    return node.getNode("jcr:content").getProperty("jcr:data").getStream();
  }

}
