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
package org.exoplatform.services.wcm.friendly.impl;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.filter.Filter;
import org.gatein.common.http.QueryStringParser;
import org.gatein.common.util.ParameterMap;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 2, 2013
 * Converts friendly url to unfriendly url.
 * Wraps request by new one containing unfriendly url. 
 */
public class FriendlyFilter implements Filter {

  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                           ServletException {
    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    if (request instanceof HttpServletRequest) {
      String uri = ((HttpServletRequest)request).getRequestURI();
      String friendlyUri = friendlyService.getUnfriendlyUri(uri);
      if (uri != null && !uri.equals(friendlyUri)) {
        chain.doFilter(new FriendlyServletRequestWrapper((HttpServletRequest)request), 
                       response);
      } else {
        chain.doFilter(request, response);
      }
    } else {
      chain.doFilter(request, response);
    }
  }
  
  /**
   * Wraps servlet request and change friendly url to unfriendy url.
   * @author vuna
   *
   */
  public class FriendlyServletRequestWrapper extends HttpServletRequestWrapper {

    private ParameterMap newParamMap_;
    private String uri_ = "";
    private String url_ = "";
    private String queryString_ = "";
    
    public FriendlyServletRequestWrapper(HttpServletRequest request) {
      super(request);
      FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
      uri_ = friendlyService.getUnfriendlyUri(request.getRequestURI());
      url_ = friendlyService.getUnfriendlyUri(request.getRequestURL().toString());
      int id = uri_.indexOf("?");
      if (id > -1) {
        queryString_ = uri_.substring(id + 1);
        uri_ = uri_.substring(0, id);
      }
      id = url_.indexOf("?");
      if (id > -1) {
        url_ = url_.substring(0, id);
      }
      
      newParamMap_ = QueryStringParser.getInstance().parseQueryString(queryString_);
      request.getParameterNames();
    }
    
    @Override
    public String getParameter(String name) {
      String[] rets = newParamMap_.get(name);
      return (rets != null && rets.length > 0)? rets[0] : super.getParameter(name); 
    }
    
    @Override
    public Enumeration getParameterNames() {
      return new FriendlyEnumeration(this.getParameterMap());
    }
    
    @Override
    public String[] getParameterValues(String name) {
      String[] ret = newParamMap_.get(name);
      return (ret != null)? ret : super.getParameterValues(name);
    }
    
    @Override
    public Map getParameterMap() {
      Map oldMap = super.getParameterMap();
      if (newParamMap_ == null) {
        return oldMap;
      }
      if (oldMap == null) {
        return newParamMap_;
      }
      Map ret = new HashMap();
      ret.putAll(oldMap);
      ret.putAll(newParamMap_);
      return ret;
    }
    
    @Override
    public String getQueryString() {
      return queryString_;
    }
    
    @Override
    public String getRequestURI() {
      return uri_;
    }
    
    @Override
    public StringBuffer getRequestURL() {
      return new StringBuffer(url_);
    }
    
    public class FriendlyEnumeration implements Enumeration {
      private Iterator iter_ = null;
      public FriendlyEnumeration(Map paramMap) {
        if (paramMap != null) {
          iter_ = paramMap.keySet().iterator();
        }
      }

      @Override
      public boolean hasMoreElements() {
        return (iter_ == null) ? false : iter_.hasNext();
      }

      @Override
      public Object nextElement() {
        return (iter_ == null) ? null : iter_.next();
      }
      
    }
    
  }

}
