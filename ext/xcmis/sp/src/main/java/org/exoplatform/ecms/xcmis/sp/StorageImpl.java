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

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.xcmis.search.InvalidQueryException;
import org.xcmis.search.SearchService;
import org.xcmis.search.Visitors;
import org.xcmis.search.model.constraint.And;
import org.xcmis.search.model.constraint.DescendantNode;
import org.xcmis.search.model.source.Selector;
import org.xcmis.search.model.source.SelectorName;
import org.xcmis.search.parser.CmisQueryParser;
import org.xcmis.search.parser.QueryParser;
import org.xcmis.search.query.QueryExecutionException;
import org.xcmis.search.result.ScoredRow;
import org.xcmis.spi.BaseItemsIterator;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.NotSupportedException;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectDataVisitor;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.Storage;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.UpdateConflictException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.ACLCapability;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.AccessControlPropagation;
import org.xcmis.spi.model.AllowableActions;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.CapabilityACL;
import org.xcmis.spi.model.CapabilityChanges;
import org.xcmis.spi.model.CapabilityContentStreamUpdatable;
import org.xcmis.spi.model.CapabilityJoin;
import org.xcmis.spi.model.CapabilityQuery;
import org.xcmis.spi.model.CapabilityRendition;
import org.xcmis.spi.model.ChangeEvent;
import org.xcmis.spi.model.Permission;
import org.xcmis.spi.model.PermissionMapping;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.Rendition;
import org.xcmis.spi.model.RepositoryCapabilities;
import org.xcmis.spi.model.RepositoryInfo;
import org.xcmis.spi.model.SupportedPermissions;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.UnfileObject;
import org.xcmis.spi.model.Updatability;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.model.Permission.BasicPermissions;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.utils.CmisUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: StorageImpl.java 804 2010-04-16 16:48:59Z
 *          alexey.zavizionov@gmail.com $
 */
public class StorageImpl extends BaseJcrStorage implements Storage
{

   private class TreeVisitor implements ObjectDataVisitor
   {
      private final Collection<BaseObjectData> items = new LinkedHashSet<BaseObjectData>();

      public void visit(ObjectData object)
      {
         TypeDefinition type = object.getTypeDefinition();
         if (type.getBaseId() == BaseType.FOLDER)
         {
            for (ItemsIterator<ObjectData> children = ((FolderDataImpl)object).getChildren(null); children.hasNext();)
            {
               children.next().accept(this);
            }
            items.add((BaseObjectData)object);
         }
         else
         {
            items.add((BaseObjectData)object);
         }
      }
   }

   private static final Comparator<ObjectData> CREATION_DATE_COMPARATOR = new Comparator<ObjectData>() {
      public int compare(ObjectData object1, ObjectData object2)
      {
         Calendar c1 = object1.getCreationDate();
         Calendar c2 = object2.getCreationDate();
         return c2.compareTo(c1);
      }
   };

   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger(StorageImpl.class);

   private RepositoryInfo repositoryInfo;

   private final PermissionService permissionService;

   /**
    * Searching service.
    */
   private SearchService searchService;

   /**
    * Cmis query parser.
    */
   private final QueryParser cmisQueryParser;

   public StorageImpl(Session session, StorageConfiguration configuration, SearchService searchService,
      PermissionService permissionService, Map<String, TypeMapping> nodeTypeMapping)
   {
      super(session, configuration, nodeTypeMapping);
      this.searchService = searchService;
      this.permissionService = permissionService;
      this.cmisQueryParser = new CmisQueryParser();
   }

   /**
    * {@inheritDoc}
    */
   public AllowableActions calculateAllowableActions(ObjectData object)
   {
      ConversationState state = ConversationState.getCurrent();
      AllowableActions actions =
         permissionService.calculateAllowableActions(object, state != null ? state.getIdentity().getUserId() : null,
            getRepositoryInfo());

      if (object instanceof JcrFile)
      {
         // Disable creation new versions for object which represents JCR nodes
         // created directly in JCR (not via xCMIS API).
         actions.setCanCheckOut(false);
         actions.setCanCheckIn(false);
         actions.setCanCancelCheckOut(false);
      }
      return actions;
   }

   /**
    * {@inheritDoc}
    */
   public DocumentData copyDocument(DocumentData source, FolderData parent, Map<String, Property<?>> properties,
      List<AccessControlEntry> acl, Collection<PolicyData> policies, VersioningState versioningState)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      // If name for copy is not provided then use name of source document.
      TypeDefinition typeDefinition = source.getTypeDefinition();
      Property<?> nameProperty = null;
      if (properties == null)
      {
         properties = new HashMap<String, Property<?>>();
      }
      else
      {
         nameProperty = properties.get(CmisConstants.NAME);
      }
      String name = null;
      if (nameProperty == null || nameProperty.getValues().size() == 0
         || (name = (String)nameProperty.getValues().get(0)) == null || name.length() == 0)
      {
         name = source.getName();
         PropertyDefinition<?> namePropertyDefinition = typeDefinition.getPropertyDefinition(CmisConstants.NAME);
         properties.put(namePropertyDefinition.getId(),
            new StringProperty(namePropertyDefinition.getId(), namePropertyDefinition.getQueryName(),
               namePropertyDefinition.getLocalName(), namePropertyDefinition.getDisplayName(), name));
      }

      try
      {
         return createDocument(parent, typeDefinition, properties, source.getContentStream(), acl, policies,
            versioningState);
      }
      catch (IOException ioe)
      {
         throw new CmisRuntimeException("Unable copy content for new document. " + ioe.getMessage(), ioe);
      }
   }

   /**
    * {@inheritDoc}
    */
   public DocumentData createDocument(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, ContentStream content, List<AccessControlEntry> acl,
      Collection<PolicyData> policies, VersioningState versioningState) throws ConstraintException,
      NameConstraintViolationException, IOException, StorageException
   {
      String name = null;
      Property<?> nameProperty = properties.get(CmisConstants.NAME);
      if (nameProperty != null && nameProperty.getValues().size() > 0)
      {
         name = (String)nameProperty.getValues().get(0);
      }
      if (name == null && content != null)
      {
         name = content.getFileName();
      }
      if (name == null || name.length() == 0)
      {
         throw new NameConstraintViolationException("Name for new document must be provided.");
      }
      
      JcrNodeEntry documentEntry =
         createDocumentEntry(parent != null ? ((FolderDataImpl)parent).getNodeEntry() : null, name, typeDefinition,
            versioningState);

      documentEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
      documentEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
      documentEntry.setValue(CmisConstants.CREATED_BY, session.getUserID());
      documentEntry.setValue(CmisConstants.CREATION_DATE, Calendar.getInstance());
      documentEntry.setValue(CmisConstants.VERSION_SERIES_ID, documentEntry.getString(JcrCMIS.JCR_VERSION_HISTORY));
      documentEntry.setValue(CmisConstants.OBJECT_ID, 
                             documentEntry.getString(JcrCMIS.JCR_VERSION_HISTORY) + JcrCMIS.ID_SEPARATOR + "1");
      documentEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
      documentEntry.setValue(CmisConstants.IS_MAJOR_VERSION, versioningState == VersioningState.MAJOR);
      // TODO : support for checked-out initial state
      documentEntry.setValue(CmisConstants.VERSION_LABEL, LATEST_LABEL);

      Property<?> contentFileNameProperty = properties.get(CmisConstants.CONTENT_STREAM_FILE_NAME);
      if (content != null && (contentFileNameProperty == null || contentFileNameProperty.getValues().isEmpty()))
      {
         String contentFileName = content.getFileName();
         if (contentFileName != null)
         {
            documentEntry.setValue(CmisConstants.CONTENT_STREAM_FILE_NAME, contentFileName);
         }
      }

      for (Property<?> property : properties.values())
      {
         PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
         Updatability updatability = definition.getUpdatability();
         if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
         {
            documentEntry.setProperty(property);
         }
      }

      documentEntry.setContentStream(content);

      if (acl != null && acl.size() > 0)
      {
         documentEntry.setACL(acl);
      }

      if (policies != null && policies.size() > 0)
      {
         for (PolicyData policy : policies)
         {
            documentEntry.applyPolicy(((BaseObjectData)policy).getNodeEntry());
         }
      }

      DocumentDataImpl document = new DocumentDataImpl(documentEntry);
      document.save();
      return document;
   }

   /**
    * {@inheritDoc}
    */
   public FolderData createFolder(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      if (parent == null)
      {
         throw new ConstraintException("Parent folder must be provided.");
      }

      String name = null;
      Property<?> nameProperty = properties.get(CmisConstants.NAME);
      if (nameProperty != null && nameProperty.getValues().size() > 0)
      {
         name = (String)nameProperty.getValues().get(0);
      }
      if (name == null || name.length() == 0)
      {
         throw new NameConstraintViolationException("Name for new folder must be provided.");
      }

      JcrNodeEntry folderEntry = createFolderEntry(((FolderDataImpl)parent).getNodeEntry(), name, typeDefinition);

      folderEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
      folderEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
      folderEntry.setValue(CmisConstants.CREATED_BY, session.getUserID());
      folderEntry.setValue(CmisConstants.CREATION_DATE, Calendar.getInstance());

      for (Property<?> property : properties.values())
      {
         PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
         Updatability updatability = definition.getUpdatability();
         if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
         {
            folderEntry.setProperty(property);
         }
      }

      if (acl != null && acl.size() > 0)
      {
         folderEntry.setACL(acl);
      }

      if (policies != null && policies.size() > 0)
      {
         for (PolicyData policy : policies)
         {
            folderEntry.applyPolicy(((BaseObjectData)policy).getNodeEntry());
         }
      }

      FolderDataImpl folder = new FolderDataImpl(folderEntry);
      folder.save();
      return folder;
   }

   /**
    * {@inheritDoc}
    */
   public PolicyData createPolicy(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      // TODO : need raise exception if parent folder is provided ??
      // Do not use parent folder, policy is not fileable.
      String name = null;
      Property<?> nameProperty = properties.get(CmisConstants.NAME);
      if (nameProperty != null && nameProperty.getValues().size() > 0)
      {
         name = (String)nameProperty.getValues().get(0);
      }
      if (name == null || name.length() == 0)
      {
         throw new NameConstraintViolationException("Name for new policy must be provided.");
      }

      JcrNodeEntry policyEntry = createPolicyEntry(name, typeDefinition);
      policyEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
      policyEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
      policyEntry.setValue(CmisConstants.CREATED_BY, session.getUserID());
      policyEntry.setValue(CmisConstants.CREATION_DATE, Calendar.getInstance());

      for (Property<?> property : properties.values())
      {
         PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
         Updatability updatability = definition.getUpdatability();
         if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
         {
            policyEntry.setProperty(property);
         }
      }

      if (acl != null && acl.size() > 0)
      {
         policyEntry.setACL(acl);
      }

      if (policies != null && policies.size() > 0)
      {
         for (PolicyData policy : policies)
         {
            policyEntry.applyPolicy(((BaseObjectData)policy).getNodeEntry());
         }
      }

      PolicyDataImpl policy = new PolicyDataImpl(policyEntry);
      policy.save();
      return policy;
   }

   /**
    * {@inheritDoc}
    */
   public RelationshipData createRelationship(ObjectData source, ObjectData target, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws NameConstraintViolationException, StorageException
   {
      String name = null;
      Property<?> nameProperty = properties.get(CmisConstants.NAME);
      if (nameProperty != null)
      {
         name = (String)nameProperty.getValues().get(0);
      }
      if (name == null || name.length() == 0)
      {
         throw new NameConstraintViolationException("Name for new relationship must be provided.");
      }

      JcrNodeEntry relationshipEntry =
         createRelationshipEntry(name, typeDefinition, ((BaseObjectData)source).getNodeEntry(),
            ((BaseObjectData)target).getNodeEntry());

      relationshipEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
      relationshipEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
      relationshipEntry.setValue(CmisConstants.CREATED_BY, session.getUserID());
      relationshipEntry.setValue(CmisConstants.CREATION_DATE, Calendar.getInstance());

      for (Property<?> property : properties.values())
      {
         PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
         Updatability updatability = definition.getUpdatability();
         if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
         {
            relationshipEntry.setProperty(property);
         }
      }

      if (acl != null && acl.size() > 0)
      {
         relationshipEntry.setACL(acl);
      }

      if (policies != null && policies.size() > 0)
      {
         for (PolicyData policy : policies)
         {
            relationshipEntry.applyPolicy(((BaseObjectData)policy).getNodeEntry());
         }
      }

      RelationshipDataImpl relationship = new RelationshipDataImpl(relationshipEntry);
      relationship.save();
      return relationship;
   }

   /**
    * {@inheritDoc}
    */
   public void deleteObject(ObjectData object, boolean deleteAllVersions) throws UpdateConflictException,
      VersioningException, StorageException
   {
      Node node = ((BaseObjectData)object).entry.node;
      Node parentNode = null;
      try {
         parentNode = node.getParent();
      }  catch (RepositoryException  e) {
         throw new CmisRuntimeException("Unable get parent. " + e.getMessage(), e);
      }
      if (object.getBaseType() == BaseType.DOCUMENT && object.getTypeDefinition().isVersionable() && deleteAllVersions)
      {
         try
         {
            if (node.getParent() instanceof Version) {
               // Delete version
               Version version = (Version)node.getParent();
               VersionHistory versionHistory = version.getContainingHistory();
               DocumentDataImpl documentDataImpl = 
                 new DocumentDataImpl(((BaseObjectData)object).entry.storage.getEntry(versionHistory.getVersionableUUID()));
               documentDataImpl.delete();
            } else {
               // Delete object
               ((BaseObjectData)object).delete();  
            }
         }
         catch (RepositoryException re)
         {
            throw new CmisRuntimeException("Unable get latest version of document. " + re.getMessage(), re);
         }
         catch (ObjectNotFoundException e)
         {
            throw new CmisRuntimeException("Unable get latest version of document. " + e.getMessage(), e);
         }
       } else if (object.getBaseType() == BaseType.DOCUMENT && !(parentNode instanceof Version) && !deleteAllVersions) {
          // use deleteAllVersions=true to delete all versions of the document
          throw new CmisRuntimeException("Unable to delete latest version at one.");
       } else {
          ((BaseObjectData)object).delete();
       }
   }

   /**
    * {@inheritDoc}
    */
   public Collection<String> deleteTree(FolderData folder, boolean deleteAllVersions, UnfileObject unfileObject,
      boolean continueOnFailure) throws UpdateConflictException
   {
      if (!deleteAllVersions)
      {
         // Throw exception to avoid unexpected removing data. Any way at the
         // moment we are not able remove 'base version' of versionable node,
         // so have not common behavior for removing just one version of document.
         throw new CmisRuntimeException("Unable delete only specified version.");
      }
      String folderId = folder.getObjectId();
      TreeVisitor visitor = new TreeVisitor();
      folder.accept(visitor);
      for (BaseObjectData o : visitor.items)
      {
         try
         {
            // Method javax.jcr.Session#save() will be called after removing
            // each items. It may cause slow behavior but section 2.2.4.15 'deleteTree'
            // of CMIS specification says the operation is not atomic.
            // Will return list of items in tree which were not removed.
            o.delete();
         }
         catch (StorageException e)
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn("Unable delete object " + o.getObjectId());
            }

            if (!continueOnFailure)
            {
               break;
            }
         }
      }

      try
      {
         folder = (FolderData)getObjectById(folderId);
         // If not deleted then traversing one more time.
         visitor = new TreeVisitor();
         folder.accept(visitor);
         List<String> failedToDelete = new ArrayList<String>(visitor.items.size());
         for (BaseObjectData o : visitor.items)
         {
            failedToDelete.add(o.getObjectId());
         }
         return failedToDelete;
      }
      catch (ObjectNotFoundException e)
      {
         // Tree removed.
      }
      return Collections.emptyList();
   }

   /**
    * {@inheritDoc}
    */
   public Collection<DocumentData> getAllVersions(String versionSeriesId) throws ObjectNotFoundException
   {
      try
      {
         Node node = ((ExtendedSession)session).getNodeByIdentifier(versionSeriesId);
         VersionHistory vh = (VersionHistory)node;
         List<DocumentData> versions = new ArrayList<DocumentData>();
         VersionIterator iterator = vh.getAllVersions();
         iterator.next(); // skip jcr:rootVersion
         while (iterator.hasNext())
         {
            Version v = iterator.nextVersion();
            versions.add(new DocumentVersion(fromNode(v.getNode(JcrCMIS.JCR_FROZEN_NODE))));
         }
         DocumentData latest = (DocumentData)getObjectById(vh.getVersionableUUID());
         versions.add(latest);
         String pwcId = latest.getVersionSeriesCheckedOutId();
         if (pwcId != null)
         {
            PWC pwc = (PWC)getObjectById(pwcId);
            versions.add(pwc);
         }
         Collections.sort(versions, CREATION_DATE_COMPARATOR);
         return versions;
      }
      catch (ItemNotFoundException infe)
      {
         throw new ObjectNotFoundException("Version series '" + versionSeriesId + "' does not exist.");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get version series " + versionSeriesId + ". " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<ChangeEvent> getChangeLog(String changeLogToken) throws ConstraintException
   {
      throw new NotSupportedException("Changes log feature is not supported.");
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<DocumentData> getCheckedOutDocuments(FolderData folder, String orderBy)
   {
      try
      {
         Node workingCopies =
            (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_WORKING_COPIES);

         List<DocumentData> checkedOut = new ArrayList<DocumentData>();
         for (NodeIterator iterator = workingCopies.getNodes(); iterator.hasNext();)
         {
            Node wc = iterator.nextNode();
            if (!wc.hasNodes())
            {
               // Must not happen.
              if (LOG.isErrorEnabled()) {
                LOG.error("PWC node not fould.");
              }
              continue;
            }
            Node node = wc.getNodes().nextNode();
            PWC pwc = new PWC(fromNode(node));
            if (folder != null)
            {
               for (FolderData parent : pwc.getParents())
               {
                  if (parent.equals(folder))
                  {
                     checkedOut.add(pwc);
                     break;
                  }
               }
            }
            else
            {
               checkedOut.add(pwc);
            }
         }
         return new BaseItemsIterator<DocumentData>(checkedOut);
      }
      catch (ObjectNotFoundException onfe)
      {
         throw new CmisRuntimeException("Unable get checked-out documents. " + onfe.getMessage(), onfe);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get checked-out documents. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getId()
   {
      return storageConfiguration.getId();
   }

   /**
    * {@inheritDoc}
    */
   public ObjectData getObjectById(String objectId) throws ObjectNotFoundException
   {
      return getObject(getEntry(objectId));
   }

   /**
    * {@inheritDoc}
    */
   public ObjectData getObjectByPath(String path) throws ObjectNotFoundException
   {
      if (path == null)
      {
         throw new CmisRuntimeException("Object path may not be null.");
      }
      try
      {
         String path1 = getJcrRootPath();
         if (!path1.equals("/"))
         {
            path = path1 + path;
         }
         Item item = session.getItem(path);
         if (!item.isNode())
         {
            throw new ObjectNotFoundException("Object '" + path + "' does not exist.");
         }
         Node node = (Node)item;
         return getObject(fromNode(node));
      }
      catch (ItemNotFoundException nfe)
      {
         throw new ObjectNotFoundException("Object  '" + path + "' does not exist.");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<Rendition> getRenditions(ObjectData object)
   {
      return RenditionManager.getInstance().getRenditions(object);
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryInfo getRepositoryInfo()
   {
      if (repositoryInfo == null)
      {
         PermissionMapping permissionMapping = new PermissionMapping();

         permissionMapping.put(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_FOLDER_TREE_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_CHILDREN_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_OBJECT_PARENTS_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_FOLDER_PARENT_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_FOLDER_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_RELATIONSHIP_SOURCE, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_RELATIONSHIP_TARGET, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_PROPERTIES_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_CONTENT_STREAM_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_RENDITIONS_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_MOVE_OBJECT_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_MOVE_OBJECT_TARGET, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_MOVE_OBJECT_SOURCE, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_DELETE_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_DELETE_TREE_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_SET_CONTENT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_TO_FOLDER_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_OBJECT_FROM_FOLDER_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_OBJECT_FROM_FOLDER_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CHECKOUT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_CANCEL_CHECKOUT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_CHECKIN_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_ALL_VERSIONS_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_OBJECT_RELATIONSHIPS_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_POLICY_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_POLICY_POLICY, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_POLICY_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_POLICY_POLICY, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_APPLIED_POLICIES_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_ACL_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_APPLY_ACL_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));

         List<Permission> permissions = new ArrayList<Permission>(4);
         for (BasicPermissions b : BasicPermissions.values())
         {
            permissions.add(new Permission(b.value(), ""));
         }

         String rootFolderPath = getJcrRootPath();
         String rootIdentifier = null;
         if ("/".equals(rootFolderPath))
         {
            rootIdentifier = org.exoplatform.services.jcr.impl.Constants.ROOT_UUID;
         }
         else
         {
            try
            {
               rootIdentifier = ((ExtendedNode)session.getItem(rootFolderPath)).getIdentifier();
            }
            catch (RepositoryException re)
            {
               throw new CmisRuntimeException("Unable get root folder id. ", re);
            }
         }

         CapabilityQuery queryCapability = searchService != null ? CapabilityQuery.BOTHCOMBINED : CapabilityQuery.NONE;

         repositoryInfo =
            new RepositoryInfo(getId(), getId(), rootIdentifier, CmisConstants.SUPPORTED_VERSION,
               new RepositoryCapabilities(CapabilityACL.MANAGE, CapabilityChanges.NONE,
                  CapabilityContentStreamUpdatable.ANYTIME, CapabilityJoin.NONE, queryCapability,
                  CapabilityRendition.READ, false, true, true, true, true, true, false, false), new ACLCapability(
                  permissionMapping, Collections.unmodifiableList(permissions),
                  AccessControlPropagation.REPOSITORYDETERMINED, SupportedPermissions.BASIC), IdentityConstants.ANONIM,
                  IdentityConstants.ANY, null, null, true, REPOSITORY_DESCRIPTION, VENDOR_NAME, PRODUCT_NAME,
               PRODUCT_VERSION, null);
      }

      // TODO update latestChangeLogToken when ChangeLogToken feature will be implemented
      return repositoryInfo;
   }

   /**
    * {@inheritDoc}
    */
   public Iterator<String> getUnfiledObjectsId() throws StorageException
   {
      return CmisUtils.emptyItemsIterator();
   }

   /**
    * {@inheritDoc}
    */
   public ObjectData moveObject(ObjectData object, FolderData target, FolderData source)
      throws UpdateConflictException, VersioningException, NameConstraintViolationException, StorageException
   {
      String objectId = object.getObjectId();
      ((BaseObjectData)object).getNodeEntry().moveTo(((BaseObjectData)target).getNodeEntry());
      try
      {
         return getObject(getEntry(objectId));
      }
      catch (ObjectNotFoundException e)
      {
         throw new StorageException("Unable to retrieve the object after moving. " + e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<Result> query(Query query) throws InvalidArgumentException
   {

      if (searchService != null)
      {
         try
         {
            boolean isRootStorage = "/".equals(getJcrRootPath());
            org.xcmis.search.model.Query realQuery = cmisQueryParser.parseQuery(query.getStatement());
            if (!isRootStorage)
            {

               //add drive path constrain
               DescendantNode rootDescendantConstraint =
                  new DescendantNode(((Selector)realQuery.getSource()).getAlias(), "["
                     + getRepositoryInfo().getRootFolderId() + "]");

               realQuery =
                  new org.xcmis.search.model.Query(realQuery.getSource(), realQuery.getConstraint() == null
                     ? rootDescendantConstraint : new And(realQuery.getConstraint(), rootDescendantConstraint),
                     realQuery.getOrderings(), realQuery.getColumns(), realQuery.getLimits());
            }
            List<ScoredRow> rows = searchService.execute(realQuery);
            //check if needed default sorting
            if (realQuery.getOrderings().size() == 0)
            {
               Set<SelectorName> selectorsReferencedBy = Visitors.getSelectorsReferencedBy(realQuery);
               Collections.sort(rows, new DocumentOrderResultSorter(selectorsReferencedBy.iterator().next().getName(),
                  this));
            }
            return new QueryResultIterator(rows, realQuery);
         }
         catch (InvalidQueryException e)
         {
            throw new InvalidArgumentException(e.getLocalizedMessage(), e);
         }
         catch (QueryExecutionException e)
         {
            throw new CmisRuntimeException(e.getLocalizedMessage(), e);
         }
      }
      else
      {
         throw new NotSupportedException("Query is not supported. ");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void unfileObject(ObjectData object)
   {
      // This method should not be called since we declare 'unfiling' capability as not supported.
      throw new NotSupportedException("Unfiling is not supported.");
   }

   ObjectData getObject(JcrNodeEntry entry)
   {
      try
      {
         TypeDefinition typeDefinition = entry.getType();
         Node node = entry.getNode();
         if (typeDefinition.getBaseId() == BaseType.DOCUMENT)
         {
            if (node.getParent().isNodeType("xcmis:workingCopy"))
            {
               return new PWC(entry);
            }
            if (node.isNodeType(JcrCMIS.NT_FROZEN_NODE))
            {
               return new DocumentVersion(entry);
            }
            if (!node.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
            {
               // Has not required mixin 'cmis:document'. Some operation for
               // this type of document will be different from default.
               if (LOG.isWarnEnabled())
               {
                  LOG.warn("Node " + node.getPath()
                     + " has not 'cmis:document' mixin type. Some operations may be disabled.");
               }
               return new JcrFile(entry);
            }
            return new DocumentDataImpl(entry);
         }
         else if (typeDefinition.getBaseId() == BaseType.FOLDER)
         {
            if (!node.isNodeType(JcrCMIS.CMIS_MIX_FOLDER))
            {
               // Has not required mixin 'cmis:folder'. Some operation for this
               // type of document will be different from default.
               if (LOG.isWarnEnabled())
               {
                  LOG.warn("Node " + node.getPath()
                     + " has not 'cmis:document' mixin type. Some operation may be disabled.");
               }
               return new JcrFolder(entry);
            }
            return new FolderDataImpl(entry);
         }
         else if (typeDefinition.getBaseId() == BaseType.POLICY)
         {
            return new PolicyDataImpl(entry);
         }
         else if (typeDefinition.getBaseId() == BaseType.RELATIONSHIP)
         {
            return new RelationshipDataImpl(entry);
         }
         // Must never happen.
         throw new CmisRuntimeException("Unknown base type. ");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   /**
    * @return the searchService
    */
   public SearchService getSearchService()
   {
      return searchService;
   }

   /**
    * @param searchService the searchService to set
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
}
