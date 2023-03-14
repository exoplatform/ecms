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
 * Filter type that can be added by search connectors in
 * ES queries (seeElasticSearchServiceConnector#getAdditionalFilters(java.util.List)).
 * Type FILTER_CUSTOM allows to define any type of filter by providing the full content of the filter.
 * @deprecated Copied from commons-search to this module.
 *  Should be reworked to be more simple.
 */
@Deprecated(forRemoval = true, since = "6.0.0")
public enum  ElasticSearchFilterType {
  FILTER_BY_TERM("term"),
  FILTER_EXIST("exist"),
  FILTER_NOT_EXIST("notExist"),
  FILTER_CUSTOM("custom"),
  FILTER_MY_WORK_DOCS("myWork"),
  FILTER_MATADATAS("metaDatas");

  private final String filterType;

  ElasticSearchFilterType(String filterType) {
    this.filterType = filterType;
  }

  @Override
  public String toString() {
    return filterType;
  }

}

