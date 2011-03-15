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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.resolver.ResourceResolver;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * May 6, 2008
 */
public class StringResourceResolver extends ResourceResolver {

  private String templateData ;

  public StringResourceResolver(String templateData) {
    this.templateData = templateData ;
  }

  public InputStream getInputStream(String template) throws Exception {
    return new ByteArrayInputStream(templateData.getBytes());
  }

  public List<InputStream> getInputStreams(String template) throws Exception {
    List<InputStream> list = new ArrayList<InputStream>();
    list.add(getInputStream(template)) ;
    return list;
  }

  @SuppressWarnings("unused")
  public URL getResource(String arg0) throws Exception { return null; }

  @SuppressWarnings("unused")
  public String getResourceScheme() { return null; }

  @SuppressWarnings("unused")
  public List<URL> getResources(String arg0) throws Exception { return null; }

  @SuppressWarnings("unused")
  public boolean isModified(String arg0, long arg1) { return false; }

}
