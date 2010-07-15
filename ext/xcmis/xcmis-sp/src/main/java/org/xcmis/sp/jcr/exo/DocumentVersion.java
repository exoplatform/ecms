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

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.RelationshipDirection;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.utils.CmisUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * @author <a href="mailto:andrey00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
public class DocumentVersion extends DocumentDataImpl
{

   /** Latest version of document. */
   private DocumentData document;

   public DocumentVersion(JcrNodeEntry jcrEntry, IndexListener indexListener, RenditionManager renditionManager)
   {
      super(jcrEntry, indexListener, renditionManager);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void applyPolicy(PolicyData policy)
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void cancelCheckout() throws VersioningException, StorageException
   {
      throw new VersioningException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkin(boolean major, String checkinComment, Map<String, Property<?>> properties,
      ContentStream content, List<AccessControlEntry> acl, Collection<PolicyData> policies) throws StorageException
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkout() throws VersioningException, StorageException
   {
      throw new VersioningException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<AccessControlEntry> getACL(boolean onlyBasicPermissions)
   {
      return Collections.emptyList();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getName()
   {
      return getLatestVersion().getName();
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
   public Collection<PolicyData> getPolicies()
   {
      return Collections.emptyList();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemsIterator<RelationshipData> getRelationships(RelationshipDirection direction, TypeDefinition type,
      boolean includeSubRelationshipTypes)
   {
      return CmisUtils.emptyItemsIterator();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionLabel()
   {
      try
      {
         return getNode().getParent().getName();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get version label. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionSeriesCheckedOutBy()
   {
      return getLatestVersion().getVersionSeriesCheckedOutBy();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionSeriesCheckedOutId()
   {
      return getLatestVersion().getVersionSeriesCheckedOutId();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isLatestVersion()
   {
      return false;
   }

   @Override
   public void removePolicy(PolicyData policy)
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setACL(List<AccessControlEntry> aces)
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setContentStream(ContentStream contentStream) throws IOException, VersioningException, StorageException
   {
      throw new VersioningException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setProperties(Map<String, Property<?>> properties) throws NameConstraintViolationException,
      VersioningException
   {
      throw new VersioningException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setProperty(Property<?> property) throws VersioningException
   {
      throw new VersioningException("Not supported for non current version of document.");
   }

   private DocumentData getLatestVersion()
   {
      if (document == null)
      {
         try
         {
            Node node = getNode();
            Session session = node.getSession();
            Version version = (Version)node.getParent();
            VersionHistory versionHistory = version.getContainingHistory();
            Node latest = ((ExtendedSession)session).getNodeByIdentifier(versionHistory.getVersionableUUID());
            document =
               new DocumentDataImpl(new JcrNodeEntry(latest, JcrTypeHelper.getTypeDefinition(latest
                  .getPrimaryNodeType(), true)), indexListener, renditionManager);
         }
         catch (RepositoryException re)
         {
            throw new CmisRuntimeException("Unexpected error. " + re.getMessage(), re);
         }
      }
      return document;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void delete() throws StorageException
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void save()
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   void unfile()
   {
      throw new CmisRuntimeException("Not supported for non current version of document.");
   }

}
