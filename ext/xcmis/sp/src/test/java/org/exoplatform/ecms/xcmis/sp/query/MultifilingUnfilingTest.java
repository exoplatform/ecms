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

import org.exoplatform.ecms.xcmis.sp.StorageImpl;
import org.junit.Ignore;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.utils.MimeType;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey
 *         Kabashnyuk</a>
 * @version $Id$
 * 
 */
@Ignore
public class MultifilingUnfilingTest extends BaseQueryTest
{
   private FolderData testRoot;

   private StorageImpl storageA;

   private TypeDefinition folderTypeDefinition;

   private TypeDefinition nasaDocumentTypeDefinition;

   /**
    * @see org.xcmis.sp.query.BaseQueryTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      storageA = (StorageImpl)registry.getConnection("driveA").getStorage();
      folderTypeDefinition = storageA.getTypeDefinition("cmis:folder", true);
      nasaDocumentTypeDefinition = storageA.getTypeDefinition(NASA_DOCUMENT, true);
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      testRoot =
         createFolder(storageA, rootFolder, "QueryUsecasesTest", storageA.getTypeDefinition("cmis:folder", true));

   }

   public void testAddMultipleParents() throws Exception
   {

      FolderData folder1 = createFolder(storageA, testRoot, "multifilingFolderTest1", folderTypeDefinition);
      FolderData folder2 = createFolder(storageA, testRoot, "multifilingFolderTest2", folderTypeDefinition);
      FolderData folder3 = createFolder(storageA, testRoot, "multifilingFolderTest3", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder1, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType(
            "plain", "text"));

      //check what document can be found only in one folder
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')",
         new ObjectData[]{doc1});
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder2.getObjectId() + "')",
         new ObjectData[]{});
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder3.getObjectId() + "')",
         new ObjectData[]{});

      folder2.addObject(doc1);
      folder3.addObject(doc1);

      assertEquals(3, doc1.getParents().size());

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')",
         new ObjectData[]{doc1});

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder2.getObjectId() + "')",
         new ObjectData[]{doc1});

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder3.getObjectId() + "')",
         new ObjectData[]{doc1});

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER('" + folder1.getObjectId() + "')",
          new ObjectData[]{doc1});

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER('" + folder2.getObjectId() + "')",
          new ObjectData[]{doc1});

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER('" + folder3.getObjectId() + "')",
          new ObjectData[]{doc1});

      storageA.deleteTree(testRoot, true, null, true);
   }

   public void testRemoveFromMultipleParentsInDouble() throws Exception
   {
      FolderData folder1 = createFolder(storageA, testRoot, "multifilingFolderTest2", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, testRoot, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType(
            "plain", "text"));
      assertEquals(1, doc1.getParents().size());
      
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER('" + folder1.getObjectId() + "')",
            new ObjectData[]{});
      
      folder1.addObject(doc1);
      assertEquals(2, doc1.getParents().size());
      
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER('" + folder1.getObjectId() + "')",
            new ObjectData[]{doc1});

      folder1.removeObject(doc1);
      assertEquals(1, doc1.getParents().size());

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER('" + folder1.getObjectId() + "')",
         new ObjectData[]{});
   }
   
   
   public void testRemoveFromMultipleParentsInTriple() throws Exception
   {

      FolderData folder1 = createFolder(storageA, testRoot, "multifilingFolderTest1", folderTypeDefinition);
      FolderData folder2 = createFolder(storageA, testRoot, "multifilingFolderTest2", folderTypeDefinition);
      FolderData folder3 = createFolder(storageA, testRoot, "multifilingFolderTest3", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder1, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType(
            "plain", "text"));
      folder2.addObject(doc1);
      folder3.addObject(doc1);
      assertEquals(3, doc1.getParents().size());

      folder2.removeObject(doc1);
      assertEquals(2, doc1.getParents().size());

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder2.getObjectId() + "')",
         new ObjectData[]{});

      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder3.getObjectId() + "')",
         new ObjectData[]{doc1});
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')",
         new ObjectData[]{doc1});
      checkResult(storageA, "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + testRoot.getObjectId() + "')",
         new ObjectData[]{doc1});

      storageA.deleteTree(testRoot, true, null, true);
   }
}
