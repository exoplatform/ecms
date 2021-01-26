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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.exoplatform.services.rest.ContainerResponseWriter;
import org.exoplatform.services.rest.impl.ContainerRequest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.tools.DummyContainerResponseWriter;
import org.exoplatform.services.test.mock.MockHttpServletRequest;
/**
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Jun 6, 2012  
 */
public abstract class BaseECMSResourceTestCase extends BaseECMSTestCase {

  /**
   * Get response with provided writer
   * @param method
   * @param requestURI
   * @param baseURI
   * @param headers
   * @param data
   * @param writer
   * @return
   * @throws Exception
   */
  public ContainerResponse service(String method,
                                   String requestURI,
                                   String baseURI,
                                   Map<String, List<String>> headers,
                                   byte[] data,
                                   ContainerResponseWriter writer) throws Exception{

    if (headers == null) {
      headers = new MultivaluedMapImpl();
    }

    ByteArrayInputStream in = null;
    if (data != null) {
      in = new ByteArrayInputStream(data);
    }

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new MockHttpServletRequest(requestURI, in, in != null ? in.available() : 0, method, headers);
    envctx.put(HttpServletRequest.class, httpRequest);
    EnvironmentContext.setCurrent(envctx);
    ContainerRequest request = new ContainerRequest(method,
                                                    new URI(requestURI),
                                                    new URI(baseURI),
                                                    in,
                                                    new InputHeadersMap(headers));
    ContainerResponse response = new ContainerResponse(writer);
    requestHandler.handleRequest(request, response);
    return response;
  }

  /**
   * Get response without provided writer
   * @param method
   * @param requestURI
   * @param baseURI
   * @param headers
   * @param data
   * @return
   * @throws Exception
   */
  public ContainerResponse service(String method,
                                   String requestURI,
                                   String baseURI,
                                   MultivaluedMap<String, String> headers,
                                   byte[] data) throws Exception {
    return service(method, requestURI, baseURI, headers, data, new DummyContainerResponseWriter());
  }
  
  /**
   * Register supplied class as per-request root resource if it has valid
   * JAX-RS annotations and no one resource with the same UriPattern already
   * registered.
   * 
   * @param resourceClass class of candidate to be root resource
   * @param properties optional resource properties. It may contains additional
   *          info about resource, e.g. description of resource, its
   *          responsibility, etc. This info can be retrieved
   *          {@link org.exoplatform.services.rest.ObjectModel#getProperties()}.
   *          This parameter may be <code>null</code>
   */
  public void addResource(final Class<?> resourceClass, MultivaluedMap<String, String> properties) {
    this.binder.addResource(resourceClass, properties);
  }

  /**
   * Register supplied Object as singleton root resource if it has valid JAX-RS
   * annotations and no one resource with the same UriPattern already
   * registered.
   * 
   * @param resource candidate to be root resource
   * @param properties optional resource properties. It may contains additional
   *          info about resource, e.g. description of resource, its
   *          responsibility, etc. This info can be retrieved
   *          {@link org.exoplatform.services.rest.ObjectModel#getProperties()}.
   *          This parameter may be <code>null</code>
   */
  public void addResource(final Object resource, MultivaluedMap<String, String> properties) {
    this.binder.addResource(resource, properties);
  }

  /**
   * Remove the resource instance of provided class from root resource
   * container.
   * 
   * @param clazz the class of resource
   */
  public void removeResource(Class clazz) {
    this.binder.removeResource(clazz);
  }

}
