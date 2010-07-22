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

package org.xcmis.sp.jcr.exo;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.Storage;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: StorageTest.java 1192 2010-05-27 14:11:42Z
 *          alexey.zavizionov@gmail.com $
 */
public class StorageTest extends BaseTest
{

   protected Storage storage;

   protected FolderData rootFolder;

   protected TypeDefinition documentTypeDefinition;

   protected TypeDefinition folderTypeDefinition;

   protected TypeDefinition policyTypeDefinition;

   protected TypeDefinition relationshipTypeDefinition;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      storage = storageProvider.getConnection().getStorage();
      rootFolder = (FolderData)storage.getObjectById(JcrCMIS.ROOT_FOLDER_ID);

      documentTypeDefinition = storage.getTypeDefinition("cmis:document", true);
      folderTypeDefinition = storage.getTypeDefinition("cmis:folder", true);
      policyTypeDefinition = storage.getTypeDefinition("cmis:policy", true);
      relationshipTypeDefinition = storage.getTypeDefinition("cmis:relationship", true);
   }

   public void testApplyACL() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "applyACLTestDocument", "cmis:document", null, null);
      AccessControlEntry ace =
         new AccessControlEntry("root", new HashSet<String>(Arrays.asList("cmis:read", "cmis:write")));
      document.setACL(Arrays.asList(ace));

      Node documentNode = (Node)session.getItem("/applyACLTestDocument");
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

      Node documentNode = (Node)session.getItem("/applyPolicyTestDocument");
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

      assertTrue(document.isVersionSeriesCheckedOut());
      assertTrue(pwc.isVersionSeriesCheckedOut());
      assertEquals(document.getVersionSeriesId(), pwc.getVersionSeriesId());
      assertEquals(document.getVersionSeriesCheckedOutId(), pwc.getObjectId());
      assertNotNull(document.getVersionSeriesCheckedOutBy());
      assertNotNull(pwc.getVersionSeriesCheckedOutBy());

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

   public void testCheckIn() throws Exception
   {
      DocumentData document =
         createDocument(rootFolder, "checkinTest", "cmis:document", new BaseContentStream("checkin test".getBytes(),
            null, new MimeType("text", "plain")), null);
      DocumentData pwc = document.checkout();
      String pwcId = pwc.getObjectId();

      // Get PWC from storage
      pwc = (DocumentData)storage.getObjectById(pwcId);

      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));
      //      pwc.setContentStream(cs);
      pwc.checkin(true, "my comment", null, cs, null, null);

      try
      {
         storage.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }

      assertFalse(document.isVersionSeriesCheckedOut());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertNull(document.getVersionSeriesCheckedOutBy());
      assertEquals("my comment", document.getProperty(CmisConstants.CHECKIN_COMMENT).getValues().get(0));

      // check content
      byte[] b = new byte[128];
      int r = document.getContentStream().getStream().read(b);
      assertEquals("checkin test. content updated", new String(b, 0, r));
   }

   public void testCheckInRename() throws Exception
   {
      DocumentDataImpl document =
         createDocument(rootFolder, "checkinTestRename", "cmis:document", new BaseContentStream("checkin test"
            .getBytes(), null, new MimeType("text", "plain")), null);
      DocumentDataImpl pwc = (DocumentDataImpl)document.checkout();
      String pwcId = pwc.getObjectId();

      // update
      pwc = (DocumentDataImpl)storage.getObjectById(pwcId);

      ContentStream cs =
         new BaseContentStream("checkin test. content updated".getBytes(), null, new MimeType("text", "plain"));

      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(CmisConstants.NAME, CmisConstants.NAME, CmisConstants.NAME,
         CmisConstants.NAME, "checkinTestRename_NewName"));
      pwc.checkin(true, "my comment", properties, cs, null, null);

      try
      {
         storage.getObjectById(pwcId);
         fail("PWC must be removed.");
      }
      catch (ObjectNotFoundException e)
      {
         // OK
      }

      //document = (DocumentDataImpl)storage.getObjectById(document.getObjectId());
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
         storage.getObjectById(pwcId);
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
      pwc = (DocumentData)storage.getObjectById(pwcId);

      // Call cancel checkout on PWC.
      pwc.cancelCheckout();

      try
      {
         storage.getObjectById(pwcId);
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
      pwc = (DocumentData)storage.getObjectById(pwcId);

      // Delete PWC.
      storage.deleteObject(pwc, true);

      try
      {
         storage.getObjectById(pwcId);
         fail("PWC mus be removed.");
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
      Collection<DocumentData> allVersions = storage.getAllVersions(versionSeriesId);
      assertEquals(1, allVersions.size());
      assertEquals(document.getObjectId(), allVersions.iterator().next().getObjectId());
   }

   public void testGetAllVersionsWithPwc() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getAllVersionsPwcTest", "cmis:document", null, null);
      String versionSeriesId = document.getVersionSeriesId();
      DocumentData pwc = document.checkout();
      Collection<DocumentData> allVersions = storage.getAllVersions(versionSeriesId);
      assertEquals(2, allVersions.size());
      Iterator<DocumentData> vi = allVersions.iterator();
      assertEquals(pwc.getObjectId(), vi.next().getObjectId());
      assertEquals(document.getObjectId(), vi.next().getObjectId());
   }

   public void testGetAllVersionsPwcVersions() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "getAllVersionsPwcVersionsTest", "cmis:document", null, null);
      String versionSeriesId = document.getVersionSeriesId();

      DocumentData pwc = document.checkout();

      pwc.checkin(true, "", null, null, null, null);

      pwc = document.checkout();

      Collection<DocumentData> allVersions = storage.getAllVersions(versionSeriesId);
      assertEquals(3, allVersions.size());

      Iterator<DocumentData> vi = allVersions.iterator();
      assertEquals(pwc.getObjectId(), vi.next().getObjectId());
      assertEquals(document.getObjectId(), vi.next().getObjectId());
   }

   public void tesGetCheckedOutDocs() throws Exception
   {
      DocumentData document1 = createDocument(rootFolder, "getCheckedOutTest01", "cmis:document", null, null);
      FolderData folder = createFolder(rootFolder, "folderCheckedOutTest", "cmis:folder");
      DocumentData document2 = createDocument(folder, "getCheckedOutTest02", "cmis:document", null, null);
      DocumentData pwc1 = document1.checkout();
      DocumentData pwc2 = document2.checkout();

      List<String> r = new ArrayList<String>();
      // Should be both documents
      for (ItemsIterator<DocumentData> checkedOutDocs = storage.getCheckedOutDocuments(null, null); checkedOutDocs
         .hasNext();)
      {
         r.add(checkedOutDocs.next().getObjectId());
      }
      assertEquals(2, r.size());
      assertTrue(r.contains(pwc1.getObjectId()));
      assertTrue(r.contains(pwc2.getObjectId()));

      r.clear();

      // Should be only PWC "from" specified folder
      for (ItemsIterator<DocumentData> checkedOutDocs = storage.getCheckedOutDocuments(folder, null); checkedOutDocs
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

   public void testCreateDocument() throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), "createDocumentTest"));

      ContentStream cs =
         new BaseContentStream("to be or not to be".getBytes(), /*"createDocumentTest"*/null, new MimeType("text",
            "plain"));
      AccessControlEntry ace =
         new AccessControlEntry("root", new HashSet<String>(Arrays.asList("cmis:read", "cmis:write")));

      DocumentData document =
         storage.createDocument(rootFolder, documentTypeDefinition, properties, cs, Arrays.asList(ace), null,
            VersioningState.MAJOR);

      assertTrue(session.itemExists("/createDocumentTest"));
      Node documentNode = (Node)session.getItem("/createDocumentTest");

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
      assertNotNull(document.getLastModificationDate());
      assertEquals(documentNode.getVersionHistory().getUUID(), document.getVersionSeriesId());
      assertNull(document.getVersionSeriesCheckedOutBy());
      assertNull(document.getVersionSeriesCheckedOutId());
      assertFalse(document.isVersionSeriesCheckedOut());
      assertEquals("latest", document.getVersionLabel());
      assertEquals("text/plain", document.getContentStreamMimeType());
      assertEquals("createDocumentTest", document.getContentStream().getFileName());
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
         storage.copyDocument(document, rootFolder, properties, null, null, VersioningState.MINOR);

      // Check is node and content copied.
      assertTrue(session.itemExists("/createDocumentSourceCopy"));
      Node documentNode = (Node)session.getItem("/createDocumentSourceCopy");
      assertEquals("nt:file", documentNode.getPrimaryNodeType().getName());
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      assertFalse("Copy must have different name.", document.getName().equals(documentCopy.getName()));
      assertFalse("Copy must have different ID.", document.getObjectId().equals(documentCopy.getObjectId()));
      assertFalse("Copy must have different versionSeriesId.", document.getVersionSeriesId().equals(
         documentCopy.getVersionSeriesId()));
      assertFalse(documentCopy.isMajorVersion());
   }

   public void testCreateDocumentUnfiled() throws Exception
   {
      DocumentData document = createDocument(null, "createUnfiledDocumentTest", "cmis:document", null, null);

      Node docNode = ((DocumentDataImpl)document).getNode();
      String path = docNode.getPath();
      assertTrue("Document must be created in unfiled store.", path.startsWith(StorageImpl.XCMIS_SYSTEM_PATH + "/"
         + StorageImpl.XCMIS_UNFILED));

      Collection<FolderData> parents = document.getParents();
      assertEquals(0, parents.size());

      // Add document in root folder.
      rootFolder.addObject(document);
      parents = document.getParents();
      assertEquals(1, parents.size());
      assertEquals(rootFolder.getObjectId(), parents.iterator().next().getObjectId());
   }

   public void testCreateFolder() throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:folder", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), "createFolderTest"));

      FolderData newFolder = storage.createFolder(rootFolder, folderTypeDefinition, properties, null, null);

      assertTrue(session.itemExists("/createFolderTest"));
      Node folderNode = (Node)session.getItem("/createFolderTest");
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

      ObjectData policy = storage.createPolicy(rootFolder, policyTypeDefinition, properties, null, null);

      String expectedPath = StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_POLICIES + "/createPolicyTest";
      assertTrue(session.itemExists(expectedPath));
      Node policyNode = (Node)session.getItem(expectedPath);

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
         storage.createRelationship(sourceDoc, targetDoc, relationshipTypeDefinition, properties, null, null);

      Node relationshipNode = ((ExtendedSession)session).getNodeByIdentifier(relationship.getObjectId());
      assertEquals("cmis:relationship", relationshipNode.getPrimaryNodeType().getName());
      assertEquals(sourceDoc.getObjectId(), relationshipNode.getProperty("cmis:sourceId").getString());
      assertEquals(targetDoc.getObjectId(), relationshipNode.getProperty("cmis:targetId").getString());
   }

   public void testDeleteContent() throws Exception
   {
      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      DocumentData document = createDocument(rootFolder, "removeContentTest", "cmis:document", cs, null);
      Node documentNode = (Node)session.getItem("/removeContentTest");
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      document.setContentStream(null);

      documentNode = (Node)session.getItem("/removeContentTest");
      assertEquals("", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("", documentNode.getProperty("jcr:content/jcr:mimeType").getString());
   }

   public void testDeleteDocument() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "deleteDocumentTest", "cmis:document", null, null);
      storage.deleteObject(document, true);
      assertFalse(session.itemExists("/deleteDocumentTest"));
   }

   public void testDeleteFolder() throws Exception
   {
      FolderData folder = createFolder(rootFolder, "deleteFolderTest", "cmis:folder");
      storage.deleteObject(folder, true);
      assertFalse(session.itemExists("/deleteFolderTest"));
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
      assertTrue(session.itemExists("/deleteMultifiledTest"));

      storage.deleteObject(document, true);

      assertFalse(folder1.getChildren(null).hasNext());
      assertFalse(folder2.getChildren(null).hasNext());
      assertFalse(folder3.getChildren(null).hasNext());
      assertFalse(session.itemExists("/deleteMultifiledTest"));
   }

   public void testDeleteUnfiledDocument() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "deleteUnfiledTest", "cmis:document", null, null);
      rootFolder.removeObject(document);
      assertEquals(0, document.getParents().size());
      assertTrue(root.getNode("xcmis:system/xcmis:unfileStore").getNodes().hasNext());
      storage.deleteObject(document, true);
      // wrapper node must be removed
      assertFalse(root.getNode("xcmis:system/xcmis:unfileStore").getNodes().hasNext());
   }

   public void testDeleteObjectWithRelationship() throws Exception
   {
      ObjectData sourceDoc =
         createDocument(rootFolder, "deleteObjectWithRelationshipSource", "cmis:document", null, null);
      ObjectData targetDoc =
         createDocument(rootFolder, "deleteObjectWithRelationshipTarget", "cmis:document", null, null);

      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      PropertyDefinition<?> defName =
         PropertyDefinitions.getPropertyDefinition("cmis:relationship", CmisConstants.NAME);
      properties.put(CmisConstants.NAME, new StringProperty(defName.getId(), defName.getQueryName(), defName
         .getLocalName(), defName.getDisplayName(), "relationship01"));

      RelationshipData relationship =
         storage.createRelationship(sourceDoc, targetDoc, relationshipTypeDefinition, properties, null, null);

      try
      {
         storage.deleteObject(targetDoc, true);
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
         storage.deleteObject(policy, true);
         fail("StorageException should be thrown.");
      }
      catch (StorageException e)
      {
         // OK. Applied policy may not be deleted.
      }
      document.removePolicy(policy);

      // Should be able delete now.
      storage.deleteObject(policy, true);
   }

   public void testDeleteRootFolder() throws Exception
   {
      try
      {
         storage.deleteObject(rootFolder, true);
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

      storage.deleteTree(folder2, true, UnfileObject.DELETE, true);

      // Expected result is
      //      /
      //      |_ 1
      //        |_5
      //          |_6

      try
      {
         doc1 = (DocumentData)storage.getObjectById(doc1Id);
         fail(doc1 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      try
      {
         doc2 = (DocumentData)storage.getObjectById(doc2Id);
         fail(doc2 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      try
      {
         doc3 = (DocumentData)storage.getObjectById(doc3Id);
         fail(doc3 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      try
      {
         doc4 = (DocumentData)storage.getObjectById(doc4Id);
         fail(doc4 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         // ok
      }
      //      printTree(folder1);
   }

   public void testDeleteTreeDeletesinglefiled() throws Exception
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

      storage.deleteTree(folder2, true, UnfileObject.DELETESINGLEFILED, true);

      // Expected result is
      //      /
      //      |_ 1
      //        |_5
      //          |_6
      //          | |_doc2
      //          |_doc1

      doc1 = (DocumentData)storage.getObjectById(doc1Id);
      doc2 = (DocumentData)storage.getObjectById(doc2Id);
      try
      {
         doc3 = (DocumentData)storage.getObjectById(doc3Id);
         fail(doc3 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         //ok
      }
      try
      {
         doc4 = (DocumentData)storage.getObjectById(doc4Id);
         fail(doc3 + " must be deleted.");
      }
      catch (ObjectNotFoundException e)
      {
         //ok
      }

      Collection<FolderData> doc1Parents = doc1.getParents();
      assertEquals(1, doc1Parents.size());
      assertEquals(folder5.getObjectId(), doc1Parents.iterator().next().getObjectId());
      Collection<FolderData> doc2Parents = doc2.getParents();
      assertEquals(1, doc2Parents.size());
      assertEquals(folder6.getObjectId(), doc2Parents.iterator().next().getObjectId());

      //      printTree(folder1);
   }

   public void testDeleteTreeUnfile() throws Exception
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

      storage.deleteTree(folder2, true, UnfileObject.UNFILE, true);

      // Expected result is
      //      /
      //      |_ 1
      //        |_5
      //          |_6
      //          | |_doc2
      //          |_doc1
      // doc3 <unfiled>
      // doc4 <unfiled>

      doc1 = (DocumentData)storage.getObjectById(doc1Id);
      doc2 = (DocumentData)storage.getObjectById(doc2Id);
      doc3 = (DocumentData)storage.getObjectById(doc3Id);
      doc4 = (DocumentData)storage.getObjectById(doc4Id);

      Collection<FolderData> doc1Parents = doc1.getParents();
      assertEquals(1, doc1Parents.size());
      assertEquals(folder5.getObjectId(), doc1Parents.iterator().next().getObjectId());
      Collection<FolderData> doc2Parents = doc2.getParents();
      assertEquals(1, doc2Parents.size());
      assertEquals(folder6.getObjectId(), doc2Parents.iterator().next().getObjectId());
      Collection<FolderData> doc3Parents = doc3.getParents();
      assertEquals(0, doc3Parents.size());
      Collection<FolderData> doc4Parents = doc4.getParents();
      assertEquals(0, doc4Parents.size());

      //      printTree(folder1);
   }

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
      ItemsIterator<TypeDefinition> iterator = storage.getTypeChildren(null, true);
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

      assertTrue(session.itemExists("/moveDocumentTest"));
      assertFalse(session.itemExists("/moveDocumentTestDestination/moveDocumentTest"));
      storage.moveObject(document, targetFolder, rootFolder);
      assertFalse(session.itemExists("/moveDocumentTest"));
      assertTrue(session.itemExists("/moveDocumentTestDestination/moveDocumentTest"));
   }

   public void testMoveFolder() throws Exception
   {
      FolderData folder = createFolder(rootFolder, "moveFolderTest", "cmis:folder");
      createDocument(folder, "childDocument", "cmis:document", null, null);
      FolderData targetFolder = createFolder(rootFolder, "moveFolderTestDestination", "cmis:folder");

      assertTrue(session.itemExists("/moveFolderTest/childDocument"));
      assertTrue(session.itemExists("/moveFolderTest"));
      assertFalse(session.itemExists("/moveFolderTestDestination/moveFolderTest/childDocument"));
      assertFalse(session.itemExists("/moveFolderTestDestination/moveFolderTest"));
      storage.moveObject(folder, targetFolder, rootFolder);
      assertFalse(session.itemExists("/moveFolderTest/childDocument"));
      assertFalse(session.itemExists("/moveFolderTest"));
      assertTrue(session.itemExists("/moveFolderTestDestination/moveFolderTest"));
      assertTrue(session.itemExists("/moveFolderTestDestination/moveFolderTest/childDocument"));
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
         + ((DocumentDataImpl)document).getNode().getPath());
   }

   public void testRenameDocument() throws Exception
   {
      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      DocumentDataImpl document = createDocument(rootFolder, "renameDocumentTest", "cmis:document", cs, null);
      setProperty(document, new StringProperty(CmisConstants.NAME, CmisConstants.NAME, CmisConstants.NAME,
         CmisConstants.NAME, "renameDocumentTest01"));

      assertTrue(session.itemExists("/renameDocumentTest01"));

      assertEquals("renameDocumentTest01", document.getName());
      assertEquals("renameDocumentTest01", document.getProperty(CmisConstants.CONTENT_STREAM_FILE_NAME).getValues()
         .get(0));
   }

   public void testRenameFolder() throws Exception
   {
      FolderDataImpl folder = createFolder(rootFolder, "renameFolderTest", "cmis:folder");
      createDocument(folder, "child1", "cmis:document", null, null);
      setProperty(folder, new StringProperty(CmisConstants.NAME, CmisConstants.NAME, CmisConstants.NAME,
         CmisConstants.NAME, "renameFolderTest01"));

      assertTrue(session.itemExists("/renameFolderTest01"));
      assertTrue(session.itemExists("/renameFolderTest01/child1"));

      assertEquals("renameFolderTest01", folder.getName());
   }

   public void testSetContent() throws Exception
   {
      DocumentData document = createDocument(rootFolder, "setContentTest", "cmis:document", null, null);
      Node documentNode = (Node)session.getItem("/setContentTest");
      assertEquals("", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("", documentNode.getProperty("jcr:content/jcr:mimeType").getString());

      ContentStream cs = new BaseContentStream("to be or not to be".getBytes(), null, new MimeType("text", "plain"));
      document.setContentStream(cs);

      documentNode = (Node)session.getItem("/setContentTest");
      assertEquals("to be or not to be", documentNode.getProperty("jcr:content/jcr:data").getString());
      assertEquals("text/plain", documentNode.getProperty("jcr:content/jcr:mimeType").getString());
   }

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
      storage.unfileObject(document);
      assertNull(document.getParent());
      assertEquals(0, document.getParents().size());
   }

   public void testUnfiling() throws Exception
   {
      assertEquals(0, getSize(storage.getUnfiledObjectsId()));
      DocumentData document = createDocument(rootFolder, "unfilingDocumentTest", "cmis:document", null, null);
      assertTrue(rootFolder.getChildren(null).hasNext());
      rootFolder.removeObject(document);
      assertFalse(rootFolder.getChildren(null).hasNext());

      assertFalse(session.itemExists("/unfilingDocumentTest"));

      Collection<FolderData> parents = document.getParents();
      assertEquals(0, parents.size());
      storage.getObjectById(document.getObjectId());

      assertEquals(1, getSize(storage.getUnfiledObjectsId()));
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
      ((FolderDataImpl)folder).getNode().accept(new ItemVisitor()
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
         ((Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED)).getNodes(); iter
         .hasNext();)
      {
         for (NodeIterator iterator = iter.nextNode().getNodes(); iterator.hasNext();)
         {
            System.out.println(iterator.nextNode().getPath());
         }
      }
      System.out.println("-----------------------");
   }

   protected DocumentDataImpl createDocument(FolderData folder, String name, String typeId, ContentStream content,
      VersioningState versioningState) throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      DocumentData document =
         storage.createDocument(folder, documentTypeDefinition, properties, content, null, null,
            versioningState == null ? VersioningState.MAJOR : versioningState);
      return (DocumentDataImpl)document;
   }

   protected FolderDataImpl createFolder(FolderData folder, String name, String typeId) throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:folder", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      FolderData newFolder = storage.createFolder(folder, folderTypeDefinition, properties, null, null);
      //      newFolder.setName(name);
      return (FolderDataImpl)newFolder;
   }

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

      PolicyData policy = storage.createPolicy(folder, policyTypeDefinition, properties, null, null);

      return (PolicyDataImpl)policy;
   }

   //   public void testCreateAiim() throws Exception
   //   {
   //      createAiimDocument(rootFolder, "aiim01", null);
   //   }
   //
   //   protected DocumentData createAiimDocument(FolderData folder, String name, ContentStream content) throws Exception
   //   {
   //      DocumentData document = storage.createDocument(folder, "aiim_2010demo", VersioningState.MAJOR);
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

}
