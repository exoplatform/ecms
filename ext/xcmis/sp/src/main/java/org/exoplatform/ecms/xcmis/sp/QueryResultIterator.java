/**
 *  Copyright (C) 2003-2010 eXo Platform SAS.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.ecms.xcmis.sp;

import org.xcmis.search.Visitors;
import org.xcmis.search.model.column.Column;
import org.xcmis.search.model.source.SelectorName;
import org.xcmis.search.result.ScoredRow;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.query.Score;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Iterator over query result's.
 */
class QueryResultIterator implements ItemsIterator<Result>
{

   private final Iterator<ScoredRow> rows;

   private final Set<SelectorName> selectors;

   private final int size;

   private final org.xcmis.search.model.Query qom;

   private Result next;

   public QueryResultIterator(List<ScoredRow> rows, org.xcmis.search.model.Query qom)
   {
      this.size = rows.size();
      this.rows = rows.iterator();
      this.selectors = Visitors.getSelectorsReferencedBy(qom);
      this.qom = qom;
      fetchNext();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNext()
   {
      return next != null;
   }

   /**
    * {@inheritDoc}
    */
   public Result next()
   {
      if (next == null)
      {
         throw new NoSuchElementException();
      }
      Result r = next;
      fetchNext();
      return r;
   }

   /**
    * {@inheritDoc}
    */
   public void remove()
   {
      throw new UnsupportedOperationException("remove");
   }

   /**
    * {@inheritDoc}
    */
   public int size()
   {
      return size;
   }

   /**
    * {@inheritDoc}
    */
   public void skip(int skip) throws NoSuchElementException
   {
      while (skip-- > 0)
      {
         next();
      }
   }

   /**
    * To fetch next <code>Result</code>.
    */
   protected void fetchNext()
   {
      next = null;
      while (next == null && rows.hasNext())
      {
         ScoredRow row = rows.next();
         for (SelectorName selectorName : selectors)
         {
            String objectId = row.getNodeIdentifer(selectorName.getName());
            List<String> properties = null;
            Score score = null;
            for (Column column : qom.getColumns())
            {
               //TODO check
               if (column.isFunction())
               {
                  score = new Score(column.getColumnName(), BigDecimal.valueOf(row.getScore()));
               }
               else
               {
                  if (selectorName.getName().equals(column.getSelectorName()))
                  {
                     if (column.getPropertyName() != null)
                     {
                        if (properties == null)
                        {
                           properties = new ArrayList<String>();
                        }
                        properties.add(column.getPropertyName());
                     }
                  }
               }
            }
            next = new ResultImpl(objectId, //
               properties == null ? null : properties.toArray(new String[properties.size()]), //
               score);
         }
      }
   }
}