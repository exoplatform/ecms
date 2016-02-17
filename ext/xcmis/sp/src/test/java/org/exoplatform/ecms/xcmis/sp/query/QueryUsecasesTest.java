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

import org.exoplatform.ecms.xcmis.sp.DocumentVersion;
import org.exoplatform.ecms.xcmis.sp.StorageImpl;
import org.junit.Ignore;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.model.CapabilityJoin;
import org.xcmis.spi.model.CapabilityQuery;
import org.xcmis.spi.model.RepositoryCapabilities;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.UnfileObject;
import org.xcmis.spi.model.impl.BooleanProperty;
import org.xcmis.spi.model.impl.DecimalProperty;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.utils.MimeType;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
@Ignore
public class QueryUsecasesTest extends BaseQueryTest
{

   private FolderData testRoot;

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
   
   /**
    * @see org.xcmis.sp.BaseTest#tearDown()
    */
   protected void tearDown() throws Exception
   {
      storageA.deleteTree(testRoot, true, UnfileObject.DELETE, true);
      super.tearDown();
   }

   public void testQueryWithChangedContentAndFullSearch() throws Exception
   {
      // Search for no documents
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      Query query = new Query(sql.toString(), true);
      ItemsIterator<Result> result = storageA.query(query);
      assertEquals(0, result.size());

      
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      
      ContentStream cs1 =
         new BaseContentStream("hello".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "testQueryOnLatest", "cmis:document", cs1, null);
      
      assertTrue(document.getObjectId().contains("_"));
      
      // multifiling document
      FolderData folder1 = createFolder(rootFolder, "deleteMultifiledTest01", "cmis:folder");
      FolderData folder2 = createFolder(rootFolder, "deleteMultifiledTest02", "cmis:folder");
      folder1.addObject(document);
      folder2.addObject(document);
      
      
      ContentStream cs11 =
         new BaseContentStream("changed".getBytes(), null, new MimeType("text", "plain"));
      document.setContentStream(cs11);
      
    // search for FIRST document with full text search
    sql = new StringBuffer();
    sql.append("SELECT * ");
    sql.append("FROM ");
    sql.append("cmis:document ");
    sql.append("WHERE ");
    sql.append("CONTAINS(\"changed\")");
    query = new Query(sql.toString(), true);
    result = storageA.query(query);
    assertEquals(1, result.size());
    // check results
    checkResult(storageA, result, new DocumentData[]{document});
      
      
    ItemsIterator<ObjectData> children = ((FolderData)storageA.getObjectById(folder1.getObjectId())).getChildren(null);
    int i = 0;
    while (children.hasNext()) {
      children.next();
      i++;
    }
    assertEquals(1, i);
    
    children = ((FolderData)storageA.getObjectById(folder1.getObjectId())).getChildren(null);
    ObjectData childDocument = children.next();
    ContentStream contentStream = childDocument.getContentStream(null);
    String content = convertStreamToString(contentStream.getStream());
    
    assertEquals("changed" , content);
    
    children = ((FolderData)storageA.getObjectById(folder2.getObjectId())).getChildren(null);
    i = 0;
    while (children.hasNext()) {
      children.next();
      i++;
    }
    assertEquals(1, i);
    
    children = ((FolderData)storageA.getObjectById(folder2.getObjectId())).getChildren(null);
    childDocument = children.next();
    contentStream = childDocument.getContentStream(null);
    content = convertStreamToString(contentStream.getStream());
    assertEquals("changed" , content);
    
    
     sql = new StringBuffer();
     sql.append("SELECT * ");
     sql.append("FROM ");
     sql.append("cmis:document ");
     sql.append("WHERE ");
     sql.append("CONTAINS(\"hello\")");
     query = new Query(sql.toString(), true);
     result = storageA.query(query);
     assertEquals(0, result.size());
   
     
     sql = new StringBuffer();
     sql.append("SELECT * ");
     sql.append("FROM ");
     sql.append("cmis:document ");
     sql.append("WHERE ");
     sql.append("IN_FOLDER( '" + folder1.getObjectId() + "')");
     query = new Query(sql.toString(), true);
     result = storageA.query(query);
     assertEquals(1, result.size());
     
     sql = new StringBuffer();
     sql.append("SELECT * ");
     sql.append("FROM ");
     sql.append("cmis:document ");
     sql.append("WHERE ");
     sql.append("IN_FOLDER( '" + folder2.getObjectId() + "')");
     query = new Query(sql.toString(), true);
     result = storageA.query(query);
     assertEquals(1, result.size());
     
     sql = new StringBuffer();
     sql.append("SELECT * ");
     sql.append("FROM ");
     sql.append("cmis:document ");
     sql.append("WHERE ");
     sql.append("IN_FOLDER( '" + rootFolder.getObjectId() + "')");
     query = new Query(sql.toString(), true);
     result = storageA.query(query);
     assertEquals(1, result.size());
      
  
      DocumentData pwc = document.checkout();
      ContentStream cs2 =
         new BaseContentStream("bye".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkin = pwc.checkin(true, "my comment", null, cs2, null, null);
      
      assertTrue(checkin.getObjectId().contains("_"));
      
      // search for FIRST document with full text search
      sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document ");
      sql.append("WHERE ");
      sql.append("CONTAINS(\"changed\")");
      query = new Query(sql.toString(), true);
      result = storageA.query(query);
      assertEquals(1, result.size());
      // check results
      checkResult(storageA, result, new DocumentData[]{document});

      
      // search for CHECKIN document with full text search
      sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document ");
      sql.append("WHERE ");
      sql.append("CONTAINS(\"bye\")");
      query = new Query(sql.toString(), true);
      result = storageA.query(query);
      assertEquals(1, result.size());
      // check results
      checkResult(storageA, result, new DocumentData[]{checkin});
      
      
      // search for 
      sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document ");
      sql.append("WHERE ");
      sql.append("cmis:objectId = '" + checkin.getObjectId() + "'");
      query = new Query(sql.toString(), true);
      result = storageA.query(query);
      assertEquals(1, result.size());
      // check results
      checkResult(storageA, result, new DocumentData[]{checkin});
   }

   public void testDoesTheIndexEmpty() throws Exception
   {
      // Search for no documents
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      Query query = new Query(sql.toString(), true);
      ItemsIterator<Result> result = storageA.query(query);
      // if the index not empty use: result.next().getObjectId() to see the id
      assertEquals(0, result.size());
   }
   
   public void testQueryOnCreatedDocument() throws Exception
   {
      // Search for no documents
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      Query query = new Query(sql.toString(), true);
      ItemsIterator<Result> result = storageA.query(query);
      assertEquals(0, result.size());
      
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      DocumentData document = createDocument(rootFolder, "testQueryOnCreatedDocument", "cmis:document", null, null);
      
      // search for documents (version 1 and the latest one)
      sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      query = new Query(sql.toString(), true);
      result = storageA.query(query);
      assertEquals(1, result.size());
      // check results
      checkResult(storageA, result, new DocumentData[]{document});
   }
   
   public void testQueryOnCheckedIn() throws Exception
   {
      // Search for no documents
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      Query query = new Query(sql.toString(), true);
      ItemsIterator<Result> result = storageA.query(query);
      assertEquals(0, result.size());

      // checkout/checkin
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      DocumentData document = createDocument(rootFolder, "testQueryOnCheckedIn", "cmis:document", null, null);
      DocumentData pwc = document.checkout();
      DocumentData checkin = pwc.checkin(true, "", null, null, null, null);
      
      // search for documents (version 1 and the latest one)
      sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      query = new Query(sql.toString(), true);
      result = storageA.query(query);
      assertEquals(2, result.size());
      // check results
      checkResult(storageA, result, new DocumentData[]{document, checkin});
   }
   
   public void testQueryOnLatest() throws Exception
   {
      // Search for no documents
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      Query query = new Query(sql.toString(), true);
      ItemsIterator<Result> result = storageA.query(query);
      assertEquals(0, result.size());

      // checkout/checkin
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      ContentStream cs1 =
         new BaseContentStream("hello".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "testQueryOnLatest", "cmis:document", cs1, null);
      
      DocumentData pwc = document.checkout();
      ContentStream cs2 =
         new BaseContentStream("bye".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkin = pwc.checkin(true, "my comment", null, cs2, null, null);
            
      // search for document with latest
      sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append("cmis:document");
      sql.append(" WHERE ");
      sql.append("cmis:isLatestVersion" + " = true");
      query = new Query(sql.toString(), true);
      result = storageA.query(query);
      assertEquals(1, result.size());
      // check results
      checkResult(storageA, result, new DocumentData[]{checkin});
   }
   
   
   public void testDeleteVersion() throws Exception
   {
      FolderData folder1 = createFolder(testRoot, "multifiledChildFolderTest01", "cmis:folder");
      
      // SEARCH TEST
      String queryString = "SELECT * FROM cmis:document WHERE IN_FOLDER('" + folder1.getObjectId() + "')";
      Query query = new Query(queryString, true);
      ItemsIterator<Result> result = storageA.query(query);
      assertEquals(0, result.size());
      
      DocumentData document =
         createDocument(folder1, "checkinTest", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);

      // SEARCH TEST
      queryString = "SELECT * FROM cmis:document WHERE IN_FOLDER('" + folder1.getObjectId() + "')";
      query = new Query(queryString, true);
      result = storageA.query(query);
      assertEquals(1, result.size());
      
      
      // FIRST VERSION ============================
      
      // CHECKOUT
      DocumentData pwc = document.checkout();
      
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument1 = pwc.checkin(true, "my comment", null, cs, null, null);
      String checkinId1 = checkinDocument1.getObjectId();
      
      // SEARCH TEST
      queryString = "SELECT * FROM cmis:document WHERE IN_FOLDER('" + folder1.getObjectId() + "')";
      query = new Query(queryString, true);
      result = storageA.query(query);
      assertEquals(2, result.size());
      
      
      // SECOND VERSION ============================
      
      // CHECKOUT
      pwc = checkinDocument1.checkout();
      
      // CHECKIN
      cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument2 = pwc.checkin(true, "my comment", null, cs, null, null);
      String checkinId2 = checkinDocument2.getObjectId();
      
      // ============================

      // SEARCH TEST
      queryString = "SELECT * FROM cmis:document WHERE IN_FOLDER('" + folder1.getObjectId() + "')";
      query = new Query(queryString, true);
      result = storageA.query(query);
      assertEquals(3, result.size());
       
      // DELETE VERSION
      while(result.hasNext()) {
         String id = result.next().getObjectId();
         ObjectData objectData = storageA.getObjectById(id);
         if (objectData instanceof DocumentVersion) {
            DocumentVersion dd = (DocumentVersion)objectData;
            if ("1".equalsIgnoreCase(dd.getVersionLabel())) {
               dd.delete();
            }
         }
      }
      
      // SEARCH TEST
      queryString = "SELECT * FROM cmis:document WHERE IN_FOLDER('" + folder1.getObjectId() + "')";
      query = new Query(queryString, true);
      result = storageA.query(query);
      assertEquals(2, result.size());
      
      // ItemsIterator<Result> objectId's to Set<String>
      Set<String> ss = new HashSet<String>();
      while (result.hasNext()) {
         Result result2 = (Result) result.next();
         ss.add(result2.getObjectId());
      }
       
      assertTrue("Should be the First checkin version in the search" , ss.contains(checkinId1));
      assertTrue("Should be the Second checkin version in the search", ss.contains(checkinId2));
   }

   /**
    * Get query capabilities.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testSearchCapabilities() throws Exception
   {
      RepositoryCapabilities repCapabilities = storageA.getRepositoryInfo().getCapabilities();
      assertEquals(CapabilityQuery.BOTHCOMBINED, repCapabilities.getCapabilityQuery());
      assertEquals(CapabilityJoin.NONE, repCapabilities.getCapabilityJoin());
      assertTrue(repCapabilities.isCapabilityPWCSearchable());
      assertFalse(repCapabilities.isCapabilityAllVersionsSearchable());
   }

   /**
    * Test query with one 'and' two or' constraints.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>Title</b> - Apollo 7 <b>PROPERTY_BOOSTER</b> - Saturn 1B
    * <b>PROPERTY_COMMANDER</b> - Walter M. Schirra</li>
    * <li>document2: <b>Title</b> - Apollo 8 <b>PROPERTY_BOOSTER</b> - Saturn V
    * <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II</li>
    * <li>document3: <b>Title</b> - Apollo 13<b>PROPERTY_BOOSTER</b> - Saturn V
    * <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.</li>
    * </ul>
    * <p>
    * Query : Select all documents where PROPERTY_BOOSTER is 'Saturn V' and
    * PROPERTY_COMMANDER is Frank F. Borman, II or James A. Lovell, Jr.
    * <p>
    * Expected result: document2 and document3
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testAndOrConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append(NASA_DOCUMENT);
      sql.append(" WHERE ");
      sql.append(PROPERTY_BOOSTER + " = " + "'Saturn V'");
      sql.append(" AND ( " + PROPERTY_COMMANDER + " = 'Frank F. Borman, II' ");
      sql.append("       OR " + PROPERTY_COMMANDER + " = 'James A. Lovell, Jr.' )");

      Query query = new Query(sql.toString(), true);

      ItemsIterator<Result> result = storageA.query(query);
      // check results
      checkResult(storageA, result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

   }

   /**
    * Test IN_FOLDER constraint.
    * <p>
    * Initial data:
    * <ul>
    * <li>testDocumentInFolderConstrain1:
    * <li>-see createNasaContent(storageA,)
    * <li>testDocumentInFolderConstrain2:
    * <li>-see createNasaContent(storageA,)
    * </ul>
    * <p>
    * Query : Select all documents that are in folder2.
    * <p>
    * Expected result: all documents that are in folder2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testDocumentInFolderConstrain() throws Exception
   {
      // create data
      FolderData testRoot1 = createFolder(storageA, testRoot, "testDocumentInFolderConstrain1", folderTypeDefinition);
      FolderData testRoot2 = createFolder(storageA, testRoot, "testDocumentInFolderConstrain2", folderTypeDefinition);

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot1);
      List<DocumentData> appolloContent2 = createNasaContent(storageA, testRoot2);

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER( '" + testRoot2.getObjectId() + "')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      // check results
      checkResult(storageA, result, appolloContent2.toArray(new DocumentData[appolloContent2.size()]));

   }

   /**
    * Test IN_FOLDER constraint.
    * <p>
    * Initial data:
    * <p>
    * folder1:
    * <p>
    * -doc1: <b>Title</b> - node1
    * <p>
    * -folder3:
    * <p>
    * --folder4 </ul>
    * <p>
    * folder2:
    * <p>
    * -doc2: <b>Title</b> - node2
    * <p>
    * Query : Select all folders that are in folder1.
    * <p>
    * Expected result: folder3
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testFolderInFolderConstrain() throws Exception
   {
      // create data
      FolderData folder1 = createFolder(storageA, testRoot, "folder1", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder1, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType(
            "text", "plain"));

      FolderData folder2 = createFolder(storageA, testRoot, "folder2", folderTypeDefinition);

      DocumentData doc2 =
         createDocument(storageA, folder2, "node2", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType(
            "text", "plain"));

      FolderData folder3 = createFolder(storageA, folder1, "folder3", folderTypeDefinition);

      DocumentData doc3 =
         createDocument(storageA, folder3, "node3", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType(
            "text", "plain"));

      FolderData folder4 = createFolder(storageA, folder3, "folder4", folderTypeDefinition);

      String statement = "SELECT * FROM cmis:folder  WHERE IN_FOLDER( '" + folder1.getObjectId() + "')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      // check results
      checkResult(storageA, result, new FolderData[]{folder3});

   }

   //
   //   /**
   //    * Constraints with multi-valued properties is not supported.
   //    *
   //    * @throws Exception
   //    */
   //   public void _testAnyInConstraint() throws Exception
   //   {
   //      // create data
   //      String name = "fileCS2.doc";
   //      String name2 = "fileCS3.doc";
   //      String contentType = "text/plain";
   //
   //      Document folder = createFolder(storageA,root, "CASETest");
   //
   //      Document doc1 = createDocument(storageA,folder.getObjectId(), name, new byte[0], contentType);
   //      doc1.setDecimals("multivalueLong", new BigDecimal[]{new BigDecimal(3), new BigDecimal(5), new BigDecimal(10)});
   //      doc1.setStrings("multivalueString", new String[]{"bla-bla"});
   //      doc1.save();
   //
   //      Document doc2 = createDocument(storageA,folder.getObjectId(), name2, new byte[0], contentType);
   //      doc2.setDecimals("multivalueLong", new BigDecimal[]{new BigDecimal(15), new BigDecimal(10)});
   //      doc2.setStrings("multivalueString", new String[]{"bla-bla"});
   //      doc2.save();
   //
   //      String statement =
   //         "SELECT * FROM " + NASA_DOCUMENT + " WHERE ANY multivalueLong IN ( 3 , 5, 6 ) ";
   //
   //      Query query = new Query(statement, true);
   //
   //      ItemsIterator<Result> result = storageA.query(query);
   //
   //      // check results
   //      checkResult(storageA,result, new Document[]{doc1});
   //   }
   //
   /**
    * Test fulltext constraint.
    * <p>
    * Initial data:
    * <p>
    * see createNasaContent(storageA,)
    * <p>
    * Query : Select all documents where data contains "moon" word.
    * <p>
    * Expected result: document2 and document3
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testFulltextConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement1 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"moon\")";
      Query query = new Query(statement1, true);
      ItemsIterator<Result> result = storageA.query(query);

      assertEquals(2, result.size());
      checkResult(storageA, result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

      String statement2 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"Moon\")";
      query = new Query(statement2, true);
      ItemsIterator<Result> result2 = storageA.query(query);

      assertEquals(2, result2.size());
      checkResult(storageA, result2, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

   }

   /**
    * Test 'IN' constraint.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>PROPERTY_COMMANDER</b> - Walter M. Schirra
    * <li>document2: <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II
    * <li>document3: <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.
    * <li>document4: <b>PROPERTY_COMMANDER</b> - Eugene A. Cernan
    * </ul>
    * <p>
    * Query : Select all documents where name is in set {'Virgil I. Grissom',
    * 'Frank F. Borman, II', 'Charles Conrad, Jr.'}.
    * <p>
    * Expected result: document2
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testINConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER
            + " IN ('Virgil I. Grissom', 'Frank F. Borman, II', 'Charles Conrad, Jr.')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      assertEquals(1, result.size());
      checkResult(storageA, result, new DocumentData[]{appolloContent.get(1)});

   }

   //
   //   /**
   //    * Test JOIN with condition constraint.
   //    * <p>
   //    * Initial data:
   //    * <ul>
   //    * <li>folder1: <b>folderName</b> - folderOne
   //    * <li>doc1: <b>Title</b> - node1 <b>parentFolderName</b> - folderOne
   //    * <li>folder2: b>folderName</b> - folderTwo
   //    * <li>doc2: <b>Title</b> - node2 <b>parentFolderName</b> - folderThree
   //    * </ul>
   //    * <p>
   //    * Query : Select all documents and folders where folders folderName equal to
   //    * document parentFolderName.
   //    * <p>
   //    * Expected result: doc1 and folder1
   //    *
   //    * @throws Exception if an unexpected error occurs
   //    */
   //   public void _testJoinWithCondition() throws Exception
   //   {
   //      // create data
   //      Document folder1 = this.createFolder(storageA,root, "folder1");
   //      folder1.setString("folderName", "folderOne");
   //
   //      Document doc1 = createDocument(storageA,folder1.getObjectId(), "node1", "hello world".getBytes(), "text/plain");
   //      doc1.setString("parentFolderName", "folderOne");
   //
   //      Document folder2 = this.createFolder(storageA,root, "folder2");
   //      folder2.setString("folderName", "folderTwo");
   //
   //      Document doc2 = createDocument(storageA,folder2.getObjectId(), "node1", "hello world".getBytes(), "text/plain");
   //      doc2.setString("parentFolderName", "folderThree");
   //
   //      String statement =
   //         "SELECT doc.* FROM " + NASA_DOCUMENT + " AS doc LEFT JOIN " + JcrCMIS.NT_FOLDER
   //            + " AS folder ON (doc.parentFolderName = folder.folderName)";
   //
   //      Query query = new Query(statement, true);
   //      ItemsIterator<Result> result = storageA.query(query);
   //
   //      assertEquals(1, result.size());
   //      // check results - must doc1 and folder1
   //   }
   //
   /**
    * Test LIKE constraint.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>PROPERTY_COMMANDER</b> - Walter M. Schirra
    * <li>document2: <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II
    * <li>document3: <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.
    * <li>document4: <b>PROPERTY_COMMANDER</b> - Eugene A. Cernan
    * </ul>
    * <p>
    * Query : Select all documents where prop begins with "James".
    * <p>
    * Expected result: doc3
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testLIKEConstraint() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " AS doc WHERE " + PROPERTY_COMMANDER + " LIKE 'James%'";

      Query query = new Query(statement, true);

      ItemsIterator<Result> result = storageA.query(query);

      // check results
      assertEquals(1, result.size());
      checkResult(storageA, result, new DocumentData[]{appolloContent.get(2)});

   }

   /**
    * Test LIKE constraint with escape symbols.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - node1 <b>prop</b> - ad%min master
    * <li>doc2: <b>Title</b> - node2 <b>prop</b> - admin operator
    * <li>doc3: <b>Title</b> - node2 <b>prop</b> - radmin
    * </ul>
    * <p>
    * Query : Select all documents where prop like 'ad\\%min%'.
    * <p>
    * Expected result: doc1
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testLIKEConstraintEscapeSymbols() throws Exception
   {

      DocumentData doc1 =
         createDocument(storageA, testRoot, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "ad%min master"));

      DocumentData doc2 =
         createDocument(storageA, testRoot, "node2", nasaDocumentTypeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "admin operator"));

      DocumentData doc3 =
         createDocument(storageA, testRoot, "node3", nasaDocumentTypeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));
      setProperty(doc3, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "radmin"));

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " AS doc WHERE  " + PROPERTY_COMMANDER + " LIKE 'ad\\%min%'";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      // check results
      assertEquals(1, result.size());
      checkResult(storageA, result, new DocumentData[]{doc1});

   }

   /**
    * Test NOT constraint.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - node1 <b>content</b> - hello world
    * <li>doc2: <b>Title</b> - node2 <b>content</b> - hello
    * </ul>
    * <p>
    * Query : Select all documents that not contains "world" word.
    * <p>
    * Expected result: doc2
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testNOTConstraint() throws Exception
   {

      Map<String, String> parameters = new HashMap<String, String>();
      parameters.put(CmisConstants.CHARSET, "ISO-8859-1");
      
      DocumentData doc1 =
         createDocument(storageA, testRoot, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain", parameters));

      FolderData folder2 = createFolder(storageA, testRoot, "folder2", folderTypeDefinition);
      DocumentData doc2 =
         createDocument(storageA, folder2, "node2", nasaDocumentTypeDefinition, "hello".getBytes(), new MimeType(
            "text", "plain"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE NOT CONTAINS(\"world\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc2});

   }

   /**
    * Test NOT IN constraint.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>PROPERTY_COMMANDER</b> - Walter M. Schirra
    * <li>document2: <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II
    * <li>document3: <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.
    * <li>document4: <b>PROPERTY_COMMANDER</b> - Eugene A. Cernan
    * </ul>
    * <p>
    * Query : Select all documents where PROPERTY_COMMANDER property not in set
    * {'Walter M. Schirra', 'James A. Lovell, Jr.'}.
    * <p>
    * Expected result: document2, document4
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testNotINConstraint() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER
            + " NOT IN ('Walter M. Schirra', 'James A. Lovell, Jr.')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{appolloContent.get(1), appolloContent.get(3)});

   }

   /**
    * Test NOT NOT (not counteraction).
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>PROPERTY_COMMANDER</b> - Walter M. Schirra
    * <li>document2: <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II
    * <li>document3: <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.
    * <li>document4: <b>PROPERTY_COMMANDER</b> - Eugene A. Cernan
    * </ul>
    * <p>
    * Query : Select all documents where PROPERTY_COMMANDER property NOT NOT IN
    * set {'James A. Lovell, Jr.'}.
    * <p>
    * Expected result: document3.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testNotNotINConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE  NOT (" + PROPERTY_COMMANDER + " NOT IN ('James A. Lovell, Jr.'))";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{appolloContent.get(2)});

   }

   /**
    * Test Order By desc.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>PROPERTY_COMMANDER</b> - Walter M. Schirra
    * <li>document2: <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II
    * <li>document3: <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.
    * <li>document4: <b>PROPERTY_COMMANDER</b> - Eugene A. Cernan
    * </ul>
    * <p>
    * Query : Order by PROPERTY_COMMANDER property value (descending).
    * <p>
    * Expected result: document1, document3, document2, document4.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testOrderByFieldDesc() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      StringBuffer sql = new StringBuffer();
      sql.append("SELECT  ");
      sql.append(CmisConstants.LAST_MODIFIED_BY + " as last , ");
      sql.append(CmisConstants.OBJECT_ID + " , ");
      sql.append(CmisConstants.LAST_MODIFICATION_DATE);
      sql.append(" FROM ");
      sql.append(NASA_DOCUMENT);
      sql.append(" ORDER BY ");
      sql.append(PROPERTY_COMMANDER);
      sql.append(" DESC");

      String statement = sql.toString();

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      // Walter M. Schirra (0)
      // James A. Lovell, Jr. (2)
      // Frank F. Borman, II (1)
      // Eugene A. Cernan  (3)
      checkResultOrder(result, new DocumentData[]{appolloContent.get(0), appolloContent.get(2), appolloContent.get(1),
         appolloContent.get(3)});

   }

   /**
    * Test ORDER BY ASC.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>PROPERTY_COMMANDER</b> - Walter M. Schirra
    * <li>document2: <b>PROPERTY_COMMANDER</b> - Frank F. Borman, II
    * <li>document3: <b>PROPERTY_COMMANDER</b> - James A. Lovell, Jr.
    * <li>document4: <b>PROPERTY_COMMANDER</b> - Eugene A. Cernan
    * </ul>
    * <p>
    * Query : Order by PROPERTY_COMMANDER property value (ascending).
    * <p>
    * Expected result: document4, document2, document3, document1.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testOrderByFieldAsk() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      StringBuffer sql = new StringBuffer();
      sql.append("SELECT ");
      sql.append(CmisConstants.LAST_MODIFIED_BY + ", ");
      sql.append(CmisConstants.OBJECT_ID + ", ");
      sql.append(CmisConstants.LAST_MODIFICATION_DATE);
      sql.append(" FROM ");
      sql.append(NASA_DOCUMENT);
      sql.append(" ORDER BY ");
      sql.append(PROPERTY_COMMANDER);

      String statement = sql.toString();

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      // Eugene A. Cernan  (3)
      // Frank F. Borman, II (1)
      // James A. Lovell, Jr. (2)
      // Walter M. Schirra (0)
      checkResultOrder(result, new DocumentData[]{appolloContent.get(3), appolloContent.get(1), appolloContent.get(2),
         appolloContent.get(0)});

   }

   /**
    * Test ORDER BY default.
    * <p>
    * Initial data:
    * <ul>
    * <li>document1: <b>Title</b> - Apollo 7
    * <li>document2: <b>Title</b> - Apollo 8
    * <li>document3: <b>Title</b> - Apollo 13
    * <li>document4: <b>Title</b> - Apollo 17
    * </ul>
    * <p>
    * Query : Select all documents in default order.
    * <p>
    * Expected result: document3, document4, document1, document2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testOrderByDefault() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      StringBuffer sql = new StringBuffer();
      sql.append("SELECT ");
      sql.append(CmisConstants.LAST_MODIFIED_BY + ", ");
      sql.append(CmisConstants.OBJECT_ID + ", ");
      sql.append(CmisConstants.LAST_MODIFICATION_DATE);
      sql.append(" FROM ");
      sql.append(NASA_DOCUMENT);

      String statement = sql.toString();

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      // Apollo 13 (2)
      // Apollo 17 (3)
      // Apollo 7 (0)
      // Apollo 8 (1)
      checkResultOrder(result, new DocumentData[]{appolloContent.get(2), appolloContent.get(3), appolloContent.get(0),
         appolloContent.get(1)});

   }

   /**
    * Test ORDER BY SCORE().
    * <p>
    * Initial data:
    * <p>
    * see createNasaContent(storageA,)
    * <p>
    * Query : Select all documents which contains word "moon" in ORDER BY SCORE.
    * <p>
    * Expected result: doc2, doc3.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testOrderByScore() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      StringBuffer sql = new StringBuffer();
      sql.append("SELECT ");
      sql.append(" SCORE() AS scoreCol, ");
      sql.append(CmisConstants.LAST_MODIFIED_BY + ", ");
      sql.append(CmisConstants.OBJECT_ID + ", ");
      sql.append(CmisConstants.LAST_MODIFICATION_DATE);
      sql.append(" FROM ");
      sql.append(NASA_DOCUMENT);
      sql.append(" WHERE CONTAINS(\"moon\") ");
      sql.append(" ORDER BY SCORE() ");

      String statement = sql.toString();

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      // Apollo 8 (1)
      // Apollo 13 (2)
      checkResultOrder(result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

   }

   //
   /**
    * Test property existence constraint (IS [NOT] NULL) .
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - node1 <b>StringProperty</b> - James A. Lovell,
    * Jr.
    * <li>doc2: <b>Title</b> - node2
    * </ul>
    * <p>
    * Query : Select all documents that has "prop" property (IS NOT NULL).
    * <p>
    * Expected result: doc1
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testPropertyExistence() throws Exception
   {
      // Document folder1 = createFolder(storageA,root, "CASETest");

      DocumentData doc1 =
         createDocument(storageA, testRoot, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "James A. Lovell, Jr."));
      DocumentData doc2 =
         createDocument(storageA, testRoot, "node2", nasaDocumentTypeDefinition, "hello".getBytes(), new MimeType(
            "text", "plain"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER + " IS NOT NULL";
      Query query = new Query(statement, true);

      ItemsIterator<Result> result = storageA.query(query);
      checkResult(storageA, result, new DocumentData[]{doc1});

   }

   /**
    * Test SCORE as column.
    * <p>
    * Initial data:
    * <p>
    * see createNasaContent(storageA,)
    * <p>
    * Query : Select all documents that contains "first" word and ordered by
    * score.
    * <p>
    * Expected result: doc4, doc1 and doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testScoreAsColumn() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement =
         "SELECT SCORE() AS scoreCol , " + CmisConstants.NAME + " AS id FROM " + NASA_DOCUMENT
            + " WHERE CONTAINS(\"first\") ORDER BY SCORE()";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      // check result
      while (result.hasNext())
      {
         Result next = result.next();
         assertTrue(next.getScore().getScoreValue().doubleValue() > 0);
      }
   }

   /**
    * Test IN_TREE constraint.
    * <p>
    * Initial data:
    * <p>
    * folder1
    * <p>
    * - document doc1
    * <p>
    * - folder2
    * <p>
    * -- document doc2
    * <p>
    * Query : Select all documents that are in tree of folder1.
    * <p>
    * Expected result: doc1,doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testTreeConstrain() throws Exception
   {
      // create data
      FolderData folder1 = createFolder(storageA, testRoot, "folder1", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder1, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType(
            "text", "plain"));

      FolderData subfolder1 = createFolder(storageA, folder1, "folder2", folderTypeDefinition);

      DocumentData doc2 =
         createDocument(storageA, subfolder1, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc1, doc2});
   }

   /**
    * Test not equal comparison (<>).
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS2.doc <b>DecimalProperty</b> - 3
    * <li>doc2: <b>Title</b> - fileCS3.doc <b>DecimalProperty</b> - 15
    * </ul>
    * <p>
    * Query : Select all documents property DecimalProperty not equals to 3.
    * <p>
    * Expected result: doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testNotEqualDecimal() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      FolderData folder = createFolder(storageA, testRoot, "NotEqualDecimal", folderTypeDefinition);
      DocumentData doc1 =
         createDocument(storageA, folder, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(3)));

      DocumentData doc2 =
         createDocument(storageA, folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(15)));
      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_BOOSTER_MASS + " <> 3";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc2});
   }

   /**
    * Test more than comparison (>).
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS2.doc <b>DecimalProperty</b> - 3
    * <li>doc2: <b>Title</b> - fileCS3.doc <b>DecimalProperty</b> - 15
    * </ul>
    * <p>
    * Query : Select all documents property DecimalProperty more than 5.
    * <p>
    * Expected result: doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testMoreThanDecimal() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      FolderData folder = createFolder(storageA, testRoot, "CASETest", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(3)));

      DocumentData doc2 =
         createDocument(storageA, folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(15)));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_BOOSTER_MASS + " > 5";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc2});
   }

   /**
    * Test not equal comparison (<>) string.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS2.doc <b>StringProperty</b> - test word
    * first
    * <li>doc2: <b>Title</b> - fileCS3.doc <b>StringProperty</b> - test word
    * second
    * </ul>
    * <p>
    * Query : Select all documents property StringPropertye not equal to
    * "test word second".
    * <p>
    * Expected result: doc1.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testNotEqualString() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      FolderData folder = createFolder(storageA, testRoot, "CASETest", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "test word first"));

      DocumentData doc2 =
         createDocument(storageA, folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "test word second"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER + " <> 'test word second'";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc1});
   }

   /**
    * Test fulltext search from jcr:content.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileFirst <b>StringProperty</b> -
    * "There must be test word"
    * <li>doc2: <b>Title</b> - fileSecond <b>StringProperty</b> -
    * "Test word is not here"
    * </ul>
    * <p>
    * Query : Select all documents that contains "here" word.
    * <p>
    * Expected result: doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testSimpleFulltext() throws Exception
   {
      // create data
      String name1 = "fileFirst";
      String name2 = "fileSecond";

      FolderData folder = createFolder(storageA, testRoot, "SimpleFullTextTest", folderTypeDefinition);
      DocumentData doc1 =
         createDocument(storageA, folder, name1, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be test word"));

      DocumentData doc2 =
         createDocument(storageA, folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "Test word is not here"));
      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"here\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc2});
   }
   
   /**
    * Test search on versioned document content.
    * 
    * Query : Select all documents that contains "checkin" word.
    * <p>
    * Expected result: checkinDocument.
    * 
    * Query : Select all documents that contains "hello" word.
    * <p>
    * Expected result: doc1.
    * 
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testSimpleFulltextOnCheckedIn() throws Exception
   {
      FolderData folder = createFolder(storageA, testRoot, "testSimpleFulltextOnCheckedIn_Folder", folderTypeDefinition);

      // will be search on this doc 
      ContentStream cs1 =
         new BaseContentStream("hello".getBytes(), null, new MimeType("text", "plain"));
      DocumentData doc1 = createDocument(folder, "testSimpleFulltextOnCheckedIn_Document", "cmis:document", cs1, null);
      
      // fake doc1
      ContentStream cs2 =
         new BaseContentStream("sorry".getBytes(), null, new MimeType("text", "plain"));
      DocumentData doc2 = createDocument(folder, "testSimpleFulltextOnCheckedIn_Document2", "cmis:document", cs2, null);
      
      // CHECKOUT
      DocumentData pwc = doc1.checkout();
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      
      // test search on latest version
      String statement = "SELECT * FROM cmis:document WHERE CONTAINS(\"checkin\")";
      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      checkResult(storageA, result, new DocumentData[]{checkinDocument});
      
      // test seardh on the first version
      statement = "SELECT * FROM cmis:document WHERE CONTAINS(\"hello\")";
      query = new Query(statement, true);
      result = storageA.query(query);
      checkResult(storageA, result, new DocumentData[]{doc1});
   }
   
   /**
    * Same as testSimpleFulltext.
    * @throws Exception
    */
  public void testSearchOnDate() throws Exception {
    String name1 = "fileFirst";

    FolderData folder = createFolder(storageA, testRoot, "SimpleFullTextTest", folderTypeDefinition);
    DocumentData doc1 = createDocument(storageA,
                                       folder,
                                       name1,
                                       nasaDocumentTypeDefinition,
                                       new byte[0],
                                       new MimeType("text", "plain"));
    setProperty(doc1, new StringProperty(PROPERTY_COMMANDER,
                                         PROPERTY_COMMANDER,
                                         PROPERTY_COMMANDER,
                                         PROPERTY_COMMANDER,
                                         "There must be test word"));

    Calendar c = doc1.getCreationDate();
    DateFormat ISO_8601_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");

    c.add(Calendar.MILLISECOND, -1);
    String beforeDate = ISO_8601_DATE_TIME.format(c.getTime());
    beforeDate = beforeDate.substring(0, 26) + ":" + beforeDate.substring(26);

    c.add(Calendar.MILLISECOND, 2);
    String afterDate = ISO_8601_DATE_TIME.format(c.getTime());
    afterDate = afterDate.substring(0, 26) + ":" + afterDate.substring(26);

    String statement = "SELECT * FROM cmis:document WHERE cmis:creationDate>=TIMESTAMP '"
        + beforeDate + "' AND cmis:creationDate<=TIMESTAMP '" + afterDate + "'";
    Query query = new Query(statement, true);
    ItemsIterator<Result> result = storageA.query(query);

    assertEquals("Search resuls with the query \n '" + statement + "'.", 1, result.size());

    checkResult(storageA, result, new DocumentData[] { doc1 });
  }

   /**
    * Test complex fulltext query.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS1.doc <b>StringProperty</b> -
    * "There must be test word"
    * <li>doc2: <b>Title</b> - fileCS2.doc <b>StringProperty</b> -
    * "Test word is not here. Another check-word."
    * <li>doc3: <b>Title</b> - fileCS3.doc <b>StringProperty</b> -
    * "There must be check-word."
    * </ul>
    * <p>
    * Query : Select all documents that contains "There must" phrase and do not
    * contain "check-word" word.
    * <p>
    * Expected result: doc1.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testExtendedFulltext() throws Exception
   {
      // create data
      String name1 = "fileCS1.doc";
      String name2 = "fileCS2.doc";
      String name3 = "fileCS3.doc";

      FolderData folder = createFolder(storageA, testRoot, "CASETest", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(storageA, folder, name1, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be test word"));

      DocumentData doc2 =
         createDocument(storageA, folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "Test word is not here. Another check-word."));

      DocumentData doc3 =
         createDocument(storageA, folder, name3, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc3, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be check-word."));

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"\\\"There must\\\" -\\\"check\\-word\\\"\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc1});
   }

   /**
    * Same as testNOTConstraint.
    */
   public void testNotContains() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      DocumentData doc1 =
         createDocument(storageA, testRoot, name, nasaDocumentTypeDefinition, new byte[0],
            new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be test word"));

      DocumentData doc2 =
         createDocument(storageA, testRoot, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text",
            "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "Test word is not here"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE NOT CONTAINS(\"here\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc1});
   }

   /**
    * Test comparison of boolean property.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS2.doc <b>BooleanProperty</b> - true
    * <li>doc2: <b>Title</b> - fileCS3.doc <b>BooleanProperty</b> - false
    * </ul>
    * <p>
    * Query : Select all documents where BooleanProperty equals to false.
    * <p>
    * Expected result: doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testBooleanConstraint() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      DocumentData doc1 =
         createDocument(storageA, testRoot, name, nasaDocumentTypeDefinition, new byte[0],
            new MimeType("text", "plain"));
      setProperty(doc1, new BooleanProperty(PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, true));

      DocumentData doc2 =
         createDocument(storageA, testRoot, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text",
            "plain"));
      setProperty(doc2, new BooleanProperty(PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, false));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE (" + PROPERTY_STATUS + " = FALSE )";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc2});
   }

   /**
    * Test comparison of date property.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS2.doc <b>dateProp</b> - 2009-08-08
    * <li>doc2: <b>Title</b> - fileCS3.doc <b>dateProp</b> - 2009-08-08
    * </ul>
    * <p>
    * Query : Select all documents where dateProp more than 2007-01-01.
    * <p>
    * Expected result: doc1, doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testDateConstraint() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      DocumentData doc1 =
         createDocument(storageA, testRoot, name, nasaDocumentTypeDefinition, new byte[0],
            new MimeType("text", "plain"));
      DocumentData doc2 =
         createDocument(storageA, testRoot, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text",
            "plain"));

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT
            + " WHERE ( cmis:lastModificationDate >= TIMESTAMP '2007-01-01T00:00:00.000Z' )";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);

      checkResult(storageA, result, new DocumentData[]{doc1, doc2});
   }

   /**
    * Simple test.
    * <p>
    * All documents from Nasa program.
    * <p>
    * Query : Select all NASA_DOCUMENT.
    * <p>
    * Expected result: all documents.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testSimpleQuery() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);
      String statement = "SELECT * FROM " + NASA_DOCUMENT;
      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storageA.query(query);
      checkResult(storageA, result, appolloContent.toArray(new DocumentData[appolloContent.size()]));

   }

   /**
    * Test fulltext constraint.
    * <p>
    * Initial data: see createNasaContent(storageA)
    * <p>
    * Before updating
    * <p>
    * Query : Select all documents where data contains "moon" word.
    * <p>
    * Expected result: doc3
    * <p>
    * After updating (content is changed to "Sun")
    * <p>
    * Query : Select all documents where data contains "moon" word.
    * <p>
    * Expected result: 0
    * <p>
    * Query : Select all documents where data contains "moon" word.
    * <p>
    * Expected result: doc3
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testUpdateFulltextConstraint() throws Exception
   {

      DocumentData doc3 =
         createDocument(storageA, testRoot, "Apollo 13", nasaDocumentTypeDefinition, ("Apollo 13 was the third "
            + "manned mission by NASA intended to land on the Moon, but a mid-mission technical "
            + "malfunction forced the lunar landing to be aborted. ").getBytes(), new MimeType("text", "plain"));
      setProperty(doc3, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "James A. Lovell, Jr."));
      setProperty(doc3, new StringProperty(PROPERTY_BOOSTER, PROPERTY_BOOSTER, PROPERTY_BOOSTER, PROPERTY_BOOSTER,
         "Saturn V"));

      String statement1 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"moon\")";
      Query query = new Query(statement1, true);
      ItemsIterator<Result> result = storageA.query(query);

      assertEquals(1, result.size());
      checkResult(storageA, result, new DocumentData[]{doc3});

      //replace content
      ContentStream cs = new BaseContentStream("Sun".getBytes(), "test", new MimeType("text", "plain"));
      doc3.setContentStream(cs);

      //check old one
      result = storageA.query(query);
      assertEquals(0, result.size());
      //check new  content
      String statement2 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"Sun\")";
      query = new Query(statement2, true);
      result = storageA.query(query);

      assertEquals(1, result.size());
      checkResult(storageA, result, new DocumentData[]{doc3});

   }

   /**
    * Included in supertype.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - node1 <b>typeID</b> - cmis:article-sports
    * <li>doc2: <b>Title</b> - node2 <b>typeID</b> - cmis:article-animals
    * </ul>
    * <p>
    * Query : Select all documents where query supertype is cmis:article.
    * <p>
    * Expected result: doc1, doc2.
    *
    * @throws Exception
    *            if an unexpected error occurs
    */
   public void testIncludedInSupertypeQueryTestTwoDocTypes() throws Exception
   {
      // create data

      TypeDefinition cmis_article_sports_typeDefinition = storageA.getTypeDefinition("cmis:article-sports", true);
      TypeDefinition cmis_article_animals_typeDefinition = storageA.getTypeDefinition("cmis:article-animals", true);

      DocumentData doc1 =
         createDocument(storageA, testRoot, "node1", cmis_article_sports_typeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));

      DocumentData doc2 =
         createDocument(storageA, testRoot, "node2", cmis_article_animals_typeDefinition, "hello world".getBytes(),
            new MimeType("text", "plain"));

      String stat = "SELECT * FROM cmis:article WHERE IN_FOLDER( '" + testRoot.getObjectId() + "')";

      Query query = new Query(stat, false);
      ItemsIterator<Result> result = storageA.query(query);

      // check results
      checkResult(storageA, result, new DocumentData[]{doc1, doc2});
   }
}
