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
package org.exoplatform.ecm.webui.comparator;

import java.util.Comparator;

import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 15, 2008 10:05:19 AM
 */
public class ItemOptionNameComparator implements Comparator<SelectItemOption> {

  public int compare(SelectItemOption o1, SelectItemOption o2) throws ClassCastException {
    try {
      String name1 = o1.getLabel().toString() ;
      String name2 = o2.getLabel().toString() ;
      return name1.compareToIgnoreCase(name2) ;
    } catch(Exception e) {
      return 0;
    }
  }

}
