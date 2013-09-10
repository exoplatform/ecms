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

  /**
   * Mock data for testing APIs of REST service LifecycleConnector
   * @return a collection of nodes that contains 2 nodes:
   *         Node 1: name is Mock node1
   *                 path is /node1
   *         Node 2: name is Mock node2
   *                 path is /node2
   *                 title is Mock node 2
   *                 publication:startPublishedDate is 03/18/2012
   * @throws Exception
   */
  @Override
  public List<Node> getContents(String fromstate, String tostate, String date, String user, String lang, String workspace) throws Exception {
    List<Node> result = new ArrayList<Node>();
    Node node1 = Mockito.mock(Node.class);
    Mockito.when(node1.getName()).thenReturn("Mock node1");
    Mockito.when(node1.getPath()).thenReturn("/node1");
    result.add(node1);

    Node node2 = Mockito.mock(Node.class);
    Mockito.when(node2.getName()).thenReturn("Mock node2");
    Mockito.when(node2.getPath()).thenReturn("/node2");

    Value titleValue = Mockito.mock(Value.class);
    Mockito.when(titleValue.getString()).thenReturn("Mock node2");
    Property titleProperty = Mockito.mock(Property.class);
    Mockito.when(titleProperty.getValue()).thenReturn(titleValue);
    Mockito.when(node2.hasProperty("exo:title")).thenReturn(true);
    Mockito.when(node2.getProperty("exo:title")).thenReturn(titleProperty);
    Mockito.when(node2.getProperty("exo:title").getString()).thenReturn("Mock node2");

    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy"); 

    Date valueDate =(formatter.parse("03/18/2012"));
    Value startPublishedDateValue = Mockito.mock(Value.class);
    Calendar valueCalendar = new GregorianCalendar();
    valueCalendar.setTime(valueDate);
    Mockito.when(startPublishedDateValue.getDate()).thenReturn(valueCalendar);
    Property startPublishedDateProperty = Mockito.mock(Property.class);
    Mockito.when(startPublishedDateProperty.getValue()).thenReturn(startPublishedDateValue);
    Mockito.when(node2.hasProperty("publication:startPublishedDate")).thenReturn(true);
    Mockito.when(node2.getProperty("publication:startPublishedDate")).thenReturn(startPublishedDateProperty);
    Mockito.when(node2.getProperty("publication:startPublishedDate").getString()).thenReturn("03/18/2012");
    result.add(node2);
    return result;
  }

}
