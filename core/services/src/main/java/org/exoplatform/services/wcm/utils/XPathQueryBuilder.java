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



/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 *          hoa.pham@exoplatform.com
 * Oct 7, 2008
 */
public class XPathQueryBuilder extends AbstractQueryBuilder {

  public void isNull(String propertyName, LOGICAL condition) {
  }

  public void isNotNull(String propertyName, LOGICAL condition) {
  }

  public void lessThan(String propertyName, String value, LOGICAL condition) {
    comparison(propertyName,value,condition,"<");
  }

  public void greaterThan(String propName, String value, LOGICAL condition) {
    comparison(propName,value,condition,">");
  }

  public void lessThanOrEqual(String propName, String value, LOGICAL condition) {
    comparison(propName,value,condition,"<=");
  }

  public void greaterOrEqual(String propName,String value, LOGICAL condition) {
    comparison(propName,value,condition,">=");
  }

  public void equal(String propName, String value , LOGICAL condition) {
    comparison(propName,value,condition,"=");
  }

  public void notEqual(String propName, String value, LOGICAL condition) {
    if(condition == LOGICAL.AND)
      propertiesClause.append("and @").append(propName).append(" != '").append(value).append("' ");
    else if(condition == LOGICAL.OR)
      propertiesClause.append("or @").append(" != '").append(value).append("' ");
    return;
  }

  private void comparison(String propName, String value, LOGICAL condition, String symbol) {
  }

  public void like(String propName, String value, LOGICAL condition) {
  }

  @Override
  public void afterDate(String datePropertyName, String date, LOGICAL condition) {
  }

  @Override
  public void beforeDate(String datePropertyName, String date, LOGICAL condition) {
  }

  @Override
  public void betweenDates(String datePropertyName, String startDate, String endDate,
      LOGICAL condition) {
  }

  @Override
  public void contains(String scope, String term, LOGICAL condition) {
  }

  @Override
  public String createQueryStatement() {
    return null;
  }

  @Override
  public void merge(AbstractQueryBuilder other) {
  }

  @Override
  public void notContains(String scope, String term, LOGICAL condition) {
  }

  @Override
  public void orderBy(String properyName, ORDERBY orderby) {
  }

  @Override
  public void reference(String propName, String value, LOGICAL condition) {
  }

  public void spellCheck(String value) {
  }

  @Override
  public void setQueryPath(String path, PATH_TYPE pathtype) {
  }

  @Override
  public void excerpt(boolean enable) {
  }

  @Override
  public void fromNodeTypes(String[] nodetypes) {
  }

  @Override
  public void selectTypes(String[] returnTypes) {
  }

  @Override
  public void closeGroup() {
  }

  @Override
  public void openGroup(LOGICAL logical) {
  }
}
