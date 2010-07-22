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

package org.exoplatform.ecms.xcmis.sp.jcr.exo.index;

import org.apache.commons.lang.Validate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.search.SearchService;
import org.xcmis.search.content.ContentEntry;
import org.xcmis.search.content.IndexModificationException;
import org.xcmis.search.content.Property;
import org.xcmis.search.content.Property.BinaryValue;
import org.xcmis.search.content.Property.ContentValue;
import org.xcmis.search.content.Property.SimpleValue;
import org.xcmis.search.value.PropertyType;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: IndexListener.java 1261 2010-06-09 09:52:06Z andrew00x $
 */
public class IndexListener
{

   private static final Log LOG = ExoLogger.getLogger(IndexListener.class);

   /**
    * Index storage.
    */
   private final SearchService searchService;

   /**
    * Content storage.
    */
   private final Storage storage;

   private ContentEntryAdapter contentEntryAdapter;

   public IndexListener(Storage storage, SearchService searchService)
   {
      Validate.notNull(searchService, "The searchService argument may not be null");
      this.storage = storage;
      this.searchService = searchService;
      this.contentEntryAdapter = new ContentEntryAdapter();
   }

   public void created(ObjectData object)
   {
      try
      {
         searchService.update(contentEntryAdapter.createEntry(object), null);
      }
      catch (IndexModificationException e)
      {
         LOG.error(e.getLocalizedMessage());
      }
      catch (IOException e)
      {
         LOG.error(e.getLocalizedMessage());
      }
   }

   public void removed(Set<String> removed)
   {
      try
      {
         searchService.update(Collections.EMPTY_LIST, removed);
      }
      catch (IndexModificationException e)
      {
         LOG.error(e.getLocalizedMessage());
      }
   }

   public void updated(ObjectData object)
   {
      try
      {
         searchService.update(contentEntryAdapter.createEntry(object), object.getObjectId());
      }
      catch (IndexModificationException e)
      {
         LOG.error(e.getLocalizedMessage());
      }
      catch (IOException e)
      {
         LOG.error(e.getLocalizedMessage());
      }
   }

   /**
    * Adapt changes produced by CMIS SPI to {@link ContentEntry} acceptable for.
    * {@link SearchService}
    */
   public static class ContentEntryAdapter
   {
      /**
       * Convert {@link ObjectData} to {@link ContentEntry}.
       *
       * @param objectData ObjectData
       * @return contentEntry ContentEntry
       * @throws IOException
       */
      public ContentEntry createEntry(ObjectData objectData) throws IOException
      {
         if (objectData != null)
         {
            switch (objectData.getBaseType())
            {
               case DOCUMENT :
                  return createFromDocument((DocumentData)objectData);
               case FOLDER :
                  return createFromFolder((FolderData)objectData);
               case POLICY :
                  return createFromPolicy((PolicyData)objectData);
               case RELATIONSHIP :
                  return createFromRelationship((RelationshipData)objectData);
               default :
                  throw new UnsupportedOperationException(objectData.getBaseType().toString()
                     + " is not supported for indexing");
            }
         }
         return null;
      }

      /**
       * @param objectData RelationshipData
       * @return ContentEntry ContentEntry;
       */
      private ContentEntry createFromRelationship(RelationshipData objectData)
      {
         MockContentEntry mockEntry = fillCommonInformation(objectData);
         //mark parent of root as parent
         mockEntry.parentIdentifiers.add("");
         return new ContentEntry(mockEntry.name, mockEntry.getTableNames(), mockEntry.identifier, mockEntry
            .getParentIdentifiers(), mockEntry.getProperties());
      }

      /**
       * @param objectData PolicyData
       * @return contentEntry ContentEntry
       */
      private ContentEntry createFromPolicy(PolicyData objectData)
      {
         MockContentEntry mockEntry = fillCommonInformation(objectData);
         //mark parent of root as parent
         mockEntry.parentIdentifiers.add("");
         return new ContentEntry(mockEntry.name, mockEntry.getTableNames(), mockEntry.identifier, mockEntry
            .getParentIdentifiers(), mockEntry.getProperties());
      }

      private MockContentEntry fillCommonInformation(ObjectData objectData)
      {
         MockContentEntry contentEntry = new MockContentEntry();
         contentEntry.tableNames.add(objectData.getTypeDefinition().getQueryName());
         contentEntry.identifier = objectData.getObjectId();
         contentEntry.name = objectData.getName();
         for (FolderData folder : objectData.getParents())
         {
            contentEntry.parentIdentifiers.add(folder.getObjectId());
         }

         for (org.xcmis.spi.model.Property<?> property : objectData.getProperties().values())
         {
            if (property.getValues().size() > 0)
            {
               contentEntry.properties.add(convertProperty(property));
            }
         }
         return contentEntry;
      }

      private <G> Property<G> convertProperty(org.xcmis.spi.model.Property<G> property)
      {
         Collection<ContentValue<G>> value = new ArrayList<ContentValue<G>>();
         for (G contentValue : property.getValues())
         {
            value.add(new SimpleValue<G>(contentValue));
         }

         return new Property<G>(CmisSchema.PROPERTY_TYPES_MAP.get(property.getType()), property.getQueryName(), value);
      }

      /**
       * Convert {@link FolderData} to {@link ContentEntry}.
       *
       * @param objectData FolderData
       * @return contentEntry ContentEntry
       */
      private ContentEntry createFromFolder(FolderData objectData)
      {
         MockContentEntry mockEntry = fillCommonInformation(objectData);
         return new ContentEntry(mockEntry.name, mockEntry.getTableNames(), mockEntry.identifier, mockEntry
            .getParentIdentifiers(), mockEntry.getProperties());
      }

      /**
       * Convert {@link DocumentData} to {@link ContentEntry}.
       *
       * @param objectData DocumentData
       * @return contentEntry ContentEntry.
       * @throws IOException
       */
      private ContentEntry createFromDocument(DocumentData objectData) throws IOException
      {
         MockContentEntry mockEntry = fillCommonInformation(objectData);
         ContentStream cs = objectData.getContentStream();
         if (cs != null)
         {
            List<ContentValue<InputStream>> vals = new ArrayList<ContentValue<InputStream>>(1);
            vals.add(new BinaryValue(cs.getStream(), cs.getMediaType().getBaseType(), cs.getMediaType().getParameter(
               CmisConstants.CHARSET), cs.length()));
            //TODO add constant for property name content
            mockEntry.properties.add(new Property<InputStream>(PropertyType.BINARY, "content", vals));
         }
         return new ContentEntry(mockEntry.name, mockEntry.getTableNames(), mockEntry.identifier, mockEntry
            .getParentIdentifiers(), mockEntry.getProperties());
      }
   }

   private static class MockContentEntry
   {
      /**
       * List of table names which identifies this content.
       */
      List<String> tableNames;

      /**
       * Name of the entry.
       */
      String name;

      /**
       * List of parent entry identifiers.
       */
      List<String> parentIdentifiers;

      /**
       * Entry identifier.
       */
      String identifier;

      /**
       * List of entry properties.
       */
      List<Property> properties;

      /**
       *
       */
      MockContentEntry()
      {
         tableNames = new ArrayList<String>();
         parentIdentifiers = new ArrayList<String>();
         properties = new ArrayList<Property>();
      }

      /**
       * @return the tableNames
       */
      public String[] getTableNames()
      {
         return tableNames.toArray(new String[tableNames.size()]);
      }

      /**
       * @return the parentIdentifiers
       */
      public String[] getParentIdentifiers()
      {
         return parentIdentifiers.toArray(new String[parentIdentifiers.size()]);
      }

      /**
       * @return the properties
       */
      public Property[] getProperties()
      {
         return properties.toArray(new Property[properties.size()]);
      }

   }
}
