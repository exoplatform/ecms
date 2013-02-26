/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

  Status              status;

  Object              entity;

  Map<String, String> headers = new HashMap<String, String>();

  List<NewCookie>     cookies = new ArrayList<NewCookie>();

  ServiceResponse status(Status status) {
    this.status = status;
    return this;
  }

  ServiceResponse ok() {
    status = Status.OK;
    return this;
  }

  ServiceResponse clientError(Object entity) {
    this.status = Status.BAD_REQUEST;
    this.entity = entity;
    return this;
  }

  ServiceResponse error(Object entity) {
    this.status = Status.INTERNAL_SERVER_ERROR;
    this.entity = entity;
    return this;
  }

  ServiceResponse entity(Object entity) {
    this.entity = entity;
    return this;
  }
  
  ServiceResponse addHeader(String name, String value) {
    this.headers.put(name, value);
    return this;
  }

  ServiceResponse cookie(NewCookie cookie) {
    cookies.add(cookie);
    return this;
  }

  ServiceResponse cookie(String name,
                         String value,
                         String path,
                         String domain,
                         String comment,
                         int maxAge,
                         boolean secure) {
    cookies.add(new NewCookie(name, value, path, domain, comment, maxAge, secure));
    return this;
  }

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
