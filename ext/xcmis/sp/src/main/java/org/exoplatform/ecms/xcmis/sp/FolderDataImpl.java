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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.LazyIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.StorageException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: FolderDataImpl.java 1160 2010-05-21 17:06:16Z
 *          alexey.zavizionov@gmail.com $
 */
class FolderDataImpl extends BaseObjectData implements FolderData
{

   private class FolderChildrenIterator extends LazyIterator<ObjectData>
   {

      private final ItemsIterator<JcrNodeEntry> iter;

      FolderChildrenIterator(ItemsIterator<JcrNodeEntry> iter)
      {
         this.iter = iter;
         fetchNext();
      }

      @Override
      protected void fetchNext()
      {
         next = null;
         if (iter.hasNext())
         {
            try
            {
               JcrNodeEntry childEntry = iter.next();
               next = ((StorageImpl)entry.storage).getObject(childEntry);
            }
            catch (Exception re)
            {
              if (LOG.isWarnEnabled()) {
                LOG.warn("Unexpected error. Failed get next CMIS object. " + re.getMessage());
              }
            }
         }
      }

      /**
       * {@inheritDoc}
       */
      public int size()
      {
         return iter.size();
      }

   }

   private static final Log LOG = ExoLogger.getLogger(FolderDataImpl.class);

   public FolderDataImpl(JcrNodeEntry jcrEntry)
   {
      super(jcrEntry);
   }

   /**
    * {@inheritDoc}
    */
   public void addObject(ObjectData object) throws ConstraintException
   {
      entry.addObject(((BaseObjectData)object).getNodeEntry());

   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<ObjectData> getChildren(String orderBy)
   {
      return new FolderChildrenIterator(entry.getChildren());
   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream(String streamId)
   {
      // TODO : renditions for Folder object.
      // It may be XML or HTML representation direct child or full tree.
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public FolderData getParent() throws ConstraintException
   {
      if (isRoot())
      {
         throw new ConstraintException("Unable get parent of root folder.");
      }
      Collection<FolderData> parents = getParents();
      return parents.iterator().next();
   }

   /**
    * {@inheritDoc}
    */
   public Collection<FolderData> getParents()
   {
      if (isRoot())
      {
         return Collections.emptyList();
      }
      Collection<JcrNodeEntry> parentEntries = entry.getParents();
      List<FolderData> parents = new ArrayList<FolderData>(parentEntries.size());
      for (JcrNodeEntry parentEntry : parentEntries)
      {
         parents.add(new FolderDataImpl(parentEntry));
      }
      return parents;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getCreationDate()
   {
      Calendar date = super.getCreationDate();
      if (date == null)
      {
         // For objects which were created via non CMIS API.
         date = entry.getDate(JcrCMIS.JCR_CREATED);
      }
      return date;
   }

   /**
    * {@inheritDoc}
    */
   public String getPath()
   {
      return entry.getPath();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasChildren()
   {
      return entry.hasChildren();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isAllowedChildType(String typeId)
   {
      String[] values = entry.getStrings(CmisConstants.ALLOWED_CHILD_OBJECT_TYPE_IDS);
      if (values != null && values.length > 0 && !Arrays.asList(values).contains(typeId))
      {
         return false;
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isRoot()
   {
      return entry.isRoot();
   }

   /**
    * {@inheritDoc}
    */
   public void removeObject(ObjectData object)
   {
      entry.removeObject(((BaseObjectData)object).getNodeEntry());

   }

   @Override
   public void delete() throws StorageException
   {
      if (isRoot())
      {
         throw new StorageException("Root folder can't be deleted.");
      }
      entry.delete();
   }

}
