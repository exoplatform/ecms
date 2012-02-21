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
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.LazyIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectDataVisitor;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.PropertyFilter;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.UpdateConflictException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.RelationshipDirection;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.Updatability;
import org.xcmis.spi.model.impl.BooleanProperty;
import org.xcmis.spi.model.impl.DateTimeProperty;
import org.xcmis.spi.model.impl.IdProperty;
import org.xcmis.spi.model.impl.IntegerProperty;
import org.xcmis.spi.model.impl.StringProperty;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: BaseObjectData.java 1160 2010-05-21 17:06:16Z
 *          alexey.zavizionov@gmail.com $
 */
abstract class BaseObjectData implements ObjectData
{
   private class RelationshipIterator extends LazyIterator<RelationshipData>
   {

      private final Iterator<JcrNodeEntry> iter;

      private final int size;

      public RelationshipIterator(Collection<JcrNodeEntry> relationships)
      {
         size = relationships.size();
         iter = relationships.iterator();
         fetchNext();
      }

      /**
       * {@inheritDoc}
       */
      public int size()
      {
         return size;
      }

      /** To fetch next item. */
      @Override
      protected void fetchNext()
      {
         next = null;
         if (iter.hasNext())
         {
            next = new RelationshipDataImpl(iter.next());
         }
      }
   }

   private static final Log LOG = ExoLogger.getLogger(BaseObjectData.class);

   protected JcrNodeEntry entry;

   public BaseObjectData(JcrNodeEntry jcrEntry)
   {
      this.entry = jcrEntry;
   }

   /**
    * {@inheritDoc}
    */
   public void accept(ObjectDataVisitor visitor)
   {
      visitor.visit(this);
   }

   /**
    * {@inheritDoc}
    */
   public void applyPolicy(PolicyData policy)
   {
      // Object is controllable by policy. It is checked in Connection.
      entry.applyPolicy(((BaseObjectData)policy).getNodeEntry());
      try
      {
         save();
      }
      catch (StorageException se)
      {
         // Remove ACL does not throw StorageException
         throw new CmisRuntimeException("Unable apply policy. " + se.getMessage(), se);
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null || obj.getClass() != getClass())
      {
         return false;
      }
      return ((BaseObjectData)obj).getNodeEntry().equals(getNodeEntry());
   }

   /**
    * {@inheritDoc}
    */
   public List<AccessControlEntry> getACL(boolean onlyBasicPermissions)
   {
      if (!getTypeDefinition().isControllableACL())
      {
         return Collections.emptyList();
      }
      return entry.getACL();
   }

   /**
    * {@inheritDoc}
    */
   public BaseType getBaseType()
   {
      return entry.getBaseType();
   }

   /**
    * {@inheritDoc}
    */
   public String getChangeToken()
   {
      return entry.getString(CmisConstants.CHANGE_TOKEN);
   }

   /**
    * {@inheritDoc}
    */
   public String getCreatedBy()
   {
      return entry.getString(CmisConstants.CREATED_BY);
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getCreationDate()
   {
      return entry.getDate(CmisConstants.CREATION_DATE);
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getLastModificationDate()
   {
      return entry.getDate(CmisConstants.LAST_MODIFICATION_DATE);
   }

   /**
    * {@inheritDoc}
    */
   public String getLastModifiedBy()
   {
      return entry.getString(CmisConstants.LAST_MODIFIED_BY);
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return entry.getName();
   }

   /**
    * {@inheritDoc}
    */
   public String getObjectId()
   {
      return entry.getId();
   }

   /**
    * {@inheritDoc}
    */
   public Collection<PolicyData> getPolicies()
   {
      if (!getTypeDefinition().isControllablePolicy())
      {
         return Collections.emptyList();
      }
      Collection<JcrNodeEntry> policyEntries = entry.getPolicies();
      Set<PolicyData> policies = new HashSet<PolicyData>(policyEntries.size());
      for (JcrNodeEntry pe : policyEntries)
      {
         policies.add(new PolicyDataImpl(pe));
      }
      return policies;
   }

   /**
    * {@inheritDoc}
    */
   public Map<String, Property<?>> getProperties()
   {
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      TypeDefinition type = getTypeDefinition();
      for (PropertyDefinition<?> definition : type.getPropertyDefinitions())
      {
         properties.put(definition.getId(), doGetProperty(definition));
      }
      return properties;
   }

   /**
    * {@inheritDoc}
    */
   public Map<String, Property<?>> getProperties(PropertyFilter filter)
   {
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      for (PropertyDefinition<?> definition : getTypeDefinition().getPropertyDefinitions())
      {
         String queryName = definition.getQueryName();
         if (filter.accept(queryName))
         {
            String id = definition.getId();
            properties.put(id, doGetProperty(definition));
         }
      }
      return properties;
   }

   /**
    * {@inheritDoc}
    */
   public Property<?> getProperty(String id)
   {
      PropertyDefinition<?> definition = getTypeDefinition().getPropertyDefinition(id);
      if (definition == null)
      {
         return null; // TODO : need to throw exception ??
      }
      return doGetProperty(definition);
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<RelationshipData> getRelationships(RelationshipDirection direction, TypeDefinition type,
      boolean includeSubRelationshipTypes)
   {
      return new RelationshipIterator(entry.getRelationships(direction, type, includeSubRelationshipTypes));
   }

   /**
    * {@inheritDoc}
    */
   public TypeDefinition getTypeDefinition()
   {
      return entry.getType();
   }

   /**
    * {@inheritDoc}
    */
   public String getTypeId()
   {
      return getTypeDefinition().getId();
   }

   @Override
   public int hashCode()
   {
      int hash = 8;
      hash = hash * 31 + getNodeEntry().hashCode();
      return hash;
   }

   /**
    * {@inheritDoc}
    */
   public void removePolicy(PolicyData policy)
   {
      // Object is controllable by policy. It is checked in Connection.
      entry.removePolicy(((BaseObjectData)policy).getNodeEntry());
      try
      {
         save();
      }
      catch (StorageException se)
      {
         // Remove policy does not throw StorageException
         throw new CmisRuntimeException(se.getMessage(), se);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setACL(List<AccessControlEntry> acl)
   {
      // Object is controllable by ACL. It is checked in Connection.
      entry.setACL(acl);
      try
      {
         save();
      }
      catch (StorageException se)
      {
         // Apply ACL does not throw StorageException
         throw new CmisRuntimeException(se.getMessage(), se);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setProperties(Map<String, Property<?>> properties) throws NameConstraintViolationException,
      UpdateConflictException, VersioningException, StorageException
   {
      for (Property<?> property : properties.values())
      {
         doSetProperty(property);
      }
      save();
   }

   /**
    * {@inheritDoc}
    */
   public void setProperty(Property<?> property) throws NameConstraintViolationException, UpdateConflictException,
      VersioningException, StorageException
   {
      doSetProperty(property);
      save();
   }

   protected Property<?> doGetProperty(PropertyDefinition<?> definition)
   {
      // Check known prepared shortcut for properties.
      // Some properties may be virtual relating to JCR, it minds they
      // are not stored in JCR as properties but may be calculated in
      // some way.
      if (definition.getId().equals(CmisConstants.OBJECT_ID))
      {
         return new IdProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), getObjectId());
      }
      else if (definition.getId().equals(CmisConstants.OBJECT_TYPE_ID))
      {
         return new IdProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), getTypeId());
      }
      else if (definition.getId().equals(CmisConstants.BASE_TYPE_ID))
      {
         return new IdProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), getBaseType().value());
      }
      else if (definition.getId().equals(CmisConstants.NAME))
      {
         return new StringProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), getName());
      }
      else if (definition.getId().equals(CmisConstants.CREATION_DATE))
      {
         return new DateTimeProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), getCreationDate());
      }
      else if (definition.getId().equals(CmisConstants.LAST_MODIFICATION_DATE))
      {
         return new DateTimeProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), getLastModificationDate());
      }
      else if (definition.getId().equals(CmisConstants.PATH))
      {
         return new StringProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), ((FolderData)this).getPath());
      }
      else if (definition.getId().equals(CmisConstants.PARENT_ID) && getBaseType() == BaseType.FOLDER
         && !((FolderData)this).isRoot())
      {
         try
         {
            return new IdProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
               .getDisplayName(), getParent().getObjectId());
         }
         catch (ConstraintException never)
         {
            // Should never happen. We have checked it is a folder and is not
            // root folder so has exactly one parent.
           if (LOG.isErrorEnabled()) {
             LOG.error("Unable get parent id. " + never.getMessage(), never);
           }
         }
      }
      else if (definition.getId().equals(CmisConstants.CONTENT_STREAM_ID))
      {
            return new IdProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
               definition.getDisplayName(), ((DocumentDataImpl)this).getContentStreamId());
      }
      else if (definition.getId().equals(CmisConstants.CONTENT_STREAM_FILE_NAME))
      {
         if (((DocumentData)this).hasContent())
         {
            return new StringProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
               definition.getDisplayName(), ((DocumentDataImpl)this).getContentStreamFileName());
         }
      }
      else if (definition.getId().equals(CmisConstants.CONTENT_STREAM_MIME_TYPE))
      {
         if (((DocumentData)this).hasContent())
         {
            return new StringProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
               definition.getDisplayName(), ((DocumentData)this).getContentStreamMimeType());
         }
      }
      else if (definition.getId().equals(CmisConstants.CONTENT_STREAM_LENGTH))
      {
         if (((DocumentData)this).hasContent())
         {
            return new IntegerProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
               definition.getDisplayName(), BigInteger.valueOf(((DocumentDataImpl)this).getContentStreamLength()));
         }
      }
      else if (definition.getId().equals(CmisConstants.VERSION_SERIES_ID))
      {
         return new IdProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), ((DocumentData)this).getVersionSeriesId());
      }
      else if (definition.getId().equals(CmisConstants.VERSION_LABEL))
      {
         return new StringProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(), definition
            .getDisplayName(), ((DocumentData)this).getVersionLabel());
      }
      else if (definition.getId().equals(CmisConstants.IS_LATEST_VERSION))
      {
         return new BooleanProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), ((DocumentData)this).isLatestVersion());
      }
      else if (definition.getId().equals(CmisConstants.IS_MAJOR_VERSION))
      {
         return new BooleanProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), ((DocumentData)this).isMajorVersion());
      }
      else if (definition.getId().equals(CmisConstants.IS_LATEST_MAJOR_VERSION))
      {
         return new BooleanProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), ((DocumentData)this).isLatestMajorVersion());
      }
      else if (definition.getId().equals(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT))
      {
         return new BooleanProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), ((DocumentData)this).isVersionSeriesCheckedOut());
      }
      else if (definition.getId().equals(CmisConstants.IS_IMMUTABLE))
      {
         return new BooleanProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), false);
      }

      return entry.getProperty(definition);
   }

   /**
    * Update properties, skip on-create and read-only properties
    *
    * @param property
    *           property to be updated
    */
   protected void doSetProperty(Property<?> property) throws NameConstraintViolationException
   {
      PropertyDefinition<?> definition = getTypeDefinition().getPropertyDefinition(property.getId());

      Updatability updatability = definition.getUpdatability();
      if (updatability == Updatability.READWRITE || updatability == Updatability.WHENCHECKEDOUT
         && getBaseType() == BaseType.DOCUMENT && ((DocumentData)this).isPWC())
      {
         // Do not store nulls
         for (Iterator<?> i = property.getValues().iterator(); i.hasNext();)
         {
            Object v = i.next();
            if (v == null)
            {
               i.remove();
            }
         }
         entry.setProperty(property);
      }
      else
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Property " + property.getId() + " is not updatable.");
         }
      }

   }

   JcrNodeEntry getNodeEntry()
   {
      return entry;
   }

   protected void save() throws StorageException
   {
      entry.save(true);
   }

   /**
    * Delete current object.
    *
    * @throws StorageException
    *            if operation can't be persisted in back-end
    */
   public abstract void delete() throws StorageException;

}
