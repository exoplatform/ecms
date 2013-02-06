/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.comparator;

import java.util.Calendar;
import java.util.Comparator;

import javax.jcr.Node;

import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jan 9, 2013  
 */
public class DateComparator implements Comparator<Node>{

  public static final String ASCENDING_ORDER = "Ascending" ;
  public static final String DESCENDING_ORDER = "Descending" ;
  private String order_ ;

  public DateComparator(String pOrder) {
    order_ = pOrder ;
  }

  public int compare(Node node1, Node node2) {
    try{
      Calendar date1 = node1.hasProperty(NodetypeConstant.EXO_DATE_MODIFIED) ? 
                          node1.getProperty(NodetypeConstant.EXO_DATE_MODIFIED).getDate() : 
                            node1.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
      Calendar date2 = node2.hasProperty(NodetypeConstant.EXO_DATE_MODIFIED) ? 
                          node2.getProperty(NodetypeConstant.EXO_DATE_MODIFIED).getDate() : 
                            node2.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
      
      if(order_.equals(ASCENDING_ORDER)) {
        return date1.compareTo(date2) ;
      }
      return date2.compareTo(date1) ;
    }catch (Exception e) {
      return 0;
    }
  }

}
