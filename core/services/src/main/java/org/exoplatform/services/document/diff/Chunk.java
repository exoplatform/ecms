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

import java.util.List;

public interface Chunk extends ToString
{

   /**
    * Returns the anchor position of the chunk.
    * 
    * @return the anchor position.
    */
   public int anchor();

   /**
    * Returns the size of the chunk.
    * 
    * @return the size.
    */
   public int size();

   /**
    * Returns the index of the first line of the chunk.
    */
   public int first();

   /**
    * Returns the index of the last line of the chunk.
    */
   public int last();

   /**
    * Returns the <i>from</i> index of the chunk in RCS terms.
    */
   public int rcsfrom();

   /**
    * Returns the <i>to</i> index of the chunk in RCS terms.
    */
   public int rcsto();

   /**
    * Returns the text saved for this chunk.
    * 
    * @return the text.
    */
   public List chunk();

   /**
    * Verifies that this chunk's saved text matches the corresponding text in the
    * given sequence.
    * 
    * @param target the sequence to verify against.
    * @return true if the texts match.
    */
   public boolean verify(List target);

   /**
    * Delete this chunk from he given text.
    * 
    * @param target the text to delete from.
    */
   public void applyDelete(List target);

   /**
    * Add the text of this chunk to the target at the given position.
    * 
    * @param start where to add the text.
    * @param target the text to add to.
    */
   public void applyAdd(int start, List target);

   /**
    * Provide a string image of the chunk using the an empty prefix and postfix.
    */
   public void toString(StringBuffer s);

   /**
    * Provide a string image of the chunk using the given prefix and postfix.
    * 
    * @param s where the string image should be appended.
    * @param prefix the text thatshould prefix each line.
    * @param postfix the text that should end each line.
    */
   public StringBuffer toString(StringBuffer s, String prefix, String postfix);

   /**
    * Retreives the specified part from a {@link List List}.
    * 
    * @param seq the list to retreive a slice from.
    * @param pos the start position.
    * @param count the number of items in the slice.
    * @return a {@link List List} containing the specified items.
    */
   public List slice(List seq, int pos, int count);

   /**
    * Retrieves a slice from an {@link Object Object} array.
    * 
    * @param seq the list to retreive a slice from.
    * @param pos the start position.
    * @param count the number of items in the slice.
    * @return a {@link List List} containing the specified items.
    */
   public List slice(Object[] seq, int pos, int count);

   /**
    * Provide a string representation of the numeric range of this chunk.
    */
   public String rangeString();

   /**
    * Provide a string representation of the numeric range of this chunk.
    * 
    * @param s where the string representation should be appended.
    */
   public void rangeString(StringBuffer s);

   /**
    * Provide a string representation of the numeric range of this chunk.
    * 
    * @param s where the string representation should be appended.
    * @param separ what to use as line separator.
    */
   public void rangeString(StringBuffer s, String separ);

}
