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

package org.xcmis.sp.jcr.exo.query;

import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS. <br/>
 * Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a>
 * @version $Id: QueryUsecasesTest.java 27 2010-02-08 07:49:20Z andrew00x $
 */
public class QueryUsecasesTest extends BaseQueryTest
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

   /**
    * Get query capabilities.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testSearchCapabilities() throws Exception
   {
      RepositoryCapabilities repCapabilities = storage.getRepositoryInfo().getCapabilities();
      assertEquals(CapabilityQuery.BOTHCOMBINED, repCapabilities.getCapabilityQuery());
      assertEquals(CapabilityJoin.NONE, repCapabilities.getCapabilityJoin());
      assertFalse(repCapabilities.isCapabilityPWCSearchable());
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
    * @throws Exception if an unexpected error occurs
    */
   public void testAndOrConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT * ");
      sql.append("FROM ");
      sql.append(NASA_DOCUMENT);
      sql.append(" WHERE ");
      sql.append(PROPERTY_BOOSTER + " = " + "'Saturn V'");
      sql.append(" AND ( " + PROPERTY_COMMANDER + " = 'Frank F. Borman, II' ");
      sql.append("       OR " + PROPERTY_COMMANDER + " = 'James A. Lovell, Jr.' )");

      Query query = new Query(sql.toString(), true);

      ItemsIterator<Result> result = storage.query(query);
      // check results
      checkResult(result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

   }

   /**
    * Test IN_FOLDER constraint.
    * <p>
    * Initial data:
    * <ul>
    * <li>testDocumentInFolderConstrain1:
    * <li>-see createNasaContent()
    * <li>testDocumentInFolderConstrain2:
    * <li>-see createNasaContent()
    * </ul>
    * <p>
    * Query : Select all documents that are in folder2.
    * <p>
    * Expected result: all documents that are in folder2.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testDocumentInFolderConstrain() throws Exception
   {
      // create data
      FolderData testRoot1 = createFolder(testRoot, "testDocumentInFolderConstrain1", folderTypeDefinition);
      FolderData testRoot2 = createFolder(testRoot, "testDocumentInFolderConstrain2", folderTypeDefinition);

      List<DocumentData> appolloContent = createNasaContent(testRoot1);
      List<DocumentData> appolloContent2 = createNasaContent(testRoot2);

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_FOLDER( '" + testRoot2.getObjectId() + "')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      // check results
      checkResult(result, appolloContent2.toArray(new DocumentData[appolloContent2.size()]));

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
    * @throws Exception if an unexpected error occurs
    */
   public void testFolderInFolderConstrain() throws Exception
   {
      // create data
      FolderData folder1 = createFolder(testRoot, "folder1", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder1, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));

      FolderData folder2 = createFolder(testRoot, "folder2", folderTypeDefinition);

      DocumentData doc2 =
         createDocument(folder2, "node2", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));

      FolderData folder3 = createFolder(folder1, "folder3", folderTypeDefinition);

      DocumentData doc3 =
         createDocument(folder3, "node3", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));

      FolderData folder4 = createFolder(folder3, "folder4", folderTypeDefinition);

      String statement = "SELECT * FROM cmis:folder  WHERE IN_FOLDER( '" + folder1.getObjectId() + "')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      // check results
      checkResult(result, new FolderData[]{folder3});

   }

   //
   //   /**
   //    * Constraints with multi-valued properties is not supported.
   //    *
   //    * @throws Exception
   //    */
   //   // XXX temporary excluded
   //   public void _testAnyInConstraint() throws Exception
   //   {
   //      // create data
   //      String name = "fileCS2.doc";
   //      String name2 = "fileCS3.doc";
   //      String contentType = "text/plain";
   //
   //      Document folder = createFolder(root, "CASETest");
   //
   //      Document doc1 = createDocument(folder.getObjectId(), name, new byte[0], contentType);
   //      doc1.setDecimals("multivalueLong", new BigDecimal[]{new BigDecimal(3), new BigDecimal(5), new BigDecimal(10)});
   //      doc1.setStrings("multivalueString", new String[]{"bla-bla"});
   //      doc1.save();
   //
   //      Document doc2 = createDocument(folder.getObjectId(), name2, new byte[0], contentType);
   //      doc2.setDecimals("multivalueLong", new BigDecimal[]{new BigDecimal(15), new BigDecimal(10)});
   //      doc2.setStrings("multivalueString", new String[]{"bla-bla"});
   //      doc2.save();
   //
   //      String statement =
   //         "SELECT * FROM " + NASA_DOCUMENT + " WHERE ANY multivalueLong IN ( 3 , 5, 6 ) ";
   //
   //      Query query = new Query(statement, true);
   //
   //      ItemsIterator<Result> result = storage.query(query);
   //
   //      // check results
   //      checkResult(result, new Document[]{doc1});
   //   }
   //
   /**
    * Test fulltext constraint.
    * <p>
    * Initial data:
    * <p>
    * see createNasaContent()
    * <p>
    * Query : Select all documents where data contains "moon" word.
    * <p>
    * Expected result: document2 and document3
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testFulltextConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

      String statement1 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"moon\")";
      Query query = new Query(statement1, true);
      ItemsIterator<Result> result = storage.query(query);

      assertEquals(2, result.size());
      checkResult(result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

      String statement2 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"Moon\")";
      query = new Query(statement2, true);
      ItemsIterator<Result> result2 = storage.query(query);

      assertEquals(2, result2.size());
      checkResult(result2, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

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
    * Query : Select all documents where name is in set {'Virgil I. Grissom', 'Frank F. Borman, II', 'Charles Conrad, Jr.'}.
    * <p>
    * Expected result: document2
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testINConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER
            + " IN ('Virgil I. Grissom', 'Frank F. Borman, II', 'Charles Conrad, Jr.')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);
      assertEquals(1, result.size());
      checkResult(result, new DocumentData[]{appolloContent.get(1)});

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
   //   // XXX temporary excluded
   //   public void _testJoinWithCondition() throws Exception
   //   {
   //      // create data
   //      Document folder1 = this.createFolder(root, "folder1");
   //      folder1.setString("folderName", "folderOne");
   //
   //      Document doc1 = createDocument(folder1.getObjectId(), "node1", "hello world".getBytes(), "text/plain");
   //      doc1.setString("parentFolderName", "folderOne");
   //
   //      Document folder2 = this.createFolder(root, "folder2");
   //      folder2.setString("folderName", "folderTwo");
   //
   //      Document doc2 = createDocument(folder2.getObjectId(), "node1", "hello world".getBytes(), "text/plain");
   //      doc2.setString("parentFolderName", "folderThree");
   //
   //      String statement =
   //         "SELECT doc.* FROM " + NASA_DOCUMENT + " AS doc LEFT JOIN " + JcrCMIS.NT_FOLDER
   //            + " AS folder ON (doc.parentFolderName = folder.folderName)";
   //
   //      Query query = new Query(statement, true);
   //      ItemsIterator<Result> result = storage.query(query);
   //
   //      assertEquals(1, result.size());
   //      // TODO check results - must doc1 and folder1
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
    * @throws Exception if an unexpected error occurs
    */
   public void testLIKEConstraint() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(testRoot);

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " AS doc WHERE " + PROPERTY_COMMANDER + " LIKE 'James%'";

      Query query = new Query(statement, true);

      ItemsIterator<Result> result = storage.query(query);

      // check results
      assertEquals(1, result.size());
      checkResult(result, new DocumentData[]{appolloContent.get(2)});

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
    * @throws Exception if an unexpected error occurs
    */
   public void testLIKEConstraintEscapeSymbols() throws Exception
   {

      DocumentData doc1 =
         createDocument(testRoot, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "ad%min master"));

      DocumentData doc2 =
         createDocument(testRoot, "node2", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "admin operator"));

      DocumentData doc3 =
         createDocument(testRoot, "node3", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));
      setProperty(doc3, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "radmin"));

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " AS doc WHERE  " + PROPERTY_COMMANDER + " LIKE 'ad\\%min%'";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      // check results
      assertEquals(1, result.size());
      checkResult(result, new DocumentData[]{doc1});

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
    * @throws Exception if an unexpected error occurs
    */
   public void testNOTConstraint() throws Exception
   {

      DocumentData doc1 =
         createDocument(testRoot, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));

      FolderData folder2 = createFolder(testRoot, "folder2", folderTypeDefinition);
      DocumentData doc2 =
         createDocument(folder2, "node2", nasaDocumentTypeDefinition, "hello".getBytes(), new MimeType("text", "plain"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE NOT CONTAINS(\"world\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc2});

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
    * Query : Select all documents where PROPERTY_COMMANDER property not in set {'Walter M. Schirra', 'James A. Lovell, Jr.'}.
    * <p>
    * Expected result: document2, document4
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testNotINConstraint() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(testRoot);

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER
            + " NOT IN ('Walter M. Schirra', 'James A. Lovell, Jr.')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{appolloContent.get(1), appolloContent.get(3)});

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
    * Query : Select all documents where PROPERTY_COMMANDER property NOT NOT IN set {'James A. Lovell, Jr.'}.
    * <p>
    * Expected result: document3.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testNotNotINConstraint() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE  NOT (" + PROPERTY_COMMANDER + " NOT IN ('James A. Lovell, Jr.'))";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{appolloContent.get(2)});

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
    * @throws Exception if an unexpected error occurs
    */
   public void testOrderByFieldDesc() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

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
      ItemsIterator<Result> result = storage.query(query);
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
    * @throws Exception if an unexpected error occurs
    */
   public void testOrderByFieldAsk() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

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
      ItemsIterator<Result> result = storage.query(query);
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
    * @throws Exception if an unexpected error occurs
    */
   public void testOrderByDefault() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

      StringBuffer sql = new StringBuffer();
      sql.append("SELECT ");
      sql.append(CmisConstants.LAST_MODIFIED_BY + ", ");
      sql.append(CmisConstants.OBJECT_ID + ", ");
      sql.append(CmisConstants.LAST_MODIFICATION_DATE);
      sql.append(" FROM ");
      sql.append(NASA_DOCUMENT);

      String statement = sql.toString();

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);
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
    * see createNasaContent()
    * <p>
    * Query : Select all documents which contains word "moon" in ORDER BY SCORE.
    * <p>
    * Expected result: doc2, doc3.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testOrderByScore() throws Exception
   {

      List<DocumentData> appolloContent = createNasaContent(testRoot);

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
      ItemsIterator<Result> result = storage.query(query);
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
    * <li>doc1: <b>Title</b> - node1 <b>StringProperty</b> - James A. Lovell, Jr.
    * <li>doc2: <b>Title</b> - node2
    * </ul>
    * <p>
    * Query : Select all documents that has "prop" property (IS NOT NULL).
    * <p>
    * Expected result: doc1
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testPropertyExistence() throws Exception
   {
      // Document folder1 = createFolder(root, "CASETest");

      DocumentData doc1 =
         createDocument(testRoot, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "James A. Lovell, Jr."));
      DocumentData doc2 =
         createDocument(testRoot, "node2", nasaDocumentTypeDefinition, "hello".getBytes(),
            new MimeType("text", "plain"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER + " IS NOT NULL";
      Query query = new Query(statement, true);

      ItemsIterator<Result> result = storage.query(query);
      checkResult(result, new DocumentData[]{doc1});

   }

   /**
    * Test SCORE as column.
    * <p>
    * Initial data:
    * <p>
    * see createNasaContent()
    * <p>
    * Query : Select all documents that contains "first" word and ordered by score.
    * <p>
    * Expected result: doc4, doc1 and doc2.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testScoreAsColumn() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(testRoot);

      String statement =
         "SELECT SCORE() AS scoreCol , " + CmisConstants.NAME + " AS id FROM " + NASA_DOCUMENT
            + " WHERE CONTAINS(\"first\") ORDER BY SCORE()";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

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
    * @throws Exception if an unexpected error occurs
    */
   public void testTreeConstrain() throws Exception
   {
      // create data
      FolderData folder1 = createFolder(testRoot, "folder1", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder1, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));

      FolderData subfolder1 = createFolder(folder1, "folder2", folderTypeDefinition);

      DocumentData doc2 =
         createDocument(subfolder1, "node1", nasaDocumentTypeDefinition, "hello world".getBytes(), new MimeType("text",
            "plain"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE IN_TREE('" + folder1.getObjectId() + "')";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc1, doc2});
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
    * @throws Exception if an unexpected error occurs
    */
   public void testNotEqualDecimal() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      FolderData folder = createFolder(testRoot, "NotEqualDecimal", folderTypeDefinition);
      DocumentData doc1 =
         createDocument(folder, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(3)));

      DocumentData doc2 =
         createDocument(folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(15)));
      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_BOOSTER_MASS + " <> 3";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc2});
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
    * @throws Exception if an unexpected error occurs
    */
   public void testMoreThanDecimal() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      FolderData folder = createFolder(testRoot, "CASETest", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(3)));

      DocumentData doc2 =
         createDocument(folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new DecimalProperty(PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS, PROPERTY_BOOSTER_MASS,
         PROPERTY_BOOSTER_MASS, new BigDecimal(15)));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_BOOSTER_MASS + " > 5";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc2});
   }

   /**
    * Test not equal comparison (<>) string.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS2.doc <b>StringProperty</b> - test word first
    * <li>doc2: <b>Title</b> - fileCS3.doc <b>StringProperty</b> - test word second
    * </ul>
    * <p>
    * Query : Select all documents property StringPropertye not equal to
    * "test word second".
    * <p>
    * Expected result: doc1.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testNotEqualString() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      FolderData folder = createFolder(testRoot, "CASETest", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "test word first"));

      DocumentData doc2 =
         createDocument(folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "test word second"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE " + PROPERTY_COMMANDER + " <> 'test word second'";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc1});
   }

   /**
    * Test fulltext search from jcr:content.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileFirst  <b>StringProperty</b> - "There must be test word"
    * <li>doc2: <b>Title</b> - fileSecond <b>StringProperty</b> - "Test word is not here"
    * </ul>
    * <p>
    * Query : Select all documents that contains "here" word.
    * <p>
    * Expected result: doc2.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testSimpleFulltext() throws Exception
   {
      // create data
      String name1 = "fileFirst";
      String name2 = "fileSecond";

      FolderData folder = createFolder(testRoot, "SimpleFullTextTest", folderTypeDefinition);
      DocumentData doc1 =
         createDocument(folder, name1, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be test word"));

      DocumentData doc2 =
         createDocument(folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "Test word is not here"));
      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"here\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc2});
   }

   /**
    * Test complex fulltext query.
    * <p>
    * Initial data:
    * <ul>
    * <li>doc1: <b>Title</b> - fileCS1.doc <b>StringProperty</b> - "There must be test word"
    * <li>doc2: <b>Title</b> - fileCS2.doc <b>StringProperty</b> - "Test word is not here. Another check-word."
    * <li>doc3: <b>Title</b> - fileCS3.doc <b>StringProperty</b> - "There must be check-word."
    * </ul>
    * <p>
    * Query : Select all documents that contains "There must" phrase and do not
    * contain "check-word" word.
    * <p>
    * Expected result: doc1.
    *
    * @throws Exception if an unexpected error occurs
    */
   public void testExtendedFulltext() throws Exception
   {
      // create data
      String name1 = "fileCS1.doc";
      String name2 = "fileCS2.doc";
      String name3 = "fileCS3.doc";

      FolderData folder = createFolder(testRoot, "CASETest", folderTypeDefinition);

      DocumentData doc1 =
         createDocument(folder, name1, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be test word"));

      DocumentData doc2 =
         createDocument(folder, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "Test word is not here. Another check-word."));

      DocumentData doc3 =
         createDocument(folder, name3, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc3, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be check-word."));

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"\\\"There must\\\" -\\\"check\\-word\\\"\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc1});
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
         createDocument(testRoot, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "There must be test word"));

      DocumentData doc2 =
         createDocument(testRoot, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "Test word is not here"));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE NOT CONTAINS(\"here\")";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc1});
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
    * @throws Exception if an unexpected error occurs
    */
   public void testBooleanConstraint() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      DocumentData doc1 =
         createDocument(testRoot, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc1, new BooleanProperty(PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, true));

      DocumentData doc2 =
         createDocument(testRoot, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      setProperty(doc2, new BooleanProperty(PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, PROPERTY_STATUS, false));

      String statement = "SELECT * FROM " + NASA_DOCUMENT + " WHERE (" + PROPERTY_STATUS + " = FALSE )";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc2});
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
    * @throws Exception if an unexpected error occurs
    */
   public void testDateConstraint() throws Exception
   {
      // create data
      String name = "fileCS2.doc";
      String name2 = "fileCS3.doc";

      DocumentData doc1 =
         createDocument(testRoot, name, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));
      DocumentData doc2 =
         createDocument(testRoot, name2, nasaDocumentTypeDefinition, new byte[0], new MimeType("text", "plain"));

      String statement =
         "SELECT * FROM " + NASA_DOCUMENT
            + " WHERE ( cmis:lastModificationDate >= TIMESTAMP '2007-01-01T00:00:00.000Z' )";

      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);

      checkResult(result, new DocumentData[]{doc1, doc2});
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
    * @throws Exception if an unexpected error occurs
    */
   public void testSimpleQuery() throws Exception
   {
      List<DocumentData> appolloContent = createNasaContent(testRoot);
      String statement = "SELECT * FROM " + NASA_DOCUMENT;
      Query query = new Query(statement, true);
      ItemsIterator<Result> result = storage.query(query);
      checkResult(result, appolloContent.toArray(new DocumentData[appolloContent.size()]));

   }

   /**
    * Test fulltext constraint.
    * <p>
    * Initial data: see createNasaContent()
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
    * @throws Exception if an unexpected error occurs
    */
   public void testUpdateFulltextConstraint() throws Exception
   {

      DocumentData doc3 =
         createDocument(testRoot, "Apollo 13", nasaDocumentTypeDefinition, ("Apollo 13 was the third "
            + "manned mission by NASA intended to land on the Moon, but a mid-mission technical "
            + "malfunction forced the lunar landing to be aborted. ").getBytes(), new MimeType("text", "plain"));
      setProperty(doc3, new StringProperty(PROPERTY_COMMANDER, PROPERTY_COMMANDER, PROPERTY_COMMANDER,
         PROPERTY_COMMANDER, "James A. Lovell, Jr."));
      setProperty(doc3, new StringProperty(PROPERTY_BOOSTER, PROPERTY_BOOSTER, PROPERTY_BOOSTER, PROPERTY_BOOSTER,
         "Saturn V"));

      String statement1 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"moon\")";
      Query query = new Query(statement1, true);
      ItemsIterator<Result> result = storage.query(query);

      assertEquals(1, result.size());
      checkResult(result, new DocumentData[]{doc3});

      //replace content
      ContentStream cs = new BaseContentStream("Sun".getBytes(), "test", new MimeType("text", "plain"));
      doc3.setContentStream(cs);

      //check old one
      result = storage.query(query);
      assertEquals(0, result.size());
      //check new  content
      String statement2 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"Sun\")";
      query = new Query(statement2, true);
      result = storage.query(query);

      assertEquals(1, result.size());
      checkResult(result, new DocumentData[]{doc3});

   }

   protected DocumentData createAppoloMission(FolderData parentFolder, String missionName, String commander,
      String commandModulePilot, String lunarModulePilot, String boosterName, double boosterMass, long sampleReturned,
      String objectives) throws Exception
   {

      DocumentData doc =
         createDocument(parentFolder, missionName, nasaDocumentTypeDefinition, objectives.getBytes(), new MimeType(
            "text", "plain"));
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
      setProperty(doc, new DecimalProperty(PROPERTY_SAMPLE_RETURNED, PROPERTY_SAMPLE_RETURNED, PROPERTY_SAMPLE_RETURNED,
         PROPERTY_SAMPLE_RETURNED, new BigDecimal(sampleReturned)));
      return doc;
   }

   /**
    * @see org.xcmis.sp.jcr.exo.BaseTest#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {

      storage.deleteTree(testRoot, true, UnfileObject.DELETE, true);
      super.tearDown();

   }

   /**
    * Create content for Apollo program.
    *
    * @param folder
    * @return
    * @throws Exception
    */
   private List<DocumentData> createNasaContent(FolderData folder) throws Exception
   {
      List<DocumentData> result = new ArrayList<DocumentData>();
      result.add(createAppoloMission(folder, "Apollo 7", "Walter M. Schirra", "Donn F. Eisele", "R. Walter Cunningham",
         "Saturn 1B", 581.844, 0, "Apollo 7 (October 11-22, 1968) was the first manned mission "
            + "in the Apollo program to be launched. It was an eleven-day "
            + "Earth-orbital mission, the first manned launch of the "
            + "Saturn IB launch vehicle, and the first three-person " + "American space mission"));

      result.add(createAppoloMission(folder, "Apollo 8", "Frank F. Borman, II", "James A. Lovell, Jr",
         "William A. Anders", "Saturn V", 3038.500, 0, "Apollo 8 was the first "
            + "manned space voyage to achieve a velocity sufficient to allow escape from the "
            + "gravitational field of planet Earth; the first to escape from the gravitational "
            + "field of another celestial body; and the first manned voyage to return to planet Earth "
            + "from another celestial body - Earth's Moon"));

      result.add(createAppoloMission(folder, "Apollo 13", "James A. Lovell, Jr.", "John L. Swigert",
         "Fred W. Haise, Jr.", "Saturn V", 3038.500, 0, "Apollo 13 was the third "
            + "manned mission by NASA intended to land on the Moon, but a mid-mission technical "
            + "malfunction forced the lunar landing to be aborted. "));

      result.add(createAppoloMission(folder, "Apollo 17", "Eugene A. Cernan", "Ronald E. Evans", "Harrison H. Schmitt",
         "Saturn V", 3038.500, 111, "Apollo 17 was the eleventh manned space "
            + "mission in the NASA Apollo program. It was the first night launch of a U.S. human "
            + "spaceflight and the sixth and final lunar landing mission of the Apollo program."));

      return result;
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
    * @throws Exception if an unexpected error occurs
    */
   public void testIncludedInSupertypeQueryTestTwoDocTypes() throws Exception
   {
      // create data

      TypeDefinition cmis_article_sports_typeDefinition = storage.getTypeDefinition("cmis:article-sports", true);
      TypeDefinition cmis_article_animals_typeDefinition = storage.getTypeDefinition("cmis:article-animals", true);

      DocumentData doc1 =
         createDocument(testRoot, "node1", cmis_article_sports_typeDefinition, "hello world".getBytes(), new MimeType(
            "text", "plain"));

      DocumentData doc2 =
         createDocument(testRoot, "node2", cmis_article_animals_typeDefinition, "hello world".getBytes(), new MimeType(
            "text", "plain"));

      String stat = "SELECT * FROM cmis:article WHERE IN_FOLDER( '" + testRoot.getObjectId() + "')";

      Query query = new Query(stat, false);
      ItemsIterator<Result> result = storage.query(query);

      // check results
      checkResult(result, new DocumentData[]{doc1, doc2});
   }
}
