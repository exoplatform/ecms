package org.exoplatform.ecm.webui.component.explorer.popup.service;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.database.HibernateService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.hibernate.OrganizationServiceImpl;

/**
 * Created by toannh on 9/5/14.
 */
public class DummyOrganizationService extends OrganizationServiceImpl {
  public DummyOrganizationService(HibernateService hservice, CacheService cservice) throws Exception {
    super(hservice, cservice);
  }

  @Override
  public UserHandler getUserHandler() {
    return new DummyHandler();
  }

  class DummyHandler extends org.exoplatform.services.organization.impl.mock.DummyOrganizationService.UserHandlerImpl {
    @Override
    public User findUserByName(java.lang.String userName) throws java.lang.Exception {
      return null;
    }
  }
}
