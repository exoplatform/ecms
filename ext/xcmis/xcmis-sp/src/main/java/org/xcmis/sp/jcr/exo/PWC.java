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

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RenditionManager;
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
 * @version $Id: $
 */
class PWC extends DocumentDataImpl
{

   /** Latest version of document. */
   private DocumentDataImpl document;

   public PWC(JcrNodeEntry jcrNodeEntry, IndexListener indexListener, RenditionManager renditionManager)
   {
      super(jcrNodeEntry, indexListener, renditionManager);
   }

   public PWC(JcrNodeEntry jcrEntry, IndexListener indexListener, RenditionManager renditionManager,
      DocumentDataImpl document)
   {
      super(jcrEntry, indexListener, renditionManager);
      this.document = document;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void cancelCheckout() throws StorageException
   {
      // Reset versioning property on latest version
      DocumentDataImpl latestVersion = getLatestVersion();
      JcrNodeEntry latestNodeAdapter = latestVersion.getNodeEntry();
      latestNodeAdapter.setValue(CmisConstants.IS_LATEST_VERSION, true);
      latestNodeAdapter.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, false);
      latestNodeAdapter.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, (Value)null);
      latestNodeAdapter.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, (Value)null);

      // Remove PWC
      try
      {
         Node node = getNode();
         Session session = node.getSession();
         node.getParent().remove();

         session.save();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable cancel checkout Document. " + re.getMessage(), re);
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
         JcrNodeEntry latestNodeAdapter = latestVersion.getNodeEntry();
         Node latestNode = latestNodeAdapter.getNode();
         Session session = latestNode.getSession();

         // send current state in version history
         latestNode.checkin();
         latestNode.checkout();

         latestNodeAdapter.setValue(CmisConstants.IS_LATEST_VERSION, true);
         latestNodeAdapter.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, false);
         latestNodeAdapter.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, (Value)null);
         latestNodeAdapter.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, (Value)null);
         // Update creation date & last modification date to emulate creation new version
         String userId = session.getUserID();
         latestNodeAdapter.setValue(CmisConstants.CREATED_BY, userId);
         latestNodeAdapter.setValue(CmisConstants.LAST_MODIFIED_BY, userId);
         Calendar cal = Calendar.getInstance();
         latestNodeAdapter.setValue(CmisConstants.CREATION_DATE, cal);
         latestNodeAdapter.setValue(CmisConstants.LAST_MODIFICATION_DATE, cal);

         // Attributes of new version
         latestNodeAdapter.setValue(CmisConstants.IS_MAJOR_VERSION, major);
         if (checkinComment != null)
         {
            latestNodeAdapter.setValue(CmisConstants.CHECKIN_COMMENT, checkinComment);
         }

         // Merge properties
         for (PropertyDefinition<?> definition : getTypeDefinition().getPropertyDefinitions())
         {
            Updatability updatability = definition.getUpdatability();
            String id = definition.getId();
            if (updatability == Updatability.READWRITE)
            {
               // Only read/write property could be updated on PWC.
               // Need copy them to current state.

               // If passed properties contains new value ignore value from PWC.
               if (properties != null && properties.containsKey(id))
               {
                  latestNodeAdapter.setProperty(properties.get(id));
               }
               else
               {
                  latestNodeAdapter.setProperty(getProperty(id));
               }
            }
            else if (updatability == Updatability.WHENCHECKEDOUT && properties != null && properties.containsKey(id))
            {
               latestNodeAdapter.setProperty(properties.get(id));
            }
         }

         try
         {
            if (content != null)
            {
               latestNodeAdapter.setContentStream(content);
            }
            else if (getContentStream() != null)
            {
               // TODO : Need to check if contents are the same then not was not updated not need to change
               latestNodeAdapter.setContentStream(getContentStream());
            }
         }
         catch (IOException ioe)
         {
            throw new CmisRuntimeException("Unable copy content for new document. " + ioe.getMessage(), ioe);
         }

         if (acl != null && acl.size() > 0)
         {
            latestNodeAdapter.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (ObjectData aPolicy : policies)
            {
               latestNodeAdapter.applyPolicy((PolicyData)aPolicy);
            }
         }

         // Remove PWC
         Node node = getNode();
         node.getParent().remove();
         session.save();

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
   protected void delete() throws StorageException
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
            Node node = getNode();
            Session session = node.getSession();
            String latestVersion = node.getProperty("xcmis:latestVersionId").getString();
            Node latestNode = ((ExtendedSession)session).getNodeByIdentifier(latestVersion);
            document = new DocumentDataImpl(new JcrNodeEntry(latestNode), indexListener, renditionManager);
         }
         catch (RepositoryException re)
         {
            throw new CmisRuntimeException("Unexpected error. " + re.getMessage(), re);
         }
      }
      return document;
   }

}
