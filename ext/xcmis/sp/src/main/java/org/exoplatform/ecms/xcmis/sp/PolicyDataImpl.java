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

import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.StorageException;

import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
class PolicyDataImpl extends BaseObjectData implements PolicyData
{

   public PolicyDataImpl(JcrNodeEntry jcrEntry)
   {
      super(jcrEntry);
   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream(String streamId)
   {
      // No renditions for Policy object.
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

   /**
    * {@inheritDoc}
    */
   public String getPolicyText()
   {
      return entry.getString(CmisConstants.POLICY_TEXT);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void delete() throws StorageException
   {
      entry.delete();
   }

}
