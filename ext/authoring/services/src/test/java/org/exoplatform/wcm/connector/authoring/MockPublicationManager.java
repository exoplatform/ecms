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
package org.exoplatform.wcm.connector.authoring;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig.Context;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.mockito.Mockito;

/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Aug 3, 2012  
 */
public class MockPublicationManager implements PublicationManager {

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#addLifecycle(org.exoplatform.container.component.ComponentPlugin)
   */
  @Override
  public void addLifecycle(ComponentPlugin plugin) {

  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#removeLifecycle(org.exoplatform.container.component.ComponentPlugin)
   */
  @Override
  public void removeLifecycle(ComponentPlugin plugin) {

  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#addContext(org.exoplatform.container.component.ComponentPlugin)
   */
  @Override
  public void addContext(ComponentPlugin plugin) {

  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#removeContext(org.exoplatform.container.component.ComponentPlugin)
   */
  @Override
  public void removeContext(ComponentPlugin plugin) {

  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#getLifecycles()
   */
  @Override
  public List<Lifecycle> getLifecycles() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#getContexts()
   */
  @Override
  public List<Context> getContexts() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#getContext(java.lang.String)
   */
  @Override
  public Context getContext(String name) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#getLifecycle(java.lang.String)
   */
  @Override
  public Lifecycle getLifecycle(String name) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.extensions.publication.PublicationManager#getLifecyclesFromUser(java.lang.String, java.lang.String)
   */
  @Override
  public List<Lifecycle> getLifecyclesFromUser(String remoteUser, String state) {
    return null;
  }

}
