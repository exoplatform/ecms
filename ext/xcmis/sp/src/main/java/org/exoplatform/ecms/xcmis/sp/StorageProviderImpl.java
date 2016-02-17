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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListenerIterator;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.observation.ObservationManagerRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;
import org.xcmis.search.SearchService;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRegistry;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.Connection;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.StorageProvider;
import org.xcmis.spi.model.BaseType;

/**
 * @author <a href="mailto:andrey00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class StorageProviderImpl implements StorageProvider, Startable
{

   public static class StorageProviderConfig
   {
      /** The storage configuration. */
      private StorageConfiguration storage;

      public StorageProviderConfig(StorageConfiguration storage)
      {
         this.storage = storage;
      }

      public StorageProviderConfig()
      {
      }

      /**
       * @return the storage configuration
       */
      public StorageConfiguration getStorage()
      {
         return storage;
      }

      /**
       * @param storage the storage configuration
       */
      public void setStorage(StorageConfiguration storage)
      {
         this.storage = storage;
      }
   }

   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger(StorageProviderImpl.class.getName());

   /** JCR repository service. */
   private final RepositoryService repositoryService;

   /** Permission service. */
   private final PermissionService permissionService;

   private final CmisRegistry registry;

   private SearchService searchService;

   private StorageConfiguration storageConfiguration;

   Map<String, TypeMapping> nodeTypeMapping = new HashMap<String, TypeMapping>();

   public static final Map<String, TypeMapping> DEFAULT_NODETYPE_MAPPING;
   static
   {
      Map<String, TypeMapping> aMap = new HashMap<String, TypeMapping>();
      // Unstructured mapping immediately. May need have access
      // to root node which often has type nt:unstructured.

      aMap
         .put(JcrCMIS.NT_UNSTRUCTURED, new TypeMapping(JcrCMIS.NT_UNSTRUCTURED, BaseType.FOLDER, CmisConstants.FOLDER));
      aMap.put("exo:taxonomy", new TypeMapping("exo:taxonomy", BaseType.FOLDER, CmisConstants.FOLDER));

      DEFAULT_NODETYPE_MAPPING = Collections.unmodifiableMap(aMap);
   }

   /**
    * This constructor is used by eXo container.
    *
    * @param repositoryService JCR repository service
    * @param permissionService PermissionService
    * @param registry CmisRegistry will be used for registered current
    *        StorageProvider after its initialization
    * @param initParams configuration parameters
    */
   public StorageProviderImpl(RepositoryService repositoryService, PermissionService permissionService,
      CmisRegistry registry, InitParams initParams)
   {
      this(repositoryService, permissionService, registry, null, getStorageConfiguration(initParams));
   }

   private static StorageConfiguration getStorageConfiguration(InitParams initParams)
   {
      StorageConfiguration storageConfiguration = null;
      if (initParams != null)
      {
         ObjectParameter param = initParams.getObjectParam("configuration");
         if (param != null)
         {
            StorageProviderConfig confs = (StorageProviderConfig)param.getObject();
            storageConfiguration = confs.getStorage();
         }
      }
      return storageConfiguration;
   }

   StorageProviderImpl(RepositoryService repositoryService, PermissionService permissionService, CmisRegistry registry,
      StorageConfiguration storageConfiguration)
   {
      this(repositoryService, permissionService, registry, null, storageConfiguration);
   }

   StorageProviderImpl(RepositoryService repositoryService, PermissionService permissionService,
      SearchService searchService, StorageConfiguration storageConfiguration)
   {
      this(repositoryService, permissionService, null, searchService, storageConfiguration);
   }

   StorageProviderImpl(RepositoryService repositoryService, PermissionService permissionService, CmisRegistry registry,
      SearchService searchService, StorageConfiguration storageConfiguration)
   {
      this.repositoryService = repositoryService;
      this.permissionService = permissionService;
      this.registry = registry;
      this.searchService = searchService;
      this.storageConfiguration = storageConfiguration;
      this.nodeTypeMapping.putAll(DEFAULT_NODETYPE_MAPPING);
   }

   /**
    * {@inheritDoc}
    */
   public Connection getConnection()
   {
      if (storageConfiguration == null)
      {
         throw new InvalidArgumentException("CMIS repository is not configured.");
      }

         StorageImpl storage =
            new StorageImpl(storageConfiguration, searchService, permissionService, nodeTypeMapping);
         return new JcrConnection(storage);
   }

   /**
    * @return the nodeTypeMapping
    */
   public Map<String, TypeMapping> getNodeTypeMapping()
   {
      return nodeTypeMapping;
   }

   public StorageConfiguration getStorageConfiguration()
   {
      return storageConfiguration;
   }

   /**
    * {@inheritDoc}
    */
   public String getStorageID()
   {
      if (storageConfiguration == null)
      {
         throw new InvalidArgumentException("CMIS storage is not configured.");
      }
      return storageConfiguration.getId();
   }

   /**
    * Set storage configuration.
    *
    * @param storageConfig storage configuration
    * @throws IllegalStateException if configuration for storage already set
    */
   void setConfiguration(StorageConfiguration storageConfig)
   {
      if (this.storageConfiguration != null)
      {
         throw new IllegalStateException("Storage configuration already set.");
      }
      this.storageConfiguration = storageConfig;
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      try
      {
         init();
         registry.addStorage(this);
      }
      catch (Exception e)
      {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unable to initialize storage. ", e);
        }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      //      if (searchService != null)
      //      {
      //         searchService.stop();
      //      }
   }

   protected synchronized void init() throws RepositoryException
   {
      if (storageConfiguration == null)
      {
         throw new CmisRuntimeException("CMIS repository is not configured.");
      }

      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = repository.getSystemSession(storageConfiguration.getWorkspace());
      try {
        Node root = session.getRootNode();
  
        Node xCmisSystem = session.itemExists(StorageImpl.XCMIS_SYSTEM_PATH) //
           ? (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH) //
           : root.addNode(StorageImpl.XCMIS_SYSTEM_PATH.substring(1), "xcmis:system");
        if (!xCmisSystem.isNodeType("exo:hiddenable")) xCmisSystem.addMixin("exo:hiddenable");
  
        if (!xCmisSystem.hasNode(StorageImpl.XCMIS_WORKING_COPIES))
        {
           xCmisSystem.addNode(StorageImpl.XCMIS_WORKING_COPIES, "xcmis:workingCopies");
           if (LOG.isDebugEnabled())
           {
              LOG.debug("CMIS Working Copies store " + StorageImpl.XCMIS_SYSTEM_PATH + "/"
                 + StorageImpl.XCMIS_WORKING_COPIES + " created.");
           }
        }
  
        if (!xCmisSystem.hasNode(StorageImpl.XCMIS_RELATIONSHIPS))
        {
           xCmisSystem.addNode(StorageImpl.XCMIS_RELATIONSHIPS, "xcmis:relationships");
           if (LOG.isDebugEnabled())
           {
              LOG.debug("CMIS relationship store " + StorageImpl.XCMIS_SYSTEM_PATH + "/"
                 + StorageImpl.XCMIS_RELATIONSHIPS + " created.");
           }
        }
  
        if (!xCmisSystem.hasNode(StorageImpl.XCMIS_POLICIES))
        {
           xCmisSystem.addNode(StorageImpl.XCMIS_POLICIES, "xcmis:policies");
           if (LOG.isDebugEnabled())
           {
              LOG.debug("CMIS policies store " + StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_POLICIES
                 + " created.");
           }
        }
  
        session.save();
  
        Boolean persistRenditions = (Boolean)storageConfiguration.getProperties().get("exo.cmis.renditions.persistent");
        if (persistRenditions == null)
        {
           persistRenditions = false;
        }
        if (persistRenditions)
        {
           Workspace workspace = session.getWorkspace();
           try
           {
              boolean exist = false;
  
              // TODO can do this simpler ?
              WorkspaceContainerFacade workspaceContainer =
                 ((RepositoryImpl)repository).getWorkspaceContainer(workspace.getName());
              ObservationManagerRegistry observationManagerRegistry =
                 (ObservationManagerRegistry)workspaceContainer.getComponent(ObservationManagerRegistry.class);
  
              for (EventListenerIterator iter = observationManagerRegistry.getEventListeners(); iter.hasNext();)
              {
                 if (iter.nextEventListener().getClass() == RenditionsUpdateListener.class)
                 {
                    exist = true;
                    break;
                 }
              }
              if (!exist)
              {
                 workspace.getObservationManager().addEventListener(
                    new RenditionsUpdateListener(repository, storageConfiguration.getWorkspace()),
                    Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/", true, null, new String[]{JcrCMIS.NT_RESOURCE},
                    false);
              }
           }
           catch (RepositoryException e)
           {
             if (LOG.isErrorEnabled()) {
               LOG.error("Unable to create event listener. " + e.getMessage(), e);
             }
           }
        }
      } finally {
        session.logout();
      }
   }

   void addNodeTypeMapping(Map<String, TypeMapping> nodeTypeMapping)
   {
      this.nodeTypeMapping.putAll(nodeTypeMapping);
   }

   /**
    * @param searchService the searchService to set
    */
   public void setSearchService(SearchService searchService)
   {
      this.searchService = searchService;
   }
}
