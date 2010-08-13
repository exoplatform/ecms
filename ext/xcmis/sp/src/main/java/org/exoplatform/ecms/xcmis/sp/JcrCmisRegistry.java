package org.exoplatform.ecms.xcmis.sp;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.ecms.xcmis.sp.index.Jcr2XcmisChangesListener;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.impl.RepositoryServiceImpl;
import org.picocontainer.Startable;
import org.xcmis.search.SearchService;
import org.xcmis.search.config.IndexConfiguration;
import org.xcmis.spi.CmisRegistry;
import org.xcmis.spi.CmisRegistryFactory;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;

public class JcrCmisRegistry extends CmisRegistry implements Startable, CmisRegistryFactory
{
   private final List<Jcr2XcmisChangesListener> listeners;

   private final Map<String, SearchService> wsSearchServices;

   protected String rootIndexDir;

   private final DocumentReaderService documentReaderService;

   protected final InitParams initParams;

   private final RepositoryServiceImpl repositoryService;

   public JcrCmisRegistry(RepositoryServiceImpl repositoryService, DocumentReaderService documentReaderService,
      InitParams initParams)
   {
      this.initParams = initParams;
      this.documentReaderService = documentReaderService;
      this.repositoryService = repositoryService;
      this.listeners = new ArrayList<Jcr2XcmisChangesListener>();
      this.wsSearchServices = new HashMap<String, SearchService>();

   }

   /**
    * @see org.xcmis.spi.CmisRegistry#addStorage(org.xcmis.spi.StorageProvider)
    */
   @Override
   public void addStorage(StorageProvider storageProvider)
   {
      super.addStorage(storageProvider);
      StorageProviderImpl sp = (StorageProviderImpl)storageProvider;
      sp.setSearchService(wsSearchServices.get(sp.getStorageConfiguration().getRepository() + '@'
         + sp.getStorageConfiguration().getWorkspace()));
   }

   /**
    * @return the storageProviders
    */
   public Map<String, StorageProvider> getStorageProviders()
   {
      return storageProviders;
   }

   public void addListener(Jcr2XcmisChangesListener jcr2XcmisChangesListener)
   {
      listeners.add(jcr2XcmisChangesListener);
   }

   /**
    * 
    * @return list of workp
    * @throws RepositoryException
    */
   public String[] getAffectedWorkspaceNames() throws RepositoryException
   {
      return repositoryService.getCurrentRepository().getWorkspaceNames();
   }

   public void addSearchService(String jcrRepositoryName, String jcrWorkspaceName, SearchService searchService)
   {
      if (searchService != null)
      {
         wsSearchServices.put(jcrRepositoryName + "@" + jcrWorkspaceName, searchService);

         //reload existed sp
         for (Entry<String, StorageProvider> spEntry : storageProviders.entrySet())
         {
            StorageProviderImpl sp = (StorageProviderImpl)spEntry.getValue();
            if (sp.getStorageConfiguration().getRepository().equals(jcrRepositoryName)
               && sp.getStorageConfiguration().getWorkspace().equals(jcrWorkspaceName))
            {
               sp.setSearchService(searchService);
            }
         }
      }
   }

   public SearchService getSearchService(String jcrRepositoryName, String jcrWorkspaceName)
   {
      return wsSearchServices.get(jcrRepositoryName + "@" + jcrWorkspaceName);
   }

   /**
    * @see org.xcmis.spi.deploy.ExoContainerCmisRegistry#start()
    */
   @Override
   public void start()
   {
      if (initParams != null)
      {
         rootIndexDir = getValueParameter("indexDir", null);

         Iterator<ValuesParam> vparams = initParams.getValuesParamIterator();
         while (vparams.hasNext())
         {
            ValuesParam next = vparams.next();
            if (next.getName().equalsIgnoreCase("renditionProviders"))
            {
               renditionProviders.addAll(next.getValues());
            }
         }
      }
      RenditionManager manager = RenditionManager.getInstance();
      manager.addRenditionProviders(renditionProviders);
      setFactory(this);

      try
      {
         String[] wsNames = getAffectedWorkspaceNames();
         String currentRepositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
         for (String wsName : wsNames)
         {
            WorkspaceContainerFacade wsContainer =
               repositoryService.getCurrentRepository().getWorkspaceContainer(wsName);
            PersistentDataManager dm = (PersistentDataManager)wsContainer.getComponent(PersistentDataManager.class);
            SessionProviderService sessionProviderService =
               (SessionProviderService)wsContainer.getComponent(SessionProviderService.class);
            NamespaceAccessor namespaceAccessor = (NamespaceAccessor)wsContainer.getComponent(NamespaceAccessor.class);
            Jcr2XcmisChangesListener changesListener =
               new Jcr2XcmisChangesListener(currentRepositoryName, wsName, dm, sessionProviderService,
                  repositoryService.getCurrentRepository(), namespaceAccessor);
            changesListener.onRegistryStart(this);
            dm.addItemPersistenceListener(changesListener);

         }
      }
      catch (RepositoryException e)
      {
         throw new CmisRuntimeException(e.getLocalizedMessage(), e);
      }
   }

   /**
    * @see org.xcmis.spi.deploy.ExoContainerCmisRegistry#stop()
    */
   @Override
   public void stop()
   {
      for (Entry<String, SearchService> entry : wsSearchServices.entrySet())
      {
         SearchService value = entry.getValue();
         if (value != null)
         {
            value.stop();
         }
      }
   }

   protected String getValueParameter(String name, String defaultValue)
   {
      String value = null;
      if (initParams != null)
      {
         ValueParam valueParam = initParams.getValueParam(name);
         if (valueParam != null)
         {
            value = valueParam.getValue();
         }
      }
      return value != null ? value : defaultValue;
   }

   public IndexConfiguration getIndexConfiguration()
   {
      IndexConfiguration indexConfiguration = null;
      if (rootIndexDir != null && documentReaderService != null)
      {
         indexConfiguration = new IndexConfiguration();
         indexConfiguration.setIndexDir(rootIndexDir);
         indexConfiguration.setDocumentReaderService(documentReaderService);
      }
      return indexConfiguration;
   }

   @Override
   public CmisRegistry getRegistry()
   {
      return (CmisRegistry)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(CmisRegistry.class);
   }
}
