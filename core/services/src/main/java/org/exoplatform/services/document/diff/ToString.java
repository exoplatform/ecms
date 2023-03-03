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
package org.exoplatform.services.document.diff;

public interface ToString
{

   /**
    * Default implementation of the {@link java.lang.Object#toString toString() }
    * method that delegates work to a {@link java.lang.StringBuffer StringBuffer}
    * base version.
    */
   public abstract String toString();

   /**
    * Place a string image of the object in a StringBuffer.
    * 
    * @param s the string buffer.
    */
   public abstract void toString(StringBuffer s);

}
