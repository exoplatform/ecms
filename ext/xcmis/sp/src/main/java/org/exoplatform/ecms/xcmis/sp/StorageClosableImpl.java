package org.exoplatform.ecms.xcmis.sp;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class StorageClosableImpl implements Storage
{

   private static final Log LOG = ExoLogger.getExoLogger(StorageClosableImpl.class);

   private final PermissionService permissionService;

   private final StorageConfiguration rootStorageConfiguration;

   private final Map<String, TypeMapping> defaultNodetypeMapping;

   private final String workspaceName;

   private final ManageableRepository repository;

   private final SessionProvider sessionProvider;

   public StorageClosableImpl(SessionProvider sessionProvider, String workspaceName, ManageableRepository repository,
      StorageConfiguration rootStorageConfiguration, PermissionService permissionService,
      Map<String, TypeMapping> defaultNodetypeMapping)
   {
      this.sessionProvider = sessionProvider;
      this.workspaceName = workspaceName;
      this.repository = repository;
      this.rootStorageConfiguration = rootStorageConfiguration;
      this.permissionService = permissionService;
      this.defaultNodetypeMapping = defaultNodetypeMapping;
   }

   public AllowableActions calculateAllowableActions(ObjectData object)
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.calculateAllowableActions(object);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   public DocumentData copyDocument(DocumentData source, FolderData parent, Map<String, Property<?>> properties,
      List<AccessControlEntry> acl, Collection<PolicyData> policies, VersioningState versioningState)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.copyDocument(source, parent, properties, acl, policies, versioningState);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public DocumentData createDocument(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, ContentStream content, List<AccessControlEntry> acl,
      Collection<PolicyData> policies, VersioningState versioningState) throws ConstraintException,
      NameConstraintViolationException, IOException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createDocument(parent, typeDefinition, properties, content, acl, policies, versioningState);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public FolderData createFolder(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createFolder(parent, typeDefinition, properties, acl, policies);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public PolicyData createPolicy(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createPolicy(parent, typeDefinition, properties, acl, policies);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public RelationshipData createRelationship(ObjectData source, ObjectData target, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws NameConstraintViolationException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.createRelationship(source, target, typeDefinition, properties, acl, policies);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public void deleteObject(ObjectData object, boolean deleteAllVersions) throws VersioningException,
      UpdateConflictException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         storage.deleteObject(object, deleteAllVersions);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
   }

   
   public Collection<String> deleteTree(FolderData folder, boolean deleteAllVersions, UnfileObject unfileObject,
      boolean continueOnFailure) throws UpdateConflictException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.deleteTree(folder, deleteAllVersions, unfileObject, continueOnFailure);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public Collection<DocumentData> getAllVersions(String versionSeriesId) throws ObjectNotFoundException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getAllVersions(versionSeriesId);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ItemsIterator<ChangeEvent> getChangeLog(String changeLogToken) throws ConstraintException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getChangeLog(changeLogToken);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ItemsIterator<DocumentData> getCheckedOutDocuments(FolderData folder, String orderBy)
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getCheckedOutDocuments(folder, orderBy);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public String getId()
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getId();
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ObjectData getObjectById(String objectId) throws ObjectNotFoundException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getObjectById(objectId);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ObjectData getObjectByPath(String path) throws ObjectNotFoundException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getObjectByPath(path);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ItemsIterator<Rendition> getRenditions(ObjectData object)
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getRenditions(object);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public RepositoryInfo getRepositoryInfo()
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getRepositoryInfo();
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public Iterator<String> getUnfiledObjectsId() throws StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getUnfiledObjectsId();
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ObjectData moveObject(ObjectData object, FolderData target, FolderData source)
      throws UpdateConflictException, VersioningException, NameConstraintViolationException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.moveObject(object, target, source);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ItemsIterator<Result> query(Query query)
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.query(query);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public void unfileObject(ObjectData object)
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         storage.unfileObject(object);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
   }

   
   public String addType(TypeDefinition type) throws ConstraintException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.addType(type);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public ItemsIterator<TypeDefinition> getTypeChildren(String typeId, boolean includePropertyDefinitions)
      throws TypeNotFoundException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getTypeChildren(typeId, includePropertyDefinitions);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public TypeDefinition getTypeDefinition(String typeId, boolean includePropertyDefinition)
      throws TypeNotFoundException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.getTypeDefinition(typeId, includePropertyDefinition);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
      return null;
   }

   
   public void removeType(String typeId) throws ConstraintException, TypeNotFoundException, StorageException
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         storage.removeType(typeId);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
      }
   }

   public boolean isSupportedNodeType(String nodeTypeName)
   {
      Session session = null;
      try
      {
         session = sessionProvider.getSession(workspaceName, repository);

         StorageImpl storage =
            new StorageImpl(session, rootStorageConfiguration, null, permissionService, defaultNodetypeMapping);
         return storage.isSupportedNodeType(nodeTypeName);
      }
      catch (Exception e)
      {
         processException(e);
      }
      finally
      {
         session.logout();
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