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
package org.exoplatform.services.document.impl.diff;

import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.document.diff.ToString;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

/**
 * This class delegates handling of the to a StringBuffer based version.
 * 
 * @version $Revision: 1.3 $ $Date: 2003/10/13 08:00:44 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 */
public class ToStringImpl implements ToString
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.ToStringImpl");

   public ToStringImpl()
   {
   }

   /*
    * (non-Javadoc)
    * @see org.exoplatform.services.diff.ToString#toString()
    */
   @Override
   public String toString()
   {
      StringBuffer s = new StringBuffer();
      toString(s);
      return s.toString();
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.diff.ToString#toString(java.lang.StringBuffer)
    */
   public void toString(StringBuffer s)
   {
      s.append(super.toString());
   }

   /**
    * Breaks a string into an array of strings. Use the value of the
    * <code>line.separator</code> system property as the linebreak character.
    * 
    * @param value the string to convert.
    */
   public String[] stringToArray(String value)
   {
      BufferedReader reader = new BufferedReader(new StringReader(value));
      List l = new LinkedList();
      String s;
      try
      {
         while ((s = reader.readLine()) != null)
         {
            l.add(s);
         }
      }
      catch (java.io.IOException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return (String[])l.toArray(new String[l.size()]);
   }

   /**
    * Converts an array of {@link Object Object} to a string Use the value of the
    * <code>line.separator</code> system property the line separator.
    * 
    * @param o the array of objects.
    */
   public String arrayToString(Object[] o)
   {
      return arrayToString(o, PrivilegedSystemHelper.getProperty("line.separator"));
   }

   /**
    * Converts an array of {@link Object Object} to a string using the given line
    * separator.
    * 
    * @param o the array of objects.
    * @param EOL the string to use as line separator.
    */
   public String arrayToString(Object[] o, String EOL)
   {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < o.length - 1; i++)
      {
         buf.append(o[i]);
         buf.append(EOL);
      }
      buf.append(o[o.length - 1]);
      return buf.toString();
   }
}
