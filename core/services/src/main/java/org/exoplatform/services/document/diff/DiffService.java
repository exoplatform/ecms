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

import org.exoplatform.commons.utils.PrivilegedSystemHelper;

/**
 * Implements a differencing engine that works on arrays of {@link Object
 * Object}.
 * <p>
 * Within this library, the word <i>text</i> means a unit of information subject
 * to version control.
 * <p>
 * Text is represented as <code>Object[]</code> because the diff engine is
 * capable of handling more than plain ascci. In fact, arrays of any type that
 * implements {@link java.lang.Object#hashCode hashCode()} and
 * {@link java.lang.Object#equals equals()} correctly can be subject to
 * differencing using this library.
 * </p>
 * <p>
 * This library provides a framework in which different differencing algorithms
 * may be used. If no algorithm is specififed, a default algorithm is used.
 * </p>
 */

public interface DiffService extends ToString
{

   /** The standard line separator. */
   public static final String NL = PrivilegedSystemHelper.getProperty("line.separator");

   /** The line separator to use in RCS format output. */
   public static final String RCS_EOL = "\n";

   /**
    * compute the difference between an original and a revision.
    * 
    * @param orig the original
    * @param rev the revision to compare with the original.
    * @return a Revision describing the differences
    */
   public Revision diff(Object[] orig, Object[] rev) throws Exception;

   /**
    * Compares the two input sequences.
    * 
    * @param orig The original sequence.
    * @param rev The revised sequence.
    * @return true if the sequences are identical. False otherwise.
    */
   public boolean compare(Object[] orig, Object[] rev);

   /**
    * Converts an array of {@link Object Object} to a string using
    * {@link DiffService#NL} as the line separator.
    * 
    * @param o the array of objects.
    */
   public String arrayToString(Object[] o);

   /**
    * Edits all of the items in the input sequence.
    * 
    * @param text The input sequence.
    * @return A sequence of the same length with all the lines differing from the
    *         corresponding ones in the input.
    */
   public Object[] editAll(Object[] text);

}
