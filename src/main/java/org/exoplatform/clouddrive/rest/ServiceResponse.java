/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

/**
 * Helper builder for REST responses. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ResponseBuilder.java 00000 Jan 8, 2013 pnedonosko $
 */
public class ServiceResponse {

  /** The status. */
  Status              status;

  /** The entity. */
  Object              entity;

  /** The headers. */
  Map<String, String> headers = new HashMap<String, String>();

  /** The cookies. */
  List<NewCookie>     cookies = new ArrayList<NewCookie>();

  /**
   * Status.
   *
   * @param status the status
   * @return the service response
   */
  ServiceResponse status(Status status) {
    this.status = status;
    return this;
  }

  /**
   * Ok.
   *
   * @return the service response
   */
  ServiceResponse ok() {
    status = Status.OK;
    return this;
  }

  /**
   * Client error.
   *
   * @param entity the entity
   * @return the service response
   */
  ServiceResponse clientError(Object entity) {
    this.status = Status.BAD_REQUEST;
    this.entity = entity;
    return this;
  }

  /**
   * Error.
   *
   * @param entity the entity
   * @return the service response
   */
  ServiceResponse error(Object entity) {
    this.status = Status.INTERNAL_SERVER_ERROR;
    this.entity = entity;
    return this;
  }

  /**
   * Entity.
   *
   * @param entity the entity
   * @return the service response
   */
  ServiceResponse entity(Object entity) {
    this.entity = entity;
    return this;
  }

  /**
   * Adds the header.
   *
   * @param name the name
   * @param value the value
   * @return the service response
   */
  ServiceResponse addHeader(String name, String value) {
    this.headers.put(name, value);
    return this;
  }

  /**
   * Cookie.
   *
   * @param cookie the cookie
   * @return the service response
   */
  ServiceResponse cookie(NewCookie cookie) {
    cookies.add(cookie);
    return this;
  }

  /**
   * Cookie.
   *
   * @param name the name
   * @param value the value
   * @param path the path
   * @param domain the domain
   * @param comment the comment
   * @param maxAge the max age
   * @param secure the secure
   * @return the service response
   */
  ServiceResponse cookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure) {
    cookies.add(new NewCookie(name, value, path, domain, comment, maxAge, secure));
    return this;
  }

  /**
   * Builds the.
   *
   * @return the response
   */
  Response build() {
    ResponseBuilder builder = Response.status(status != null ? status : Status.OK);

    if (entity != null) {
      builder.entity(entity);
    }

    if (cookies.size() > 0) {
      builder.cookie(cookies.toArray(new NewCookie[cookies.size()]));
    }

    for (Map.Entry<String, String> he : headers.entrySet()) {
      builder.header(he.getKey(), he.getValue());
    }

    return builder.build();
  }

}
