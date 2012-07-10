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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.impl.ApplicationContextImpl;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.security.ConversationState;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Jun 6, 2012  
 */
public class BaseECMSTestCase extends AbstractECMSTestCase {  

  protected PortalContainer        container;

  protected ProviderBinder         providers;

  protected ResourceBinder         binder;

  protected RequestHandlerImpl     requestHandler;

  protected OrganizationService    orgService;

  protected CredentialsImpl        credentials;

  protected RepositoryService      repositoryService;
  
  protected SessionProvider        sessionProvider;

  protected Session                session;

  protected ManageableRepository   repository;

  protected SessionProviderService sessionProviderService_;

  protected final String           REPO_NAME        = "repository";

  protected final String           DMSSYSTEM_WS     = "dms-system";

  protected final String           SYSTEM_WS        = "system";

  protected final String           COLLABORATION_WS = "collaboration";
  
  @Override
  protected void afterContainerStart() {
    initServices();
  }

  /**
   * Apply a system session
   * @throws RepositoryConfigurationException 
   * @throws RepositoryException 
   */
  public void applySystemSession() throws RepositoryConfigurationException, RepositoryException {
    System.setProperty("gatein.tenant.repository.name", REPO_NAME);
    container = PortalContainer.getInstance();

    repositoryService.setCurrentRepositoryName(REPO_NAME);
    repository = repositoryService.getCurrentRepository();

    closeOldSession();
    sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    session = sessionProvider.getSession(COLLABORATION_WS, repository);
    sessionProvider.setCurrentRepository(repository);
    sessionProvider.setCurrentWorkspace(COLLABORATION_WS);
  }
  
  /**
   * Apply an user session with a given user name, password and workspace name
   * @param username name of user
   * @param password password of user
   * @param workspaceName workspace name
   * @throws RepositoryConfigurationException 
   * @throws RepositoryException 
   * @throws Exception
   */
  public void applyUserSession(String username, String password, String workspaceName) throws RepositoryConfigurationException, RepositoryException  {    
    repositoryService.setCurrentRepositoryName(REPO_NAME);
    repository = repositoryService.getCurrentRepository();
    credentials = new CredentialsImpl(username, password.toCharArray());
    
    closeOldSession();
    session = (SessionImpl) repository.login(credentials, workspaceName);    
    ((DumpThreadLocalSessionProviderService)sessionProviderService_).applyUserSession(session);
  }
  

  private void initServices(){
    container = PortalContainer.getInstance();
    orgService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
    repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
    binder = (ResourceBinder) container.getComponentInstanceOfType(ResourceBinder.class);
    requestHandler = (RequestHandlerImpl) container.getComponentInstanceOfType(RequestHandlerImpl.class);
    ProviderBinder.setInstance(new ProviderBinder());
    providers = ProviderBinder.getInstance();
    ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, providers));
    binder.clear();
    sessionProviderService_ = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    String loginConf = this.getClass().getResource("/conf/standalone/login.conf").toString();
    System.setProperty("java.security.auth.login.config", loginConf);
    try {
      applySystemSession();
    } catch (Exception e) {
      fail();
    }
  }
  
  /**
   * End current session
   */
  protected void endSession() {
    sessionProviderService_.removeSessionProvider(null);
    ConversationState.setCurrent(null);
  }
  /**
   * Close current session
   */
  private void closeOldSession() {
    if (session != null && session.isLive()) {
      session.logout();

      // remove user session
      ((DumpThreadLocalSessionProviderService) sessionProviderService_).applyUserSession(null);
    }
  }

}
