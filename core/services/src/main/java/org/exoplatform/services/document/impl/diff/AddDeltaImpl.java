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

import org.exoplatform.services.document.diff.AddDelta;
import org.exoplatform.services.document.diff.Chunk;
import org.exoplatform.services.document.diff.RevisionVisitor;

import java.util.List;

/**
 * Holds an add-delta between to revisions of a text.
 * 
 * @version $Id: AddDeltaImpl.java 5799 2006-05-28 17:55:42Z geaz $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @see DeltaImpl
 * @see DiffServiceImpl
 * @see ChunkImpl
 */
public class AddDeltaImpl extends DeltaImpl implements AddDelta
{

   AddDeltaImpl()
   {
      super();
   }

   public AddDeltaImpl(int origpos, Chunk rev)
   {
      init(new ChunkImpl(origpos, 0), rev);
   }

   public void verify(List target) throws Exception
   {
      if (original.first() > target.size())
      {
         throw new IllegalStateException("original.first() > target.size()");
      }
   }

   public void applyTo(List target)
   {
      revised.applyAdd(original.first(), target);
   }

   public void toString(StringBuffer s)
   {
      s.append(original.anchor());
      s.append("a");
      s.append(revised.rangeString());
      s.append(DiffServiceImpl.NL);
      revised.toString(s, "> ", DiffServiceImpl.NL);
   }

   public void toRCSString(StringBuffer s, String EOL)
   {
      s.append("a");
      s.append(original.anchor());
      s.append(" ");
      s.append(revised.size());
      s.append(EOL);
      revised.toString(s, "", EOL);
   }

   public void accept(RevisionVisitor visitor)
   {
      visitor.visit(this);
   }
}
