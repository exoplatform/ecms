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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;

/**
 * @author <a href="mailto:andrey00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
class JcrFile extends DocumentDataImpl
{

   public JcrFile(JcrNodeEntry jcrEntry)
   {
      super(jcrEntry);
//      try
//      {
//         Node node = entry.getNode();
//         Session session = node.getSession();
//         if (entry.getType().isVersionable() && node.canAddMixin(JcrCMIS.MIX_VERSIONABLE))
//         {
//            node.addMixin(JcrCMIS.MIX_VERSIONABLE);
//            session.save();
//         }
//      }
//      catch (RepositoryException e)
//      {
//         throw new CmisRuntimeException(e.getMessage(), e);
//      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void cancelCheckout() throws StorageException
   {
      throw new CmisRuntimeException("Not implemented for not CMIS type.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkin(boolean major, String checkinComment, Map<String, Property<?>> properties,
      ContentStream content, List<AccessControlEntry> acl, Collection<PolicyData> policies) throws StorageException
   {
      throw new CmisRuntimeException("Not implemented for not CMIS type.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkout() throws VersioningException, StorageException
   {
      throw new CmisRuntimeException("Not implemented for not CMIS type.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionLabel()
   {
      return StorageImpl.LATEST_LABEL;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionSeriesId()
   {
      return entry.getString(JcrCMIS.JCR_VERSION_HISTORY);
   }

   @Override
   protected void save() throws StorageException
   {
      entry.save(false);
   }
}
