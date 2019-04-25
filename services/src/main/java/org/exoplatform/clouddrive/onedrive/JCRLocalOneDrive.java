package org.exoplatform.clouddrive.onedrive;

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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.time.Instant;
import java.util.Calendar;

public class JCRLocalOneDrive extends JCRLocalCloudDrive implements UserTokenRefreshListener {
    /**
     *
     */
    private static final Log LOG = ExoLogger.getLogger(JCRLocalOneDrive.class);

    protected JCRLocalOneDrive(CloudUser user,
                               Node driveNode,
                               SessionProviderService sessionProviders,
                               NodeFinder finder,
                               ExtendedMimeTypeResolver mimeTypes)
            throws CloudDriveException,
            RepositoryException {
        super(user, driveNode, sessionProviders, finder, mimeTypes);
    }

    @Override
    protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
        LOG.info("getConnectCommand()");
        return new OneDriveConnectCommand();
    }

    @Override
    protected SyncCommand getSyncCommand() throws DriveRemovedException, SyncNotSupportedException, RepositoryException {
        LOG.info("getSyncCommand()");
        return new OneDriveSyncCommand();
    }

    @Override
    protected CloudFileAPI createFileAPI() throws DriveRemovedException, SyncNotSupportedException, RepositoryException {
        LOG.info("createFileAPI()");
        return new OneDriveFileAPI();
    }

    @Override
    protected Long readChangeId() throws CloudDriveException, RepositoryException {
        LOG.info("readChangeId()");
        return 0L;
    }

    @Override
    protected void saveChangeId(Long id) throws CloudDriveException, RepositoryException {
        LOG.info("saveChangeId()");
    }

    @Override
    public CloudUser getUser() {
        LOG.info("getUser()");
        return this.user;
    }

    @Override
    protected void refreshAccess() throws CloudDriveException {
        LOG.info("refreshAccess()");
    }

    @Override
    protected void updateAccess(CloudUser user) throws CloudDriveException, RepositoryException {
        LOG.info("------");
    }

    @Override
    public void onUserTokenRefresh(UserToken token) throws CloudDriveException {
        LOG.info("------");
    }

    @Override
    public void onUserTokenRemove() throws CloudDriveException {
        LOG.info("------");
    }

    protected class OneDriveConnectCommand extends ConnectCommand {
        /**
         * Connect command constructor.
         *
         * @throws RepositoryException   the repository exception
         * @throws DriveRemovedException the drive removed exception
         */
        protected OneDriveConnectCommand() throws RepositoryException, DriveRemovedException {
        }

        @Override
        protected void fetchFiles() throws CloudDriveException, RepositoryException {

            Node fileNode = openFolder("ID_1", "ROOT", driveNode);
            initFolder(fileNode,
                    "ID_1",
                    "ROOT",
                    "type",
                    "http://",
                    "Name",
                    "User",
                    Calendar.getInstance(),
                    Calendar.getInstance());

            fileNode = openFile("ID_2", "File_1", fileNode);
            initFile(fileNode,
                    "ID_2",
                    "File_1",
                    "type",
                    "http://",
                    "prevLink",
                    "thumbLink",
                    "Name",
                    "User",
                    Calendar.getInstance(),
                    Calendar.getInstance(),
                    1000L);
        }
    }

    protected class OneDriveSyncCommand extends SyncCommand {

        @Override
        protected void preSaveChunk() throws CloudDriveException, RepositoryException {

        }

        @Override
        protected void syncFiles() throws CloudDriveException, RepositoryException, InterruptedException {

        }
    }
}





