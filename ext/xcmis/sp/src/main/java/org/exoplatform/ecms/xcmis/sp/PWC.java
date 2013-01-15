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
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.Updatability;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
class PWC extends DocumentDataImpl
{

   /** Latest version of document. */
   private DocumentDataImpl document;

   public PWC(JcrNodeEntry jcrNodeEntry)
   {
      super(jcrNodeEntry);
   }

   public PWC(JcrNodeEntry jcrEntry, DocumentDataImpl document)
   {
      super(jcrEntry);
      this.document = document;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void cancelCheckout() throws StorageException
   {
      //String id = getObjectId();
      // Reset versioning property on latest version.
      DocumentDataImpl latestVersion = getLatestVersion();
      JcrNodeEntry latestEntry = latestVersion.getNodeEntry();
      latestEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
      latestEntry.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, false);
      latestEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, (Value)null);
      latestEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, (Value)null);
      // Remove PWC with wrapped node.
      try
      {
         Node node = entry.getNode();
         Session session = node.getSession();
         node.getParent().remove();
         session.save();

      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable cancel checkout of ocument. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkin(boolean major, String checkinComment, Map<String, Property<?>> properties,
      ContentStream content, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws NameConstraintViolationException, StorageException
   {
      try
      {
         DocumentDataImpl latestVersion = getLatestVersion();
         JcrNodeEntry latestEntry = latestVersion.getNodeEntry();
         Node latestNode = latestEntry.getNode();
         Session session = latestNode.getSession();

         // send current state in version history
         latestNode.checkin();
         latestNode.checkout();

         latestEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
         latestEntry.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, false);
         latestEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, (Value)null);
         latestEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, (Value)null);
         // Update creation date & last modification date to emulate creation new version
         String[] split = latestEntry.getString(CmisConstants.OBJECT_ID).split(JcrCMIS.ID_SEPARATOR);
         String newId = split[0] + JcrCMIS.ID_SEPARATOR + (Integer.parseInt(split[1]) + 1);
         latestEntry.setValue(CmisConstants.OBJECT_ID, newId);
         String userId = session.getUserID();
         latestEntry.setValue(CmisConstants.CREATED_BY, userId);
         latestEntry.setValue(CmisConstants.LAST_MODIFIED_BY, userId);
         Calendar cal = Calendar.getInstance();
         latestEntry.setValue(CmisConstants.CREATION_DATE, cal);
         latestEntry.setValue(CmisConstants.LAST_MODIFICATION_DATE, cal);
         // Attributes of new version
         latestEntry.setValue(CmisConstants.IS_MAJOR_VERSION, major);
         if (checkinComment != null)
         {
            latestEntry.setValue(CmisConstants.CHECKIN_COMMENT, checkinComment);
         }

         // Merge properties
         for (PropertyDefinition<?> propertyDefinition : getTypeDefinition().getPropertyDefinitions())
         {
            Updatability updatability = propertyDefinition.getUpdatability();
            String propertyId = propertyDefinition.getId();
            if (updatability == Updatability.READWRITE)
            {
               // Only read/write property could be updated on PWC. Need copy
               // them to current state. If passed properties contains new value
               // then ignore value from PWC.
               if (properties != null && properties.containsKey(propertyId))
               {
                  latestEntry.setProperty(properties.get(propertyId));
               }
               else
               {
                  latestEntry.setProperty(getProperty(propertyId));
               }
            }
            else if (updatability == Updatability.WHENCHECKEDOUT && properties != null
               && properties.containsKey(propertyId))
            {
               latestEntry.setProperty(properties.get(propertyId));
            }
         }

         try
         {
            if (content != null)
            {
               latestEntry.setContentStream(content);
            }
            else if (getContentStream() != null)
            {
               // TODO : Need to check if contents are the same then not was not updated not need to change
               latestEntry.setContentStream(getContentStream());
            }
         }
         catch (IOException ioe)
         {
            throw new CmisRuntimeException("Unable copy content for new document. " + ioe.getMessage(), ioe);
         }

         if (acl != null && acl.size() > 0)
         {
            latestEntry.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (PolicyData policy : policies)
            {
               latestEntry.applyPolicy(((BaseObjectData)policy).getNodeEntry());
            }
         }

         // Remove PWC with wrapped node.
         Node node = entry.getNode();
         node.getParent().remove();
         session.save();

         // To recreate the latestVersion Document instance since 
         // the Id of DocumentDataImpl and JcrNodeEntry wouldn't change
         try {
            JcrNodeEntry fromNode = entry.storage.fromNode(latestVersion.entry.getNode());
            latestVersion = new DocumentDataImpl(fromNode);
         } catch (ObjectNotFoundException e) {
            throw new StorageException("Can't recreate the latest document object from node", e);
         }
         
         return latestVersion;
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable checkin Document. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void delete() throws StorageException
   {
      cancelCheckout();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public FolderData getParent() throws ConstraintException
   {
      return getLatestVersion().getParent();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Collection<FolderData> getParents()
   {
      return getLatestVersion().getParents();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isPWC()
   {
      return true;
   }

   private DocumentDataImpl getLatestVersion()
   {
      if (document == null)
      {
         try
         {
            String latestVersion = entry.getString("xcmis:latestVersionId");
            document = new DocumentDataImpl(entry.storage.getEntry(latestVersion));
         }
         catch (ObjectNotFoundException e)
         {
            throw new CmisRuntimeException("Unable get latest version of document. " + e.getMessage(), e);
         }
      }
      return document;
   }

}
