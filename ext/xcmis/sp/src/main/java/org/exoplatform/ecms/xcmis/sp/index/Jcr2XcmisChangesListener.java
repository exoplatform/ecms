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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.ecms.xcmis.sp.JcrCMIS;
import org.exoplatform.ecms.xcmis.sp.StorageClosableImpl;
import org.exoplatform.ecms.xcmis.sp.StorageConfiguration;
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
import org.xcmis.search.value.ToStringNameConverter;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.Storage;

/**
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: Jcr2XcmisChangesListener.java 34360 2009-07-22 23:58:59Z
 *          aheritier $
 */
public class Jcr2XcmisChangesListener implements ItemsPersistenceListener {

  private static final Log             LOG = ExoLogger.getExoLogger(Jcr2XcmisChangesListener.class);

  private final String                 currentRepositoryName;

  private final String                 workspaceName;

  private final SessionProviderService sessionProviderService;

  private final ManageableRepository   repository;

  private SearchService                searchService;

  private final PersistentDataManager  dataManager;

  private Storage                      rootStorage;

  private final LocationFactory        locationFactory;

  private final ContentEntryAdapter    contentEntryAdapter;

  // not used
  private final DocumentReaderService  documentReaderService;

  public Jcr2XcmisChangesListener(String currentRepositoryName,
                                  String workspaceName,
                                  PersistentDataManager dataManager,
                                  SessionProviderService sessionProviderService,
                                  ManageableRepository repository,
                                  NamespaceAccessor namespaceAccessor,
                                  DocumentReaderService documentReaderService) {
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
  public SearchService getSearchService() {
    return searchService;
  }

  /**
   * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#isTXAware()
   */
  public boolean isTXAware() {
    return false;
  }

  /**
   * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener
   * #onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
   */
  public void onSaveItems(ItemStateChangesLog itemStates) {
    applyChangesLog(itemStates);
  }

  private void applyChangesLog(ItemStateChangesLog itemStates) {
    if (searchService != null) {
      // nodes that need to be removed from the index.
      final Set<String> removedNodes = new HashSet<String>();
      // nodes that need to be added to the index.
      final Set<String> addedNodes = new HashSet<String>();
      // updated
      final Map<String, List<ItemState>> updatedNodes = new HashMap<String, List<ItemState>>();

      String versionHistoryId = null;
      String linkedUUid = null;

      for (ItemState itemState : itemStates.getAllStates()) {
        if (itemState.getData().isNode() && itemState.getData() instanceof NodeData
            && itemState.isDeleted()) {
          NodeData nodeData = (NodeData) itemState.getData();

          if (nodeData.getPrimaryTypeName().equals(Constants.NT_VERSION)) {

            String v = nodeData.getQPath().getName().getName();

            if (v.equals("rootVersion")) {
              // save parent for the root version delete
              if (versionHistoryId != null) {
                removedNodes.add(versionHistoryId + JcrCMIS.ID_SEPARATOR + "1");
                versionHistoryId = null;
              }
              versionHistoryId = nodeData.getParentIdentifier();
            } else {
              Integer versionInt = null;
              try {
                versionInt = Integer.parseInt(v);
              } catch (NumberFormatException e) {
                if (LOG.isWarnEnabled()) {
                  LOG.warn(e.getMessage());
                }
              }
              if (versionInt != null) {
                String versionId = nodeData.getParentIdentifier() + JcrCMIS.ID_SEPARATOR + v;
                if (versionHistoryId != null) {
                  if (versionId.startsWith(versionHistoryId)) {
                    String rootVersionId = nodeData.getParentIdentifier() + JcrCMIS.ID_SEPARATOR
                        + String.valueOf(versionInt + 1);
                    removedNodes.add(rootVersionId);
                  } else {
                    removedNodes.add(versionHistoryId + JcrCMIS.ID_SEPARATOR + "1");
                  }
                  versionHistoryId = null;
                }
                removedNodes.add(versionId);
              }
            }

          } else if (nodeData.getPrimaryTypeName()
                             .getAsString()
                             .equalsIgnoreCase("[http://www.exoplatform.com/jcr/xcmis/1.0]linkedFile")) {
            if (linkedUUid != null) {
              removedNodes.add(linkedUUid);
              addedNodes.add(linkedUUid);
            }
          }
        } else if (!itemState.getData().isNode() && itemState.isDeleted()) {
          PropertyData propertyData = (PropertyData) itemState.getData();

          // Parse the path as example:
          // "[]:1[http://www.exoplatform.com/jcr/exo/1.0]drives:1[]driveA:1" +
          // "[]multifilingFolderTest1:1[]multifilingDocumentTest:1[]cmisMultifilingObjectId_d2abd7987f00010110f98160154c674d:1"
          String qPath = propertyData.getQPath().getAsString();
          if (qPath.contains("[]" + JcrCMIS.JCR_MULTIFILING_PROPERTY_PREFIX)) {
            int beginIndex = qPath.lastIndexOf(JcrCMIS.JCR_MULTIFILING_PROPERTY_PREFIX)
                + JcrCMIS.JCR_MULTIFILING_PROPERTY_PREFIX.length();
            int endIndex = qPath.lastIndexOf(":");
            linkedUUid = qPath.substring(beginIndex, endIndex);
          }
        }

        try {
          acceptChanges(removedNodes, addedNodes, updatedNodes, itemState);
        } catch (RepositoryException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(e.getLocalizedMessage(), e);
          }
        } catch (IllegalStateException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(e.getLocalizedMessage(), e);
          }
        } catch (IOException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(e.getLocalizedMessage(), e);
          }
        }
      }
      if (versionHistoryId != null) {
        removedNodes.add(versionHistoryId + JcrCMIS.ID_SEPARATOR + "1");
        versionHistoryId = null;
      }

      for (String uuid : updatedNodes.keySet()) {
        removedNodes.add(uuid);
        addedNodes.add(uuid);
      }
      if (removedNodes.size() > 0 || addedNodes.size() > 0) {
        List<ContentEntry> addedEntries = new ArrayList<ContentEntry>(addedNodes.size());

        // process addedNodes to addedEntries
        for (String id : addedNodes) {
          try {
            addEntry(id, addedEntries, removedNodes);
          } catch (ObjectNotFoundException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(e.getLocalizedMessage(), e);
            }
          } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(e.getLocalizedMessage(), e);
            }
          } catch (RepositoryException e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(e.getLocalizedMessage(), e);
            }
          }
        }
        // remove one by one. ignore exceptions
        for (String uuid : removedNodes) {
          try {
            searchService.update(null, uuid);
          } catch (IndexModificationException e) {
            // LOG.error(e.getLocalizedMessage(), e);
          }
        }
        try {
          searchService.update(addedEntries, Collections.EMPTY_SET);
        } catch (IndexModificationException e) {
          if (LOG.isErrorEnabled()) {
            LOG.error(e.getLocalizedMessage(), e);
          }
        }
      }
    }
  }

  /**
   * Process the uuid from addedNodes to addedEntries. Can be updated
   * removedNodes.
   * 
   * @param uuid to be add.
   * @param addedEntries will be added in index.
   * @param removedNodes
   * @throws RepositoryException
   * @throws ObjectNotFoundException
   * @throws IOException
   */
  private void addEntry(String uuid, List<ContentEntry> addedEntries, Set<String> removedNodes) throws RepositoryException,
                                                                                               ObjectNotFoundException,
                                                                                               IOException {
    ItemData data = dataManager.getItemData(uuid);
    if (data != null && data.isNode()) {

      String nodeTypeName = locationFactory.createJCRName(((NodeData) data).getPrimaryTypeName())
                                           .getAsString();
      if (((StorageClosableImpl) rootStorage).isSupportedNodeType(nodeTypeName)) {
        ObjectData objectData = ((StorageClosableImpl) rootStorage).getObjectById(uuid);

        addedEntries.add(contentEntryAdapter.createEntry(objectData));

        if (objectData instanceof DocumentData) {
          String objectId = objectData.getObjectId();
          if (uuid != objectData.getObjectId()) {
            // for multifiling: remove document from index before adding in it
            // again
            // for checkout/checkin: remove document from index before adding in
            // it again
            // for updating the document: to remove from index with appropriate
            // id before adding in it again
            // id must has ID_SEPARATOR since cannot be filled PWC
            removedNodes.add(objectId);
          }
        }

      } else if (nodeTypeName.equals(JcrCMIS.JCR_XCMIS_LINKEDFILE)) {
        List<PropertyData> list = dataManager.getChildPropertiesData((NodeData) data);

        PropertyData propertyWithId = null;
        for (Iterator<PropertyData> iter = list.iterator(); iter.hasNext()
            && propertyWithId == null;) {
          PropertyData nextProperty = iter.next();
          // iterate while don't get the property with CMIS Object Id in the
          // name.
          // xcmis:linkedFile extends nt:base which has two properties by
          // default: jcr:primaryType and jcr:mixinTypes
          if (!nextProperty.getQPath()
                           .getAsString()
                           .contains(Constants.JCR_PRIMARYTYPE.getAsString())
              && !nextProperty.getQPath()
                              .getAsString()
                              .contains(Constants.JCR_MIXINTYPES.getAsString())) {
            propertyWithId = nextProperty;
          }
        }

        String linkedUUid = new String(propertyWithId.getValues().get(0).getAsByteArray());
        addEntry(linkedUUid, addedEntries, removedNodes);
      }
    }
  }

  /**
   * @param removedNodes
   * @param addedNodes
   * @param updatedNodes
   * @param itemState
   * @throws RepositoryException
   * @throws IOException
   * @throws IllegalStateException
   */
  private void acceptChanges(final Set<String> removedNodes,
                             final Set<String> addedNodes,
                             final Map<String, List<ItemState>> updatedNodes,
                             ItemState itemState) throws RepositoryException,
                                                 IllegalStateException,
                                                 IOException {
    {
      String uuidNode = itemState.isNode() ? itemState.getData().getIdentifier()
                                          : itemState.getData().getParentIdentifier();

      if (itemState.isAdded()) {
        if (itemState.isNode()) {
          addedNodes.add(uuidNode);
        } else {
          addToUpdate(uuidNode, itemState, updatedNodes);
        }
      } else if (itemState.isRenamed()) {
        if (itemState.isNode()) {
          addedNodes.add(uuidNode);
        } else {
          addToUpdate(uuidNode, itemState, updatedNodes);
        }
      } else if (itemState.isUpdated()) {
        addToUpdate(uuidNode, itemState, updatedNodes);
      } else if (itemState.isMixinChanged()) {
        addToUpdate(uuidNode, itemState, updatedNodes);
      } else if (itemState.isDeleted()) {
        if (itemState.isNode()) {
          if (addedNodes.contains(uuidNode)) {
            addedNodes.remove(uuidNode);
          }
          removedNodes.add(uuidNode);
          updatedNodes.remove(uuidNode);
        } else {
          // if deleted some property in node uuidNode
          addToUpdate(uuidNode, itemState, updatedNodes);
        }
      }
    }
  }

  private void addToUpdate(String key, ItemState state, Map<String, List<ItemState>> updatedNodes) {
    List<ItemState> list = updatedNodes.get(key);
    if (list == null) {
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
                                                                            SearchServiceException {

    if (readOnlyIndexConfiguration != null && rootStorage == null) {

      try {
        StorageConfiguration rootStorageConfiguration = new StorageConfiguration(UUID.randomUUID()
                                                                                     .toString(),
                                                                                 currentRepositoryName,
                                                                                 workspaceName,
                                                                                 "/",
                                                                                 Collections.EMPTY_MAP,
                                                                                 "Virtual root storage");
        SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);

        // to session check
        Session session = null;
        try {
          session = sessionProvider.getSession(workspaceName, repository);
        } finally {
          session.logout();
        }
        rootStorage = new StorageClosableImpl(sessionProvider,
                                              workspaceName,
                                              repository,
                                              rootStorageConfiguration,
                                              new PermissionService(),
                                              StorageProviderImpl.DEFAULT_NODETYPE_MAPPING);

        // prepare search service
        CmisSchema schema = new CmisSchema(rootStorage);
        CmisSchemaTableResolver tableResolver = new CmisSchemaTableResolver(new ToStringNameConverter(),
                                                                            schema,
                                                                            rootStorage);

        File rootFolder = new File(readOnlyIndexConfiguration.getIndexDir());
        File indexFolder = new File(new File(rootFolder, currentRepositoryName), workspaceName);

        IndexConfiguration indexConfiguration = new IndexConfiguration(indexFolder.getPath(),
                                                                       Constants.ROOT_PARENT_UUID,
                                                                       Constants.ROOT_UUID);

        SearchServiceConfiguration configuration = new SearchServiceConfiguration(schema,
                                                                                  tableResolver,
                                                                                  new CmisContentReader(rootStorage),
                                                                                  indexConfiguration);

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
