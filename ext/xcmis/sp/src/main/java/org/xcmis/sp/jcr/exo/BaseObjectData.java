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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
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
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: BaseObjectData.java 1160 2010-05-21 17:06:16Z
 *          alexey.zavizionov@gmail.com $
 */
abstract class BaseObjectData implements ObjectData
{
   private class RelationshipIterator implements ItemsIterator<RelationshipData>
   {

      private final PropertyIterator iter;

      private final TypeDefinition type;

      private final boolean includeSubRelationshipTypes;

      private final RelationshipDirection direction;

      private RelationshipData next;

      RelationshipIterator(PropertyIterator iter, TypeDefinition type, RelationshipDirection direction,
         boolean includeSubRelationshipTypes)
      {
         this.iter = iter;
         this.type = type;
         this.direction = direction;
         this.includeSubRelationshipTypes = includeSubRelationshipTypes;
         fetchNext();
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasNext()
      {
         return next != null;
      }

      /**
       * {@inheritDoc}
       */
      public RelationshipData next()
      {
         if (next == null)
         {
            throw new NoSuchElementException();
         }
         RelationshipData n = next;
         fetchNext();
         return n;
      }

      /**
       * {@inheritDoc}
       */
      public void remove()
      {
         throw new UnsupportedOperationException("remove");
      }

      /**
       * {@inheritDoc}
       */
      public int size()
      {
         return -1;
      }

      /**
       * {@inheritDoc}
       */
      public void skip(int skip) throws NoSuchElementException
      {
         while (skip-- > 0)
         {
            fetchNext();
            if (next == null)
            {
               throw new NoSuchElementException();
            }
         }
      }

      /**
       * To fetch next item.
       */
      void fetchNext()
      {
         next = null;
         while (next == null && iter.hasNext())
         {
            javax.jcr.Property prop = iter.nextProperty();
            try
            {
               String propName = prop.getName();
               if ((direction == RelationshipDirection.EITHER && (propName.equals(CmisConstants.SOURCE_ID) || propName
                  .equals(CmisConstants.TARGET_ID)))
                  || (direction == RelationshipDirection.SOURCE && propName.equals(CmisConstants.SOURCE_ID))
                  || (direction == RelationshipDirection.TARGET && propName.equals(CmisConstants.TARGET_ID)))
               {
                  Node relNode = prop.getParent();
                  NodeType nodeType = relNode.getPrimaryNodeType();
                  if (nodeType.getName().equals(type.getLocalName()) //
                     || (includeSubRelationshipTypes && nodeType.isNodeType(type.getLocalName())))
                  {
                     next = new RelationshipDataImpl(new JcrNodeEntry(relNode), indexListener);
                  }
               }
            }
            catch (RepositoryException re)
            {
               LOG.error(re.getMessage());
            }
         }
      }
   }

   private static final Log LOG = ExoLogger.getLogger(BaseObjectData.class);

   protected IndexListener indexListener;

   protected JcrNodeEntry jcrEntry;

   public BaseObjectData(JcrNodeEntry jcrEntry, IndexListener indexListener)
   {
      this.jcrEntry = jcrEntry;
      this.indexListener = indexListener;
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
      jcrEntry.applyPolicy(policy);
      try
      {
         save();
      }
      catch (StorageException se)
      {
         throw new CmisRuntimeException("Unable apply policy. " + se.getMessage(), se);
      }
   }

   public boolean equals(Object obj)
   {
      if (obj == null)
      {
         return false;
      }
      if (obj.getClass() != getClass())
      {
         return false;
      }
      return ((BaseObjectData)obj).getObjectId().equals(getObjectId());
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
      return jcrEntry.getACL();
   }

   /**
    * {@inheritDoc}
    */
   public BaseType getBaseType()
   {
      return jcrEntry.getBaseType();
   }

   /**
    * {@inheritDoc}
    */
   public String getChangeToken()
   {
      return jcrEntry.getString(CmisConstants.CHANGE_TOKEN);
   }

   /**
    * {@inheritDoc}
    */
   public String getCreatedBy()
   {
      return jcrEntry.getString(CmisConstants.CREATED_BY);
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getCreationDate()
   {
      return jcrEntry.getDate(CmisConstants.CREATION_DATE);
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getLastModificationDate()
   {
      return jcrEntry.getDate(CmisConstants.LAST_MODIFICATION_DATE);
   }

   /**
    * {@inheritDoc}
    */
   public String getLastModifiedBy()
   {
      return jcrEntry.getString(CmisConstants.LAST_MODIFIED_BY);
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      return jcrEntry.getName();
   }

   /**
    * {@inheritDoc}
    */
   public String getObjectId()
   {
      return jcrEntry.getId();
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
      Collection<JcrNodeEntry> policyEntries = jcrEntry.getPolicies();
      Set<PolicyData> policies = new HashSet<PolicyData>(policyEntries.size());
      for (JcrNodeEntry pe : policyEntries)
      {
         policies.add(new PolicyDataImpl(pe, indexListener));
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
      try
      {
         PropertyIterator iter = getNode().getReferences();
         return new RelationshipIterator(iter, type, direction, includeSubRelationshipTypes);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get relationships. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public TypeDefinition getTypeDefinition()
   {
      return jcrEntry.getType();
   }

   /**
    * {@inheritDoc}
    */
   public String getTypeId()
   {
      return getTypeDefinition().getId();
   }

   public int hashCode()
   {
      return getObjectId().hashCode();
   }

   /**
    * {@inheritDoc}
    */
   public void removePolicy(PolicyData policy)
   {
      // Object is controllable by policy. It is checked in Connection.
      jcrEntry.removePolicy(policy);
      try
      {
         save();
      }
      catch (StorageException se)
      {
         // remove policy does not throw StorageException
         throw new CmisRuntimeException(se.getMessage(), se);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setACL(List<AccessControlEntry> acl)
   {
      // Object is controllable by ACL. It is checked in Connection.
      jcrEntry.setACL(acl);
      try
      {
         save();
      }
      catch (StorageException se)
      {
         // apply ACL does not throw StorageException
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
            LOG.error("Unable get parent id. " + never.getMessage());
         }
      }
      else if (definition.getId().equals(CmisConstants.CONTENT_STREAM_FILE_NAME))
      {
         if (((DocumentData)this).hasContent())
         {
            return new StringProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
               definition.getDisplayName(), getName());
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
      else if (definition.getId().equals(CmisConstants.IS_LATEST_MAJOR_VERSION))
      {
         return new BooleanProperty(definition.getId(), definition.getQueryName(), definition.getLocalName(),
            definition.getDisplayName(), ((DocumentData)this).isLatestMajorVersion());
      }

      return jcrEntry.getProperty(definition);
   }

   /**
    * Update properties, skip on-create and read-only properties
    *
    * @param property property to be updated
    */
   protected void doSetProperty(Property<?> property) throws NameConstraintViolationException
   {
      PropertyDefinition<?> definition = getTypeDefinition().getPropertyDefinition(property.getId());

      Updatability updatability = definition.getUpdatability();
      if (updatability == Updatability.READWRITE
         || (updatability == Updatability.WHENCHECKEDOUT && getBaseType() == BaseType.DOCUMENT && ((DocumentData)this)
            .isPWC()))
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
         jcrEntry.setProperty(property);
      }
      else
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Property " + property.getId() + " is not updatable.");
         }
      }

   }

   Node getNode()
   {
      return jcrEntry.getNode();
   }

   JcrNodeEntry getNodeEntry()
   {
      return jcrEntry;
   }

   protected void save() throws StorageException
   {
      boolean isNew = getNode().isNew();
      jcrEntry.updateAndSave();
      if (indexListener != null)
      {
         if (isNew)
         {
            indexListener.created(this);
         }
         else
         {
            indexListener.updated(this);
         }
      }
   }

   protected abstract void delete() throws StorageException;

}
