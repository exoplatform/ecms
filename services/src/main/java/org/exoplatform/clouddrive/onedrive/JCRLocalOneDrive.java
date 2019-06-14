package org.exoplatform.clouddrive.onedrive;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.FileSystemInfo;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.SharingLink;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionRequestBuilder;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.clouddrive.*;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudFile;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.oauth2.UserTokenRefreshListener;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JCRLocalOneDrive extends JCRLocalCloudDrive implements UserTokenRefreshListener {

    private static final Log LOG = ExoLogger.getLogger(JCRLocalOneDrive.class);

    protected JCRLocalOneDrive(CloudUser user,
                               Node driveNode,
                               SessionProviderService sessionProviders,
                               NodeFinder finder,
                               ExtendedMimeTypeResolver mimeTypes)
            throws CloudDriveException,
            RepositoryException {
        super(user, driveNode, sessionProviders, finder, mimeTypes);
        if (LOG.isDebugEnabled()) {
            LOG.debug("JCRLocalOneDrive():  ");
        }
        getUser().api().getStoredToken().addListener(this);
    }

    protected JCRLocalOneDrive(OneDriveConnector.API apiBuilder,
                               OneDriveProvider provider,
                               Node driveNode,
                               SessionProviderService sessionProviders,
                               NodeFinder finder,
                               ExtendedMimeTypeResolver mimeTypes)
            throws RepositoryException,
            CloudDriveException,
            IOException {
        super(loadUser(apiBuilder, provider, driveNode), driveNode, sessionProviders, finder, mimeTypes);
        if (LOG.isDebugEnabled()) {
            LOG.debug("JCRLocalOneDrive():  ");
        }
        getUser().api().getStoredToken().addListener(this);
    }

    protected static OneDriveUser loadUser(OneDriveConnector.API apiBuilder,
                                           OneDriveProvider provider,
                                           Node driveNode) throws RepositoryException, CloudDriveException, IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("LoadUser(): ");
        }
        String username = driveNode.getProperty("ecd:cloudUserName").getString();
        String email = driveNode.getProperty("ecd:userEmail").getString();
        String userId = driveNode.getProperty("ecd:cloudUserId").getString();
        String accessToken = driveNode.getProperty("onedrive:oauth2AccessToken").getString();
        String refreshToken;
        try {
            refreshToken = driveNode.getProperty("onedrive:oauth2RefreshToken").getString();
        } catch (PathNotFoundException e) {
            refreshToken = null;
        }
        long expirationTime = driveNode.getProperty("onedrive:oauth2TokenExpirationTime").getLong();
        OneDriveAPI driveAPI = apiBuilder.load(refreshToken, accessToken, expirationTime).build();

        return new OneDriveUser(userId, username, email, provider, driveAPI);
    }

    @Override
    protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
        return new OneDriveConnectCommand();
    }

    @Override
    protected SyncCommand getSyncCommand() {
        return new OneDriveSyncCommand();
    }

    @Override
    protected CloudFileAPI createFileAPI() {
        return new OneDriveFileAPI();
    }

    @Override
    protected Long readChangeId() {
        return System.currentTimeMillis();
    }

    @Override
    protected void saveChangeId(Long id) {
    }

    @Override
    public OneDriveUser getUser() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getUser()");
        }
        return (OneDriveUser) this.user;
    }

    @Override
    protected void refreshAccess() throws CloudDriveException {

    }

    @Override
    protected void initDrive(Node driveNode) throws CloudDriveException, RepositoryException {
        super.initDrive(driveNode);
        driveNode.setProperty("ecd:id", getUser().api().getRootId());
//    driveNode.save();
    }

    @Override
    protected void updateAccess(CloudUser newUser) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("updateAccess");
        }
        getUser().api().updateToken(((OneDriveUser) newUser).api().getStoredToken());
    }

    @Override
    public void onUserTokenRefresh(UserToken token) throws CloudDriveException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onUserTokenRefresh(): ");
        }
        try {
            jcrListener.disable();
            Node driveNode = rootNode();
            try {
                driveNode.setProperty("onedrive:oauth2AccessToken", token.getAccessToken());
                driveNode.setProperty("onedrive:oauth2RefreshToken", token.getRefreshToken());
                driveNode.setProperty("onedrive:oauth2TokenExpirationTime", token.getExpirationTime());

                driveNode.save();
            } catch (RepositoryException e) {
                rollback(driveNode);
                throw new CloudDriveException("Error updating access key: " + e.getMessage(), e);
            }
        } catch (DriveRemovedException e) {
            throw new CloudDriveException("Error openning drive node: " + e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new CloudDriveException("Error reading drive node: " + e.getMessage(), e);
        } finally {
            jcrListener.enable();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUserTokenRemove() throws CloudDriveException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("onUserTokenRemove(): ");
        }
        try {
            jcrListener.disable();
            Node driveNode = rootNode();
            try {
                if (driveNode.hasProperty("onedrive:oauth2AccessToken")) {
                    driveNode.getProperty("onedrive:oauth2AccessToken").remove();
                }
                if (driveNode.hasProperty("onedrive:oauth2RefreshToken")) {
                    driveNode.getProperty("onedrive:oauth2RefreshToken").remove();
                }
                if (driveNode.hasProperty("onedrive:oauth2TokenExpirationTime")) {
                    driveNode.getProperty("onedrive:oauth2TokenExpirationTime").remove();
                }

                driveNode.save();
            } catch (RepositoryException e) {
                rollback(driveNode);
                throw new CloudDriveException("Error removing access key: " + e.getMessage(), e);
            }
        } catch (DriveRemovedException e) {
            throw new CloudDriveException("Error openning drive node: " + e.getMessage(), e);
        } catch (RepositoryException e) {
            throw new CloudDriveException("Error reading drive node: " + e.getMessage(), e);
        } finally {
            jcrListener.enable();
        }
    }

    private void initFolderByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
        initFolder(fileNode,
                item.id,
                item.name,
                "folder",
                item.webUrl,
                item.createdBy.user.displayName,
                item.lastModifiedBy.user.displayName,
                item.createdDateTime,
                item.lastModifiedDateTime);

    }

    private void initFileByDriveItem(Node fileNode, DriveItem item) throws RepositoryException {
        final SharingLink link = getUser().api().createLink(item.id);
        initFile(fileNode,
                item.id,
                item.name,
                item.file.mimeType,
                item.webUrl,
                link.webUrl,
                null, // TODO may be something better can be here?
                item.createdBy.user.displayName,
                item.lastModifiedBy.user.displayName,
                item.createdDateTime,
                item.lastModifiedDateTime,
                item.size);
    }

    private JCRLocalCloudFile createCloudFolder(Node fileNode, DriveItem item) throws RepositoryException {
        return new JCRLocalCloudFile(fileNode.getPath(),
                item.id,
                item.name,
                item.webUrl,
                "folder",
                item.lastModifiedBy.user.displayName,
                item.createdBy.user.displayName,
                item.createdDateTime,
                item.lastModifiedDateTime,
                fileNode,
                true);

    }

    private JCRLocalCloudFile createCloudFile(Node fileNode, DriveItem item) throws RepositoryException {
        return new JCRLocalCloudFile(fileNode.getPath(),
                item.id,
                item.name,
                item.webUrl,
                item.file.mimeType,
                item.lastModifiedBy.user.displayName,
                item.createdBy.user.displayName,
                item.createdDateTime,
                item.lastModifiedDateTime,
                fileNode,
                true);

    }

    class OneDriveConnectCommand extends ConnectCommand {

        private final OneDriveAPI api;


        OneDriveConnectCommand() throws RepositoryException, DriveRemovedException {
            this.api = getUser().api();
        }

        private JCRLocalCloudFile openInitFolder(DriveItem item, Node localFile) throws RepositoryException, CloudDriveException {
            Node fileNode = openFolder(item.id, item.name, localFile);
            initFolderByDriveItem(fileNode, item);
            fetchFiles(item.id, fileNode);
            return createCloudFolder(fileNode, item);
        }

        private JCRLocalCloudFile openInitFile(DriveItem item, Node localFile) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("before openInitFile");
            }
            Node fileNode = openFile(item.id, item.name, localFile);

            if (LOG.isDebugEnabled()) {
                LOG.debug("after openInitFile");
            }
            initFileByDriveItem(fileNode, item);
            return createCloudFile(fileNode, item);
        }


        private void fetchFiles(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("fetchFiles():  ");
            }
            OneDriveAPI.ChildIterator childIterator = api.getChildIterator(fileId);
            iterators.add(childIterator);
            while (childIterator.hasNext()) {
                DriveItem item = childIterator.next();
                if (!isConnected(fileId, item.id)) {
                    JCRLocalCloudFile jcrLocalCloudFile;
                    if (item.folder != null) {
                        jcrLocalCloudFile = openInitFolder(item, localFile);
                    } else /*if (item.file != null)*/ {
                        jcrLocalCloudFile = openInitFile(item, localFile);
                    }
                    addConnected(fileId, jcrLocalCloudFile);
                }
            }
        }

        @Override
        protected void fetchFiles() throws CloudDriveException, RepositoryException {
            String rootId = getUser().api().getRootId();
            fetchFiles(rootId, driveNode);
        }

    }

    protected class OneDriveSyncCommand extends SyncCommand {

        private final OneDriveAPI api;

        private OneDriveAPI.ChangesIterator changes;

        OneDriveSyncCommand() {
            this.api = getUser().api();
        }

        void saveDeltaToken(String deltaToken) throws RepositoryException {
            driveNode.setProperty("onedrive:changeToken", deltaToken);
        }

        String getDeltaToken() throws RepositoryException {
            if (driveNode.hasProperty("onedrive:changeToken")) {
                return driveNode.getProperty("onedrive:changeToken").getString();
            }
            return null;
        }

        @Override
        protected void preSaveChunk() {
            if (LOG.isDebugEnabled()) {
                LOG.debug("preSaveChunk()");
            }
        }

        private void addFileNode(DriveItem driveItem, Node parentNode) throws RepositoryException, CloudDriveException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Add File(): id:  " + driveItem.id + " name: " + driveItem.name + "parentId: " + driveItem.parentReference.id);
            }
            Node fileNode = openFile(driveItem.id, driveItem.name, parentNode);
            initFileByDriveItem(fileNode, driveItem);
            // TODO make better list creation, w/o "The serializable class  does not declare a static final serialVersionUID"
            this.nodes.put(driveItem.id, new ArrayList<Node>() {{
                add(fileNode);
            }});
        }

        private void fetchChilds(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("fetchFiles():  ");
            }
            OneDriveAPI.ChildIterator childIterator = api.getChildIterator(fileId);
            while (childIterator.hasNext()) {
                DriveItem item = childIterator.next();
                if (item.folder != null) {
                    Node folderNode = addFolderNode(item,localFile);
                    fetchChilds(item.id, folderNode);
                } else if (item.file != null) {
                   addFileNode(item,localFile);
                }
            }
        }

        private void updateNode(DriveItem driveItem, Node fileNode) throws RepositoryException, CloudDriveException {
            if (api.getRootId().equals(driveItem.id)) return;
            List<Node> destParentNodes = this.nodes.get(driveItem.parentReference.id);
            if (destParentNodes == null || destParentNodes.isEmpty()) {
                syncNext();
                destParentNodes = this.nodes.get(driveItem.parentReference.id);

            }
            Node destParentNode = destParentNodes.get(0);
            if (!fileAPI.getParentId(fileNode).equals(driveItem.parentReference.id)) {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("must be moved, name= " + driveItem.name);
                }

                try {
                    Node node = moveFile(driveItem.id, driveItem.name, fileNode, destParentNode);
                } catch (Throwable ex) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("try move node after exception");
                    }
                    deleteItem(driveItem.id);
                    DriveItem item = api.getItem(driveItem.id);
                    if (item.file != null) { // file
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("try move file");
                        }
                        addFileNode(driveItem,destParentNode);
                    }else{//folder
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("try move folder");
                        }
                        Node folderNode = addFolderNode(driveItem,destParentNode);
                        fetchChilds(item.id,folderNode);
                    }
                }
            } else if (!fileAPI.getTitle(fileNode).equals(driveItem.name) ) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("must be renamed, name= " + driveItem.name);
                }
                try {
                    Node node = moveFile(driveItem.id, driveItem.name, fileNode, destParentNode);
                    JCRLocalCloudFile jcrLocalCloudFile;
                    if (node != null) {
                        if (driveItem.folder != null) { //folder
                            initFolderByDriveItem(node, driveItem);
                            jcrLocalCloudFile = createCloudFolder(node, driveItem);
                        } else { //file
                            initFileByDriveItem(fileNode, driveItem);
                            jcrLocalCloudFile = createCloudFile(node, driveItem);
                        }
                        addChanged(jcrLocalCloudFile);
                    }
                } catch (Throwable ex) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("try rename node after exception");
                    }
                    deleteItem(driveItem.id);
                    DriveItem item = api.getItem(driveItem.id);
                    if (item.file != null) { // file
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("try move file");
                        }
                        addFileNode(driveItem,destParentNode);
                    }else{//folder
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("try move folder");
                        }
                        Node folderNode = addFolderNode(driveItem,destParentNode);
                        fetchChilds(item.id,folderNode);
                    }
                }
            }

        }

        public void syncNext() throws CloudDriveException, RepositoryException {
            while (changes.hasNext()) {
                DriveItem driveItem = changes.next();
                if (driveItem.file != null) {
                    if (driveItem.deleted == null) {
                        List<Node> nodes = null;
                        if (this.nodes.containsKey(driveItem.id)) {
                            nodes = this.nodes.get(driveItem.id);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Nodes size: " + nodes.size() + " for item with id " + driveItem.id);
                            }
                        }

                        if (nodes == null || nodes.size() == 0) { // add
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Add File(): id:  " + driveItem.id + " name: " + driveItem.name + "parentId: "
                                        + driveItem.parentReference.id);
                            }
                            Node parentNode = this.nodes.get(driveItem.parentReference.id).get(0);
                            addFileNode(driveItem, parentNode);
                        } else { // update
                            Node fileNode = nodes.get(0);
                            updateNode(driveItem, fileNode);
                        }
                    } else {
                        deleteItem(driveItem.id);
                    }
                } else { // folder

                    if (driveItem.deleted == null) {
                        List<Node> nodes = null;
                        if (this.nodes.containsKey(driveItem.id)) {
                            nodes = this.nodes.get(driveItem.id);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("(folder)Nodes size: " + nodes.size() + " for item with id " + driveItem.id);
                            }
                        }

                        if (nodes == null || nodes.size() == 0) { // add
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Add Folder(): id:  " + driveItem.id + " name: " + driveItem.name + "parentId: "
                                        + driveItem.parentReference.id);
                            }
                            Node parentNode = this.nodes.get(driveItem.parentReference.id).get(0);
                            addFolderNode(driveItem, parentNode);
                        } else {
                            Node fileNode = nodes.get(0);
                            updateNode(driveItem, fileNode);
                        }
                    } else {
                        deleteItem(driveItem.id);
                    }

                }
            }
        }

        protected boolean notInRange(String path, Collection<String> range) {
            for (String p : range) {
                if (path.startsWith(p)) {
                    return false;
                }
            }
            return true;
        }
        @Override
        protected void syncFiles() throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("syncFiles()");
            }

            String deltaToken = getDeltaToken();
            if (deltaToken == null) {
                deltaToken = "latest";
            }
            changes = api.changes(deltaToken);
            iterators.add(changes);
            if (changes.hasNext()) {
                readLocalNodes();
                // TODO: do we have a failed sycn restoration procedure, may be we would?
                try {
                    syncNext();
                } catch (Throwable ex) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("try update all drive after ex ",ex);
                    }
                    // remove all nodes
                    nodes.remove(api.getRootId());
                    for (Iterator<List<Node>> niter = nodes.values().iterator(); niter.hasNext() && !Thread.currentThread().isInterrupted();) {
                        List<Node> nls = niter.next();
                        niter.remove();
                        for (Node n : nls) {
                            String npath = n.getPath();
                            if (notInRange(npath, getRemoved())) {
                                // remove file links outside the drive, then the node itself
                                removeNode(n);
                                addRemoved(npath);
                            }
                        }
                    }

                    saveDeltaToken("ALL");
                    syncFiles();
                }
            }
            saveDeltaToken(changes.getDeltaToken());
        }

        private Node addFolderNode(DriveItem driveItem, Node parentNode) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Add Folder(): id:  " + driveItem.id + " name: " + driveItem.name + "parentId: "
                        + driveItem.parentReference.id);
            }
            Node fileNode = openFolder(driveItem.id, driveItem.name, parentNode);
            initFolderByDriveItem(fileNode, driveItem);
            this.nodes.put(driveItem.id, new ArrayList<Node>() {{
                add(fileNode);
            }});
            return fileNode;
        }

        private void deleteItem(String itemId) throws CloudDriveException, RepositoryException {
            List<Node> existing = nodes.remove(itemId);
            if (existing != null) {
                for (Node en : existing) {
                    removeLocalNode(en);
                }
            }
        }
    }

    class OneDriveFileAPI extends AbstractFileAPI {

        private OneDriveAPI api;

        OneDriveFileAPI() {
            this.api = getUser().api();
        }

        @Override
        public boolean removeFile(String id) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("removeFile(): ");
            }
            try {
                api.removeFile(id);
            } catch (Throwable ex) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("error during file delete", ex);
                }

            }
            return true;
        }

        @Override
        public boolean removeFolder(String id) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("removeFolder(): ");
            }
            try {
                api.removeFolder(id);
            } catch (Throwable ex) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("error during folder delete", ex);
                }
            }
            return true;
        }

        @Override
        public boolean isTrashSupported() {
            return false;
        }

        @Override
        public boolean trashFile(String id) throws CloudDriveException {
            throw new SyncNotSupportedException("Trash not supported");
        }

        @Override
        public boolean trashFolder(String id) throws CloudDriveException {
            throw new SyncNotSupportedException("Trash not supported");
        }

        @Override
        public CloudFile untrashFile(Node fileNode) throws CloudDriveException {
            throw new SyncNotSupportedException("Trash not supported");
        }

        @Override
        public CloudFile untrashFolder(Node fileNode) throws CloudDriveException {
            throw new SyncNotSupportedException("Trash not supported");
        }

        private String extractAppropriateOneDrivePath(Node fileNode) throws RepositoryException {
            StringBuilder oneDrivePath = new StringBuilder();
            while (fileNode != null && fileNode.hasProperty("exo:title")) {
                oneDrivePath.insert(0, "/" + getTitle(fileNode));
                fileNode = fileNode.getParent();
            }

            String path = oneDrivePath.substring(oneDrivePath.indexOf("/", oneDrivePath.indexOf("/") + 1) + 1);
            if (LOG.isDebugEnabled()) {
                LOG.debug("OneDrivePath: " + path);
            }
            return path;
        }

        @Override
        public CloudFile createFile(Node fileNode,
                                    Calendar created,
                                    Calendar modified,
                                    String mimeType,
                                    InputStream content) throws RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Create File Path : " + fileNode.getPath() + "\n" + "Create File Name: " + getTitle(fileNode));
            }

            String path = extractAppropriateOneDrivePath(fileNode);
            if (LOG.isDebugEnabled()) {
                LOG.debug("One Drive Path: " + path);
            }
            DriveItem createdDriveItem = api.insert(path, getTitle(fileNode), created, modified, mimeType, content);
            if (createdDriveItem != null) {

                initFileByDriveItem(fileNode, createdDriveItem);
                return createCloudFile(fileNode, createdDriveItem);
            }
            return null;
        }

        @Override
        public CloudFile createFolder(Node folderNode, Calendar created) throws RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("createFolder(): ");
            }
            String parentId = getParentId(folderNode);
            DriveItem createdFolder = api.createFolder(parentId, getTitle(folderNode), created);
            initFolderByDriveItem(folderNode, createdFolder);
            return createCloudFolder(folderNode, createdFolder);
        }

        @Override
        public CloudFile copyFile(Node srcFileNode, Node destFileNode) throws RepositoryException, RefreshAccessException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("copyFile(): ");
            }
            DriveItem file = api.copyFile(getParentId(destFileNode), getTitle(destFileNode), getId(srcFileNode));
            initFileByDriveItem(destFileNode, file);
            return createCloudFolder(destFileNode, file);
        }

        private void fetchChilds(String fileId, Node localFile) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("fetchFiles():  ");
            }
            OneDriveAPI.ChildIterator childIterator = api.getChildIterator(fileId);
            while (childIterator.hasNext()) {
                DriveItem item = childIterator.next();
                JCRLocalCloudFile jcrLocalCloudFile = null; // TODO we don't need a variable?
                if (item.folder != null) {
                    jcrLocalCloudFile = openInitFolder(item, localFile);
                } else if (item.file != null) {
                    jcrLocalCloudFile = openInitFile(item, localFile);
                }
            }
        }

        private JCRLocalCloudFile openInitFolder(DriveItem item, Node localFile) throws RepositoryException, CloudDriveException {
            Node fileNode = openFolder(item.id, item.name, localFile);
            initFolderByDriveItem(fileNode, item);
            fetchChilds(item.id, fileNode);
            return createCloudFolder(fileNode, item);
        }

        private JCRLocalCloudFile openInitFile(DriveItem item, Node localFile) throws CloudDriveException, RepositoryException {
            Node fileNode = openFile(item.id, item.name, localFile);
            initFileByDriveItem(fileNode, item);
            return createCloudFile(fileNode, item);

        }


        @Override
        public CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws RepositoryException, RefreshAccessException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("copyFolder(): ");
            }
            DriveItem folder = api.copyFolder(getParentId(destFolderNode), getTitle(destFolderNode), getId(srcFolderNode));
            if (folder != null) {
                initSubtree(destFolderNode, folder);
                return createCloudFolder(destFolderNode, folder);
            } // TODO an error here?
            return null;
        }

        private void initSubtree(Node folderNode, DriveItem driveItem) throws RepositoryException {
            initFolderByDriveItem(folderNode, driveItem);
            for (NodeIterator niter = folderNode.getNodes(); niter.hasNext(); ) {
                Node node = niter.nextNode();
                String onedrivePath = extractAppropriateOneDrivePath(node);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("initsubtree() onedrivepath = " + onedrivePath);
                }
                driveItem = api.getItemByPath(onedrivePath);
                if (isFolder(node)) { //folder
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("copyFolder(): initFolderByDriveITem");
                    }
                    initSubtree(node, driveItem);
                } else { //file
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("copyFolder(): initFolderByDriveITem");
                    }
                    initFileByDriveItem(node, driveItem);
                }

            }
        }


        @Override
        public CloudFile updateFile(Node fileNode, Calendar modified) throws RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("updateFile(): ");
            }
            DriveItem modifiedItem = updateItem(fileNode, modified);
            initFileByDriveItem(fileNode, modifiedItem);
            return createCloudFile(fileNode, modifiedItem);
        }

        @Override
        public CloudFile updateFolder(Node folderNode, Calendar modified) throws RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("updateFolder(): ");
            }
            DriveItem modifiedItem = updateItem(folderNode, modified);
            initFolderByDriveItem(folderNode, modifiedItem);
            return createCloudFolder(folderNode, modifiedItem);
        }

        @Override
        public CloudFile updateFileContent(Node fileNode,
                                           Calendar modified,
                                           String mimeType,
                                           InputStream content) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("updateFileContent(): ");
            }
            DriveItem updatedDriveItem = api.updateFileContent(extractAppropriateOneDrivePath(fileNode),
                    getTitle(fileNode),
                    null,
                    modified,
                    mimeType,
                    content);
            if (updatedDriveItem != null) {
                initFileByDriveItem(fileNode, updatedDriveItem);
                return createCloudFile(fileNode, updatedDriveItem);
            } // TODO an error here?
            return null;
        }

        @Override
        public CloudFile restore(String id, String path) throws CloudDriveException, RepositoryException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("restore(): ");
            }
            return null;
        }

        private DriveItem updateItem(Node itemNode, Calendar modified) throws RepositoryException {
            DriveItem driveItemModifiedFields = prepareModifiedDriveItem(itemNode, modified);
            api.updateFile(driveItemModifiedFields);
            return api.getItem(driveItemModifiedFields.id);
        }

        private DriveItem prepareModifiedDriveItem(Node fileNode, Calendar modified) throws RepositoryException {
            DriveItem driveItem = new DriveItem();
            driveItem.id = getId(fileNode);
            driveItem.name = getTitle(fileNode);
            driveItem.parentReference = new ItemReference();
            String parentId = getParentId(fileNode);
            if (parentId == null || parentId.isEmpty()) {
                parentId = api.getRootId();
            }
            driveItem.parentReference.id = parentId;
            driveItem.fileSystemInfo = new FileSystemInfo();
            driveItem.fileSystemInfo.lastModifiedDateTime = modified;
            return driveItem;
        }
    }
}
