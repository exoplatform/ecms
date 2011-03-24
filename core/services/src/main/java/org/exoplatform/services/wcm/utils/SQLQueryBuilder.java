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
package org.exoplatform.services.wcm.utils;

import java.util.Calendar;

import org.exoplatform.commons.utils.ISO8601;



/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Oct 7, 2008
 */

/*
 * This class is implementation of AbstractQueryBuilder for sql-based query.
 * This class help developer create sql query statement easier.
 * */
public class SQLQueryBuilder extends AbstractQueryBuilder {

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#isNull(java.lang
   * .String, org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void isNull(String propertyName, LOGICAL condition) {
    if(condition == LOGICAL.AND)
      propertiesClause.append(" AND").append(propertyName).append("IS NULL ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append(" OR").append(propertyName).append("IS NULL ");
    else
      propertiesClause.append(propertyName).append("IS NULL ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#isNotNull(java
   * .lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void isNotNull(String propertyName, LOGICAL condition) {
    if(condition == LOGICAL.AND)
      propertiesClause.append("AND ").append(propertyName).append(" IS NOT NULL ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("OR ").append(propertyName).append(" IS NOT NULL ");
    else
      propertiesClause.append(propertyName).append(" IS NOT NULL ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#lessThan(java.
   * lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void lessThan(String propertyName, String value, LOGICAL condition) {
    comparison(propertyName,value,condition,"<");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#greaterThan(java
   * .lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void greaterThan(String propName, String value, LOGICAL condition) {
    comparison(propName,value,condition,">");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#lessThanOrEqual
   * (java.lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void lessThanOrEqual(String propName, String value, LOGICAL condition) {
    comparison(propName,value,condition,"<=");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#greaterOrEqual
   * (java.lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void greaterOrEqual(String propName,String value, LOGICAL condition) {
    comparison(propName,value,condition,">=");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#equal(java.lang
   * .String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void equal(String propName, String value , LOGICAL condition) {
    comparison(propName,value,condition,"=");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#notEqual(java.
   * lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void notEqual(String propName, String value, LOGICAL condition) {
    if(condition == LOGICAL.AND)
      propertiesClause.append("AND ").append(propName).append(" <> '").append(value).append("' ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("OR ").append(propName).append(" <> '").append(value).append("' ");
    else
      propertiesClause.append(propName).append(" <> '").append(value).append("' ");
  }

  /**
   * Comparison.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   * @param symbol the symbol
   */
  private void comparison(String propName, String value, LOGICAL condition, String symbol) {
    if(condition == LOGICAL.AND)
      propertiesClause.append("AND ").append(propName).append(" ").append(symbol).append(" '").append(value).append("' ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("OR ").append(propName).append(" ").append(symbol).append(" '").append(value).append("' ");
    else
      propertiesClause.append(propName).append(" ").append(symbol).append(" '").append(value).append("' ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#like(java.lang
   * .String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void like(String propName, String value, LOGICAL condition) {
    if(condition == LOGICAL.AND)
      propertiesClause.append("AND ").append(propName).append(" LIKE '").append(value).append("%' ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("OR ").append(propName).append(" LIKE '").append(value).append("%' ");
    else
      propertiesClause.append(propName).append(" LIKE '").append(value).append("%' ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#reference(java
   * .lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void reference(String propName, String value, LOGICAL condition) {
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#beforeDate(java
   * .lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void beforeDate(String propName, String comparedDate, LOGICAL condition) {
    Calendar calendar = ISO8601.parse(comparedDate);
    String time = calendar.getTime().toString();
    if(condition == LOGICAL.AND)
      propertiesClause.append("AND ").append(propName).append(" <= '").append(time).append("' ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("OR ").append(propName).append(" <= '").append(time).append("' ");
    else

      propertiesClause.append(propName).append(" <= '").append(time).append("' ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#afterDate(java
   * .lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void afterDate(String propName, String comparedDate, LOGICAL condition) {
    Calendar calendar = ISO8601.parse(comparedDate);
    String time = calendar.getTime().toString();
    if(condition == LOGICAL.AND)
      propertiesClause.append("AND ").append(propName).append(" >= '").append(time).append("' ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("OR ").append(propName).append(" >= '").append(time).append("' ");
    else
      propertiesClause.append(propName).append(" >= '").append(time).append("' ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#betweenDates(java
   * .lang.String, java.lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void betweenDates(String propName, String startDate, String endDate, LOGICAL condition) {
    String startTime = ISO8601.parse(startDate).getTime().toString();
    String endTime = ISO8601.parse(endDate).getTime().toString();
    if (condition == LOGICAL.AND)
      propertiesClause.append("AND ")
                      .append(propName)
                      .append(" between TIMESTAMP '")
                      .append(startTime)
                      .append("' and TIMESTAMP '")
                      .append(endTime)
                      .append("' ");
    else if (condition == LOGICAL.OR)
      propertiesClause.append("OR ")
                      .append(propName)
                      .append(" between TIMESTAMP '")
                      .append(startTime)
                      .append("' and  TIMESTAMP '")
                      .append(endTime)
                      .append("' ");
    else
      propertiesClause.append(propName)
                      .append(" between TIMESTAMP '")
                      .append(startTime)
                      .append("' and TIMESTAMP '")
                      .append(endTime)
                      .append("' ");
  }

  public void betweenDates(String propName, Calendar startDate, Calendar endDate, LOGICAL condition) {
    String startTime = ISO8601.format(startDate);
    String endTime = ISO8601.format(endDate);
    if (condition == LOGICAL.AND)
      propertiesClause.append("AND ")
                      .append(propName)
                      .append(" between TIMESTAMP '")
                      .append(startTime)
                      .append("' and TIMESTAMP '")
                      .append(endTime)
                      .append("' ");
    else if (condition == LOGICAL.OR)
      propertiesClause.append("OR ")
                      .append(propName)
                      .append(" between TIMESTAMP '")
                      .append(startTime)
                      .append("' and TIMESTAMP '")
                      .append(endTime)
                      .append("' ");
    else
      propertiesClause.append(propName)
                      .append(" between TIMESTAMP '")
                      .append(startTime)
                      .append("' and TIMESTAMP '")
                      .append(endTime)
                      .append("' ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#setQueryPath(java
   * .lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.PATH_TYPE)
   */
  public void setQueryPath(String path, PATH_TYPE pathtype) {
    if (PATH_TYPE.EXACT == pathtype) {
      if (path.indexOf("[%]") > 0)
        pathClause = new StringBuilder().append("jcr:path LIKE '").append(path).append("' ");
      else
        pathClause = new StringBuilder().append("jcr:path = '").append(path).append("' ");
    } else if (PATH_TYPE.CHILDNODES == pathtype) {
      pathClause = new StringBuilder().append("jcr:path LIKE '")
                                      .append(path)
                                      .append("/%'")
                                      .append("AND NOT jcr:path like '")
                                      .append(path)
                                      .append("/%/%' ");
    } else if (PATH_TYPE.DECENDANTS == pathtype) {
      pathClause = new StringBuilder().append("jcr:path LIKE '").append(path).append("/%' ");
    } else if (PATH_TYPE.DECENDANTS_OR_SELFT == pathtype) {
      pathClause = new StringBuilder().append("jcr:path LIKE '")
                                      .append(path)
                                      .append("'")
                                      .append("OR jcr:path LIKE '")
                                      .append(path)
                                      .append("/%' ");
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#contains(java.
   * lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void contains(String scope, String term, LOGICAL condition) {
    if (scope == null)
      scope = ".";
    if (LOGICAL.AND == condition)
      containsClause.append("AND CONTAINS(").append(scope).append(",'").append(term).append("') ");
    else if (LOGICAL.OR == condition)
      containsClause.append("OR CONTAINS(").append(scope).append(",'").append(term).append("') ");
    else
      containsClause.append("CONTAINS(").append(scope).append(",'").append(term).append("') ");
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#notContains(java
   * .lang.String, java.lang.String,
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void notContains(String scope, String term, LOGICAL condition) {
    if (scope == null)
      scope = ".";
    if (LOGICAL.AND == condition)
      containsClause.append("AND NOT CONTAINS(")
                    .append(scope)
                    .append(",'")
                    .append(term)
                    .append("') ");
    else if (LOGICAL.OR == condition)
      containsClause.append("OR NOT CONTAINS(")
                    .append(scope)
                    .append(",'")
                    .append(term)
                    .append("') ");
    else
      containsClause.append("NOT CONTAINS(").append(scope).append(",'").append(term).append("') ");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.AbstractQueryBuilder#fromNodeTypes(java.lang.String[])
   */
  public void fromNodeTypes(String[] nodetypes) {
    if(nodetypes == null) {
      fromClause = new StringBuilder("FROM nt:base");
      return;
    }
    fromClause = new StringBuilder("FROM ");
    for(int i = 0; i<nodetypes.length; i++) {
      fromClause.append(nodetypes[i]);
      if(i<nodetypes.length-1)
        fromClause.append(",");
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.AbstractQueryBuilder#selectTypes(java.lang.String[])
   */
  public void selectTypes(String[] returnTypes) {
    if(returnTypes == null) {
      selectClause = new StringBuilder("SELECT * ");
      return;
    }
    selectClause = new StringBuilder("SELECT ");
    for(int i = 0; i<returnTypes.length; i++) {
      selectClause.append(returnTypes[i]);
      if(i<returnTypes.length-1)
        selectClause.append(",");
      selectClause.append(" ");
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#orderBy(java.lang
   * .String, org.exoplatform.services.wcm.search.AbstractQueryBuilder.ORDERBY)
   */
  public void orderBy(String properyName, ORDERBY orderby) {
    if(orderByClause.length()>0)
      orderByClause = orderByClause.append(", ");
    if(ORDERBY.ASC == orderby)
      orderByClause.append(properyName).append(" ASC");
    else
      orderByClause.append(properyName).append(" DESC");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.AbstractQueryBuilder#excerpt(boolean)
   */
  public void excerpt(boolean enable) {
    if(enable)
      excerptClause = new StringBuilder("excerpt(.)");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.AbstractQueryBuilder#spellCheck(java.lang.String)
   */
  public void spellCheck(String value) {
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#openGroup(org.
   * exoplatform.services.wcm.search.AbstractQueryBuilder.LOGICAL)
   */
  public void openGroup(LOGICAL logical) {
    if(LOGICAL.AND == logical)
    propertiesClause = propertiesClause.append("AND( ");
    else if(LOGICAL.OR == logical)
      propertiesClause = propertiesClause.append("OR( ");
    else if(LOGICAL.AND_NOT == logical)
      propertiesClause = propertiesClause.append("AND NOT(");
    else if(LOGICAL.OR_NOT == logical)
      propertiesClause = propertiesClause.append("OR NOT(");
    else
      propertiesClause = propertiesClause.append("( ");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.AbstractQueryBuilder#closeGroup()
   */
  public void closeGroup() {
    propertiesClause = propertiesClause.append(")");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.search.AbstractQueryBuilder#createQueryStatement()
   */
  public String createQueryStatement() {
    StringBuffer statement = new StringBuffer();
    statement = statement.append(selectClause.toString())
                         .append(fromClause.toString())
                         .append(" WHERE ");
    if (containsClause.length() > 0) {
      statement = statement.append(containsClause.toString());
      if (pathClause.length() > 0) {
        statement = statement.append("AND ").append(pathClause.toString());
      }
    } else {
      if (pathClause.length() > 0) {
        statement = statement.append(pathClause.toString());
      }
    }
    if (propertiesClause.length() > 0) {
      statement = statement.append(propertiesClause.toString());
    }
    if (orderByClause.length() > 0) {
      statement = statement.append("ORDER BY ").append(orderByClause.toString());
    }
    return statement.toString();
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.search.AbstractQueryBuilder#merge(org.exoplatform
   * .services.wcm.search.AbstractQueryBuilder)
   */
  public void merge(AbstractQueryBuilder other) {
  }

  public void queryByNodeName(String rootPath, String nodeName) {
    pathClause = new StringBuilder().append(" jcr:path LIKE '")
                                    .append(rootPath)
                                    .append("/%/")
                                    .append(nodeName)
                                    .append("' ")
                                    .append(" or jcr:path like '")
                                    .append(rootPath)
                                    .append("/")
                                    .append(nodeName)
                                    .append("' ");
  }
}
