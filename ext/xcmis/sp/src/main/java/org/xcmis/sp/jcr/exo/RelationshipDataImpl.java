/**
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

import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.StorageException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: RelationshipDataImpl.java 1177 2010-05-25 12:03:35Z
 *          alexey.zavizionov@gmail.com $
 */
class RelationshipDataImpl extends BaseObjectData implements RelationshipData
{

   public RelationshipDataImpl(JcrNodeEntry jcrEntry, IndexListener indexListener)
   {
      super(jcrEntry, indexListener);
   }

   /**
    * {@inheritDoc}
    */
   public String getSourceId()
   {
      return jcrEntry.getString(CmisConstants.SOURCE_ID);
   }

   /**
    * {@inheritDoc}
    */
   public String getTargetId()
   {
      return jcrEntry.getString(CmisConstants.TARGET_ID);
   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream(String streamId)
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public FolderData getParent() throws ConstraintException
   {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<FolderData> getParents()
   {
      return Collections.emptyList();
   }

   protected void delete() throws StorageException
   {
      {
         String objectId = getObjectId();
         try
         {
            Node node = getNode();
            Session session = node.getSession();
            node.remove();
            session.save();
         }
         catch (RepositoryException re)
         {
            throw new StorageException("Unable delete object. " + re.getMessage(), re);
         }
         if (indexListener != null)
         {
            Set<String> removed = new HashSet<String>();
            removed.add(objectId);
            indexListener.removed(removed);
         }
      }
   }

}
