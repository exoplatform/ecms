package org.exoplatform.ecms.xcmis.sp.index;

import org.xcmis.search.SearchService;
import org.xcmis.search.content.ContentEntry;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapt changes produced by CMIS SPI to {@link ContentEntry} acceptable for.
 * {@link SearchService}
 */
public class ContentEntryAdapter
{
   /**
    * Convert {@link ObjectData} to {@link ContentEntry}.
    *
    * @param objectData
    *           ObjectData
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
    * @param objectData
    *           RelationshipData
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
    * @param objectData
    *           PolicyData
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
    * @param objectData
    *           FolderData
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
    * @param objectData
    *           DocumentData
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
