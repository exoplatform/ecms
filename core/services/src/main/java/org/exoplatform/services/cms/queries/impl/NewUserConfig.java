 /*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

package org.exoplatform.services.cms.queries.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Benjamin Mestrallet
 * benjamin.mestrallet@exoplatform.com
 */
public class NewUserConfig {
  private String repository ;
  private String template;
  private List<User> users = new ArrayList<User>(5);

  public String getRepository() { return repository ; }
  public void setRepository(String rp) { this.repository = rp; }

  public String getTemplate() { return template; }
  public void setTemplate(String template) { this.template = template; }

  public List<User> getUsers() {   return users; }
  public void setUsers(List<User> s) {  this.users = s; }

  static public class User {
    private String userName ;
    private List<Query>  queries = new ArrayList<Query>(5) ;

    public User() { }

    public String getUserName() {   return userName;  }
    public void setUserName(String userName) {  this.userName = userName;   }

    public List<Query> getQueries() { return queries; }
    public void setQueries(List<Query> queries) { this.queries = queries;  }

  }

  static public class Query {
    private String queryName ;
    private String language;
    private String query;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }
  }
}
