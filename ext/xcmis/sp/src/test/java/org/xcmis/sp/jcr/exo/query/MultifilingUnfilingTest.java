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
package org.xcmis.sp.jcr.exo.query;

import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.utils.MimeType;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey
 *         Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class MultifilingUnfilingTest extends BaseQueryTest
{
   private FolderData testRoot;

   /**
    * @see org.xcmis.sp.jcr.exo.query.BaseQueryTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      testRoot = createFolder(rootFolder, "QueryUsecasesTest", folderTypeDefinition);
      // create data

   }

   public void testAddMultipleParents() throws Exception
   {

      FolderData folder1 = createFolder(testRoot, "multifilingFolderTest1", folderTypeDefinition);
      FolderData folder2 = createFolder(testRoot, "multifilingFolderTest2", folderTypeDefinition);
      FolderData folder3 = createFolder(testRoot, "multifilingFolderTest3", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder1, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType("plain", "text"));

      //check what document can be found only in one folder
      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')",
         new ObjectData[]{doc1});
      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder2.getObjectId() + "')",
         new ObjectData[]{});

      folder2.addObject(doc1);
      folder3.addObject(doc1);

      assertEquals(3, doc1.getParents().size());

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder2.getObjectId() + "')",
         new ObjectData[]{doc1});

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder3.getObjectId() + "')",
         new ObjectData[]{doc1});
      storage.deleteTree(testRoot, true, null, true);
   }

   public void testRemoveFromMultipleParents() throws Exception
   {

      FolderData folder1 = createFolder(testRoot, "multifilingFolderTest1", folderTypeDefinition);
      FolderData folder2 = createFolder(testRoot, "multifilingFolderTest2", folderTypeDefinition);
      FolderData folder3 = createFolder(testRoot, "multifilingFolderTest3", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder1, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType("plain", "text"));
      folder2.addObject(doc1);
      folder3.addObject(doc1);
      assertEquals(3, doc1.getParents().size());

      folder2.removeObject(doc1);
      assertEquals(2, doc1.getParents().size());

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder2.getObjectId() + "')",
         new ObjectData[]{});

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder3.getObjectId() + "')",
         new ObjectData[]{doc1});
      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')",
         new ObjectData[]{doc1});
      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + testRoot.getObjectId() + "')",
         new ObjectData[]{doc1});

      storage.deleteTree(testRoot, true, null, true);
   }

   public void testRemoveFromLastParent() throws Exception
   {

      FolderData folder1 = createFolder(testRoot, "multifilingFolderTest1", folderTypeDefinition);
      FolderData folder2 = createFolder(testRoot, "multifilingFolderTest2", folderTypeDefinition);
      FolderData folder3 = createFolder(testRoot, "multifilingFolderTest3", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder1, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType("plain", "text"));
      folder2.addObject(doc1);
      folder3.addObject(doc1);
      assertEquals(3, doc1.getParents().size());

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + testRoot.getObjectId() + "')",
         new ObjectData[]{doc1});

      folder2.removeObject(doc1);
      folder3.removeObject(doc1);
      folder1.removeObject(doc1);

      assertEquals(0, doc1.getParents().size());

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + testRoot.getObjectId() + "')",
         new ObjectData[]{});

      storage.deleteObject(doc1, true);
      storage.deleteTree(testRoot, true, null, true);
   }

   public void testSearchUnfiled() throws Exception
   {
      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"helloworld\")", new ObjectData[]{});
      DocumentData doc1 =
         createDocument(testRoot, "node1", nasaDocumentTypeDefinition, "helloworld".getBytes(), new MimeType("text", "plain"));

      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"helloworld\")", new ObjectData[]{doc1});

      testRoot.removeObject(doc1);
      //check if document have no parents
      assertEquals(0, doc1.getParents().size());
      //check if we can find document
      checkResult("SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"helloworld\")", new ObjectData[]{doc1});

      storage.deleteObject(doc1, true);
      storage.deleteTree(testRoot, true, null, true);
   }

}
