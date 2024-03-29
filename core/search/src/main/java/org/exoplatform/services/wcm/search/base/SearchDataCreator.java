/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.wcm.search.base;

import javax.jcr.Node;
import javax.jcr.query.Row;

import org.exoplatform.ecms.legacy.search.data.SearchResult;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Jun 20, 2011  
 */
public interface SearchDataCreator<E> {
  
  public E createData(Node node, Row row, SearchResult searchResult);

}
