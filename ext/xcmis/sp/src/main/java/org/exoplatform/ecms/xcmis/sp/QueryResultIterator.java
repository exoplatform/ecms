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
