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
 * hoa.pham@exoplatform.com
 * Oct 7, 2008
 */

/*
 * This abstract class help developer can easy create jcr query statement base on query criteria.
 * This class will have 2 implementation for Sql-based query and XPath-based query
 * */
public abstract class AbstractQueryBuilder {

  /** The from clause. */
  protected StringBuilder fromClause = new StringBuilder();

  /** The properties clause. */
  protected StringBuilder propertiesClause = new StringBuilder();

  /** The path clause. */
  protected StringBuilder pathClause= new StringBuilder();

  /** The order by clause. */
  protected StringBuilder orderByClause = new StringBuilder();

  /** The select clause. */
  protected StringBuilder selectClause = new StringBuilder();

  /** The contains clause. */
  protected StringBuilder containsClause = new StringBuilder();

  /** The spell check clause. */
  protected StringBuilder spellCheckClause = new StringBuilder();

  /** The excerpt clause. */
  protected StringBuilder excerptClause = new StringBuilder();

  /**
   * Checks a property is null.
   *
   * @param propertyName the property name
   * @param condition the condition
   */
  public abstract void isNull(String propertyName, LOGICAL condition);

  /**
   * Checks a property is not null.
   *
   * @param propertyName the property name
   * @param condition the condition
   */
  public abstract void isNotNull(String propertyName, LOGICAL condition);

  /**
   * compare less than
   *
   * @param propertyName the property name
   * @param value the value
   * @param condition the condition
   */
  public abstract void lessThan(String propertyName, String value, LOGICAL condition);

  /**
   * Greater than.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void greaterThan(String propName, String value, LOGICAL condition);

  /**
   * Less than or equal.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void lessThanOrEqual(String propName, String value, LOGICAL condition);

  /**
   * Greater or equal.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void greaterOrEqual(String propName,String value, LOGICAL condition);

  /**
   * Equal.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void equal(String propName, String value , LOGICAL condition);

  /**
   * Not equal.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void notEqual(String propName, String value, LOGICAL condition);

  /**
   * Like.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void like(String propName, String value, LOGICAL condition);

  /**
   * Reference.
   *
   * @param propName the prop name
   * @param value the value
   * @param condition the condition
   */
  public abstract void reference(String propName, String value, LOGICAL condition);

  /**
   * Before date.
   *
   * @param datePropertyName the date property name
   * @param date the date
   * @param condition the condition
   */
  public abstract void beforeDate(String datePropertyName, String date, LOGICAL condition);

  /**
   * After date.
   *
   * @param datePropertyName the date property name
   * @param date the date
   * @param condition the condition
   */
  public abstract void afterDate(String datePropertyName, String date, LOGICAL condition);

  /**
   * Between dates.
   *
   * @param datePropertyName the date property name
   * @param startDate the start date
   * @param endDate the end date
   * @param condition the condition
   */
  public abstract void betweenDates(String datePropertyName, String startDate, String endDate, LOGICAL condition);

  /**
   * Sets the query path.
   *
   * @param path the path
   * @param pathtype the pathtype
   */
  public abstract void setQueryPath(String path, PATH_TYPE pathtype);

  /**
   * Contains.
   *
   * @param scope the scope
   * @param term the term
   * @param condition the condition
   */
  public abstract void contains(String scope, String term, LOGICAL condition);

  /**
   * Not contains.
   *
   * @param scope the scope
   * @param term the term
   * @param condition the condition
   */
  public abstract void notContains(String scope, String term, LOGICAL condition);

  /**
   * Excerpt.
   *
   * @param enable the enable
   */
  public abstract void excerpt(boolean enable);

  /**
   * Spell check.
   *
   * @param term the term
   */
  public abstract void spellCheck(String term);

  /**
   * Order by.
   *
   * @param properyName the propery name
   * @param orderby the orderby
   */
  public abstract void orderBy(String properyName, ORDERBY orderby);

  /**
   * Select types.
   *
   * @param returnTypes the return types
   */
  public abstract void selectTypes(String[] returnTypes);

  /**
   * From node types.
   *
   * @param nodetypes the nodetypes
   */
  public abstract void fromNodeTypes(String[] nodetypes);

  /**
   * Open group condition for where clause
   *
   * @param logical the logical
   */
  public abstract void openGroup(LOGICAL logical);

  /**
   * Close group.
   */
  public abstract void closeGroup();

  /**
   * Creates the query statement.
   *
   * @return the string
   */
  public abstract String createQueryStatement();

  /**
   * Merge.
   *
   * @param other the other
   */
  public abstract void merge(AbstractQueryBuilder other);

  /**
   * The Class QueryTermHelper.
   */
  public static class QueryTermHelper {

    /** The term. */
    private String term = "";

    /**
     * Instantiates a new query term helper.
     */
    public QueryTermHelper() { }

    /**
     * Contains a word or phase
     *
     * @param s the s
     *
     * @return the query term helper
     */
    public QueryTermHelper contains(String s) {
      term = term.concat(s);
      return this;
    }

    /**
     * Not contains a word or phase
     *
     * @param s the s
     *
     * @return the query term helper
     */
    public QueryTermHelper notContains(String s) {
      if(s.indexOf(" ")>0)
        term = term.concat("-\\" + s + "\\");
      else
        term = term.concat("-"+ s);
      return this;
    }

    /**
     * Builds the term.
     *
     * @return the string
     */
    public String buildTerm() { return term; }

    /**
     * Allow fuzzy search.
     *
     * @return the query term helper
     */
    public QueryTermHelper allowFuzzySearch(double fuzzySearchIndex) {
      term = term.concat("~").concat(String.valueOf(fuzzySearchIndex));
      return this;
    }

    /**
     * Allow synonym search.
     *
     * @return the query term helper
     */
    public QueryTermHelper allowSynonymSearch() {
      term =  "~".concat(term);
      return this;
    }

    /**
     *
     * @param fuzzySearchIndex
     * @param separator
     * @return
     */
    public QueryTermHelper appendAfter(final double fuzzySearchIndex, final String separator) {

      if (separator == null) {
        return this;
      }
      final int pos = term.indexOf(separator);
      if (pos == -1) {
        return this;
      }
      term.substring(pos + separator.length());
      term =  term.concat("~").concat(String.valueOf(fuzzySearchIndex));
      return this;
    }

  }

  /**
   * The Enum type of path constraints can be used in query statement
   */
  public enum PATH_TYPE { EXACT, CHILDNODES, DECENDANTS, DECENDANTS_OR_SELFT };

  /**
   * The Enum LOGICAL function for query
   */
  public enum LOGICAL { AND, OR, NULL, NOT, AND_NOT, OR_NOT};

  /**
   * The Enum ORDERBY function for query
   */
  public enum ORDERBY { ASC, DESC };
  
  /**
   * The Enum COMPARISON_TYPE function for query
   */
  public enum COMPARISON_TYPE {
    EQUAL, LIKE
  };
}
