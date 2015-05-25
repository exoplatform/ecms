package org.exoplatform.ecms.xcmis.sp.query;

import org.exoplatform.ecms.xcmis.sp.StorageImpl;
import org.junit.Ignore;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;

import java.util.List;

@Ignore
public class QueryDriveTest extends BaseQueryTest
{

   private StorageImpl storageA;

   private StorageImpl storageB;

   /**
    * @see org.exoplatform.ecms.xcmis.sp.BaseTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      storageA = (StorageImpl)registry.getConnection("driveA").getStorage();
      storageB = (StorageImpl)registry.getConnection("driveB").getStorage();
   }

   public void testSearchSameConten() throws Exception
   {
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      FolderData testRoot =
         createFolder(storageA, rootFolder, "QueryUsecasesTest", storageA.getTypeDefinition("cmis:folder", true));
      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      String statement1 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"moon\")";
      Query query = new Query(statement1, true);
      ItemsIterator<Result> result = storageA.query(query);

      assertEquals(2, result.size());
      checkResult(storageA, result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

      ItemsIterator<Result> resultB = storageB.query(query);

      assertEquals(0, resultB.size());

   }

   public void testSearchSameContenInDifferentDrives() throws Exception
   {
      //storageA
      FolderData rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());
      FolderData testRoot =
         createFolder(storageA, rootFolder, "QueryUsecasesTest", storageA.getTypeDefinition("cmis:folder", true));

      List<DocumentData> appolloContent = createNasaContent(storageA, testRoot);

      //storageB
      FolderData rootFolderB = (FolderData)storageA.getObjectById(storageB.getRepositoryInfo().getRootFolderId());
      FolderData testRootB =
         createFolder(storageB, rootFolderB, "QueryUsecasesTest", storageB.getTypeDefinition("cmis:folder", true));

      List<DocumentData> appolloContentB = createNasaContent(storageB, testRootB);

      String statement1 = "SELECT * FROM " + NASA_DOCUMENT + " WHERE CONTAINS(\"moon\")";
      Query query = new Query(statement1, true);
      ItemsIterator<Result> result = storageA.query(query);

      assertEquals(2, result.size());
      checkResult(storageA, result, new DocumentData[]{appolloContent.get(1), appolloContent.get(2)});

      ItemsIterator<Result> resultB = storageB.query(query);

      assertEquals(2, resultB.size());
      checkResult(storageB, resultB, new DocumentData[]{appolloContentB.get(1), appolloContentB.get(2)});
   }

}
