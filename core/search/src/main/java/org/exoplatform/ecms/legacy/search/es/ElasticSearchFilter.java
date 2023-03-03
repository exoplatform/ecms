/**
 * Copyright (C) 2023 eXo Platform SAS.
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
package org.exoplatform.ecms.legacy.search.es;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 11/27/15
 * @deprecated Copied from commons-search to this module.
 *  Should be reworked to be more simple.
 */
@Deprecated(forRemoval = true, since = "6.0.0")
public class ElasticSearchFilter {

  private ElasticSearchFilterType type;
  private String field;
  private String value;

  public ElasticSearchFilter() {
  }

  public ElasticSearchFilter(ElasticSearchFilterType type, String field, String value) {
    this.type = type;
    this.field = field;
    this.value = value;
  }

  public ElasticSearchFilterType getType() {
    return type;
  }

  public void setType(ElasticSearchFilterType type) {
    this.type = type;
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}

