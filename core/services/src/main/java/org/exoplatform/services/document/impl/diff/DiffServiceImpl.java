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

import org.exoplatform.services.document.diff.DiffAlgorithm;
import org.exoplatform.services.document.diff.DiffService;
import org.exoplatform.services.document.diff.Revision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DiffServiceImpl extends ToStringImpl implements DiffService
{

   /** The differencing algorithm to use. */
   protected DiffAlgorithm algorithm;

   public DiffServiceImpl(DiffAlgorithm algorithm)
   {
      this.algorithm = algorithm;
   }

   /**
    * compute the difference between an original and a revision.
    * 
    * @param orig the original
    * @param rev the revision to compare with the original.
    * @return a Revision describing the differences
    */
   public Revision diff(Object[] orig, Object[] rev) throws Exception
   {
      if (orig == null || rev == null)
      {
         throw new IllegalArgumentException();
      }
      return algorithm.diff(orig, rev);
   }

   /**
    * Compares the two input sequences.
    * 
    * @param orig The original sequence.
    * @param rev The revised sequence.
    * @return true if the sequences are identical. False otherwise.
    */
   public boolean compare(Object[] orig, Object[] rev)
   {
      if (orig.length != rev.length)
      {
         return false;
      }
      else
      {
         for (int i = 0; i < orig.length; i++)
         {
            if (!orig[i].equals(rev[i]))
            {
               return false;
            }
         }
         return true;
      }
   }

   /**
    * Converts an array of {@link Object Object} to a string using
    * {@link DiffServiceImpl#NL Diff.NL} as the line separator.
    * 
    * @param o the array of objects.
    */
   public String arrayToString(Object[] o)
   {
      return arrayToString(o, NL);
   }

   /**
    * Edits all of the items in the input sequence.
    * 
    * @param text The input sequence.
    * @return A sequence of the same length with all the lines differing from the
    *         corresponding ones in the input.
    */
   public Object[] editAll(Object[] text)
   {
      Object[] result = new String[text.length];

      for (int i = 0; i < text.length; i++)
         result[i] = text[i] + " <edited>";

      return result;
   }

   /**
    * Performs random edits on the input sequence. Useful for testing.
    * 
    * @param text The input sequence.
    * @return The sequence with random edits performed.
    */
   public Object[] randomEdit(Object[] text)
   {
      return randomEdit(text, text.length);
   }

   /**
    * Performs random edits on the input sequence. Useful for testing.
    * 
    * @param text The input sequence.
    * @param seed A seed value for the randomizer.
    * @return The sequence with random edits performed.
    */
   public Object[] randomEdit(Object[] text, long seed)
   {
      List result = new ArrayList(Arrays.asList(text));
      Random r = new Random(seed);
      int nops = r.nextInt(10);
      for (int i = 0; i < nops; i++)
      {
         boolean del = r.nextBoolean();
         int pos = r.nextInt(result.size() + 1);
         int len = Math.min(result.size() - pos, 1 + r.nextInt(4));
         if (del && result.size() > 0)
         { // delete
            result.subList(pos, pos + len).clear();
         }
         else
         {
            for (int k = 0; k < len; k++, pos++)
            {
               result.add(pos, "[" + i + "] random edit[" + i + "][" + i + "]");
            }
         }
      }
      return result.toArray();
   }

   /**
    * Shuffles around the items in the input sequence.
    * 
    * @param text The input sequence.
    * @return The shuffled sequence.
    */
   public Object[] shuffle(Object[] text)
   {
      return shuffle(text, text.length);
   }

   /**
    * Shuffles around the items in the input sequence.
    * 
    * @param text The input sequence.
    * @param seed A seed value for randomizing the suffle.
    * @return The shuffled sequence.
    */
   public Object[] shuffle(Object[] text, long seed)
   {
      List result = new ArrayList(Arrays.asList(text));
      Collections.shuffle(result);
      return result.toArray();
   }

   /**
    * Generate a random sequence of the given size.
    * 
    * @param size The size of the sequence to generate.
    * @return The generated sequence.
    */
   public Object[] randomSequence(int size)
   {
      return randomSequence(size, size);
   }

   /**
    * Generate a random sequence of the given size.
    * 
    * @param size The size of the sequence to generate.
    * @param seed A seed value for randomizing the generation.
    * @return The generated sequence.
    */
   public Object[] randomSequence(int size, long seed)
   {
      Integer[] result = new Integer[size];
      Random r = new Random(seed);
      for (int i = 0; i < result.length; i++)
      {
         result[i] = new Integer(r.nextInt(size));
      }
      return result;
   }

}
