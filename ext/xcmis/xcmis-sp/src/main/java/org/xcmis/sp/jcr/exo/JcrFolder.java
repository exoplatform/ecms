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

import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageException;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrey00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
class JcrFolder extends FolderDataImpl
{

   public JcrFolder(JcrNodeEntry jcrEntry, IndexListener indexListener, RenditionManager renditionManager)
   {
      super(jcrEntry, indexListener, renditionManager);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getCreationDate()
   {
      try
      {
         Node node = getNode();
         if (node.isNodeType(JcrCMIS.NT_FOLDER))
         {
            return node.getProperty(JcrCMIS.JCR_CREATED).getDate();
         }
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get cteation date. " + re.getMessage(), re);
      }
   }

   protected void save() throws StorageException
   {
      jcrEntry.save();
      if (indexListener != null)
      {
         indexListener.updated(this);
      }
   }

}
