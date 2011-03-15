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
package org.exoplatform.ecm.jcr;

import java.util.Comparator;

import org.exoplatform.ecm.jcr.model.Preference;


public class PropertiesComparator implements Comparator {
  private String order_;

  public PropertiesComparator(String pOrder) {
    order_ = pOrder ;
  }

  public int compare(Object o1, Object o2) throws ClassCastException {
    if(Preference.ASCENDING_ORDER.equals(order_)) {
      return ((String) o1).compareToIgnoreCase((String) o2) ;
    }
    return -1 * ((String) o1).compareToIgnoreCase((String) o2) ;
  }
}
