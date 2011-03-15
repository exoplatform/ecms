/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.portletcache;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class Util
{

  static Map<String, String[]> clone(Map<String, String[]> map)
  {
    if (map.isEmpty())
    {
      return Collections.emptyMap();
    }
    Map<String, String[]> copy = new HashMap<String, String[]>(map);
    for (Map.Entry<String, String[]> entry : copy.entrySet())
    {
      entry.setValue(entry.getValue().clone());
    }
    return copy;
  }

  static int hashCode(Map<String, String[]> map)
  {
    int hashCode = 0;
    if (map.size() > 0)
    {
      for (Map.Entry<String, String[]> parameter : map.entrySet())
      {
        int parameterHashCode = parameter.getKey().hashCode();
        for (String parameterValue : parameter.getValue())
        {
          parameterHashCode = parameterHashCode * 43 + parameterValue.hashCode();
        }
        hashCode = hashCode * 43 + parameterHashCode;
      }
    }
    return hashCode;
  }

  static boolean equals(Map<String, String[]> map1, Map<String, String[]> map2)
  {
    if (map1.keySet().equals(map2.keySet()))
    {
      for (Map.Entry<String, String[]> parameter : map1.entrySet())
      {
        String[] thatParameterValues = map2.get(parameter.getKey());
        if (thatParameterValues != null)
        {
          if (!Arrays.equals(parameter.getValue(), thatParameterValues))
          {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  static void toString(Map<String, String[]> map, StringBuilder sb)
  {
    sb.append('{');
    for (Iterator<Map.Entry<String, String[]>> i = map.entrySet().iterator();i.hasNext();)
    {
      Map.Entry<String, String[]> entry = i.next();
      sb.append(entry.getKey()).append('=').append('[');
      String[] value = entry.getValue();
      for (int j = 0;j < value.length;j++)
      {
        if (j > 0)
        {
          sb.append(',');
        }
        sb.append(value[j]);
      }
      sb.append(']');
      if (i.hasNext())
      {
        sb.append(',');
      }
    }
    sb.append('}');
  }
}
