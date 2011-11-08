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
package org.exoplatform.services.wcm.search;

import java.util.Calendar;

import org.exoplatform.services.wcm.utils.AbstractQueryBuilder.COMPARISON_TYPE;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Oct 7, 2008
 */
/*
 * This is query criteria for SiteSearch service. Base on search criteria, SiteSearch service
 * can easy create query statement to search.
 * */
public class QueryCriteria {

  /** The Constant ALL_PROPERTY_SCOPE. */
  public static final String ALL_PROPERTY_SCOPE = ".";

  /** The site name. */
  private String siteName;

  /** The categories. */
  private String[] categoryUUIDs = null;

  /** The tags. */
  private String[] tagUUIDs = null;

  /** The start publication date. */
  private DatetimeRange startPublicationDateRange = null;

  /** The end publication date. */
  private DatetimeRange endPublicationDateRange = null;

  /** The authors. */
  private String[] authors = null;

  /** The content types. */
  private String[] contentTypes = null;

  /** The mime types. */
  private String[] mimeTypes = null;
  
  /** The node types. */
  private String[] nodeTypes = null;

  /** The created date. */
  private DatetimeRange createdDateRange = null;

  /** The last modified date. */
  private DatetimeRange lastModifiedDateRange = null;

  /** The search webpage. */
  private boolean searchWebpage = true;

  /** The search document. */
  private boolean searchDocument = true;

  /** The keyword. */
  private String keyword = null;

  /** The search web content. */
  private boolean searchWebContent = true;

  /** The fulltext search. */
  private boolean fulltextSearch = true;

  /** The is live mode. */
  private boolean isLiveMode = true;

  /** The starting offset */
  private long offset = -1;

  /** Pagination mode :
   * - none : no pagination
   * - more : twitter like pagination with "more" link
   * - pagination : pagination mode with page number and total size */
  private String pageMode = SiteSearchService.PAGE_MODE_NONE;

  public String getPageMode() {
    return pageMode;
  }

  public void setPageMode(String pageMode) {
    this.pageMode = pageMode;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }
  
  /**
   * Checks if is live mode.
   *
   * @return true, if is live mode
   */
  public boolean isLiveMode() {
    return isLiveMode;
  }

  /**
   * Sets the live mode.
   *
   * @param isLiveMode the new live mode
   */
  public void setLiveMode(boolean isLiveMode) {
    this.isLiveMode = isLiveMode;
  }

  /** The query metadatas. */
  private QueryProperty[] queryMetadatas = null;

  /** The fulltext search property. */
  private String[]            fulltextSearchProperty = new String[] { ALL_PROPERTY_SCOPE };

  /** The date range selected. */
  private DATE_RANGE_SELECTED dateRangeSelected = null;

  /**
   * Gets the site name.
   *
   * @return the site name
   */
  public String getSiteName() { return siteName; }

  /**
   * Sets the site name.
   *
   * @param siteName the new site name
   */
  public void setSiteName(String siteName) { this.siteName = siteName; }


  /**
   * Gets the authors.
   *
   * @return the authors
   */
  public String[] getAuthors() { return authors; }

  /**
   * Sets the authors.
   *
   * @param authors the new authors
   */
  public void setAuthors(String[] authors) { this.authors = authors; }

  /**
   * Gets the content types.
   *
   * @return the content types
   */
  public String[] getContentTypes() { return contentTypes; }

  /**
   * Sets the content types.
   *
   * @param contentTypes the new content types
   */
  public void setContentTypes(String[] contentTypes) { this.contentTypes = contentTypes; }

  /**
   * Gets the mime types.
   *
   * @return the mime types
   */
  public String[] getMimeTypes() { return mimeTypes; }

  /**
   * Sets the mime types.
   *
   * @param mimeTypes the new mime types
   */
  public void setMimeTypes(String[] mimeTypes) { this.mimeTypes = mimeTypes; }
  
  
  public String[] getNodeTypes() {
    return nodeTypes;
  }
  
  public void setNodeTypes(String[] nodeTypes) {
    this.nodeTypes = nodeTypes;
  }

  /**
   * Gets the start publication date range.
   *
   * @return the start publication date range
   */
  public DatetimeRange getStartPublicationDateRange() {
    return startPublicationDateRange;
  }

  /**
   * Sets the start publication date range.
   *
   * @param startPublicationDateRange the new start publication date range
   */
  public void setStartPublicationDateRange(DatetimeRange startPublicationDateRange) {
    this.startPublicationDateRange = startPublicationDateRange;
  }

  /**
   * Gets the end publication date range.
   *
   * @return the end publication date range
   */
  public DatetimeRange getEndPublicationDateRange() {
    return endPublicationDateRange;
  }

  /**
   * Sets the end publication date range.
   *
   * @param endPublicationDateRange the new end publication date range
   */
  public void setEndPublicationDateRange(DatetimeRange endPublicationDateRange) {
    this.endPublicationDateRange = endPublicationDateRange;
  }

  /**
   * Gets the created date range.
   *
   * @return the created date range
   */
  public DatetimeRange getCreatedDateRange() {
    return createdDateRange;
  }

  /**
   * Sets the created date range.
   *
   * @param createdDateRange the new created date range
   */
  public void setCreatedDateRange(DatetimeRange createdDateRange) {
    this.createdDateRange = createdDateRange;
  }

  /**
   * Gets the last modified date range.
   *
   * @return the last modified date range
   */
  public DatetimeRange getLastModifiedDateRange() {
    return lastModifiedDateRange;
  }

  /**
   * Sets the last modified date range.
   *
   * @param lastModifiedDateRange the new last modified date range
   */
  public void setLastModifiedDateRange(DatetimeRange lastModifiedDateRange) {
    this.lastModifiedDateRange = lastModifiedDateRange;
  }

  /**
   * Checks if is fulltext search.
   *
   * @return true, if is fulltext search
   */
  public boolean isFulltextSearch() {
    return fulltextSearch;
  }


  /**
   * Sets the fulltext search.
   *
   * @param fulltextSearch the new fulltext search
   */
  public void setFulltextSearch(boolean fulltextSearch) {
    this.fulltextSearch = fulltextSearch;
  }

  /**
   * Gets the keyword.
   *
   * @return the keyword
   */
  public String getKeyword() { return this.keyword; }

  /**
   * Sets the keyword.
   *
   * @param s the new keyword
   */
  public void setKeyword(String s) { this.keyword = s; }

  /**
   * Checks if is search webpage.
   *
   * @return true, if is search webpage
   */
  public boolean isSearchWebpage() { return searchWebpage;}

  /**
   * Sets the search webpage.
   *
   * @param searchWebpage the new search webpage
   */
  public void setSearchWebpage(boolean searchWebpage) {
    this.searchWebpage = searchWebpage;
  }

  /**
   * Checks if is search document.
   *
   * @return true, if is search document
   */
  public boolean isSearchDocument() { return searchDocument;}

  /**
   * Sets the search document.
   *
   * @param searchDocument the new search document
   */
  public void setSearchDocument(boolean searchDocument) {
    this.searchDocument = searchDocument;
  }

  /**
   * Checks if is search web content.
   *
   * @return true, if is search web content
   */
  public boolean isSearchWebContent() {
    return searchWebContent;
  }

  /**
   * Sets the search web content.
   *
   * @param searchWebContent the new search web content
   */
  public void setSearchWebContent(boolean searchWebContent) {
    this.searchWebContent = searchWebContent;
  }

  /**
   * The Class DatetimeRange.
   */
  public static class DatetimeRange {

    /** The from date. */
    private Calendar fromDate;

    /** The to date. */
    private Calendar toDate;

    /**
     * Instantiates a new datetime range.
     *
     * @param fromDate the from date
     * @param toDate the to date
     */
    public DatetimeRange(Calendar fromDate, Calendar toDate) {
      this.fromDate = fromDate;
      this.toDate = toDate;
    }

    /**
     * Gets the from date.
     *
     * @return the from date
     */
    public Calendar getFromDate() {
      return fromDate;
    }

    /**
     * Sets the from date.
     *
     * @param fromDate the new from date
     */
    public void setFromDate(Calendar fromDate) {
      this.fromDate = fromDate;
    }

    /**
     * Gets the to date.
     *
     * @return the to date
     */
    public Calendar getToDate() {
      return toDate;
    }

    /**
     * Sets the to date.
     *
     * @param toDate the new to date
     */
    public void setToDate(Calendar toDate) {
      this.toDate = toDate;
    }
  }

  /**
   * The Enum DATE_RANGE_SELECTED.
   */
  public enum DATE_RANGE_SELECTED {
    /** The CREATED. */
    CREATED, /** The MODIFIDED. */
    MODIFIDED, /** The STAR t_ publication. */
    START_PUBLICATION, /** The EN d_ publication. */
    END_PUBLICATION
  }

  /**
   * Gets the date range selected.
   *
   * @return the date range selected
   */
  public DATE_RANGE_SELECTED getDateRangeSelected() {
    return dateRangeSelected;
  }

  /**
   * Sets the date range selected.
   *
   * @param dateRangeSelected the new date range selected
   */
  public void setDateRangeSelected(DATE_RANGE_SELECTED dateRangeSelected) {
    this.dateRangeSelected = dateRangeSelected;
  }

  /**
   * Gets the category uui ds.
   *
   * @return the category uui ds
   */
  public String[] getCategoryUUIDs() {
    return categoryUUIDs;
  }

  /**
   * Sets the category uui ds.
   *
   * @param categoryUUIDs the new category uui ds
   */
  public void setCategoryUUIDs(String[] categoryUUIDs) {
    this.categoryUUIDs = categoryUUIDs;
  }

  /**
   * Gets the tag uui ds.
   *
   * @return the tag uui ds
   */
  public String[] getTagUUIDs() {
    return tagUUIDs;
  }

  /**
   * Sets the tag uui ds.
   *
   * @param tagUUIDs the new tag uui ds
   */
  public void setTagUUIDs(String[] tagUUIDs) {
    this.tagUUIDs = tagUUIDs;
  };


  /**
   * The Class QueryProperty.
   */
  public class QueryProperty {

    /** The name. */
    private String name;

    /** The value. */
    private String value;
    
    /** The comparison type */
    private COMPARISON_TYPE comparisonType;

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    public void setValue(String value) {
      this.value = value;
    }

    /**
     * @param comparisonType the comparisonType to set
     */
    public void setComparisonType(COMPARISON_TYPE comparisonType) {
      this.comparisonType = comparisonType;
    }

    /**
     * @return the comparisonType
     */
    public COMPARISON_TYPE getComparisonType() {
      if (comparisonType == null) {
        return COMPARISON_TYPE.LIKE;
      }
      return comparisonType;
    }
  }


  /**
   * Gets the query metadatas.
   *
   * @return the query metadatas
   */
  public QueryProperty[] getQueryMetadatas() {
    return queryMetadatas;
  }

  /**
   * Sets the query metadatas.
   *
   * @param queryMetadatas the new query metadatas
   */
  public void setQueryMetadatas(QueryProperty[] queryMetadatas) {
    this.queryMetadatas = queryMetadatas;
  }

  /**
   * Gets the fulltext search property.
   *
   * @return the fulltext search property
   */
  public String[] getFulltextSearchProperty() {
    return fulltextSearchProperty;
  }

  /**
   * Sets the fulltext search property.
   *
   * @param fulltextSearchProperty the new fulltext search property
   */
  public void setFulltextSearchProperty(String[] fulltextSearchProperty) {
    this.fulltextSearchProperty = fulltextSearchProperty;
  }
}
