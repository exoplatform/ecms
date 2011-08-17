/*
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
package org.exoplatform.ecms.xcmis.sp.index;

import org.exoplatform.ecms.xcmis.sp.StorageClosableImpl;
import org.exoplatform.ecms.xcmis.sp.StorageConfiguration;
import org.exoplatform.ecms.xcmis.sp.StorageImpl;
import org.exoplatform.ecms.xcmis.sp.StorageProviderImpl;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.search.SearchService;
import org.xcmis.search.SearchServiceException;
import org.xcmis.search.config.IndexConfiguration;
import org.xcmis.search.config.SearchServiceConfiguration;
import org.xcmis.search.content.ContentEntry;
import org.xcmis.search.content.IndexModificationException;
import org.xcmis.search.content.command.InvocationContext;
import org.xcmis.search.value.SlashSplitter;
import org.xcmis.search.value.ToStringNameConverter;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.Storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: Jcr2XcmisChangesListener.java 34360 2009-07-22 23:58:59Z
 *          aheritier $
 *
 */
public class Jcr2XcmisChangesListener implements ItemsPersistenceListener
{

   private static final Log LOG = ExoLogger.getExoLogger(Jcr2XcmisChangesListener.class);

   private final String currentRepositoryName;

   private final String workspaceName;

   private final SessionProviderService sessionProviderService;

   private final ManageableRepository repository;

   private SearchService searchService;

   private final PersistentDataManager dataManager;

   private Storage rootStorage;

   private final LocationFactory locationFactory;

   private final ContentEntryAdapter contentEntryAdapter;

   private final DocumentReaderService documentReaderService;

   public Jcr2XcmisChangesListener(String currentRepositoryName, String workspaceName,
      PersistentDataManager dataManager, SessionProviderService sessionProviderService,
      ManageableRepository repository, NamespaceAccessor namespaceAccessor, DocumentReaderService documentReaderService)
   {
      super();
      this.currentRepositoryName = currentRepositoryName;
      this.workspaceName = workspaceName;
      this.dataManager = dataManager;
      this.sessionProviderService = sessionProviderService;
      this.repository = repository;
      this.documentReaderService = documentReaderService;
      this.locationFactory = new LocationFactory(namespaceAccessor);
      this.contentEntryAdapter = new ContentEntryAdapter();
   }

   /**
    * @return the searchService
    */
   public SearchService getSearchService()
   {
      return searchService;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#isTXAware()
    */
   public boolean isTXAware()
   {
      return false;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      applyChangesLog(itemStates);
   }

   private void applyChangesLog(ItemStateChangesLog itemStates)
   {
      if (searchService != null)
      {
         // nodes that need to be removed from the index.
         final Set<String> removedNodes = new HashSet<String>();
         // nodes that need to be added to the index.
         final Set<String> addedNodes = new HashSet<String>();
         //updated
         final Map<String, List<ItemState>> updatedNodes = new HashMap<String, List<ItemState>>();

         for (ItemState itemState : itemStates.getAllStates())
         {
            try
            {
               acceptChanges(removedNodes, addedNodes, updatedNodes, itemState);
            }
            catch (RepositoryException e)
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug(e.getLocalizedMessage(), e);
               }
            }
            catch (IllegalStateException e)
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug(e.getLocalizedMessage(), e);
               }
            }
            catch (IOException e)
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug(e.getLocalizedMessage(), e);
               }
            }
         }
         for (String uuid : updatedNodes.keySet())
         {
            removedNodes.add(uuid);
            addedNodes.add(uuid);
         }
         if (removedNodes.size() > 0 || addedNodes.size() > 0)
         {
            List<ContentEntry> addedEntries = new ArrayList<ContentEntry>(addedNodes.size());

            for (String id : addedNodes)
            {
               try
               {
                  addEntry(id, addedEntries, removedNodes);
               }
               catch (ObjectNotFoundException e)
               {
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug(e.getLocalizedMessage(), e);
                  }
               }
               catch (IOException e)
               {
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug(e.getLocalizedMessage(), e);
                  }
               }
               catch (RepositoryException e)
               {
                  if (LOG.isDebugEnabled())
                  {
                     LOG.debug(e.getLocalizedMessage(), e);
                  }
               }
            }
            //remove one by one. ignore exceptions
            for (String uuid : removedNodes)
            {
               try
               {
                  searchService.update(null, uuid);
               }
               catch (IndexModificationException e)
               {
                  //LOG.error(e.getLocalizedMessage(), e);
               }
            }
            try
            {
               searchService.update(addedEntries, Collections.EMPTY_SET);
            }
            catch (IndexModificationException e)
            {
               LOG.error(e.getLocalizedMessage(), e);
            }
         }
      }
   }

   private void addEntry(String uuid, List<ContentEntry> addedEntries, Set<String> removedNodes)
      throws RepositoryException, ObjectNotFoundException, IOException
   {
      ItemData data = dataManager.getItemData(uuid);
      if (data != null && data.isNode())
      {

         String nodeTypeName = locationFactory.createJCRName(((NodeData)data).getPrimaryTypeName()).getAsString();
         if (((StorageClosableImpl)rootStorage).isSupportedNodeType(nodeTypeName))
         {

            addedEntries.add(contentEntryAdapter.createEntry(rootStorage.getObjectById(uuid)));
         }
         else if (nodeTypeName.equals("nt:linkedFile"))
         {

            ItemData content =
               dataManager.getItemData((NodeData)data, new QPathEntry(locationFactory.parseJCRName("jcr:content")
                  .getInternalName(), 0));
            if (!content.isNode())
            {
               String linkedUUid = new String(((PropertyData)content).getValues().get(0).getAsByteArray());
               addEntry(linkedUUid, addedEntries, removedNodes);
               removedNodes.add(linkedUUid);
            }
         }
      }
   }

   /**
    *
    * @param removedNodes
    * @param addedNodes
    * @param updatedNodes
    * @param itemState
    * @throws RepositoryException
    * @throws IOException
    * @throws IllegalStateException
    */
   private void acceptChanges(final Set<String> removedNodes, final Set<String> addedNodes,
      final Map<String, List<ItemState>> updatedNodes, ItemState itemState) throws RepositoryException,
      IllegalStateException, IOException
   {
      {
         String uuid =
            itemState.isNode() ? itemState.getData().getIdentifier() : itemState.getData().getParentIdentifier();

         if (itemState.isAdded())
         {
            if (itemState.isNode())
            {
               addedNodes.add(uuid);
            }
            else
            {
               if (!addedNodes.contains(uuid))
               {
                  createNewOrAdd(uuid, itemState, updatedNodes);
               }
            }
         }
         else if (itemState.isRenamed())
         {
            if (itemState.isNode())
            {
               addedNodes.add(uuid);
            }
            else
            {
               createNewOrAdd(uuid, itemState, updatedNodes);
            }
         }
         else if (itemState.isUpdated())
         {
            createNewOrAdd(uuid, itemState, updatedNodes);
         }
         else if (itemState.isMixinChanged())
         {
            createNewOrAdd(uuid, itemState, updatedNodes);
         }
         else if (itemState.isDeleted())
         {
            if (itemState.isNode())
            {
               if (addedNodes.contains(uuid))
               {
                  addedNodes.remove(uuid);
                  removedNodes.remove(uuid);
               }
               else
               {

                  removedNodes.add(uuid);
               }
               // remove all changes after node remove
               updatedNodes.remove(uuid);
            }
            else
            {
               if (!removedNodes.contains(uuid) && !addedNodes.contains(uuid))
               {
                  createNewOrAdd(uuid, itemState, updatedNodes);
               }
            }
         }
      }
   }

   private void createNewOrAdd(String key, ItemState state, Map<String, List<ItemState>> updatedNodes)
   {
      List<ItemState> list = updatedNodes.get(key);
      if (list == null)
      {
         list = new ArrayList<ItemState>();
         updatedNodes.put(key, list);
      }
      list.add(state);

   }

   /**
    * Called on CmisRegistry start. It will initialize search service.
    *
    * @param cmisRegistry
    * @param readOnlyIndexConfiguration
    * @throws RepositoryException
    * @throws SearchServiceException
    */
   public void onRegistryStart(IndexConfiguration readOnlyIndexConfiguration) throws RepositoryException,
      SearchServiceException
   {

      if (readOnlyIndexConfiguration != null && rootStorage == null)
      {

         try {
	         StorageConfiguration rootStorageConfiguration =
	            new StorageConfiguration(UUID.randomUUID().toString(), currentRepositoryName, workspaceName, "/",
	               Collections.EMPTY_MAP, "Virtual root storage");
	         SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
	         
	      // to session check
            Session session = null;
            try
            {
               session = sessionProvider.getSession(workspaceName, repository);
            }
            finally
            {
               session.logout();
            }
            rootStorage =
             new StorageClosableImpl(sessionProvider, workspaceName, repository, rootStorageConfiguration, new PermissionService(),
                StorageProviderImpl.DEFAULT_NODETYPE_MAPPING);
	
	         //prepare search service
	         CmisSchema schema = new CmisSchema(rootStorage);
	         CmisSchemaTableResolver tableResolver =
	            new CmisSchemaTableResolver(new ToStringNameConverter(), schema, rootStorage);
	
	         File rootFolder = new File(readOnlyIndexConfiguration.getIndexDir());
	         File indexFolder = new File(new File(rootFolder, currentRepositoryName), workspaceName);
	         
	         IndexConfiguration indexConfiguration = new IndexConfiguration(indexFolder.getPath(), Constants.ROOT_PARENT_UUID, Constants.ROOT_UUID);
	
	         SearchServiceConfiguration configuration = new SearchServiceConfiguration(schema, tableResolver,
	        		 new CmisContentReader(rootStorage), indexConfiguration);
	
	         searchService = new SearchService(configuration);
	         searchService.start();
	         
    	 } catch (org.apache.tika.mime.MimeTypeException e) {
    	    throw new SearchServiceException(e.getLocalizedMessage(), e);
	     } catch (java.io.IOException e) {
		    throw new SearchServiceException(e.getLocalizedMessage(), e);
	     }
      }
   }

}
