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

import org.exoplatform.ecms.xcmis.sp.JcrCMIS;
import org.exoplatform.ecms.xcmis.sp.JcrCmisRegistry;
import org.exoplatform.ecms.xcmis.sp.StorageConfiguration;
import org.exoplatform.ecms.xcmis.sp.StorageImpl;
import org.exoplatform.ecms.xcmis.sp.StorageProviderImpl;
import org.exoplatform.ecms.xcmis.sp.TypeMapping;
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
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.model.BaseType;

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

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z
 *          aheritier $
 * 
 */
public class Jcr2XcmisChangesListener implements ItemsPersistenceListener
{

   private static final Log LOG = ExoLogger.getExoLogger(Jcr2XcmisChangesListener.class);

   private final String currentRepositoryName;

   private final String workspaceName;

   private final List<StorageProviderImpl> linkedStorages;

   private final SessionProviderService sessionProviderService;

   private final ManageableRepository repository;

   private SearchService searchService;

   private final PersistentDataManager dataManager;

   private StorageImpl rootStorage;

   private final LocationFactory locationFactory;

   private final ContentEntryAdapter contentEntryAdapter;

   public Jcr2XcmisChangesListener(String currentRepositoryName, String workspaceName,
      PersistentDataManager dataManager, SessionProviderService sessionProviderService,
      ManageableRepository repository, NamespaceAccessor namespaceAccessor)
   {
      super();
      this.currentRepositoryName = currentRepositoryName;
      this.workspaceName = workspaceName;
      this.dataManager = dataManager;
      this.sessionProviderService = sessionProviderService;
      this.repository = repository;
      this.linkedStorages = new ArrayList<StorageProviderImpl>();
      this.locationFactory = new LocationFactory(namespaceAccessor);
      this.contentEntryAdapter = new ContentEntryAdapter();

   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#isTXAware()
    */
   @Override
   public boolean isTXAware()
   {
      return false;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   @Override
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
         if (rootStorage.isSupportedNodeType(nodeTypeName))
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
               removedNodes.remove(linkedUUid);
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

                  //                  String nodeTypeName =
                  //                     locationFactory.createJCRName(((NodeData)itemState.getData()).getPrimaryTypeName()).getAsString();
                  //                  if (nodeTypeName.equals("nt:linkedFile"))
                  //                  {
                  //                     List<ItemState> nodes = updatedNodes.get(itemState.getData().getIdentifier());
                  //                     ItemData jcrContent = null;
                  //                     for (ItemState itemState2 : nodes)
                  //                     {
                  //                        if (itemState2.getData().getQPath().getName().equals(
                  //                           locationFactory.parseJCRName("jcr:content").getInternalName()))
                  //                        {
                  //                           jcrContent = itemState2.getData();
                  //                        }
                  //                     }
                  //                     if (jcrContent == null)
                  //                     {
                  //                        jcrContent =
                  //                           dataManager.getItemData((NodeData)itemState.getData(), new QPathEntry(locationFactory
                  //                              .parseJCRName("jcr:content").getInternalName(), 0));
                  //                     }
                  //                     if (!jcrContent.isNode())
                  //                     {
                  //                        String linkedUUid = new String(((PropertyData)jcrContent).getValues().get(0).getAsByteArray());
                  //                        createNewOrAdd(linkedUUid, null, updatedNodes);
                  //                     }
                  //
                  //                  }

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

   public void onRegistryStart(JcrCmisRegistry cmisRegistry)
   {

      initializeSearchService(cmisRegistry.getIndexConfiguration());
      cmisRegistry.addSearchService(currentRepositoryName, workspaceName, searchService);

   }

   private void initializeSearchService(IndexConfiguration readOnlyIndexConfiguration)
   {
      if (readOnlyIndexConfiguration != null)
      {
         try
         {

            rootStorage = createRootStorage();

            //prepare search service
            CmisSchema schema = new CmisSchema(rootStorage);
            CmisSchemaTableResolver tableResolver =
               new CmisSchemaTableResolver(new ToStringNameConverter(), schema, rootStorage);

            IndexConfiguration indexConfiguration = new IndexConfiguration();

            File rootFolder = new File(readOnlyIndexConfiguration.getIndexDir());
            File indexFolder = new File(new File(rootFolder, currentRepositoryName), workspaceName);

            indexConfiguration.setIndexDir(indexFolder.getPath());
            indexConfiguration.setDocumentReaderService(readOnlyIndexConfiguration.getDocumentReaderService());
            indexConfiguration.setRootUuid(Constants.ROOT_UUID);

            //if list of root parents is empty it will be indexed as empty string
            indexConfiguration.setRootParentUuid("");

            //default invocation context
            InvocationContext invocationContext = new InvocationContext();
            invocationContext.setNameConverter(new ToStringNameConverter());
            invocationContext.setSchema(schema);
            invocationContext.setPathSplitter(new SlashSplitter());
            invocationContext.setTableResolver(tableResolver);

            SearchServiceConfiguration configuration = new SearchServiceConfiguration();
            configuration.setIndexConfiguration(indexConfiguration);
            configuration.setContentReader(new CmisContentReader(rootStorage));
            configuration.setNameConverter(new ToStringNameConverter());
            configuration.setDefaultInvocationContext(invocationContext);
            configuration.setTableResolver(tableResolver);
            configuration.setPathSplitter(new SlashSplitter());

            searchService = new SearchService(configuration);
            searchService.start();

            //attach listener to the created storage
            //indexListener = new IndexListener(searchService);
            //storage.setIndexListener(indexListener);

         }
         catch (RepositoryException e)
         {
            throw new CmisRuntimeException(e.getLocalizedMessage(), e);

         }
         catch (SearchServiceException e)
         {
            throw new CmisRuntimeException(e.getLocalizedMessage(), e);
         }
      }
   }

   private StorageImpl createRootStorage() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      //TODO change this
      Map<String, TypeMapping> nodeTypeMapping = new HashMap<String, TypeMapping>();
      // Unstructured mapping immediately. May need have access
      // to root node which often has type nt:unstructured.
      nodeTypeMapping.put(JcrCMIS.NT_UNSTRUCTURED, new TypeMapping(JcrCMIS.NT_UNSTRUCTURED, BaseType.FOLDER,
         CmisConstants.FOLDER));
      nodeTypeMapping.put("exo:taxonomy", new TypeMapping("exo:taxonomy", BaseType.FOLDER, CmisConstants.FOLDER));

      StorageConfiguration rootStorageConfiguration =
         new StorageConfiguration(UUID.randomUUID().toString(), currentRepositoryName, workspaceName, "/",
            Collections.EMPTY_MAP, "Virtual root storage");
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      return new StorageImpl(sessionProvider.getSession(workspaceName, repository), rootStorageConfiguration, null,
         new PermissionService(), nodeTypeMapping);

      //return rootStorage;
   }

}
