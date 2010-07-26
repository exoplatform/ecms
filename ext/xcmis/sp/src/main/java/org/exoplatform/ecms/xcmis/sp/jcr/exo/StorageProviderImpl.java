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

package org.exoplatform.ecms.xcmis.sp.jcr.exo;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.ecms.xcmis.sp.jcr.exo.index.CmisContentReader;
import org.exoplatform.ecms.xcmis.sp.jcr.exo.index.CmisSchema;
import org.exoplatform.ecms.xcmis.sp.jcr.exo.index.CmisSchemaTableResolver;
import org.exoplatform.ecms.xcmis.sp.jcr.exo.index.IndexListener;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;
import org.xcmis.search.SearchService;
import org.xcmis.search.SearchServiceException;
import org.xcmis.search.config.IndexConfiguration;
import org.xcmis.search.config.SearchServiceConfiguration;
import org.xcmis.search.content.command.InvocationContext;
import org.xcmis.search.value.SlashSplitter;
import org.xcmis.search.value.ToStringNameConverter;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.Connection;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.Storage;
import org.xcmis.spi.StorageProvider;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

/**
 * @author <a href="mailto:andrey00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: StorageProviderImpl.java 1262 2010-06-09 10:07:01Z andrew00x $
 */
public class StorageProviderImpl implements StorageProvider, Startable
{

   public static class StorageProviderConfig
   {

      /**
       * The storage configuration.
       */
      private StorageConfiguration storage;

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

   private static final Log LOG = ExoLogger.getLogger(StorageProviderImpl.class);

   private final RepositoryService repositoryService;

   private final DocumentReaderService documentReaderService;

   private RenditionManager renditionManager;

   private StorageConfiguration storageConfig = null;

   private PermissionService permissionService;

   private final Map<String, SearchService> searchServices = new HashMap<String, SearchService>();

   public StorageProviderImpl(RepositoryService repositoryService, InitParams initParams,
      DocumentReaderService documentReaderService, PermissionService permissionService)
   {
      this.repositoryService = repositoryService;
      this.documentReaderService = documentReaderService;
      this.permissionService = permissionService;

      if (initParams != null)
      {
         ObjectParameter param = initParams.getObjectParam("configs");

         if (param == null)
         {
            LOG.error("Init-params does not contain configuration for any CMIS repository.");
         }

         StorageProviderConfig confs = (StorageProviderConfig)param.getObject();

         this.storageConfig = confs.getStorage();
      }
      else
      {
         LOG.error("Not found configuration for any storages.");
      }

   }

   /**
    * {@inheritDoc}
    */
   public Connection getConnection()
   {
      if (storageConfig == null)
      {
         throw new InvalidArgumentException("Not any CMIS repository  exist.");
      }

      String repositoryId = storageConfig.getRepository();
      String ws = storageConfig.getWorkspace();

      try
      {
         ManageableRepository repository = repositoryService.getRepository(repositoryId);
         Session session = repository.login(ws);

         SearchService searchService = getSearchService(storageConfig.getId());
         Storage storage =
            new QueryableStorage(session, storageConfig, renditionManager, searchService, permissionService);
         IndexListener indexListener = new IndexListener(storage, searchService);
         //TODO make this method public
         ((StorageImpl)storage).setIndexListener(indexListener);

         return new JcrConnection(storage);

      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get CMIS repository " + storageConfig.getId() + ". " + re.getMessage(),
            re);
      }
      catch (RepositoryConfigurationException rce)
      {
         throw new CmisRuntimeException(
            "Unable get CMIS repository " + storageConfig.getId() + ". " + rce.getMessage(), rce);
      }
      catch (SearchServiceException rce)
      {
         throw new CmisRuntimeException(
            "Unable get CMIS repository " + storageConfig.getId() + ". " + rce.getMessage(), rce);
      }
   }

   /**
    * Gets the search service.
    * 
    * @param id String
    * @return instance of {@link SearchService}
    * @throws SearchServiceException
    */
   private SearchService getSearchService(String id) throws SearchServiceException
   {
      return searchServices.get(id);
   }

   public String getStorageID()
   {
      return storageConfig.getId();
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      SessionProvider systemProvider = SessionProvider.createSystemProvider();

      try
      {
         ManageableRepository repository = repositoryService.getRepository(storageConfig.getRepository());

         Session session = systemProvider.getSession(storageConfig.getWorkspace(), repository);

         Node root = session.getRootNode();

         Node xCmisSystem = session.itemExists(StorageImpl.XCMIS_SYSTEM_PATH) //
            ? (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH) //
            : root.addNode(StorageImpl.XCMIS_SYSTEM_PATH.substring(1), "xcmis:system");

         if (!xCmisSystem.hasNode(StorageImpl.XCMIS_UNFILED))
         {
            xCmisSystem.addNode(StorageImpl.XCMIS_UNFILED, "xcmis:unfiled");
            if (LOG.isDebugEnabled())
            {
               LOG.debug("CMIS unfiled storage " + StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED
                  + " created.");
            }
         }

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
         this.renditionManager = RenditionManager.getInstance();

         boolean isPersistRenditions = false;

         if (storageConfig.getProperties() != null
            && storageConfig.getProperties().get("exo.cmis.renditions.persistent") != null)
         {
            isPersistRenditions = (Boolean)storageConfig.getProperties().get("exo.cmis.renditions.persistent");
         }
         if (isPersistRenditions)
         {
            Workspace workspace = session.getWorkspace();
            try
            {
               EventListenerIterator it = workspace.getObservationManager().getRegisteredEventListeners();
               boolean exist = false;
               while (it.hasNext())
               {
                  EventListener one = it.nextEventListener();
                  if (one.getClass() == RenditionsUpdateListener.class)
                  {
                     exist = true;
                  }
               }

               if (!exist)
               {
                  workspace.getObservationManager().addEventListener(
                     new RenditionsUpdateListener(repository, storageConfig.getWorkspace(), renditionManager),
                     Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED, "/", true, null,
                     new String[]{JcrCMIS.NT_FILE, JcrCMIS.NT_RESOURCE}, false);
               }
            }
            catch (Exception ex)
            {
               LOG.error("Unable to create event listener, " + ex.getMessage());
            }
         }
         //prepare search service
         StorageImpl storage = new StorageImpl(session, storageConfig, permissionService);
         CmisSchema schema = new CmisSchema(storage);
         CmisSchemaTableResolver tableResolver =
            new CmisSchemaTableResolver(new ToStringNameConverter(), schema, storage);

         IndexConfiguration indexConfiguration = storageConfig.getIndexConfiguration();
         indexConfiguration.setRootUuid(storage.getRepositoryInfo().getRootFolderId());
         //if list of root parents is empty it will be indexed as empty string
         indexConfiguration.setRootParentUuid("");
         indexConfiguration.setDocumentReaderService(documentReaderService);

         //default invocation context
         InvocationContext invocationContext = new InvocationContext();
         invocationContext.setNameConverter(new ToStringNameConverter());

         invocationContext.setSchema(schema);
         invocationContext.setPathSplitter(new SlashSplitter());

         invocationContext.setTableResolver(tableResolver);

         SearchServiceConfiguration configuration = new SearchServiceConfiguration();
         configuration.setIndexConfiguration(indexConfiguration);
         configuration.setContentReader(new CmisContentReader(storage));
         configuration.setNameConverter(new ToStringNameConverter());
         configuration.setDefaultInvocationContext(invocationContext);
         configuration.setTableResolver(tableResolver);
         configuration.setPathSplitter(new SlashSplitter());

         SearchService searchService = new SearchService(configuration);
         searchService.start();

         //attach listener to the created storage
         IndexListener indexListener = new IndexListener(storage, searchService);
         storage.setIndexListener(indexListener);

         searchServices.put(storageConfig.getId(), searchService);

      }
      catch (RepositoryConfigurationException rce)
      {
         LOG.error("Unable to initialize storage. ", rce);
      }
      catch (javax.jcr.RepositoryException re)
      {
         LOG.error("Unable to initialize storage. ", re);
      }
      catch (SearchServiceException e)
      {
         LOG.error("Unable to initialize storage. ", e);
      }
      finally
      {
         systemProvider.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      for (SearchService searchService : searchServices.values())
      {
         searchService.stop();
      }
   }

   public StorageConfiguration getStorageConfiguration()
   {
      return storageConfig;
   }
}
