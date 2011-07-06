/***************************************************************************
 * Copyright 2001-2010 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.portal.webui.util;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 21, 2010
 */
/**
 * Use org.exoplatform.services.wcm.utils.WCMCoreUtils instead
 */
@Deprecated
public class SessionProviderFactory {

  @Deprecated
  public static boolean isAnonim()
  {
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if (userId == null)
      return true;
    return false;
  }

  @Deprecated
  /**
   * Deprecated by org.exoplatform.services.wcm.utils.WCMCoreUtils.getSystemSessionProvider()
   */  
  public static SessionProvider createSystemProvider()
  {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionProviderService service =
      (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);
    return service.getSystemSessionProvider(null);
  }

  @Deprecated
  /**
   * Deprecated by org.exoplatform.services.wcm.utils.WCMCoreUtils.getUserSessionProvider()
   */
  public static SessionProvider createSessionProvider()
  {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionProviderService service =
      (SessionProviderService)container.getComponentInstanceOfType(SessionProviderService.class);
    return service.getSessionProvider(null);
  }

  @Deprecated
  /**
   * Deprecated by org.exoplatform.services.wcm.utils.WCMCoreUtils.createAnonimProvider()
   */  
  public static SessionProvider createAnonimProvider()
  {
    return SessionProvider.createAnonimProvider();
  }
}
