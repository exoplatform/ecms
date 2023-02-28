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

public interface Delta extends ToString
{

   /**
    * Verifies that this delta can be used to patch the given text.
    * 
    * @param target the text to patch.
    * @throws Exception if the patch cannot be applied.
    */
   public void verify(List target) throws Exception;

   /**
    * Applies this delta as a patch to the given text.
    * 
    * @param target the text to patch.
    * @throws Exception if the patch cannot be applied.
    */
   public void patch(List target) throws Exception;

   /**
    * Applies this delta as a patch to the given text.
    * 
    * @param target the text to patch.
    */
   public void applyTo(List target);

   /**
    * Converts this delta into its RCS style string representation.
    * 
    * @param s a {@link StringBuffer StringBuffer} to which the string
    *          representation will be appended.
    * @param EOL the string to use as line separator.
    */
   public void toRCSString(StringBuffer s, String EOL);

   /**
    * Converts this delta into its RCS style string representation.
    * 
    * @param EOL the string to use as line separator.
    */
   public String toRCSString(String EOL);

   /**
    * Accessor method to return the chunk representing the original sequence of
    * items
    * 
    * @return the original sequence
    */
   public Chunk getOriginal();

   /**
    * Accessor method to return the chunk representing the updated sequence of
    * items.
    * 
    * @return the updated sequence
    */
   public Chunk getRevised();

   /**
    * Accepts a visitor.
    * <p>
    * See the Visitor pattern in "Design Patterns" by the GOF4.
    * 
    * @param visitor The visitor.
    */
   public abstract void accept(RevisionVisitor visitor);

}
