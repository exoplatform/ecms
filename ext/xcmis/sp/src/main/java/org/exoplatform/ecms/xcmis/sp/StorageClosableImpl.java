package org.exoplatform.ecms.xcmis.sp;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.Storage;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.TypeNotFoundException;
import org.xcmis.spi.UpdateConflictException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.AllowableActions;
import org.xcmis.spi.model.ChangeEvent;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.Rendition;
import org.xcmis.spi.model.RepositoryInfo;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.UnfileObject;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;

public class StorageClosableImpl implements Storage
{

   private static final Log LOG = ExoLogger.getExoLogger(StorageClosableImpl.class.getName());

   private final PermissionService permissionService;

   private final StorageConfiguration rootStorageConfiguration;

   private final Map<String, TypeMapping> defaultNodetypeMapping;


   public StorageClosableImpl(StorageConfiguration rootStorageConfiguration, PermissionService permissionService,
      Map<String, TypeMapping> defaultNodetypeMapping)
   {
      this.rootStorageConfiguration = rootStorageConfiguration;
      this.permissionService = permissionService;
      this.defaultNodetypeMapping = defaultNodetypeMapping;
   }

   public AllowableActions calculateAllowableActions(ObjectData object)
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.calculateAllowableActions(object);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }

   public DocumentData copyDocument(DocumentData source, FolderData parent, Map<String, Property<?>> properties,
      List<AccessControlEntry> acl, Collection<PolicyData> policies, VersioningState versioningState)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.copyDocument(source, parent, properties, acl, policies, versioningState);
      }
      catch (Exception e)
      {
         processException(e);
      }
      
      return null;
   }


   public DocumentData createDocument(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, ContentStream content, List<AccessControlEntry> acl,
      Collection<PolicyData> policies, VersioningState versioningState) throws ConstraintException,
      NameConstraintViolationException, IOException, StorageException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createDocument(parent, typeDefinition, properties, content, acl, policies, versioningState);
      }
      catch (Exception e)
      {
         processException(e);
      }
     
      return null;
   }


   public FolderData createFolder(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createFolder(parent, typeDefinition, properties, acl, policies);
      }
      catch (Exception e)
      {
         processException(e);
      }
      
      return null;
   }


   public PolicyData createPolicy(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createPolicy(parent, typeDefinition, properties, acl, policies);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public RelationshipData createRelationship(ObjectData source, ObjectData target, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws NameConstraintViolationException, StorageException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createRelationship(source, target, typeDefinition, properties, acl, policies);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public void deleteObject(ObjectData object, boolean deleteAllVersions) throws VersioningException,
      UpdateConflictException, StorageException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         storage.deleteObject(object, deleteAllVersions);
      }
      catch (Exception e)
      {
         processException(e);
      }
   }


   public Collection<String> deleteTree(FolderData folder, boolean deleteAllVersions, UnfileObject unfileObject,
      boolean continueOnFailure) throws UpdateConflictException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.deleteTree(folder, deleteAllVersions, unfileObject, continueOnFailure);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public Collection<DocumentData> getAllVersions(String versionSeriesId) throws ObjectNotFoundException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getAllVersions(versionSeriesId);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public ItemsIterator<ChangeEvent> getChangeLog(String changeLogToken) throws ConstraintException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getChangeLog(changeLogToken);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public ItemsIterator<DocumentData> getCheckedOutDocuments(FolderData folder, String orderBy)
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getCheckedOutDocuments(folder, orderBy);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public String getId()
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getId();
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public ObjectData getObjectById(String objectId) throws ObjectNotFoundException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getObjectById(objectId);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public ObjectData getObjectByPath(String path) throws ObjectNotFoundException
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getObjectByPath(path);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public ItemsIterator<Rendition> getRenditions(ObjectData object)
   {
      try
      {
         StorageImpl storage =
            new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getRenditions(object);
      }
      catch (Exception e)
      {
         processException(e);
      }
      return null;
   }


   public RepositoryInfo getRepositoryInfo()
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.getRepositoryInfo();
     } catch (Exception e) {
       processException(e);
     }
     return null;
   }

   public Iterator<String> getUnfiledObjectsId() throws StorageException
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.getUnfiledObjectsId();
     } catch (Exception e) {
       processException(e);
     }
     return null;
   }


   public ObjectData moveObject(ObjectData object, FolderData target, FolderData source)
      throws UpdateConflictException, VersioningException, NameConstraintViolationException, StorageException
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.moveObject(object, target, source);
     } catch (Exception e) {
       processException(e);
     }
     return null;
   }


   public ItemsIterator<Result> query(Query query)
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.query(query);
     } catch (Exception e) {
       processException(e);
     }
     return null;
   }


   public void unfileObject(ObjectData object)
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       storage.unfileObject(object);
     } catch (Exception e) {
       processException(e);
     }
   }


   public String addType(TypeDefinition type) throws ConstraintException, StorageException
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.addType(type);
     } catch (Exception e) {
       processException(e);
     }
     return null;
   }


   public ItemsIterator<TypeDefinition> getTypeChildren(String typeId, boolean includePropertyDefinitions)
      throws TypeNotFoundException
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.getTypeChildren(typeId, includePropertyDefinitions);
     } catch (Exception e) {
       processException(e);
     }
     return null;       
   }


   public TypeDefinition getTypeDefinition(String typeId, boolean includePropertyDefinition)
      throws TypeNotFoundException
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.getTypeDefinition(typeId, includePropertyDefinition);
     } catch (Exception e) {
       processException(e);
     }
     return null;
   }


   public void removeType(String typeId) throws ConstraintException, TypeNotFoundException, StorageException
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       storage.removeType(typeId);
     } catch (Exception e) {
       processException(e);
     }
   }

   public boolean isSupportedNodeType(String nodeTypeName)
   {
     try {
       StorageImpl storage =
          new StorageImpl(rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
       return storage.isSupportedNodeType(nodeTypeName);
     } catch (Exception e) {
       processException(e);
     }
     return false;
   }

   private static void processException(Exception e) {
     if(e instanceof LoginException)
     {
       if (LOG.isErrorEnabled()) {
         LOG.error(e.getLocalizedMessage(), e);
       }
     }
     else if(e instanceof NoSuchWorkspaceException)
     {
       if (LOG.isErrorEnabled()) {
         LOG.error(e.getLocalizedMessage(), e);
       }
     }
     else if(e instanceof RepositoryException)
     {
       if (LOG.isErrorEnabled()) {
         LOG.error(e.getLocalizedMessage(), e);
       }
     }
   }

}