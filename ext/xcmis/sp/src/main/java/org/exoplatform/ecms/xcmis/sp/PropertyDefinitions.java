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
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.Choice;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.Updatability;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapping for known CMIS object properties.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public final class PropertyDefinitions
{

   private static final Map<String, Map<String, PropertyDefinition<?>>> all =
      new HashMap<String, Map<String, PropertyDefinition<?>>>();

   static
   {
      for (BaseType objectType : BaseType.values())
      {
         // Common properties.
         add(objectType.value(), createPropertyDefinition(CmisConstants.BASE_TYPE_ID, PropertyType.ID,
            CmisConstants.BASE_TYPE_ID, CmisConstants.BASE_TYPE_ID, null, CmisConstants.BASE_TYPE_ID, false, false,
            false, false, false, Updatability.READONLY, "Base type id.", null, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.OBJECT_TYPE_ID, PropertyType.ID,
            CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_ID, null, CmisConstants.OBJECT_TYPE_ID, false,
            false, false, false, false, Updatability.ONCREATE, "Object type id.", null, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.OBJECT_ID, PropertyType.ID,
            CmisConstants.OBJECT_ID, CmisConstants.OBJECT_ID, null, CmisConstants.OBJECT_ID, false, false, false,
            false, false, Updatability.READONLY, "Object id.", null, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.NAME, PropertyType.STRING, CmisConstants.NAME,
            CmisConstants.NAME, null, CmisConstants.NAME, true, false, false, false, false, Updatability.READWRITE,
            "Object name.", true, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.CREATED_BY, PropertyType.STRING,
            CmisConstants.CREATED_BY, CmisConstants.CREATED_BY, null, CmisConstants.CREATED_BY, false, false, false,
            false, false, Updatability.READONLY, "User who created the object.", null, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.CREATION_DATE, PropertyType.DATETIME,
            CmisConstants.CREATION_DATE, CmisConstants.CREATION_DATE, null, CmisConstants.CREATION_DATE, false, false,
            false, false, false, Updatability.READONLY, "DateTime when the object was created.", null, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.LAST_MODIFIED_BY, PropertyType.STRING,
            CmisConstants.LAST_MODIFIED_BY, CmisConstants.LAST_MODIFIED_BY, null, CmisConstants.LAST_MODIFIED_BY,
            false, false, false, false, false, Updatability.READONLY, "User who last modified the object.", null, null,
            null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.LAST_MODIFICATION_DATE, PropertyType.DATETIME,
            CmisConstants.LAST_MODIFICATION_DATE, CmisConstants.LAST_MODIFICATION_DATE, null,
            CmisConstants.LAST_MODIFICATION_DATE, false, false, false, false, false, Updatability.READONLY,
            "DateTime when the object was last modified.", null, null, null));

         add(objectType.value(), createPropertyDefinition(CmisConstants.CHANGE_TOKEN, PropertyType.STRING,
            CmisConstants.CHANGE_TOKEN, CmisConstants.CHANGE_TOKEN, null, CmisConstants.CHANGE_TOKEN, false, false,
            false, false, false, Updatability.READONLY, "Opaque token used for optimistic locking.", null, null, null));

         // Type specific.
         if (objectType == BaseType.DOCUMENT)
         {
            add(objectType.value(), createPropertyDefinition(CmisConstants.IS_IMMUTABLE, PropertyType.BOOLEAN,
               CmisConstants.IS_IMMUTABLE, CmisConstants.IS_IMMUTABLE, null, CmisConstants.IS_IMMUTABLE, false, false,
               false, false, false, Updatability.READONLY,
               "TRUE if the repository MUST throw an error at any attempt to update or delete the object.", null, null,
               null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.IS_LATEST_VERSION, PropertyType.BOOLEAN,
               CmisConstants.IS_LATEST_VERSION, CmisConstants.IS_LATEST_VERSION, null, CmisConstants.IS_LATEST_VERSION,
               false, false, false, false, false, Updatability.READONLY,
               "TRUE if object represents latest version of object.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.IS_MAJOR_VERSION, PropertyType.BOOLEAN,
               CmisConstants.IS_MAJOR_VERSION, CmisConstants.IS_MAJOR_VERSION, null, CmisConstants.IS_MAJOR_VERSION,
               false, false, false, false, false, Updatability.WHENCHECKEDOUT,
               "TRUE if object represents major version of object.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.IS_LATEST_MAJOR_VERSION,
               PropertyType.BOOLEAN, CmisConstants.IS_LATEST_MAJOR_VERSION, CmisConstants.IS_LATEST_MAJOR_VERSION,
               null, CmisConstants.IS_LATEST_MAJOR_VERSION, false, false, false, false, false, Updatability.READONLY,
               "TRUE if object represents latest major version of object.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.VERSION_LABEL, PropertyType.STRING,
               CmisConstants.VERSION_LABEL, CmisConstants.VERSION_LABEL, null, CmisConstants.VERSION_LABEL, false,
               false, false, false, false, Updatability.READONLY, "Version label.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.VERSION_SERIES_ID, PropertyType.ID,
               CmisConstants.VERSION_SERIES_ID, CmisConstants.VERSION_SERIES_ID, null, CmisConstants.VERSION_SERIES_ID,
               false, false, false, false, false, Updatability.READONLY, "ID of version series.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT,
               PropertyType.BOOLEAN, CmisConstants.IS_VERSION_SERIES_CHECKED_OUT,
               CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, null, CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, false,
               false, false, false, false, Updatability.READONLY,
               "TRUE if some document in version series is checkedout.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY,
               PropertyType.STRING, CmisConstants.VERSION_SERIES_CHECKED_OUT_BY,
               CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, null, CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, false,
               false, false, false, false, Updatability.READONLY, "User who checkedout document.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID,
               PropertyType.ID, CmisConstants.VERSION_SERIES_CHECKED_OUT_ID,
               CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, null, CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, false,
               false, false, false, false, Updatability.READONLY, "ID of checkedout document.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.CHECKIN_COMMENT, PropertyType.STRING,
               CmisConstants.CHECKIN_COMMENT, CmisConstants.CHECKIN_COMMENT, null, CmisConstants.CHECKIN_COMMENT,
               false, false, false, false, false, Updatability.WHENCHECKEDOUT, "Check-In comment.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.CONTENT_STREAM_LENGTH, PropertyType.INTEGER,
               CmisConstants.CONTENT_STREAM_LENGTH, CmisConstants.CONTENT_STREAM_LENGTH, null,
               CmisConstants.CONTENT_STREAM_LENGTH, false, false, false, false, false, Updatability.READONLY,
               "Length of document content in bytes.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.CONTENT_STREAM_MIME_TYPE,
               PropertyType.STRING, CmisConstants.CONTENT_STREAM_MIME_TYPE, CmisConstants.CONTENT_STREAM_MIME_TYPE,
               null, CmisConstants.CONTENT_STREAM_MIME_TYPE, false, false, false, false, false, Updatability.READONLY,
               "Media type of document content.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.CONTENT_STREAM_FILE_NAME,
               PropertyType.STRING, CmisConstants.CONTENT_STREAM_FILE_NAME, CmisConstants.CONTENT_STREAM_FILE_NAME,
               null, CmisConstants.CONTENT_STREAM_FILE_NAME, false, false, false, false, false, Updatability.READWRITE,
               "Document's content file name.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.CONTENT_STREAM_ID, PropertyType.ID,
               CmisConstants.CONTENT_STREAM_ID, CmisConstants.CONTENT_STREAM_ID, null, CmisConstants.CONTENT_STREAM_ID,
               false, false, false, false, false, Updatability.READONLY, "Document's content stream ID.", null, null,
               null));
         }
         else if (objectType == BaseType.FOLDER)
         {
            add(objectType.value(), createPropertyDefinition(CmisConstants.PARENT_ID, PropertyType.ID,
               CmisConstants.PARENT_ID, CmisConstants.PARENT_ID, null, CmisConstants.PARENT_ID, false, false, false,
               false, false, Updatability.READONLY, "ID of parent folder.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.ALLOWED_CHILD_OBJECT_TYPE_IDS,
               PropertyType.ID, CmisConstants.ALLOWED_CHILD_OBJECT_TYPE_IDS,
               CmisConstants.ALLOWED_CHILD_OBJECT_TYPE_IDS, null, CmisConstants.ALLOWED_CHILD_OBJECT_TYPE_IDS, false,
               false, false, false, true, Updatability.READONLY, "Set of allowed child types for folder.", null, null,
               null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.PATH, PropertyType.STRING,
               CmisConstants.PATH, CmisConstants.PATH, null, CmisConstants.PATH, false, false, false, false, false,
               Updatability.READONLY, "Full path to folder object.", null, null, null));
         }
         else if (objectType == BaseType.POLICY)
         {
            add(objectType.value(),
               createPropertyDefinition(CmisConstants.POLICY_TEXT, PropertyType.STRING, CmisConstants.POLICY_TEXT,
                  CmisConstants.POLICY_TEXT, null, CmisConstants.POLICY_TEXT, true, false, false, false, false,
                  Updatability.ONCREATE, "User-friendly description of the policy.", null, null, null));
         }
         else if (objectType == BaseType.RELATIONSHIP)
         {
            add(objectType.value(), createPropertyDefinition(CmisConstants.SOURCE_ID, PropertyType.ID,
               CmisConstants.SOURCE_ID, CmisConstants.SOURCE_ID, null, CmisConstants.SOURCE_ID, false, false, false,
               false, false, Updatability.ONCREATE, "ID of relationship's source object.", null, null, null));

            add(objectType.value(), createPropertyDefinition(CmisConstants.TARGET_ID, PropertyType.ID,
               CmisConstants.TARGET_ID, CmisConstants.TARGET_ID, null, CmisConstants.TARGET_ID, false, false, false,
               false, false, Updatability.ONCREATE, "ID of relationship's target object.", null, null, null));
         }
      }
   }

   /**
    * Get all property definitions for <code>objectTypeId</code>.
    *
    * @param objectTypeId object type id
    * @return set of object property definitions
    */
   public static Map<String, PropertyDefinition<?>> getAll(String objectTypeId)
   {
      Map<String, PropertyDefinition<?>> defs = all.get(objectTypeId);
      if (defs == null)
      {
         return Collections.emptyMap();
      }
      return Collections.unmodifiableMap(defs);
   }

   /**
    * Get all property IDs supported for <code>objectTypeId</code>.
    *
    * @param objectTypeId object type id
    * @return set of object property definition IDs.
    */
   public static Set<String> getPropertyIds(String objectTypeId)
   {
      Map<String, PropertyDefinition<?>> defs = all.get(objectTypeId);
      if (defs == null)
      {
         return Collections.emptySet();
      }
      return Collections.unmodifiableSet(defs.keySet());
   }

   /**
    * Get one property definition with <code>propDefId</code> for
    * <code>objectTypeId</code>.
    *
    * @param objectTypeId object type id
    * @param propDefId property definition id
    * @return property definition or null
    */
   public static PropertyDefinition<?> getPropertyDefinition(String objectTypeId, String propDefId)
   {
      Map<String, PropertyDefinition<?>> defs = all.get(objectTypeId);
      if (defs == null)
      {
         return null;
      }
      return defs.get(propDefId);
   }

   private static void add(String typeId, PropertyDefinition<?> propDef)
   {
      Map<String, PropertyDefinition<?>> defs = all.get(typeId);
      if (defs == null)
      {
         defs = new HashMap<String, PropertyDefinition<?>>();
         all.put(typeId, defs);
      }
      defs.put(propDef.getId(), propDef);
   }

   private static <T> PropertyDefinition<T> createPropertyDefinition(String id, PropertyType propertyType,
      String queryName, String localName, String localNamespace, String displayName, boolean required,
      boolean queryable, boolean orderable, boolean inherited, boolean isMultivalued, Updatability updatability,
      String description, Boolean openChoice, List<Choice<T>> choices, T[] defValue)
   {
      PropertyDefinition<T> propertyDefinition =
         new PropertyDefinition<T>(id, queryName, localName, localNamespace, displayName, description, propertyType,
            updatability, inherited, required, queryable, orderable, openChoice, isMultivalued, choices, defValue);

      return propertyDefinition;
   }

   /**
    * Not instantiable.
    */
   private PropertyDefinitions()
   {
   }
}
