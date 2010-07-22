/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xcmis.sp.jcr.exo;

import org.xcmis.search.InvalidQueryException;
import org.xcmis.search.SearchService;
import org.xcmis.search.Visitors;
import org.xcmis.search.model.column.Column;
import org.xcmis.search.model.source.SelectorName;
import org.xcmis.search.parser.CmisQueryParser;
import org.xcmis.search.parser.QueryParser;
import org.xcmis.search.query.QueryExecutionException;
import org.xcmis.search.result.ScoredRow;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.Storage;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.query.Score;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Session;

/**
 * {@link Storage} with implemented Storage.query(Query query) method.
 */
public class QueryableStorage extends StorageImpl
{
   /**
    * Searching service.
    */
   private final SearchService searchService;

   /**
    * Cmis query parser.
    */
   private final QueryParser cmisQueryParser;

   /**
    * @param session Session
    * @param configuration StorageConfiguration
    * @param searchService the search service
    */
   public QueryableStorage(Session session, StorageConfiguration configuration, SearchService searchService,
      PermissionService permissionService)
   {
      super(session, configuration, permissionService);
      this.searchService = searchService;
      this.cmisQueryParser = new CmisQueryParser();
   }

   /**
    * Constructor.
    * 
    * @param session Session
    * @param configuration StorageConfiguration
    * @param renditionManager RenditionManager
    * @param searchService the search service
    */
   public QueryableStorage(Session session, StorageConfiguration configuration, RenditionManager renditionManager,
      SearchService searchService, PermissionService permissionService)
   {
      super(session, configuration, renditionManager, permissionService);
      this.searchService = searchService;
      this.cmisQueryParser = new CmisQueryParser();
   }

   /**
    * @see org.xcmis.sp.jcr.exo.StorageImpl#query(org.xcmis.spi.query.Query)
    */
   @Override
   public ItemsIterator<Result> query(Query query) throws InvalidArgumentException
   {
      try
      {
         org.xcmis.search.model.Query qom = cmisQueryParser.parseQuery(query.getStatement());
         List<ScoredRow> rows = searchService.execute(qom);
         //check if needed default sorting
         if (qom.getOrderings().size() == 0)
         {
            Set<SelectorName> selectorsReferencedBy = Visitors.getSelectorsReferencedBy(qom);
            Collections.sort(rows, new DocumentOrderResultSorter(selectorsReferencedBy.iterator().next().getName(),
               this));
         }
         return new QueryResultIterator(rows, qom);
      }
      catch (InvalidQueryException e)
      {
         throw new InvalidArgumentException(e.getLocalizedMessage(), e);
      }
      catch (QueryExecutionException e)
      {
         throw new CmisRuntimeException(e.getLocalizedMessage(), e);
      }
   }

   /**
    * Iterator over query result's.
    */
   private static class QueryResultIterator implements ItemsIterator<Result>
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

   /**
    * Single row from query result.
    */
   public static class ResultImpl implements Result
   {

      private final String id;

      private final String[] properties;

      private final Score score;

      public ResultImpl(String id, String[] properties, Score score)
      {
         this.id = id;
         this.properties = properties;
         this.score = score;
      }

      public String[] getPropertyNames()
      {
         return properties;
      }

      public String getObjectId()
      {
         return id;
      }

      public Score getScore()
      {
         return score;
      }

   }

   public static class DocumentOrderResultSorter implements Comparator<ScoredRow>
   {

      /** The selector name. */
      private final String selectorName;

      private final Map<String, ObjectData> itemCache;

      private final Storage storage;

      /**
       * The Constructor.
       * 
       * @param selectorName String selector name
       * @param storage the storage
       */
      public DocumentOrderResultSorter(final String selectorName, Storage storage)
      {
         this.selectorName = selectorName;
         this.storage = storage;
         this.itemCache = new HashMap<String, ObjectData>();
      }

      /**
       * {@inheritDoc}
       */
      public int compare(ScoredRow o1, ScoredRow o2)
      {
         if (o1.equals(o2))
         {
            return 0;
         }
         final String path1 = getPath(o1.getNodeIdentifer(selectorName));
         final String path2 = getPath(o2.getNodeIdentifer(selectorName));
         // TODO should be checked
         if (path1 == null || path2 == null)
         {
            return 0;
         }
         return path1.compareTo(path2);
      }

      /**
       * Return comparable location of the object.
       * 
       * @param identifer String
       * @return path String
       */
      public String getPath(String identifer)
      {
         ObjectData obj = itemCache.get(identifer);
         if (obj == null)
         {
            try
            {
               obj = storage.getObjectById(identifer);
            }
            catch (ObjectNotFoundException e)
            {
               // XXX : correct ?
               return null;
            }
            itemCache.put(identifer, obj);
         }
         if (obj.getBaseType() == BaseType.FOLDER)
         {
            if (((FolderData)obj).isRoot())
            {
               return obj.getName();
            }
         }
         Collection<FolderData> parents = obj.getParents();
         if (parents.size() == 0)
         {
            return obj.getName();
         }
         return parents.iterator().next().getPath() + "/" + obj.getName();
      }
   }

}
