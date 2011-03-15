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
package org.exoplatform.ecms.xcmis.sp;

import org.xcmis.search.result.ScoredRow;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.Storage;
import org.xcmis.spi.model.BaseType;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class DocumentOrderResultSorter implements Comparator<ScoredRow>
{

   /** The selector name. */
   private final String selectorName;

   private final Map<String, ObjectData> itemCache;

   private final Storage storage;

   /**
    * The Constructor.
    *
    * @param selectorName
    *           String selector name
    * @param storage
    *           the storage
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
    * @param identifer
    *           String
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
