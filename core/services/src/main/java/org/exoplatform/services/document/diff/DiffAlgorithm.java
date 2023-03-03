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

/**
 * A simple interface for implementations of differencing algorithms.
 */
public interface DiffAlgorithm
{
   /**
    * Computes the difference between the original sequence and the revised
    * sequence and returns it as a
    * {@link org.exoplatform.services.document.impl.diff.RevisionImpl Revision} object.
    * <p>
    * The revision can be used to construct the revised sequence from the
    * original sequence.
    * 
    * @param rev the revised text
    * @return the revision script.
    * @throws Exception if the diff could not be computed.
    */
   public Revision diff(Object[] orig, Object[] rev) throws Exception;
}
