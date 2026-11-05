/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.search.model;

/**
 * One matched item of a search result's match line.
 */
public class SearchFieldMatch {

  private String label;

  private String valueHtml;

  /**
   * An empty item, for the JSON readers.
   */
  public SearchFieldMatch() {
  }

  /**
   * An item.
   *
   * @param label the item's label, as text
   * @param valueHtml the matched value as HTML: escaped text whose only tags
   *          are {@code <mark>} around the matched words
   */
  public SearchFieldMatch(String label, String valueHtml) {
    this.label = label;
    this.valueHtml = valueHtml;
  }

  /**
   * @return the item's label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the item's label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * @return the matched value as HTML
   */
  public String getValueHtml() {
    return valueHtml;
  }

  /**
   * @param valueHtml the matched value as HTML
   */
  public void setValueHtml(String valueHtml) {
    this.valueHtml = valueHtml;
  }

}
