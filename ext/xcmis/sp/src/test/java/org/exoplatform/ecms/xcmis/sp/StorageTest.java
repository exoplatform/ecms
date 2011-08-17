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

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.UnfileObject;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.utils.MimeType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: StorageTest.java 1192 2010-05-27 14:11:42Z
 *          alexey.zavizionov@gmail.com $
 */
public class StorageTest extends BaseTest
{

   protected FolderData rootFolder;

   protected TypeDefinition documentTypeDefinition;

   protected TypeDefinition policyTypeDefinition;

   protected TypeDefinition relationshipTypeDefinition;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      storageA = (StorageImpl)registry.getConnection("driveA").getStorage();
      rootFolder = (FolderData)storageA.getObjectById(storageA.getRepositoryInfo().getRootFolderId());

      documentTypeDefinition = storageA.getTypeDefinition("cmis:document", true);
      folderTypeDefinition = storageA.getTypeDefinition("cmis:folder", true);
      policyTypeDefinition = storageA.getTypeDefinition("cmis:policy", true);
      relationshipTypeDefinition = storageA.getTypeDefinition("cmis:relationship", true);
   }

   public void testDeleteDocumentLatestVersion() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "testDeleteLatestVersionDocument_Document", "cmis:document", null, null);
      try {
         storageA.deleteObject(document, false);
         fail("Must not remove latest version with deleteAllVersions == false");
      } catch (CmisRuntimeException e) {
         assertEquals("Unable to delete latest version at one.", e.getMessage());
      }
      
      try {
         storageA.deleteObject(document, true);
      } catch (CmisRuntimeException e) {
         fail("Must remove latest version and all versions with deleteAllVersions == true");
      }
   }
   
   public void testDeleteDocumentLatestVersionAfterCheckin() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "testDeleteLatestVersionDocument_Document", "cmis:document", null, null);
      // CHECKOUT
      DocumentData pwc = document.checkout();
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      checkinDocument = (DocumentData)storageA.getObjectById(checkinDocument.getObjectId());
      
      try {
         storageA.deleteObject(checkinDocument, false);
         fail("Must not remove latest version with deleteAllVersions == false");
      } catch (CmisRuntimeException e) {
         assertEquals("Unable to delete latest version at one.", e.getMessage());
      }
      
      try {
         storageA.deleteObject(checkinDocument, true);
      } catch (CmisRuntimeException e) {
         fail("Must remove latest version and all versions with deleteAllVersions == true");
      }
   }
   
   public void testDeleteDocumentBeforeLatestVersion() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "testDeleteLatestVersionDocument_Document", "cmis:document", null, null);
      String documentId = document.getObjectId();
      // CHECKOUT
      DocumentData pwc = document.checkout();
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      
      document = (DocumentData)storageA.getObjectById(documentId);
      
      try {
         storageA.deleteObject(document, false);
         fail("Must not remove before latest version with deleteAllVersions == false");
      } catch (CmisRuntimeException e) {
         String expectedMessage = "Unable to delete document version with label '1'. There are Reference property pointed to this Version " +
                                  "[]:1[http://www.exoplatform.com/jcr/exo/1.0]drives:1[]driveA:1" +
                                  "[]testDeleteLatestVersionDocument_Document:1[http://www.jcp.org/jcr/1.0]";
         String actualMessage = e.getMessage().length() > expectedMessage.length() ? e.getMessage().substring(0, expectedMessage.length()) : e.getMessage();
         assertEquals(expectedMessage, actualMessage);
      }
      
      try {
         storageA.deleteObject(document, true);
      } catch (CmisRuntimeException e) {
         fail("Must remove version and all versions with deleteAllVersions == true");
      }
   }
            
   public void testApplyACL() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "applyACLTestDocument", "cmis:document", null, null);
      AccessControlEntry ace =
         new AccessControlEntry("root", new HashSet<String>(Arrays.asList("cmis:read", "cmis:write")));
      document.setACL(Arrays.asList(ace));

      Node documentNode = getNodeFromStorage(storageA, "/applyACLTestDocument", false);
      AccessControlList acl = ((ExtendedNode)documentNode).getACL();

      List<String> permissions = acl.getPermissions("root");
      assertTrue(permissions.contains(PermissionType.READ));
      assertTrue(permissions.contains(PermissionType.REMOVE));
      assertTrue(permissions.contains(PermissionType.SET_PROPERTY));
      assertTrue(permissions.contains(PermissionType.ADD_NODE));

      System.out.println(document.getACL(false));
   }

   public void testApplyPolicy() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "applyPolicyTestDocument", "cmis:document", null, null);
      PolicyData policy = createPolicy(rootFolder, "applyPolicyTestPolicy01", "test apply policy", "cmis:policy");
      document.applyPolicy(policy);

      Node documentNode = getNodeFromStorage(storageA, "/applyPolicyTestDocument", false);
      assertTrue(documentNode.hasProperty(policy.getObjectId()));

      Collection<PolicyData> policies = document.getPolicies();
      assertEquals(1, policies.size());
      assertEquals(policy.getObjectId(), policies.iterator().next().getObjectId());
   }

   public void testCheckOut() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "checkoutTest", "cmis:document", new BaseContentStream("checkout test".getBytes(),
            null, new MimeType("text", "plain")), null);

      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();
      
      // Get PWC from storage
      try
      {
         pwc = (DocumentData)storageA.getObjectById(pwcId);
         // OK
      }
      catch (ObjectNotFoundException e)
      {
         fail("The PWC should be in storage, for the id '" + pwcId + "'. " + e.getMessage());
      }

      // test document
      assertEquals(document.getVersionSeriesId(), pwc.getVersionSeriesId());
      assertEquals(document.getVersionSeriesCheckedOutId(), pwc.getObjectId());
      assertTrue(document.isVersionSeriesCheckedOut());
      assertNotNull(document.getVersionSeriesCheckedOutBy());
      assertFalse(document.isLatestVersion());
      assertFalse(document.isPWC());

      // test pwc
      assertTrue(pwc.isVersionSeriesCheckedOut());
      assertNotNull(pwc.getVersionSeriesCheckedOutBy());
      assertTrue(pwc.isLatestVersion());
      assertTrue(pwc.isPWC());

      // check content
      byte[] b = new byte[128];
      int r = pwc.getContentStream().getStream().read(b);
      assertEquals("checkout test", new String(b, 0, r));
   }

   public void testCheckoutFail() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "checkoutTest", "cmis:document", new BaseContentStream("checkout test".getBytes(),
            null, new MimeType("text", "plain")), null);

      document.checkout();
      try
      {
         document.checkout();
         fail("VersioningException should be thrown.");
      }
      catch (VersioningException ve)
      {
         // OK
      }
   }

   public void testSuffixInObjectId() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getAllVersionsPwcVersionsTest", "cmis:document", null, null);
      String documentId = document.getObjectId();
      assertNotNull(documentId);
      assertTrue("First document id must ends with '_1' suffix", documentId.endsWith("_1"));
   }

   public void testConsistentId() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "consistentTest", "cmis:document", new BaseContentStream("consistent test".getBytes(),
            null, new MimeType("text", "plain")), null);
      String documentId = document.getObjectId();
      
      // get document by Id 1
      DocumentData version = (DocumentData)storageA.getObjectById(documentId);

      // test to be consistent id 1
      assertEquals("Document id should be the same as used for getObjectById.", documentId, version.getObjectId());
   }
   
   public void testConsistentIdWithCheckin() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "consistentTest", "cmis:document", new BaseContentStream("consistent test".getBytes(),
            null, new MimeType("text", "plain")), null);
      String documentId1 = document.getObjectId();
      
      // CHECKOUT
      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();
      
      // test
      assertNotNull(pwcId);
      assertTrue("PWC id should not contain the suffix", !pwcId.contains(JcrCMIS.ID_SEPARATOR));
      
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      String documentId2 = checkinDocument.getObjectId();

      // test
      assertNotNull(documentId2);
      assertTrue("Second document id must ends with '_2' suffix", documentId2.endsWith("_2"));
      
      // ==================
      
      // not the same id for both document versions
      assertTrue("The objectId before checkin and after should not be the same.", !documentId1.equals(documentId2));
      assertTrue("The objectId before checkin and after should not be the same.", !pwcId.equals(documentId1));
      assertTrue("The objectId before checkin and after should not be the same.", !pwcId.equals(documentId2));
      

      // get document by Id 2
      DocumentData version2 = (DocumentData)storageA.getObjectById(documentId2);
      
      // test to be consistent id 2
      assertEquals("Document id should be the same as used for getObjectById.", documentId2, version2.getObjectId());
      
      // test to get exception on the Id without suffix
      try {
         String dontSuffixedId = documentId1.split(JcrCMIS.ID_SEPARATOR)[0];
         storageA.getObjectById(dontSuffixedId);
         fail("Must not be the document with id '" + dontSuffixedId + "' (with no suffix id)");
      } catch (ObjectNotFoundException e) {
         // OK
      }
   }
   
   public void testCheckIn() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "checkinTest", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);
      String objectId1 = document.getObjectId();
      
      // CHECKOUT
      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();

      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test, content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkin = pwc.checkin(true, "my comment", null, cs, null, null);
      checkin = (DocumentData)storageA.getObjectById(checkin.getObjectId());

      // Check whether is removed PWC
      try
      {
         storageA.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }
      
      // Check first document id
      try
      {
         storageA.getObjectById(objectId1);
         // OK
      }
      catch (ObjectNotFoundException e)
      {
         fail("The first version should be in storage, for the id '" + objectId1 + "'. " + e.getMessage());
      }
      
      // Check second document id
      try
      {
         storageA.getObjectById(checkin.getObjectId());
         // OK
      }
      catch (ObjectNotFoundException e)
      {
         fail("The second version should be in storage, for the id '" + checkin.getObjectId() + "'. " + e.getMessage());
      }
      
      document = (DocumentData)storageA.getObjectById(objectId1);
      
      // check first document state
      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
      // check content
      String content = convertStreamToString(document.getContentStream().getStream());
      assertEquals("checkin test", content);
      
      // check latest document state
      assertFalse(checkin.isVersionSeriesCheckedOut());
      assertNull(checkin.getVersionSeriesCheckedOutId());
      assertNull(checkin.getVersionSeriesCheckedOutBy());
      assertEquals("my comment", checkin.getProperty(CmisConstants.CHECKIN_COMMENT).getValues().get(0));
      // check content
      content = convertStreamToString(checkin.getContentStream().getStream());
      assertEquals("checkin test, content updated", content);
   }
   
   public void testCheckInDouble() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "testCheckInDouble_Document1", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);
      String objectId1 = document.getObjectId();
      
      // VERSION 1 
      
      DocumentData pwc1 = document.checkout();

      ContentStream cs1 =
         new BaseContentStream("checkin test, content updated 1".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkin1 = pwc1.checkin(true, "my comment 1", null, cs1, null, null);
      String objectId2 = checkin1.getObjectId();
      checkin1 = (DocumentData)storageA.getObjectById(objectId2);
      
      // VERSION 2

      DocumentData pwc2 = checkin1.checkout();
      String pwcId2 = pwc2.getObjectId();
      
      ContentStream cs2 =
         new BaseContentStream("checkin test, content updated 2".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkin2 = pwc2.checkin(true, "my comment 2", null, cs2, null, null);
      String objectId3 = checkin2.getObjectId();
      checkin2 = (DocumentData)storageA.getObjectById(objectId2);

      // Check whether is removed PWC 2
      try
      {
         storageA.getObjectById(pwcId2);
         fail("PWC 2 must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }
      
      // Check second document id
      try
      {
         storageA.getObjectById(objectId2);
         // OK
      }
      catch (ObjectNotFoundException e)
      {
         fail("The second version should be in storage, for the id '" + objectId2 + "'. " + e.getMessage());
      }
      
      // Check third version document id
      try
      {
         storageA.getObjectById(objectId3);
         // OK
      }
      catch (ObjectNotFoundException e)
      {
         fail("The second version should be in storage, for the id '" + objectId3 + "'. " + e.getMessage());
      }

      // get it again to get updated properties
      document = (DocumentData)storageA.getObjectById(objectId1);
      checkin1 = (DocumentData)storageA.getObjectById(objectId2);
      checkin2 = (DocumentData)storageA.getObjectById(objectId3);
      
      // check first document state
      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
      // check content
      String content = convertStreamToString(document.getContentStream().getStream());
      assertEquals("checkin test", content);
      
      
      // check second document state
      assertFalse(checkin1.isVersionSeriesCheckedOut());
      assertNull(checkin1.getVersionSeriesCheckedOutId());
      assertNull(checkin1.getVersionSeriesCheckedOutBy());
      assertEquals("my comment 1", checkin1.getProperty(CmisConstants.CHECKIN_COMMENT).getValues().get(0));
      // check content
      content = convertStreamToString(checkin1.getContentStream().getStream());
      assertEquals("checkin test, content updated 1", content);

      
      // check latest document state
      assertFalse(checkin2.isVersionSeriesCheckedOut());
      assertNull(checkin2.getVersionSeriesCheckedOutId());
      assertNull(checkin2.getVersionSeriesCheckedOutBy());
      assertEquals("my comment 2", checkin2.getProperty(CmisConstants.CHECKIN_COMMENT).getValues().get(0));
      // check content
      content = convertStreamToString(checkin2.getContentStream().getStream());
      assertEquals("checkin test, content updated 2", content);
      
   }

   public void testCheckInTestId() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "testCheckInCheckId_Document1", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);
      
      String documentId = document.getObjectId();
      
      DocumentData pwc = document.checkout();
      
      String pwcId = pwc.getObjectId();

      // Get PWC from storage
      pwc = (DocumentData)storageA.getObjectById(pwcId);

      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));

      DocumentData checkedInDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      
      String checkedInDocumentId = checkedInDocument.getObjectId();
      
      String documentIdAfterCheckedIn = document.getObjectId();
      
      assertEquals("Document id should be the same as before checkout/checkin.", documentId, documentIdAfterCheckedIn);
      assertNotSame("Document checked in id should NOT be the same as PWC id.", pwcId, documentIdAfterCheckedIn);
      assertNotSame("Document checked in id should NOT be the same as first document id.", checkedInDocumentId, documentIdAfterCheckedIn);
   }

   public void testCheckInRename() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "checkinTestRename", "cmis:document", new BaseContentStream("checkin test"
            .getBytes(), null, new MimeType("text", "plain")), null);
      DocumentData pwc = (DocumentData)document.checkout();
      String pwcId = pwc.getObjectId();

      // update
      pwc = (DocumentDataImpl)storageA.getObjectById(pwcId);

      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));

      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(CmisConstants.NAME, CmisConstants.NAME, CmisConstants.NAME,
         CmisConstants.NAME, "checkinTestRename_NewName"));
      pwc.checkin(true, "my comment", properties, cs, null, null);

      try
      {
         storageA.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }

      //document = (DocumentDataImpl)storageA.getObjectById(document.getObjectId());
      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
      assertEquals("my comment", document.getProperty(CmisConstants.CHECKIN_COMMENT).getValues().get(0));

      assertEquals("checkinTestRename_NewName", document.getName());
      // check content
      byte[] b = new byte[128];
      int r = document.getContentStream().getStream().read(b);
      assertEquals("checkin test. content updated", new String(b, 0, r));
   }

   public void testCancelCheckOutDocument() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "cancelCheckoutDocumentTest", "cmis:document", new BaseContentStream(
            "cancel checkout test".getBytes(), null, new MimeType("text", "plain")), null);
      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();

      // Call cancel checkout on other then PWC version in repository.
      document.cancelCheckout();

      try
      {
         storageA.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }

      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
   }

   public void testCancelCheckOutPWC() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "cancelCheckoutPWCTest", "cmis:document", new BaseContentStream(
            "cancel checkout test".getBytes(), null, new MimeType("text", "plain")), null);

      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();

      // Get PWC from storage
      pwc = (DocumentData)storageA.getObjectById(pwcId);

      // Call cancel checkout on PWC.
      pwc.cancelCheckout();

      try
      {
         storageA.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }

      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
   }

   public void testDeletePWC() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "deletePWCTest", "cmis:document", new BaseContentStream("delete PWC test"
            .getBytes(), null, new MimeType("text", "plain")), null);

      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();

      // Get PWC from storage
      pwc = (DocumentData)storageA.getObjectById(pwcId);

      // Delete PWC.
      storageA.deleteObject(pwc, true);

      try
      {
         storageA.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }

      // Property on document must be the same as in not checked-out state.
      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
   }

   public void testGetAllVersions() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getAllVersionsTest", "cmis:document", null, null);
      String versionSeriesId = document.getVersionSeriesId();
      Collection<DocumentData> allVersions = storageA.getAllVersions(versionSeriesId);
      assertEquals(1, allVersions.size());
      assertEquals(document.getObjectId(), allVersions.iterator().next().getObjectId());
   }

   public void testGetAllVersionsWithPwc() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getAllVersionsPwcTest", "cmis:document", null, null);
      String versionSeriesId = document.getVersionSeriesId();
      DocumentData pwc = document.checkout();
      
      Collection<DocumentData> allVersions = storageA.getAllVersions(versionSeriesId);
      assertEquals(2, allVersions.size());
      Iterator<DocumentData> vi = allVersions.iterator();
      assertEquals(pwc.getObjectId(), vi.next().getObjectId());
      assertEquals(document.getObjectId(), vi.next().getObjectId());
   }

   public void testGetAllVersionsWithCheckin() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "testGetAllVersionsWithCheckin_Document1", "cmis:document", null, null);
      String document_id = document.getObjectId();
      String versionSeriesId = document.getVersionSeriesId();

      DocumentData pwc = document.checkout();

      DocumentData checkin = pwc.checkin(true, "", null, null, null, null);
      
      assertEquals(document_id.split("_")[0] + "_" + "2", checkin.getObjectId());
      
      String checkinObjectId = checkin.getObjectId();

      DocumentData checkinObject = (DocumentData)storageA.getObjectById(checkinObjectId);
      
      DocumentData pwc2 = checkinObject.checkout();
      String pwc_checkout2_id = pwc2.getObjectId();
      
      assertEquals("The document id should not changed.", document_id, document.getObjectId());

      Collection<DocumentData> allVersions = storageA.getAllVersions(versionSeriesId);
      
      assertEquals(3, allVersions.size());
      
      // check all id
      Iterator<DocumentData> vi = allVersions.iterator();
      boolean hasPwc = false;
      boolean hasLatest = false;
      while (vi.hasNext()) {
         DocumentData documentData = (DocumentData) vi.next();
         String versionLabel = documentData.getVersionLabel();
         if ("pwc".equalsIgnoreCase(versionLabel)) {
            if (hasPwc) 
               fail("Already was a PWC document");
            else
               hasPwc = true;
            assertEquals("The id of 'pwc' label document should be the same as the pwc checked out document.", pwc_checkout2_id, documentData.getObjectId());
         } else if ("latest".equalsIgnoreCase(versionLabel)) {
            if (hasLatest) 
               fail("Already was a Latest document");
            else
               hasLatest = true;
            assertEquals("The id of 'latest' label document should be the same as the latest document.", checkinObjectId, documentData.getObjectId());
         } else {
            assertEquals("The id of first label document should be the same as the '"+ versionLabel + "' (first) versioned document.", document_id, documentData.getObjectId());            
         }
      }
   }

   public void testGetCheckedOutDocs() throws Exception
   {
      DocumentData document1 = createDocument(rootFolder, "getCheckedOutTest01", "cmis:document", null, null);
      FolderData folder = createFolder(rootFolder, "folderCheckedOutTest", "cmis:folder");
      DocumentData document2 = createDocument(folder, "getCheckedOutTest02", "cmis:document", null, null);
      DocumentData pwc1 = document1.checkout();
      DocumentData pwc2 = document2.checkout();

      List<String> r = new ArrayList<String>();
      // Should be both documents
      for (ItemsIterator<DocumentData> checkedOutDocs = storageA.getCheckedOutDocuments(null, null); checkedOutDocs
         .hasNext();)
      {
         r.add(checkedOutDocs.next().getObjectId());
      }
      assertEquals(2, r.size());
      assertTrue(r.contains(pwc1.getObjectId()));
      assertTrue(r.contains(pwc2.getObjectId()));

      r.clear();

      // Should be only PWC "from" specified folder
      for (ItemsIterator<DocumentData> checkedOutDocs = storageA.getCheckedOutDocuments(folder, null); checkedOutDocs
         .hasNext();)
      {
         r.add(checkedOutDocs.next().getObjectId());
      }
      assertEquals(1, r.size());
      assertTrue(r.contains(pwc2.getObjectId()));
   }

   public void testChildren() throws Exception
   {
      FolderData folder = createFolder(rootFolder, "folderChildrenTest", "cmis:folder");
      Set<String> source = new HashSet<String>();
      String name = "testChildren";
      for (int i = 1; i <= 20; i++)
      {
         DocumentData document = createDocument(folder, name + i, "cmis:document", null, null);
         source.add(document.getObjectId());
      }
      // Check children viewing with paging. It should be close to real usage.
      int maxItems = 5;
      for (int i = 0, skipCount = 0; i < 4; i++, skipCount += maxItems)
      {
         ItemsIterator<ObjectData> children = folder.getChildren(null);
         children.skip(skipCount);
         for (int count = 0; children.hasNext() && count < maxItems; count++)
         {
            ObjectData next = children.next();
            //            System.out.println(next.getName());
            source.remove(next.getObjectId());
         }
      }
      if (source.size() > 0)
      {
         StringBuilder sb = new StringBuilder();
         for (String s : source)
         {
            if (sb.length() > 0)
            {
               sb.append(',');
            }
            sb.append(s);
         }
         fail("Object(s) " + sb.toString() + " were not found in children list.");
      }
   }
   
   public void testGetChildrenAfterCheckin() throws Exception
   {
      FolderData folder1 = createFolder(rootFolder, "testGetChildrenAfterCheckin_Folder1", "cmis:folder");
      
      DocumentData document =
         createDocument(folder1, "checkinTest", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);

      // CHECKOUT
      DocumentData pwc = document.checkout();
      
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      
      // ============================
      
      folder1 = (FolderData) storageA.getObjectById(folder1.getObjectId());
            
      List<String> chs = new ArrayList<String>();
      for (ItemsIterator<ObjectData> children = folder1.getChildren(null); children.hasNext();)
      {
         chs.add(children.next().getObjectId());
      }
      assertEquals(1, chs.size());
      // Test the Id child the same as last checkin
      assertEquals("Test the Id child the same as last checkin", checkinDocument.getObjectId(), chs.iterator().next());
   }
   
   public void testGetChildrenAfterDoubleCheckin() throws Exception
   {
      FolderData folder1 = createFolder(rootFolder, "testGetChildrenAfterDoubleCheckin_Folder1", "cmis:folder");
      
      DocumentData document =
         createDocument(folder1, "checkinTest", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);

      // FIRST VERSION
      
      // CHECKOUT
      DocumentData pwc = document.checkout();
      
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      String checkinId = checkinDocument.getObjectId();
      
      // SECOND VERSION
      
      // CHECKOUT
      document = (DocumentData)storageA.getObjectById(checkinId);
      pwc = document.checkout();
      
      // CHECKIN
      cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument2 = pwc.checkin(true, "my comment", null, cs, null, null);
      
      // ============================
      
      folder1 = (FolderData) storageA.getObjectById(folder1.getObjectId());
            
      List<String> chs = new ArrayList<String>();
      for (ItemsIterator<ObjectData> children = folder1.getChildren(null); children.hasNext();)
      {
         chs.add(children.next().getObjectId());
      }
      assertEquals(1, chs.size());
      // Test the Id child the same as last checkin
      assertEquals("Test the Id child the same as last checkin", checkinDocument2.getObjectId(), chs.iterator().next());
   }
   
   public void testDeleteVersion() throws Exception
   {
      FolderData folder1 = createFolder(rootFolder, "testDeleteVersion_Folder1", "cmis:folder");
      
      DocumentData document =
         createDocument(folder1, "checkinTest", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);

      
      // FIRST VERSION
      
      // CHECKOUT
      DocumentData pwc = document.checkout();
      
      // CHECKIN
      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument = pwc.checkin(true, "my comment", null, cs, null, null);
      
      
      // SECOND VERSION
      
      // CHECKOUT
      pwc = checkinDocument.checkout();
      
      // CHECKIN
      cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      DocumentData checkinDocument2 = pwc.checkin(true, "my comment", null, cs, null, null);
      
      // ============================
      
      assertTrue(document.getObjectId().endsWith("_1"));
      assertTrue(checkinDocument.getObjectId().endsWith("_2"));
      assertTrue(checkinDocument2.getObjectId().endsWith("_3"));
      
      document = (DocumentData)storageA.getObjectById(document.getObjectId());
      storageA.deleteObject(document, false);
      
      try {
         storageA.getObjectById(document.getObjectId());
         fail("Should be no document version '" + document.getObjectId() + "'");
      } catch (Exception e) {
         // OK, the version '1' was removed
      }

      try {
         storageA.getObjectById(checkinDocument.getObjectId());
      } catch (Exception e) {
         fail("Should be the document version '3' available \n" + e.getMessage());
      }
      try {
         storageA.getObjectById(checkinDocument2.getObjectId());
      } catch (Exception e) {
         fail("Should be the document version '3' available \n" + e.getMessage());
      }
   }

   public void testCreateDocument() throws Exception
   {
      PropertyDefinition<?> nameDef = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(nameDef.getId(), nameDef.getQueryName(),
         nameDef.getLocalName(), nameDef.getDisplayName(), "createDocumentTest"));
      PropertyDefinition<?> contentNameDef = PropertyDefinitions.getPropertyDefinition("cmis:document",
         CmisConstants.CONTENT_STREAM_FILE_NAME);
      properties.put(CmisConstants.CONTENT_STREAM_FILE_NAME, new StringProperty(contentNameDef.getId(),
         contentNameDef.getQueryName(), contentNameDef.getLocalName(), contentNameDef.getDisplayName(),
         "createDocumentTest_ContentFile.txt"));

      ContentStream cs =
         new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text",
            "plain"));
      AccessControlEntry ace =
         new AccessControlEntry("root", new HashSet<String>(Arrays.asList("cmis:read", "cmis:write")));

      DocumentData document =
         storageA.createDocument(rootFolder, documentTypeDefinition, properties, cs, Arrays.asList(ace), null,
            VersioningState.MAJOR);

      assertTrue(itemExistsInStorage(storageA, "/createDocumentTest", false));
      Node documentNode = getNodeFromStorage(storageA, "/createDocumentTest", false);

      // check content.
      assertEquals("nt:file", documentNode.getPrimaryNodeType().getName());
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      // check permissions
      List<String> permissions = ((ExtendedNode)documentNode).getACL().getPermissions("root");
      assertTrue(permissions.size() > 0); // ACL applied to back-end node.
      System.out.println("root: " + permissions);

      // CMIS properties
      assertEquals(true, document.isLatestVersion());
      assertEquals(true, document.isMajorVersion());
      assertEquals(true, document.isLatestMajorVersion());
      assertEquals("root", document.getCreatedBy());
      assertEquals("root", document.getLastModifiedBy());
      assertNotNull(document.getCreationDate());

      Calendar c = document.getCreationDate();
      DateFormat ISO_8601_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
      String creationDate = ISO_8601_DATE_TIME.format(c.getTime());
      c = Calendar.getInstance();
      String expectedDate = ISO_8601_DATE_TIME.format(c.getTime());
      
      assertEquals(expectedDate, creationDate);
      
      assertNotNull(document.getLastModificationDate());
      assertEquals(documentNode.getVersionHistory().getUUID(), document.getVersionSeriesId());
      assertNull(document.getVersionSeriesCheckedOutBy());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertFalse(document.isVersionSeriesCheckedOut());
      assertEquals("latest", document.getVersionLabel());
      assertEquals("text/plain", document.getContentStreamMimeType());
      assertEquals("createDocumentTest_ContentFile.txt", document.getContentStream().getFileName());
   }

   public void testCreateDocumentFromSource() throws Exception
   {
      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "createDocumentSource", "cmis:document", cs, null);

      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), "createDocumentSourceCopy"));

      DocumentData documentCopy =
         storageA.copyDocument(document, rootFolder, properties, null, null, VersioningState.MINOR);

      // Check is node and content copied.
      assertTrue(itemExistsInStorage(storageA, "/createDocumentSourceCopy", false));
      Node documentNode = getNodeFromStorage(storageA, "/createDocumentSourceCopy", false);
      assertEquals("nt:file", documentNode.getPrimaryNodeType().getName());
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      assertFalse("Copy must have different name.", document.getName().equals(documentCopy.getName()));
      assertFalse("Copy must have different ID.", document.getObjectId().equals(documentCopy.getObjectId()));
      assertFalse("Copy must have different versionSeriesId.", document.getVersionSeriesId().equals(
         documentCopy.getVersionSeriesId()));
      assertFalse(documentCopy.isMajorVersion());
   }

   public void testCreateFolder() throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:folder", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), "createFolderTest"));

      storageA.createFolder(rootFolder, folderTypeDefinition, properties, null, null);

      assertTrue(itemExistsInStorage(storageA, "/createFolderTest", false));
      Node folderNode = getNodeFromStorage(storageA, "/createFolderTest", false);
      assertEquals("nt:folder", folderNode.getPrimaryNodeType().getName());
   }

   public void testCreatePolicy() throws Exception
   {
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();

      PropertyDefinition<?> defName = PropertyDefinitions.getPropertyDefinition("cmis:policy", CmisConstants.NAME);
      properties.put(CmisConstants.NAME, new StringProperty(defName.getId(), defName.getQueryName(), defName
         .getLocalName(), defName.getDisplayName(), "createPolicyTest"));

      PropertyDefinition<?> defPolicyText =
         PropertyDefinitions.getPropertyDefinition("cmis:policy", CmisConstants.POLICY_TEXT);
      properties.put(CmisConstants.POLICY_TEXT, new StringProperty(defPolicyText.getId(), defPolicyText.getQueryName(),
         defPolicyText.getLocalName(), defPolicyText.getDisplayName(), "simple policy"));

      storageA.createPolicy(rootFolder, policyTypeDefinition, properties, null, null);

      String expectedPath = StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_POLICIES + "/createPolicyTest";
      assertTrue(itemExistsInStorage(storageA, expectedPath, true));
      Node policyNode = getNodeFromStorage(storageA, expectedPath, true);

      assertEquals("cmis:policy", policyNode.getPrimaryNodeType().getName());
      assertEquals("simple policy", policyNode.getProperty("cmis:policyText").getString());
   }

   public void testCreateRelationship() throws Exception
   {
      ObjectData sourceDoc = createDocument(rootFolder, "createRelationshipSource", "cmis:document", null, null);
      ObjectData targetDoc = createDocument(rootFolder, "createRelationshipTarget", "cmis:document", null, null);

      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      PropertyDefinition<?> defName =
         PropertyDefinitions.getPropertyDefinition("cmis:relationship", CmisConstants.NAME);
      properties.put(CmisConstants.NAME, new StringProperty(defName.getId(), defName.getQueryName(), defName
         .getLocalName(), defName.getDisplayName(), "createRelationshipTest"));

      RelationshipData relationship =
         storageA.createRelationship(sourceDoc, targetDoc, relationshipTypeDefinition, properties, null, null);

      Node relationshipNode = getNodeByIdentifierFromStorage(storageA, relationship.getObjectId());
      assertEquals("cmis:relationship", relationshipNode.getPrimaryNodeType().getName());
      assertEquals(sourceDoc.getObjectId(), relationship.getSourceId());
      assertEquals(targetDoc.getObjectId(), relationship.getTargetId());
   }

   public void testDeleteContent() throws Exception
   {
      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "removeContentTest", "cmis:document", cs, null);
      Node documentNode = getNodeFromStorage(storageA, "/removeContentTest", false);
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      document.setContentStream(null);

      documentNode = getNodeFromStorage(storageA, "/removeContentTest", false);
      assertEquals("", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("", documentNode.getProperty("jcr:content/jcr:mimeType").getString());
   }

   public void testDeleteDocument() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "deleteDocumentTest", "cmis:document", null, null);
      storageA.deleteObject(document, true);
      assertFalse(itemExistsInStorage(storageA, "/deleteDocumentTest", false));
   }

   public void testDeleteFolder() throws Exception
   {
      FolderData folder = createFolder(rootFolder, "deleteFolderTest", "cmis:folder");
      storageA.deleteObject(folder, true);
      assertFalse(itemExistsInStorage(storageA, "/deleteFolderTest", false));
   }

   public void testDeleteMultifiledObject() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "deleteMultifiledTest", "cmis:document", null, null);

      FolderData folder1 = createFolder(rootFolder, "deleteMultifiledTest01", "cmis:folder");
      FolderData folder2 = createFolder(rootFolder, "deleteMultifiledTest02", "cmis:folder");
      FolderData folder3 = createFolder(rootFolder, "deleteMultifiledTest03", "cmis:folder");
      folder1.addObject(document);
      folder2.addObject(document);
      folder3.addObject(document);

      assertTrue(folder1.getChildren(null).hasNext());
      assertTrue(folder2.getChildren(null).hasNext());
      assertTrue(folder3.getChildren(null).hasNext());
      assertTrue(itemExistsInStorage(storageA, "/deleteMultifiledTest", false));

      storageA.deleteObject(document, true);

      assertFalse(folder1.getChildren(null).hasNext());
      assertFalse(folder2.getChildren(null).hasNext());
      assertFalse(folder3.getChildren(null).hasNext());
      assertFalse(itemExistsInStorage(storageA, "/deleteMultifiledTest", false));
   }
   
   // Unsupported unfiling.
   //   public void testCreateDocumentUnfiled() throws Exception
   //   {
   //      DocumentData document = createDocument(null, "createUnfiledDocumentTest", "cmis:document", null, null);
   //
   //      Node docNode = ((DocumentDataImpl)document).getNodeEntry().getNode();
   //      String path = docNode.getPath();
   //      assertTrue("Document must be created in unfiled store.", path.startsWith(StorageImpl.XCMIS_SYSTEM_PATH + "/"
   //         + StorageImpl.XCMIS_UNFILED));
   //
   //      Collection<FolderData> parents = document.getParents();
   //      assertEquals(0, parents.size());
   //
   //      // Add document in root folder.
   //      rootFolder.addObject(document);
   //      parents = document.getParents();
   //      assertEquals(1, parents.size());
   //      assertEquals(rootFolder.getObjectId(), parents.iterator().next().getObjectId());
   //   }
   //
   // CmisRuntimeException: Unable remove object from last folder in which it is filed.
   //   public void testDeleteUnfiledDocument() throws Exception
   //   {
   //      DocumentData document = createDocument(rootFolder, "deleteUnfiledTest", "cmis:document", null, null);
   //      rootFolder.removeObject(document);
   //      assertEquals(0, document.getParents().size());
   //      assertTrue(((FolderDataImpl)rootFolder).getNodeEntry().getNode().getNode("xcmis:system/xcmis:unfileStore").getNodes().hasNext());
   //      storageA.deleteObject(document, true);
   //      // wrapper node must be removed
   //      assertFalse(((FolderDataImpl)rootFolder).getNodeEntry().getNode().getNode("xcmis:system/xcmis:unfileStore").getNodes().hasNext());
   //   }

   public void testDeleteObjectWithRelationship() throws Exception
   {
      ObjectData sourceDoc =
         createDocument(rootFolder, "deleteObjectWithRelationship_Source", "cmis:document", null, null);
      ObjectData targetDoc =
         createDocument(rootFolder, "deleteObjectWithRelationship_Target", "cmis:document", null, null);

      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      PropertyDefinition<?> defName =
         PropertyDefinitions.getPropertyDefinition("cmis:relationship", CmisConstants.NAME);
      properties.put(CmisConstants.NAME, new StringProperty(defName.getId(), defName.getQueryName(), defName
         .getLocalName(), defName.getDisplayName(), "relationship01"));

      storageA.createRelationship(sourceDoc, targetDoc, relationshipTypeDefinition, properties, null, null);

      try
      {
         storageA.deleteObject(targetDoc, true);
         fail("StorageException should be thrown");
      }
      catch (StorageException e)
      {
         // OK
         System.out.println(e.getMessage());
      }
   }

   public void testDeletePolicy() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "deletePolicyTestDocument", "cmis:document", null, null);
      PolicyData policy = createPolicy(rootFolder, "deletePolicyTestPolicy01", "test delete policy", "cmis:policy");
      document.applyPolicy(policy);
      try
      {
         storageA.deleteObject(policy, true);
         fail("StorageException should be thrown.");
      }
      catch (StorageException e)
      {
         // OK. Applied policy may not be deleted.
      }
      document.removePolicy(policy);

      // Should be able delete now.
      storageA.deleteObject(policy, true);
   }

   public void testDeleteRootFolder() throws Exception
   {
      try
      {
         storageA.deleteObject(rootFolder, true);
         fail("StorageException should be thrown");
      }
      catch (StorageException e)
      {
         // OK
      }
   }

   public void testDeleteTreeDelete() throws Exception
   {
      // Create tree.
      FolderData folder1 = createFolder(rootFolder, "1", "cmis:folder");
      FolderData folder2 = createFolder(folder1, "2", "cmis:folder");
      FolderData folder3 = createFolder(folder2, "3", "cmis:folder");
      FolderData folder4 = createFolder(folder3, "4", "cmis:folder");
      FolderData folder5 = createFolder(folder1, "5", "cmis:folder");
      FolderData folder6 = createFolder(folder5, "6", "cmis:folder");
      FolderData folder7 = createFolder(folder3, "7", "cmis:folder");
      DocumentData doc1 = createDocument(folder2, "doc1", "cmis:document", null, null);
      DocumentData doc2 = createDocument(folder2, "doc2", "cmis:document", null, null);
      DocumentData doc3 = createDocument(folder4, "doc3", "cmis:document", null, null);
      DocumentData doc4 = createDocument(folder4, "doc4", "cmis:document", null, null);

      folder5.addObject(doc1);
      folder6.addObject(doc2);
      folder7.addObject(doc3);
      folder7.addObject(doc4);

      String doc1Id = doc1.getObjectId();
      String doc2Id = doc2.getObjectId();
      String doc3Id = doc3.getObjectId();
      String doc4Id = doc4.getObjectId();

      //      /
      //      |_ 1
      //        |_2
      //        | |_doc1
      //        | |_doc2
      //        | |_3
      //        |   |_4
      //        |   | |_doc3
      //        |   | |_doc4
      //        |   |_7
      //        |     |_doc3
      //        |     |_doc4
      //        |_5
      //          |_6
      //          | |_doc2
      //          |_doc1

      //      printTree(folder1);

      storageA.deleteTree(folder2, true, UnfileObject.DELETE, true);

      // Expected result is
      //      /
      //      |_ 1
      //        |_5
      //          |_6

      try
      {
         doc1 = (DocumentData)storageA.getObjectById(doc1Id);
         fail(doc1 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      try
      {
         doc2 = (DocumentData)storageA.getObjectById(doc2Id);
         fail(doc2 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      try
      {
         doc3 = (DocumentData)storageA.getObjectById(doc3Id);
         fail(doc3 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      try
      {
         doc4 = (DocumentData)storageA.getObjectById(doc4Id);
         fail(doc4 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      //      printTree(folder1);
   }

   //   public void testDeleteTreeDeletesinglefiled() throws Exception
   //   {
   //      // Create tree.
   //      FolderData folder1 = createFolder(rootFolder, "1", "cmis:folder");
   //      FolderData folder2 = createFolder(folder1, "2", "cmis:folder");
   //      FolderData folder3 = createFolder(folder2, "3", "cmis:folder");
   //      FolderData folder4 = createFolder(folder3, "4", "cmis:folder");
   //      FolderData folder5 = createFolder(folder1, "5", "cmis:folder");
   //      FolderData folder6 = createFolder(folder5, "6", "cmis:folder");
   //      FolderData folder7 = createFolder(folder3, "7", "cmis:folder");
   //      DocumentData doc1 = createDocument(folder2, "doc1", "cmis:document", null, null);
   //      DocumentData doc2 = createDocument(folder2, "doc2", "cmis:document", null, null);
   //      DocumentData doc3 = createDocument(folder4, "doc3", "cmis:document", null, null);
   //      DocumentData doc4 = createDocument(folder4, "doc4", "cmis:document", null, null);
   //
   //      folder5.addObject(doc1);
   //      folder6.addObject(doc2);
   //      folder7.addObject(doc3);
   //      folder7.addObject(doc4);
   //
   //      String doc1Id = doc1.getObjectId();
   //      String doc2Id = doc2.getObjectId();
   //      String doc3Id = doc3.getObjectId();
   //      String doc4Id = doc4.getObjectId();
   //
   //      //      /
   //      //      |_ 1
   //      //        |_2
   //      //        | |_doc1
   //      //        | |_doc2
   //      //        | |_3
   //      //        |   |_4
   //      //        |   | |_doc3
   //      //        |   | |_doc4
   //      //        |   |_7
   //      //        |     |_doc3
   //      //        |     |_doc4
   //      //        |_5
   //      //          |_6
   //      //          | |_doc2
   //      //          |_doc1
   //
   //      //      printTree(folder1);
   //
   //      storageA.deleteTree(folder2, true, UnfileObject.DELETESINGLEFILED, true);
   //
   //      // Expected result is
   //      //      /
   //      //      |_ 1
   //      //        |_5
   //      //          |_6
   //      //          | |_doc2
   //      //          |_doc1
   //
   //      doc1 = (DocumentData)storageA.getObjectById(doc1Id);
   //      doc2 = (DocumentData)storageA.getObjectById(doc2Id);
   //      try
   //      {
   //         doc3 = (DocumentData)storageA.getObjectById(doc3Id);
   //         fail(doc3 + " must be deleted.");
   //      }
   //      catch (ObjectNotFoundException e)
   //      {
   //         //ok
   //      }
   //      try
   //      {
   //         doc4 = (DocumentData)storageA.getObjectById(doc4Id);
   //         fail(doc3 + " must be deleted.");
   //      }
   //      catch (ObjectNotFoundException e)
   //      {
   //         //ok
   //      }
   //
   //      Collection<FolderData> doc1Parents = doc1.getParents();
   //      assertEquals(1, doc1Parents.size());
   //      assertEquals(folder5.getObjectId(), doc1Parents.iterator().next().getObjectId());
   //      Collection<FolderData> doc2Parents = doc2.getParents();
   //      assertEquals(1, doc2Parents.size());
   //      assertEquals(folder6.getObjectId(), doc2Parents.iterator().next().getObjectId());
   //
   //      //      printTree(folder1);
   //   }

   //   public void testDeleteTreeUnfile() throws Exception
   //   {
   //      // Create tree.
   //      FolderData folder1 = createFolder(rootFolder, "1", "cmis:folder");
   //      FolderData folder2 = createFolder(folder1, "2", "cmis:folder");
   //      FolderData folder3 = createFolder(folder2, "3", "cmis:folder");
   //      FolderData folder4 = createFolder(folder3, "4", "cmis:folder");
   //      FolderData folder5 = createFolder(folder1, "5", "cmis:folder");
   //      FolderData folder6 = createFolder(folder5, "6", "cmis:folder");
   //      FolderData folder7 = createFolder(folder3, "7", "cmis:folder");
   //      DocumentData doc1 = createDocument(folder2, "doc1", "cmis:document", null, null);
   //      DocumentData doc2 = createDocument(folder2, "doc2", "cmis:document", null, null);
   //      DocumentData doc3 = createDocument(folder4, "doc3", "cmis:document", null, null);
   //      DocumentData doc4 = createDocument(folder4, "doc4", "cmis:document", null, null);
   //
   //      folder5.addObject(doc1);
   //      folder6.addObject(doc2);
   //      folder7.addObject(doc3);
   //      folder7.addObject(doc4);
   //
   //      String doc1Id = doc1.getObjectId();
   //      String doc2Id = doc2.getObjectId();
   //      String doc3Id = doc3.getObjectId();
   //      String doc4Id = doc4.getObjectId();
   //
   //      //      /
   //      //      |_ 1
   //      //        |_2
   //      //        | |_doc1
   //      //        | |_doc2
   //      //        | |_3
   //      //        |   |_4
   //      //        |   | |_doc3
   //      //        |   | |_doc4
   //      //        |   |_7
   //      //        |     |_doc3
   //      //        |     |_doc4
   //      //        |_5
   //      //          |_6
   //      //          | |_doc2
   //      //          |_doc1
   //
   //      //      printTree(folder1);
   //
   //      storageA.deleteTree(folder2, true, UnfileObject.UNFILE, true);
   //
   //      // Expected result is
   //      //      /
   //      //      |_ 1
   //      //        |_5
   //      //          |_6
   //      //          | |_doc2
   //      //          |_doc1
   //      // doc3 <unfiled>
   //      // doc4 <unfiled>
   //
   //      doc1 = (DocumentData)storageA.getObjectById(doc1Id);
   //      doc2 = (DocumentData)storageA.getObjectById(doc2Id);
   //      doc3 = (DocumentData)storageA.getObjectById(doc3Id);
   //      doc4 = (DocumentData)storageA.getObjectById(doc4Id);
   //
   //      Collection<FolderData> doc1Parents = doc1.getParents();
   //      assertEquals(1, doc1Parents.size());
   //      assertEquals(folder5.getObjectId(), doc1Parents.iterator().next().getObjectId());
   //      Collection<FolderData> doc2Parents = doc2.getParents();
   //      assertEquals(1, doc2Parents.size());
   //      assertEquals(folder6.getObjectId(), doc2Parents.iterator().next().getObjectId());
   //      Collection<FolderData> doc3Parents = doc3.getParents();
   //      assertEquals(0, doc3Parents.size());
   //      Collection<FolderData> doc4Parents = doc4.getParents();
   //      assertEquals(0, doc4Parents.size());
   //
   //      //      printTree(folder1);
   //   }

   public void testGetParent() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getParentTest", "cmis:document", null, null);
      assertEquals(rootFolder.getObjectId(), document.getParent().getObjectId());
   }

   public void testGetParents() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getParentsTest", "cmis:document", null, null);
      Collection<FolderData> parents = document.getParents();
      assertEquals(1, parents.size());
      assertEquals(rootFolder.getObjectId(), parents.iterator().next().getObjectId());
      FolderData folder = createFolder(rootFolder, "getParentsTestFolder01", "cmis:folder");
      folder.addObject(document);
      parents = document.getParents();
      assertEquals(2, parents.size());
      try
      {
         document.getParent();
         fail("ConstraintException should be thrown. Object has more then one parent.");
      }
      catch (ConstraintException e)
      {
         // OK. Object has more then one parent.
      }
   }

   public void testGetRootParent() throws Exception
   {
      try
      {
         rootFolder.getParent();
         fail("ConstraintException must be throw. No parent for root folder.");
      }
      catch (ConstraintException ce)
      {
         // OK
      }
   }

   public void testGetRootParents() throws Exception
   {
      assertTrue("Must be empty collection.", rootFolder.getParents().isEmpty());
   }

   public void testGetTypeChildren() throws Exception
   {
      ItemsIterator<TypeDefinition> iterator = storageA.getTypeChildren(null, true);
      List<String> result = new ArrayList<String>();
      while (iterator.hasNext())
      {
         TypeDefinition next = iterator.next();
         result.add(next.getId() + "," + next.getLocalName());
      }
      assertEquals(4, result.size());
      assertTrue(result.contains("cmis:document,nt:file"));;
      assertTrue(result.contains("cmis:folder,nt:folder"));;
      assertTrue(result.contains("cmis:policy,cmis:policy"));;
      assertTrue(result.contains("cmis:relationship,cmis:relationship"));;
   }

   public void testMoveDocument() throws Exception
   {
      ObjectData document = createDocument(rootFolder, "moveDocumentTest", "cmis:document", null, null);
      FolderData targetFolder = createFolder(rootFolder, "moveDocumentTestDestination", "cmis:folder");

      assertTrue(itemExistsInStorage(storageA, "/moveDocumentTest", false));
      assertFalse(itemExistsInStorage(storageA, "/moveDocumentTestDestination/moveDocumentTest", false));
      storageA.moveObject(document, targetFolder, rootFolder);
      assertFalse(itemExistsInStorage(storageA, "/moveDocumentTest", false));
      assertTrue(itemExistsInStorage(storageA, "/moveDocumentTestDestination/moveDocumentTest", false));
   }

   public void testMoveFolder() throws Exception
   {
      FolderData folder = createFolder(rootFolder, "moveFolderTest", "cmis:folder");
      createDocument(folder, "childDocument", "cmis:document", null, null);
      FolderData targetFolder = createFolder(rootFolder, "moveFolderTestDestination", "cmis:folder");

      assertTrue(itemExistsInStorage(storageA, "/moveFolderTest/childDocument", false));
      assertTrue(itemExistsInStorage(storageA, "/moveFolderTest", false));
      assertFalse(itemExistsInStorage(storageA, "/moveFolderTestDestination/moveFolderTest/childDocument", false));
      assertFalse(itemExistsInStorage(storageA, "/moveFolderTestDestination/moveFolderTest", false));
      storageA.moveObject(folder, targetFolder, rootFolder);
      assertFalse(itemExistsInStorage(storageA, "/moveFolderTest/childDocument", false));
      assertFalse(itemExistsInStorage(storageA, "/moveFolderTest", false));
      assertTrue(itemExistsInStorage(storageA, "/moveFolderTestDestination/moveFolderTest", false));
      assertTrue(itemExistsInStorage(storageA, "/moveFolderTestDestination/moveFolderTest/childDocument", false));
   }

   public void testMultifiledChild() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "multifiledChildTest", "cmis:document", null, null);
      FolderData folder1 = createFolder(rootFolder, "multifiledChildFolderTest01", "cmis:folder");
      DocumentData child1 = createDocument(folder1, "child1", "cmis:document", null, null);

      List<String> chs = new ArrayList<String>();
      for (ItemsIterator<ObjectData> children = folder1.getChildren(null); children.hasNext();)
      {
         chs.add(children.next().getObjectId());
      }
      assertEquals(1, chs.size());

      folder1.addObject(document);

      chs.clear();
      for (ItemsIterator<ObjectData> children = folder1.getChildren(null); children.hasNext();)
      {
         chs.add(children.next().getObjectId());
      }

      assertEquals(2, chs.size());
   }

   public void testMultifiling() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "multifilingDocumentTest", "cmis:document", null, null);
      FolderData folder1 = createFolder(rootFolder, "multifilingFolderTest1", "cmis:folder");
      FolderData folder2 = createFolder(rootFolder, "multifilingFolderTest2", "cmis:folder");
      FolderData folder3 = createFolder(rootFolder, "multifilingFolderTest3", "cmis:folder");
      FolderData folder4 = createFolder(rootFolder, "multifilingFolderTest4", "cmis:folder");
      folder1.addObject(document);
      folder2.addObject(document);
      folder3.addObject(document);
      folder4.addObject(document);

      Set<String> expectedParents =
         new HashSet<String>(Arrays.asList(rootFolder.getObjectId(), folder1.getObjectId(), folder2.getObjectId(),
            folder3.getObjectId(), folder4.getObjectId()));
      Collection<FolderData> parents = document.getParents();

      assertEquals(expectedParents.size(), parents.size());
      for (FolderData f : parents)
      {
         assertTrue("Folder " + f.getObjectId() + " must be in parents list.", expectedParents
            .contains(f.getObjectId()));
      }

      // remove from three folders and check parents again
      folder1.removeObject(document);
      folder3.removeObject(document);
      rootFolder.removeObject(document);
      expectedParents = new HashSet<String>(Arrays.asList(folder2.getObjectId(), folder4.getObjectId()));

      parents = document.getParents();

      assertEquals(expectedParents.size(), parents.size());
      for (FolderData f : parents)
      {
         assertTrue("Folder " + f.getObjectId() + " must be in parents list.", expectedParents
            .contains(f.getObjectId()));
      }
      System.out.println(" StorageTest.testMultifiling > new location: "
         + ((DocumentDataImpl)document).getNodeEntry().getNode().getPath());
   }

   public void testGetMultifiledByPath() throws Exception
   {
      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "multifiledByPathTest", "cmis:document", cs, null);
      FolderData folder1 = createFolder(rootFolder, "multifiledByPathTest1", "cmis:folder");
      FolderData folder2 = createFolder(rootFolder, "multifiledByPathTest2", "cmis:folder");
      folder1.addObject(document);
      folder2.addObject(document);

      DocumentData doc = (DocumentData)storageA.getObjectByPath("/multifiledByPathTest2/multifiledByPathTest");
      assertEquals(cs.length(), doc.getContentStream().length());
   }

   public void testRenameDocument() throws Exception
   {
      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "renameDocumentTest", "cmis:document", cs, null);
      setProperty(document, new StringProperty(CmisConstants.NAME, CmisConstants.NAME, CmisConstants.NAME,
         CmisConstants.NAME, "renameDocumentTest01"));

      assertTrue(itemExistsInStorage(storageA, "/renameDocumentTest01", false));

      assertEquals("renameDocumentTest01", document.getName());
      assertEquals("renameDocumentTest01", document.getProperty(CmisConstants.CONTENT_STREAM_FILE_NAME).getValues()
         .get(0));
   }

   public void testRenameFolder() throws Exception
   {
      FolderData folder = createFolder(rootFolder, "renameFolderTest", "cmis:folder");
      createDocument(folder, "child1", "cmis:document", null, null);
      setProperty(folder, new StringProperty(CmisConstants.NAME, CmisConstants.NAME, CmisConstants.NAME,
         CmisConstants.NAME, "renameFolderTest01"));

      assertTrue(itemExistsInStorage(storageA, "/renameFolderTest01", false));
      assertTrue(itemExistsInStorage(storageA, "/renameFolderTest01/child1", false));

      assertEquals("renameFolderTest01", folder.getName());
   }

   public void testSetContent() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "setContentTest", "cmis:document", null, null);
      Node documentNode = getNodeFromStorage(storageA, "/setContentTest", false);
      assertEquals("", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      document.setContentStream(cs);

      documentNode = getNodeFromStorage(storageA, "/setContentTest", false);
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());
   }
   
   
   
   
   
   
   
   /*
      public void testUnfileAll() throws Exception
      {
         DocumentData document = createDocument(rootFolder, "unfilingDocumentAllTest", "cmis:document", null, null);
   
         FolderData folder1 = createFolder(rootFolder, "unfilingFolderAllTest01", "cmis:folder");
         FolderData folder2 = createFolder(rootFolder, "unfilingFolderAllTest02", "cmis:folder");
         FolderData folder3 = createFolder(rootFolder, "unfilingFolderAllTest03", "cmis:folder");
         folder1.addObject(document);
         folder2.addObject(document);
         folder3.addObject(document);
   
         assertEquals(4, document.getParents().size());
         storageA.unfileObject(document);
         assertNull(document.getParent());
         assertEquals(0, document.getParents().size());
      }

      public void testUnfiling() throws Exception
      {
         assertEquals(0, getSize(storageA.getUnfiledObjectsId()));
         DocumentData document = createDocument(rootFolder, "unfilingDocumentTest", "cmis:document", null, null);
         assertTrue(rootFolder.getChildren(null).hasNext());
         rootFolder.removeObject(document);
         assertFalse(rootFolder.getChildren(null).hasNext());
   
         assertFalse(itemExistsInCurrentDrive(storageA,"/unfilingDocumentTest"));
   
         Collection<FolderData> parents = document.getParents();
         assertEquals(0, parents.size());
         storageA.getObjectById(document.getObjectId());
   
         assertEquals(1, getSize(storageA.getUnfiledObjectsId()));
      }

   private int getSize(Iterator<String> iterator)
   {
      int result = 0;

      while (iterator.hasNext())
      {
         iterator.next();
         result++;
      }
      return result;
   }

   private void printTree(FolderData folder) throws Exception
   {
      System.out.println("--------- TREE --------");
      System.out.println(folder.getPath());
      ((FolderDataImpl)folder).entry.node.accept(new ItemVisitor()
      {
         int l = 0;

         public void visit(javax.jcr.Property property) throws RepositoryException
         {
         }

         public void visit(Node node) throws RepositoryException
         {
            l++;
            for (int i = 0; i < l; i++)
            {
               System.out.print("  ");
            }
            System.out.println(node.getName() + " <" + node.getPrimaryNodeType().getName() + ">");
            for (NodeIterator children = node.getNodes(); children.hasNext();)
            {
               children.nextNode().accept(this);
            }
            l--;
         }
      });

      // Unfiled storage
      System.out.println("------- UNFILED -------");
      for (NodeIterator iter =
         (getNodeFromCurrentDrive(storageA,StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED)).getNodes(); iter
         .hasNext();)
      {
         for (NodeIterator iterator = iter.nextNode().getNodes(); iterator.hasNext();)
         {
            System.out.println(iterator.nextNode().getPath());
         }
      }
      System.out.println("-----------------------");
   }
   */



   protected PolicyDataImpl createPolicy(FolderData folder, String name, String policyText, String typeId)
      throws Exception
   {
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();

      PropertyDefinition<?> defName = PropertyDefinitions.getPropertyDefinition("cmis:policy", CmisConstants.NAME);
      properties.put(CmisConstants.NAME, new StringProperty(defName.getId(), defName.getQueryName(), defName
         .getLocalName(), defName.getDisplayName(), name));

      PropertyDefinition<?> defPolicyText =
         PropertyDefinitions.getPropertyDefinition("cmis:policy", CmisConstants.POLICY_TEXT);
      properties.put(CmisConstants.POLICY_TEXT, new StringProperty(defPolicyText.getId(), defPolicyText.getQueryName(),
         defPolicyText.getLocalName(), defPolicyText.getDisplayName(), policyText));

      PolicyData policy = storageA.createPolicy(folder, policyTypeDefinition, properties, null, null);

      return (PolicyDataImpl)policy;
   }

   //   public void testCreateAiim() throws Exception
   //   {
   //      createAiimDocument(rootFolder, "aiim01", null);
   //   }
   //
   //   protected DocumentData createAiimDocument(FolderData folder, String name, ContentStream content) throws Exception
   //   {
   //      DocumentData document = storageA.createDocument(folder, "aiim_2010demo", VersioningState.MAJOR);
   //      document.setName(name);
   //      document.setProperty(new StringProperty("aiim_Ioinc", "aiim_Ioinc", "aiim_Ioinc", "aiim_Ioinc",
   //         "Consultation Note 11488-4"));
   //      document.setProperty(new StringProperty("aiim_procedure", "aiim_procedure", "aiim_procedure", "aiim_procedure",
   //         "Cystoscopy 24139008"));
   //
   //      document.setContentStream(content);
   //      for (Map.Entry<String, Property<?>> p : document.getProperties().entrySet())
   //      {
   //         System.out.println(p.getKey()+ ": " + p.getValue().getValues());
   //      }
   //      return document;
   //   }
   
   private boolean itemExistsInStorage(StorageImpl storage, String nodePath, boolean isSystem)
      throws RepositoryException, RepositoryConfigurationException
   {
      return getJcrSession(storage.getStorageConfiguration().getRepository(),
         storage.getStorageConfiguration().getWorkspace()).itemExists(
         (isSystem ? "" : storage.getJcrRootPath()) + nodePath);
   }

   private Node getNodeFromStorage(StorageImpl storage, String nodePath, boolean isSystem)
      throws PathNotFoundException, RepositoryException, RepositoryConfigurationException
   {

      return (Node)getJcrSession(storage.getStorageConfiguration().getRepository(),
         storage.getStorageConfiguration().getWorkspace()).getItem(
         (isSystem ? "" : storage.getJcrRootPath()) + nodePath);
   }

   private Node getNodeByIdentifierFromStorage(StorageImpl storage, String nodeId) throws PathNotFoundException,
      RepositoryException, RepositoryConfigurationException
   {

      return (Node)getJcrSession(storage.getStorageConfiguration().getRepository(),
         storage.getStorageConfiguration().getWorkspace()).getNodeByUUID(nodeId);
   }
   
}
