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

import org.exoplatform.services.document.diff.Chunk;
import org.exoplatform.services.document.diff.Delta;
import org.exoplatform.services.document.diff.DiffService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

/**
 * Holds a "delta" difference between to revisions of a text.
 * 
 * @version $Revision: 1.6 $ $Date: 2003/10/13 08:00:24 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @author <a href="mailto:bwm@hplb.hpl.hp.com">Brian McBride</a>
 * @see DiffServiceImpl
 * @see ChunkImpl
 * @see RevisionImpl modifications 27 Apr 2003 bwm Added getOriginal() and
 *      getRevised() accessor methods Added visitor pattern accept() method
 */

public abstract class DeltaImpl implements Delta
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.DeltaImpl");

   protected Chunk original;

   protected Chunk revised;

   static Class[][] DeltaClass;

   static
   {
      DeltaClass = new Class[2][2];
      DeltaClass[0][0] = org.exoplatform.services.document.impl.diff.ChangeDeltaImpl.class;
      DeltaClass[0][1] = org.exoplatform.services.document.impl.diff.AddDeltaImpl.class;
      DeltaClass[1][0] = org.exoplatform.services.document.impl.diff.DeleteDeltaImpl.class;
      DeltaClass[1][1] = org.exoplatform.services.document.impl.diff.ChangeDeltaImpl.class;
   }

   /**
    * Returns a Delta that corresponds to the given chunks in the original and
    * revised text respectively.
    * 
    * @param orig the chunk in the original text.
    * @param rev the chunk in the revised text.
    */
   public static DeltaImpl newDelta(Chunk orig, Chunk rev)
   {
      Class c = DeltaClass[orig.size() > 0 ? 1 : 0][rev.size() > 0 ? 1 : 0];
      DeltaImpl result;
      try
      {
         result = (DeltaImpl)c.newInstance();
      }
      catch (InstantiationException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
         return null;
      }
      catch (IllegalAccessException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
         return null;
      }
      result.init(orig, rev);
      return result;
   }

   /**
    * Creates an uninitialized delta.
    */
   protected DeltaImpl()
   {
   }

   /**
    * Creates a delta object with the given chunks from the original and revised
    * texts.
    */
   protected DeltaImpl(Chunk orig, Chunk rev)
   {
      init(orig, rev);
   }

   /**
    * Initializaes the delta with the given chunks from the original and revised
    * texts.
    */
   protected void init(Chunk orig, Chunk rev)
   {
      original = orig;
      revised = rev;
   }

   /**
    * Verifies that this delta can be used to patch the given text.
    * 
    * @param target the text to patch.
    * @throws Exception if the patch cannot be applied.
    */
   public abstract void verify(List target) throws Exception;

   /**
    * Applies this delta as a patch to the given text.
    * 
    * @param target the text to patch.
    * @throws Exception if the patch cannot be applied.
    */
   public final void patch(List target) throws Exception
   {
      verify(target);
      applyTo(target);
   }

   /**
    * Applies this delta as a patch to the given text.
    * 
    * @param target the text to patch.
    */
   public abstract void applyTo(List target);

   /**
    * Converts this delta into its Unix diff style string representation.
    * 
    * @param s a {@link StringBuffer StringBuffer} to which the string
    *          representation will be appended.
    */
   public void toString(StringBuffer s)
   {
      original.rangeString(s);
      s.append("x");
      revised.rangeString(s);
      s.append(DiffService.NL);
      original.toString(s, "> ", "\n");
      s.append("---");
      s.append(DiffService.NL);
      revised.toString(s, "< ", "\n");
   }

   /**
    * Converts this delta into its RCS style string representation.
    * 
    * @param s a {@link StringBuffer StringBuffer} to which the string
    *          representation will be appended.
    * @param EOL the string to use as line separator.
    */
   public abstract void toRCSString(StringBuffer s, String EOL);

   /**
    * Converts this delta into its RCS style string representation.
    * 
    * @param EOL the string to use as line separator.
    */
   public String toRCSString(String EOL)
   {
      StringBuffer s = new StringBuffer();
      toRCSString(s, EOL);
      return s.toString();
   }

   /**
    * Accessor method to return the chunk representing the original sequence of
    * items
    * 
    * @return the original sequence
    */
   public Chunk getOriginal()
   {
      return original;
   }

   /**
    * Accessor method to return the chunk representing the updated sequence of
    * items.
    * 
    * @return the updated sequence
    */
   public Chunk getRevised()
   {
      return revised;
   }

}
