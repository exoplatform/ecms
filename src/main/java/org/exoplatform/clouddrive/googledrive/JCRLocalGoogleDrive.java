/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.googledrive;

import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.CloudFileAPI;
import org.exoplatform.clouddrive.CloudProviderException;
import org.exoplatform.clouddrive.CloudUser;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.SyncNotSupportedException;
import org.exoplatform.clouddrive.googledrive.GoogleDriveAPI.ChangesIterator;
import org.exoplatform.clouddrive.googledrive.GoogleDriveAPI.ChildIterator;
import org.exoplatform.clouddrive.googledrive.GoogleDriveConnector.API;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudFile;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * JCR local storage for Google Drive. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: JCRLocalGoogleDrive.java 00000 Sep 13, 2012 pnedonosko $
 */
public class JCRLocalGoogleDrive extends JCRLocalCloudDrive {

  /**
   * Connect algorithm for Google Drive.
   */
  protected class Connect extends ConnectCommand {

    /**
     * Google Drive service API.
     */
    protected final GoogleDriveAPI api;

    /**
     * Create connect to Google Drive command.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected Connect() throws RepositoryException, DriveRemovedException {
      super();
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fetchFiles() throws CloudDriveException, RepositoryException {
      About about = api.about();
      // drive id
      String id = about.getRootFolderId();

      fetchChilds(id, rootNode);

      // connect metadata
      rootNode.setProperty("gdrive:oauth2AccessToken", api.getAccessToken());
      rootNode.setProperty("gdrive:oauth2RefreshToken", api.getRefreshToken());
      rootNode.setProperty("gdrive:oauth2TokenExpirationTime", api.getExpirationTime());
      rootNode.setProperty("gdrive:largestChangeId", about.getLargestChangeId());
    }

    protected void fetchChilds(String fileId, Node parent) throws CloudDriveException, RepositoryException {
      ChildIterator children = api.children(fileId);
      iterators.add(children);

      while (children.hasNext()) {
        ChildReference child = children.next();
        File gf = api.file(child.getId());
        if (!gf.getLabels().getTrashed()) { // skip files in Trash
          // create JCR node here
          boolean isFolder = api.isFolder(gf);

          DateTime createDate = gf.getCreatedDate();
          if (createDate == null) {
            throw new GoogleDriveException("File " + gf.getTitle() + " doesn't have Created Date.");
          }
          Calendar created = api.parseDate(createDate.toStringRfc3339());
          DateTime modifiedDate = gf.getModifiedDate();
          if (modifiedDate == null) {
            throw new GoogleDriveException("File " + gf.getTitle() + " doesn't have Modified Date.");
          }
          Calendar modified = api.parseDate(modifiedDate.toStringRfc3339());

          // TODO apply multiple owners in cloud file and show them in ECM

          Node localNode;
          if (isFolder) {
            localNode = openFolder(gf.getId(), gf.getTitle(), parent);
            initFolder(localNode,
                       gf.getId(),
                       gf.getTitle(),
                       gf.getMimeType(),
                       gf.getAlternateLink(),
                       gf.getOwnerNames().get(0),
                       gf.getLastModifyingUserName(),
                       created,
                       modified);

            // go recursive
            fetchChilds(gf.getId(), localNode);
          } else {
            localNode = openFile(gf.getId(), gf.getTitle(), gf.getMimeType(), parent);
            initFile(localNode,
                     gf.getId(),
                     gf.getTitle(),
                     gf.getMimeType(),
                     gf.getAlternateLink(),
                     gf.getEmbedLink(),
                     gf.getThumbnailLink(),
                     gf.getOwnerNames().get(0),
                     gf.getLastModifyingUserName(),
                     created,
                     modified);
          }

          changed.add(new JCRLocalCloudFile(localNode.getPath(),
                                            gf.getId(),
                                            gf.getTitle(),
                                            gf.getAlternateLink(),
                                            gf.getEmbedLink(),
                                            gf.getThumbnailLink(),
                                            gf.getMimeType(),
                                            gf.getLastModifyingUserName(),
                                            gf.getOwnerNames().get(0),
                                            created,
                                            modified,
                                            isFolder));
        }
      }
    }
  }

  /**
   * Sync algorithm for Google Drive.
   */
  protected class Sync extends SyncCommand {

    /**
     * Google Drive service API.
     */
    protected final GoogleDriveAPI api;

    /**
     * Existing files being synchronized with cloud.
     */
    protected final Set<Node>      synced = new HashSet<Node>();

    protected ChangesIterator      changes;

    /**
     * Create command for Google Drive synchronization.
     * 
     * @throws RepositoryException
     * @throws DriveRemovedException
     */
    protected Sync() throws RepositoryException, DriveRemovedException {
      super();
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncFiles() throws RepositoryException, CloudDriveException {
      String largestChangeId = "";

      try {
        About about = api.about();
        largestChangeId = String.valueOf(about.getLargestChangeId());
      } catch (Exception e) {
        // TODO can we have more precise exception from Google API?
        throw new NoRefreshTokenException("Error calling About service of Google Drive: " + e.getMessage(), e);
      }

      String localChangeId = rootNode.getProperty("gdrive:largestChangeId").getString();
      if (largestChangeId.equals(localChangeId)) {
        // nothing changed
        return;
      }

      long changeId;
      try {
        changeId = Long.parseLong(localChangeId);
      } catch (NumberFormatException e) {
        throw new CloudDriveException("Error parse localChangeId", e);
      }
      long startChangeId = changeId + 1;

      LOG.info("Synchronizing changes from " + startChangeId + " to about " + largestChangeId); // TODO
                                                                                                // cleanup

      changes = api.changes(startChangeId);
      iterators.add(changes);

      if (changes.hasNext()) {
        readLocalNodes(); // read all local nodes to nodes list
        syncNext(); // process changes
      }

      // update sync metadata, use actual change id from the last iterator
      rootNode.setProperty("gdrive:largestChangeId", changes.getLargestChangeId());
    }

    protected void syncNext() throws RepositoryException, CloudDriveException {
      while (changes.hasNext()) {
        Change ch = changes.next();
        File gf = ch.getFile(); // gf will be null for deleted

        String[] parents; // gf.getUnknownKeys()

        // if parents empty - file deleted or shared file was removed from user drive (My Drive)
        // if file in Trash - proceed to delete also, inside it should be checked for the same ETag
        if (ch.getDeleted() || (parents = getParents(gf)).length == 0) {
          if (hasChanged(ch.getFileId(), FileChange.REMOVE)) {
            removeChanged(ch.getFileId(), FileChange.REMOVE);
            LOG.info(">> Returned file removal " + ch.getFileId());
          } else {
            LOG.info(">> File removal " + ch.getFileId());
          }
          deleteFile(ch.getFileId());
        } else {
          if (gf.getLabels().getTrashed()) {
            if (hasChanged(gf.getId(), FileChange.REMOVE)) {
              removeChanged(gf.getId(), FileChange.REMOVE);
              LOG.info(">> Returned file trashing " + gf.getId() + " " + gf.getTitle());
            } else {
              LOG.info(">> File trashing " + gf.getId() + " " + gf.getTitle());
              deleteFile(gf.getId());
            }
          } else {
            if (hasChanged(gf.getId(), FileChange.UPDATE)) {
              removeChanged(gf.getId(), FileChange.UPDATE);
              LOG.info(">> Returned file update " + gf.getId() + " " + gf.getTitle());
            } else {
              LOG.info(">> File update " + gf.getId() + " " + gf.getTitle());
              updateFile(gf, parents);
            }
          }
        }
      }
    }

    /**
     * Remove file's node.
     * 
     * @param fileId {@link String}
     * @throws RepositoryException
     */
    protected void deleteFile(String fileId) throws RepositoryException {
      List<Node> existing = nodes.get(fileId);
      if (existing != null) {
        // remove existing file,
        // also clean the nodes map from the descendants (they can be recorded in delta)
        for (Node en : existing) {
          String enpath = en.getPath();
          for (Iterator<List<Node>> ecnliter = nodes.values().iterator(); ecnliter.hasNext();) {
            List<Node> ecnl = ecnliter.next();
            if (ecnl != existing) {
              for (Iterator<Node> ecniter = ecnl.iterator(); ecniter.hasNext();) {
                Node ecn = ecniter.next();
                if (ecn.getPath().startsWith(enpath)) {
                  ecniter.remove();
                }
              }
              if (ecnl.size() == 0) {
                ecnliter.remove();
              }
            } // else will be removed below
          }
          removed.add(en.getPath());
          en.remove();
        }
        nodes.remove(fileId);
      }
    }

    /**
     * Create or update file's node.
     * 
     * @param gf {@link File}
     * @param parentIds array of Ids of parents (folders)
     * @throws CloudDriveException
     * @throws IOException
     * @throws RepositoryException
     */
    protected void updateFile(File gf, String[] parentIds) throws CloudDriveException, RepositoryException {
      // using changes only related to current parent:
      // * for deleted checking if node exists and remove it
      // * for others update on its parents
      List<Node> existing = nodes.get(gf.getId());

      boolean isFolder = api.isFolder(gf);

      for (String parentFileId : parentIds) {
        List<Node> fileParent = nodes.get(parentFileId);
        if (fileParent == null) {
          // no yet existing locally parent... wait for it
          syncNext();

          fileParent = nodes.get(parentFileId);
          if (fileParent == null) {
            throw new CloudDriveException("Inconsistent changes: cannot find parent Node for '"
                + gf.getTitle() + "'");
          }
        }

        for (Node fp : fileParent) {
          Node localNode = null;
          Node localNodeCopy = null;
          if (existing == null) {
            existing = new ArrayList<Node>();
            nodes.put(gf.getId(), existing);
          } else {
            for (Node n : existing) {
              localNodeCopy = n;
              if (n.getParent().isSame(fp)) {
                localNode = n;
                break;
              }
            }
          }

          if (localNode == null) {
            // create new Node in local JCR
            if (isFolder) {
              if (localNodeCopy == null) {
                localNode = openFolder(gf.getId(), gf.getTitle(), fp);
              } else {
                // copy from local copy of the folder to a new parent
                localNode = copyNode(localNodeCopy, fp);
              }
            } else {
              localNode = openFile(gf.getId(), gf.getTitle(), gf.getMimeType(), fp);
            }

            // add created Node to list of existing
            existing.add(localNode);
          } else if (!localNode.getProperty("exo:title").getString().equals(gf.getTitle())) {
            // file was renamed, rename (move) its Node also
            localNode = moveFile(gf.getId(), gf.getTitle(), localNode, fp);
          }

          Calendar created = api.parseDate(gf.getCreatedDate().toStringRfc3339());
          Calendar modified = api.parseDate(gf.getModifiedDate().toStringRfc3339());

          // TODO apply multiple owners in cloud file and show them in WCM

          if (isFolder) {
            initFolder(localNode,
                       gf.getId(),
                       gf.getTitle(),
                       gf.getMimeType(),
                       gf.getAlternateLink(),
                       gf.getOwnerNames().get(0),
                       gf.getLastModifyingUserName(),
                       created,
                       modified);
          } else {
            initFile(localNode,
                     gf.getId(),
                     gf.getTitle(),
                     gf.getMimeType(),
                     gf.getAlternateLink(),
                     gf.getEmbedLink(),
                     gf.getThumbnailLink(),
                     gf.getOwnerNames().get(0),
                     gf.getLastModifyingUserName(),
                     created,
                     modified);
          }

          changed.add(new JCRLocalCloudFile(localNode.getPath(),
                                            gf.getId(),
                                            gf.getTitle(),
                                            gf.getAlternateLink(),
                                            gf.getEmbedLink(),
                                            gf.getThumbnailLink(),
                                            gf.getMimeType(),
                                            gf.getLastModifyingUserName(),
                                            gf.getOwnerNames().get(0),
                                            created,
                                            modified,
                                            isFolder));

          synced.add(localNode);
        }
      }

      if (existing != null) {
        // need remove other existing (not listed on delta parents)
        for (Iterator<Node> niter = existing.iterator(); niter.hasNext();) {
          Node n = niter.next();
          if (!synced.contains(n)) {
            removed.add(n.getPath());
            niter.remove();
            n.remove();
          }
        }
      }
    }

    protected String[] getParents(File gfile) {
      if (gfile != null) {
        List<ParentReference> parents = gfile.getParents();
        if (parents != null) {
          String[] parentIds = new String[parents.size()];
          for (int i = 0; i < parents.size(); i++) {
            parentIds[i] = parents.get(i).getId();
          }
          return parentIds;
        }
      }
      return new String[0];
    }
  }

  protected class FileAPI extends AbstractFileAPI {

    /**
     * Google Drive service API.
     */
    protected final GoogleDriveAPI api;

    FileAPI() {
      this.api = getUser().api();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createFile(Node fileNode,
                             String description,
                             Calendar created,
                             Calendar modified,
                             String mimeType,
                             InputStream content) throws CloudDriveException, RepositoryException {

      // File's metadata.
      File gf = new File();
      gf.setTitle(getTitle(fileNode));
      gf.setDescription(description);
      gf.setMimeType(mimeType);
      gf.setParents(Arrays.asList(new ParentReference().setId(getParentId(fileNode))));
      gf.setCreatedDate(new DateTime(created.getTime()));
      gf.setModifiedDate(new DateTime(modified.getTime()));

      InputStreamContent fileContent = new InputStreamContent(mimeType, content);

      gf = api.insert(gf, fileContent);

      modified = api.parseDate(gf.getModifiedDate().toStringRfc3339()); // use actual from Google

      initFile(fileNode,
               gf.getId(),
               gf.getTitle(),
               gf.getMimeType(),
               gf.getAlternateLink(),
               gf.getEmbedLink(),
               gf.getThumbnailLink(),
               gf.getOwnerNames().get(0),
               gf.getLastModifyingUserName(),
               created,
               modified);

      return gf.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createFolder(Node folderNode, String description, Calendar created) throws CloudDriveException,
                                                                                     RepositoryException {
      // Folder metadata.
      File gf = new File();
      gf.setTitle(getTitle(folderNode));
      gf.setDescription(description);
      gf.setMimeType(GoogleDriveAPI.FOLDER_MIMETYPE);
      gf.setParents(Arrays.asList(new ParentReference().setId(getParentId(folderNode))));
      gf.setCreatedDate(new DateTime(created.getTime()));

      gf = api.insert(gf);

      Calendar modified = api.parseDate(gf.getModifiedDate().toStringRfc3339());

      initFolder(folderNode,
                 gf.getId(),
                 gf.getTitle(),
                 gf.getMimeType(),
                 gf.getAlternateLink(),
                 gf.getOwnerNames().get(0),
                 gf.getLastModifyingUserName(),
                 created,
                 modified);

      return gf.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFile(Node fileNode, String description, Calendar modified) throws CloudDriveException,
                                                                                RepositoryException {
      // Update existing file metadata and parent (location).
      File gf = api.file(getId(fileNode));
      gf.setTitle(getTitle(fileNode));
      gf.setDescription(description);
      // TODO merge parents (in case of move replace source on destination)
      gf.setParents(Arrays.asList(new ParentReference().setId(getParentId(fileNode))));
      gf.setModifiedDate(new DateTime(modified.getTime()));

      api.update(gf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFolder(Node folderNode, String description, Calendar modified) throws CloudDriveException,
                                                                                    RepositoryException {
      // Update existing folder metadata and parent (location).
      File gf = api.file(getId(folderNode));
      gf.setTitle(getTitle(folderNode));
      gf.setDescription(description);
      // TODO merge parents (in case of move replace source on destination)
      gf.setParents(Arrays.asList(new ParentReference().setId(getParentId(folderNode))));
      gf.setModifiedDate(new DateTime(modified.getTime()));

      api.update(gf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFileContent(Node fileNode,
                                  String description,
                                  Calendar modified,
                                  String mimeType,
                                  InputStream content) throws CloudDriveException, RepositoryException {
      // Update existing file content and related metadata.
      File gf = api.file(getId(fileNode));
      gf.setDescription(description);
      gf.setMimeType(mimeType);
      gf.setModifiedDate(new DateTime(modified.getTime()));

      InputStreamContent fileContent = new InputStreamContent(mimeType, content);

      api.update(gf, fileContent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(String id) throws CloudDriveException, RepositoryException {
      api.delete(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean trash(String id) throws CloudDriveException, RepositoryException {
      File file = api.trash(id);
      return file.getLabels().getTrashed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean untrash(Node fileNode) throws CloudDriveException, RepositoryException {
      File file = api.untrash(getId(fileNode));
      return !file.getLabels().getTrashed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTrashSupported() {
      return true;
    }
  }

  /**
   * File sync algorithm for Google Drive.
   */
  @Deprecated
  protected class SyncFile extends SyncFileCommand {

    /**
     * Google Drive service API.
     */
    protected final GoogleDriveAPI api;

    /**
     * Existing files being synchronized with cloud.
     */
    protected final Set<Node>      synced = new HashSet<Node>();

    protected ChangesIterator      changes;

    /**
     * Create command for Google Drive file synchronization.
     * 
     * @param file {@link Node}
     * @throws RepositoryException
     * @throws DriveRemovedException
     * @throws SyncNotSupportedException
     */
    protected SyncFile(Node file) throws RepositoryException, SyncNotSupportedException {
      super(file);
      this.api = getUser().api();
    }
  }

  /**
   * Create newly connecting drive.
   * 
   * @param user
   * @param driveNode
   * @param sessionProviders
   * @throws CloudDriveException
   * @throws RepositoryException
   */
  protected JCRLocalGoogleDrive(GoogleUser user, Node driveNode, SessionProviderService sessionProviders) throws CloudDriveException,
      RepositoryException {
    super(user, driveNode, sessionProviders);
  }

  /**
   * Create drive by loading it from local JCR node.
   * 
   * @param apiBuilder {@link API} API builder
   * @param provider {@link GoogleProvider}
   * @param driveNode {@link Node} root of the drive
   * @param sessionProviders
   * @throws RepositoryException if local storage error
   * @throws CloudDriveException if cannot load tokens stored locally
   * @throws GoogleDriveException if error communicating with Google Drive services
   */
  protected JCRLocalGoogleDrive(API apiBuilder,
                                GoogleProvider provider,
                                Node driveNode,
                                SessionProviderService sessionProviders) throws RepositoryException,
      GoogleDriveException,
      CloudDriveException {
    super(loadUser(apiBuilder, provider, driveNode), driveNode, sessionProviders);
  }

  /**
   * Load user from the drive Node.
   * 
   * @param apiBuilder {@link API} API builder
   * @param provider {@link GoogleProvider}
   * @param driveNode {@link Node} root of the drive
   * @return {@link GoogleUser}
   * @throws RepositoryException
   * @throws GoogleDriveException
   * @throws CloudDriveException
   */
  protected static GoogleUser loadUser(API apiBuilder, GoogleProvider provider, Node driveNode) throws RepositoryException,
                                                                                               GoogleDriveException,
                                                                                               CloudDriveException {
    String username = driveNode.getProperty("ecd:cloudUserName").getString();
    String email = driveNode.getProperty("ecd:userEmail").getString();
    String userId = driveNode.getProperty("ecd:cloudUserId").getString();
    String accessToken = driveNode.getProperty("gdrive:oauth2AccessToken").getString();
    String refreshToken;
    try {
      refreshToken = driveNode.getProperty("gdrive:oauth2RefreshToken").getString();
    } catch (PathNotFoundException e) {
      refreshToken = null;
    }
    long expirationTime = driveNode.getProperty("gdrive:oauth2TokenExpirationTime").getLong();

    GoogleDriveAPI driveAPI = apiBuilder.load(userId, refreshToken, accessToken, expirationTime).build();

    return new GoogleUser(userId, username, email, provider, driveAPI);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GoogleUser getUser() {
    return (GoogleUser) user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getChangesLink() throws DriveRemovedException, RepositoryException {
    // long-polling of changes not supported by Google as for Nov 10 2013
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateChangesLink() throws DriveRemovedException, CloudProviderException, RepositoryException {
    // do nothing for Google
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkAccess() throws GoogleDriveException {
    getUser().api().checkAccess();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateAccess(CloudUser newUser) throws CloudDriveException, RepositoryException {
    GoogleDriveAPI api = getUser().api();
    api.updateToken(((GoogleUser) newUser).api());

    Node driveNode = rootNode();
    try {
      jcrListener.disable();

      driveNode.setProperty("gdrive:oauth2AccessToken", api.getAccessToken());
      driveNode.setProperty("gdrive:oauth2RefreshToken", api.getRefreshToken());
      driveNode.setProperty("gdrive:oauth2TokenExpirationTime", api.getExpirationTime());

      driveNode.save();
    } catch (RepositoryException e) {
      rollback(driveNode);
      throw new GoogleDriveException("Error updating access key: " + e.getMessage(), e);
    } finally {
      jcrListener.enable();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean isSyncSupported(CloudFile cloudFile) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getChangeId() throws DriveRemovedException, RepositoryException {
    try {
      return rootNode().getProperty("gdrive:largestChangeId").getString();
    } catch (PathNotFoundException e) {
      LOG.warn("Change Id not found for the drive " + title());
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    super.initDrive(driveNode);

    About about = getUser().api().about();
    driveNode.setProperty("ecd:id", about.getRootFolderId());
    driveNode.setProperty("ecd:url", about.getSelfLink());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ConnectCommand getConnectCommand() throws DriveRemovedException, RepositoryException {
    return new Connect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected SyncCommand getSyncCommand() throws DriveRemovedException,
                                        SyncNotSupportedException,
                                        RepositoryException {
    return new Sync();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CloudFileAPI createFileAPI() throws DriveRemovedException,
                                        SyncNotSupportedException,
                                        RepositoryException {
    return new FileAPI();
  }

}
