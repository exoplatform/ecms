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

package org.exoplatform.ecms.xcmis.sp.query;

import org.exoplatform.ecms.xcmis.sp.BaseTest;
import org.exoplatform.ecms.xcmis.sp.PropertyDefinitions;
import org.exoplatform.ecms.xcmis.sp.StorageImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.model.impl.DecimalProperty;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.utils.MimeType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SAS. <br>
 * Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id$
 */
public abstract class BaseQueryTest extends BaseTest
{
   private static final Log LOG = ExoLogger.getLogger(BaseQueryTest.class.getName());

   protected final static String NASA_DOCUMENT = "cmis:nasa-mission";

   protected final static String PROPERTY_BOOSTER = "cmis:booster-name";

   protected final static String PROPERTY_COMMANDER = "cmis:commander";

   protected final static String PROPERTY_COMMAND_MODULE_PILOT = "cmis:command-module-pilot";

   protected final static String PROPERTY_LUNAR_MODULE_PILOT = "cmis:lunar-module-pilot";

   protected final static String PROPERTY_BOOSTER_MASS = "cmis:booster-mass";

   protected final static String PROPERTY_SAMPLE_RETURNED = "cmis:sample-returned";

   protected final static String PROPERTY_STATUS = "cmis:status";

   // protected Storage storage;

   // protected FolderData rootFolder;

   //protected TypeDefinition nasaDocumentTypeDefinition;

   //protected TypeDefinition folderTypeDefinition;

   //   @Override
   //   public void setUp() throws Exception
   //   {
   //      super.setUp();
   //      //storage = storageProvider.getConnection().getStorage();
   //      rootFolder = (FolderData)storage.getObjectById(storage.getRepositoryInfo().getRootFolderId());
   //
   //      nasaDocumentTypeDefinition = storage.getTypeDefinition(NASA_DOCUMENT, true);
   //      folderTypeDefinition = storage.getTypeDefinition("cmis:folder", true);
   //   }

   protected DocumentData createDocument(StorageImpl storage, FolderData folder, String name,
      TypeDefinition typeDefinition, byte[] content, MimeType mimeType) throws Exception
   {
      return createDocument(storage, folder, name, typeDefinition, new BaseContentStream(content, null, mimeType), null);
   }

   protected DocumentData createDocument(StorageImpl storage, FolderData folder, String name,
      TypeDefinition typeDefinition, ContentStream content, VersioningState versioningState) throws Exception//   /**
   {

      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      DocumentData document =
         storage.createDocument(folder, typeDefinition, properties, content, null, null, versioningState == null
            ? VersioningState.MAJOR : versioningState);
      return document;
   }

   protected FolderData createFolder(StorageImpl storage, FolderData folder, String name, TypeDefinition typeDefinition)
      throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:folder", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      FolderData newFolder = storage.createFolder(folder, typeDefinition, properties, null, null);
      return newFolder;
   }

   protected void checkResult(StorageImpl storage, ItemsIterator<Result> result, String[] columns,
      String[][] expectedResults) throws Exception
   {
      // expected
      Set<String> expectedPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("expected:");
      }
      for (String[] expectedResult : expectedResults)
      {
         String newRow = "";
         for (String element : expectedResult)
         {
            newRow += "|" + element;
         }
         expectedPaths.add(newRow);
         if (LOG.isDebugEnabled())
         {
            LOG.debug(newRow);
         }
      }
      // actual

      Set<String> resultPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("result:");
      }
      while (result.hasNext())
      {
         String objectId = result.next().getObjectId();
         String resultRow = "";
         for (String column : columns)
         {
            resultRow += "|" + getProperty(storage, objectId, column);

         }
         resultPaths.add(resultRow);
         if (LOG.isDebugEnabled())
         {
            LOG.debug(resultRow);
         }
      }

      checkResults(expectedPaths, resultPaths);
   }

   protected void checkResult(StorageImpl storage, String statement, ObjectData[] nodes)
   {
      Query query = new Query(statement, true);

      ItemsIterator<Result> result = storage.query(query);
      checkResult(storage, result, nodes);
   }

   protected void checkResult(StorageImpl storage, ItemsIterator<Result> result, ObjectData[] nodes)
   {
      assertEquals(nodes.length, result.size());

      // collect rows
      Set<String> expectedPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("expected:");
      }

      for (ObjectData node : nodes)
      {
         expectedPaths.add(node.getObjectId());
         if (LOG.isDebugEnabled())
         {
            LOG.debug(node.getObjectId());
         }
      }

      Set<String> resultPaths = new HashSet<String>();
      if (LOG.isDebugEnabled())
      {
         LOG.debug("result:");
      }
      while (result.hasNext())
      {
         Result next = result.next();
         if (LOG.isDebugEnabled())
         {
            LOG.debug(next.getObjectId());
         }
         String id = next.getObjectId();
         resultPaths.add(id);
         try
         {
            storage.getObjectById(id);
         }
         catch (ObjectNotFoundException e)
         {
            fail(e.getMessage());
         }
         //LOG.debug("id:=" + id + " path:=" + object.getParent().getPath() + "/" + object.getName());
      }

      checkResults(expectedPaths, resultPaths);
   }

   protected void checkResultOrder(ItemsIterator<Result> result, ObjectData[] nodes)
   {
      // collect rows
      String expectedPaths = "";
      if (LOG.isDebugEnabled())
      {
         LOG.debug("expected:");
      }

      for (ObjectData node : nodes)
      {
         expectedPaths += ":" + node.getObjectId();
         if (LOG.isDebugEnabled())
         {
            LOG.debug(node.getObjectId());
         }
      }

      String resultPaths = "";
      if (LOG.isDebugEnabled())
      {
         LOG.debug("result:");
      }
      while (result.hasNext())
      {
         String id = result.next().getObjectId();
         resultPaths += ":" + id;
         if (LOG.isDebugEnabled())
         {
            LOG.debug(id);
         }
      }

      assertEquals(expectedPaths, resultPaths);
   }

   private void checkResults(Set<String> expectedRowResults, Set<String> actualRowResults)
   {
      // check if all expected are in result
      for (String path : expectedRowResults)
      {
         assertTrue(path + " is not part of the result set " + actualRowResults, actualRowResults.contains(path));
      }
      // check result does not contain more than expected
      for (String path : actualRowResults)
      {
         assertTrue(path + " is not expected to be part of the result set", expectedRowResults.contains(path));
      }
   }

   private String getProperty(StorageImpl storage, String objectId, String propertyId) throws Exception
   {
      ObjectData entry = storage.getObjectById(objectId);
      return entry.getProperty(propertyId).toString();
   }

   /**
    * Create content for Apollo program.
    *
    * @param folder
    * @return
    * @throws Exception
    */
   protected List<DocumentData> createNasaContent(StorageImpl storage, FolderData folder) throws Exception
   {
      List<DocumentData> result = new ArrayList<DocumentData>();
      result.add(createAppoloMission(storage, folder, "Apollo 7", "Walter M. Schirra", "Donn F. Eisele",
         "R. Walter Cunningham", "Saturn 1B", 581.844, 0,
         "Apollo 7 (October 11-22, 1968) was the first manned mission "
            + "in the Apollo program to be launched. It was an eleven-day "
            + "Earth-orbital mission, the first manned launch of the "
            + "Saturn IB launch vehicle, and the first three-person " + "American space mission"));

      result.add(createAppoloMission(storage, folder, "Apollo 8", "Frank F. Borman, II", "James A. Lovell, Jr",
         "William A. Anders", "Saturn V", 3038.500, 0, "Apollo 8 was the first "
            + "manned space voyage to achieve a velocity sufficient to allow escape from the "
            + "gravitational field of planet Earth; the first to escape from the gravitational "
            + "field of another celestial body; and the first manned voyage to return to planet Earth "
            + "from another celestial body - Earth's Moon"));

      result.add(createAppoloMission(storage, folder, "Apollo 13", "James A. Lovell, Jr.", "John L. Swigert",
         "Fred W. Haise, Jr.", "Saturn V", 3038.500, 0, "Apollo 13 was the third "
            + "manned mission by NASA intended to land on the Moon, but a mid-mission technical "
            + "malfunction forced the lunar landing to be aborted. "));

      result.add(createAppoloMission(storage, folder, "Apollo 17", "Eugene A. Cernan", "Ronald E. Evans",
         "Harrison H. Schmitt", "Saturn V", 3038.500, 111, "Apollo 17 was the eleventh manned space "
            + "mission in the NASA Apollo program. It was the first night launch of a U.S. human "
            + "spaceflight and the sixth and final lunar landing mission of the Apollo program."));

      return result;
   }

   /**
    * @see org.exoplatform.ecms.xcmis.sp.BaseTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
   }

   protected DocumentData createAppoloMission(StorageImpl storage, FolderData parentFolder, String missionName,
      String commander, String commandModulePilot, String lunarModulePilot, String boosterName, double boosterMass,
      long sampleReturned, String objectives) throws Exception
   {

      //      folderTypeDefinition = storage.getTypeDefinition("cmis:folder", true);
      TypeDefinition nasaDocumentTypeDefinition = storage.getTypeDefinition(NASA_DOCUMENT, true);
      DocumentData doc =
         createDocument(storage, parentFolder, missionName, nasaDocumentTypeDefinition, objectives.getBytes(),
            new MimeType("text", "plain"));
      setProperty(doc, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, commander));
      setProperty(doc, new StringProperty(PROPERTY_COMMAND_MODULE_PILOT, PROPERTY_COMMAND_MODULE_PILOT,
         PROPERTY_COMMAND_MODULE_PILOT, PROPERTY_COMMAND_MODULE_PILOT, commandModulePilot));
      setProperty(doc, new StringProperty(PROPERTY_LUNAR_MODULE_PILOT, PROPERTY_LUNAR_MODULE_PILOT,
         PROPERTY_LUNAR_MODULE_PILOT, PROPERTY_LUNAR_MODULE_PILOT, lunarModulePilot));
      setProperty(doc, new StringProperty(PROPERTY_BOOSTER, PROPERTY_BOOSTER, PROPERTY_BOOSTER, PROPERTY_BOOSTER,
         boosterName));

      setProperty(doc, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(boosterMass)));
      setProperty(doc, new DecimalProperty(PROPERTY_SAMPLE_RETURNED, PROPERTY_SAMPLE_RETURNED,
         PROPERTY_SAMPLE_RETURNED, PROPERTY_SAMPLE_RETURNED, new BigDecimal(sampleReturned)));
      return doc;
   }
}
