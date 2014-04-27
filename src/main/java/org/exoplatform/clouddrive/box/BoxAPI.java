/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.clouddrive.box;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxEvent;
import com.box.boxjavalibv2.dao.BoxEventCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.dao.BoxServerError;
import com.box.boxjavalibv2.dao.BoxSharedLink;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.interfaces.IAuthData;
import com.box.boxjavalibv2.interfaces.IAuthSecureStorage;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxEventRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileUploadRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxItemRestoreRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxOAuthRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.box.boxjavalibv2.resourcemanagers.BoxFoldersManager;
import com.box.boxjavalibv2.utils.ISO8601DateParser;
import com.box.restclientv2.exceptions.BoxRestException;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.FileTrashRemovedException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * All calls to Box API here.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BoxAPI.java 00000 Aug 30, 2013 pnedonosko $
 * 
 */
public class BoxAPI {

  class BoxToken extends UserToken implements OAuthRefreshListener, IAuthSecureStorage {

    void store(BoxOAuthToken btoken) throws CloudDriveException {
      this.store(btoken.getAccessToken(), btoken.getRefreshToken(), btoken.getExpiresIn());
    }

    /**
     * @param newAuthData
     */
    public void onRefresh(IAuthData newAuthData) {
      // save the auth data.
      BoxOAuthToken newToken = (BoxOAuthToken) newAuthData;
      try {
        store(newToken);
      } catch (CloudDriveException e) {
        LOG.error("Error storing refreshed access token", e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAuth(IAuthData auth) {
      try {
        store((BoxOAuthToken) auth);
      } catch (CloudDriveException e) {
        LOG.error("Error saving access token", e);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAuthData getAuth() {
      BoxOAuthToken auth = new BoxOAuthToken();
      auth.put(BoxOAuthToken.FIELD_ACCESS_TOKEN, getAccessToken());
      auth.put(BoxOAuthToken.FIELD_REFRESH_TOKEN, getRefreshToken());
      auth.put(BoxOAuthToken.FIELD_EXPIRES_IN, getExpirationTime());
      auth.put(BoxOAuthToken.FIELD_TOKEN_TYPE, "bearer");
      return auth;
    }
  }

  /**
   * Iterator over whole set of items from Box service. This iterator hides next-chunk logic on
   * request to the service. <br>
   * Iterator methods can throw {@link BoxException} in case of remote or communication errors.
   */
  class ItemsIterator extends ChunkIterator<BoxItem> {
    final String folderId;

    int          limit, offset;

    /**
     * Parent folder.
     */
    BoxFolder    parent;

    ItemsIterator(String folderId) throws CloudDriveException {
      this.folderId = folderId;
      this.limit = 1000;
      this.offset = 0;

      // fetch first
      this.iter = nextChunk();
    }

    protected Iterator<BoxItem> nextChunk() throws AuthTokenException, CloudDriveException {
      BoxFolderRequestObject obj = BoxFolderRequestObject.getFolderItemsRequestObject(limit, offset);
      obj.addField("id");
      obj.addField("parent");
      obj.addField("name");
      obj.addField("type");
      obj.addField("etag");
      obj.addField("sequence_id");
      obj.addField("created_at");
      obj.addField("modified_at");
      obj.addField("description");
      obj.addField("size");
      obj.addField("created_by");
      obj.addField("modified_by");
      obj.addField("owned_by");
      obj.addField("shared_link");
      obj.addField("item_status");
      obj.addField("item_collection");
      try {
        // TODO handle big drives in several requests with chunk_size
        // BoxCollection items = client.getFoldersManager().getFolderItems(folderId, obj);
        // offset = items.getNextStreamPosition();

        parent = client.getFoldersManager().getFolder(folderId, obj);

        BoxCollection items = parent.getItemCollection();
        available(items.getTotalCount());

        ArrayList<BoxItem> oitems = new ArrayList<BoxItem>();
        // put folders first, then files
        oitems.addAll(BoxFoldersManager.getFolders(items));
        oitems.addAll(BoxFilesManager.getFiles(items));
        return oitems.iterator();
      } catch (BoxRestException e) {
        throw new BoxException("Error getting folder items: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        throw new BoxException("Error reading folder items: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        if (e.isCallerResponsibleForFix()) {
          // we need new access token (refresh token already expired here)
          throw new AuthTokenException("Authentication failure. Reauthenticate.");
        }
        throw new BoxException("Authentication error on folder items: " + e.getMessage(), e);
      }
    }

    protected boolean hasNextChunk() {
      return false; // XXX we assume all fully loaded by once
    }
  }

  /**
   * Iterator over set of events from Box service. This iterator hides next-chunk logic on
   * request to the service. <br>
   * Iterator methods can throw {@link BoxException} in case of remote or communication errors.
   */
  class EventsIterator extends ChunkIterator<BoxEvent> {
    final int      limit;

    long           streamPosition;

    List<BoxEvent> nextChunk;

    EventsIterator(long streamPosition) throws BoxException, AuthTokenException {
      this.streamPosition = streamPosition;
      this.limit = 1000;

      // fetch first
      this.iter = nextChunk();
    }

    BoxEventCollection events(long position) throws BoxException, AuthTokenException {
      try {
        BoxEventRequestObject request = BoxEventRequestObject.getEventsRequestObject(position <= -1 ? BoxEventRequestObject.STREAM_POSITION_NOW
                                                                                                   : position);

        // interest to tree changes only
        request.setStreamType(BoxEventRequestObject.STREAM_TYPE_CHANGES);
        request.setLimit(limit);

        return client.getEventsManager().getEvents(request);
      } catch (BoxRestException e) {
        throw new BoxException("Error requesting Events service: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        throw new BoxException("Error reading Events service: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        if (e.isCallerResponsibleForFix()) {
          // we need new access token (refresh token already expired here)
          throw new AuthTokenException("Authentication failure. Reauthenticate.");
        }
        throw new BoxException("Authentication error for Events service: " + e.getMessage(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    protected Iterator<BoxEvent> nextChunk() throws BoxException, AuthTokenException {
      List<BoxEvent> events;
      if (nextChunk == null) {
        BoxEventCollection ec = events(streamPosition);
        events = readEvents(ec);
        streamPosition = ec.getNextStreamPosition();
      } else {
        events = nextChunk;
      }

      BoxEventCollection ec = events(streamPosition);
      List<BoxEvent> nextEvents = readEvents(ec);
      if (ec.getChunkSize() > 0 && hasNewEvents(events, nextEvents)) {
        nextChunk = nextEvents;
      } else {
        nextChunk = null;
      }

      return events.iterator();
    }

    /**
     * {@inheritDoc}
     */
    protected boolean hasNextChunk() {
      return nextChunk != null;
    }

    private List<BoxEvent> readEvents(BoxEventCollection collection) {
      ArrayList<BoxEvent> events = new ArrayList<BoxEvent>();
      for (BoxTypedObject eobj : collection.getEntries()) {
        BoxEvent event = (BoxEvent) eobj;
        if (BOX_EVENTS.contains(event.getEventType())) {
          events.add(event);
        }
      }
      return events;
    }

    private boolean hasNewEvents(List<BoxEvent> events, List<BoxEvent> nextEvents) {
      // HashSet for better performance on large collections
      Set<String> ids = new HashSet<String>();
      for (BoxEvent event : events) {
        ids.add(event.getId());
      }

      int consumed = 0;
      for (BoxEvent event : nextEvents) {
        if (ids.contains(event.getId())) {
          consumed++;
        }
      }
      return nextEvents.size() > consumed;
    }
  }

  class ChangesLink {
    final String type;

    final String url;

    final long   ttl;

    final long   maxRetries;

    final long   retryTimeout;

    ChangesLink(String type, String url, long ttl, long maxRetries, long retryTimeout) {
      this.type = type;
      this.url = url;
      this.ttl = ttl;
      this.maxRetries = maxRetries;
      this.retryTimeout = retryTimeout;
    }

    /**
     * @return the type
     */
    String getType() {
      return type;
    }

    /**
     * @return the url
     */
    String getUrl() {
      return url;
    }

    /**
     * @return the ttl
     */
    long getTtl() {
      return ttl;
    }

    /**
     * @return the maxRetries
     */
    long getMaxRetries() {
      return maxRetries;
    }

    /**
     * @return the retryTimeout
     */
    long getRetryTimeout() {
      return retryTimeout;
    }
  }

  protected static final Log      LOG                      = ExoLogger.getLogger(BoxAPI.class);

  public static final String      NO_STATE                 = "__no_state_set__";

  /**
   * Id of root folder on Box.
   */
  public static final String      BOX_ROOT_ID              = "0";

  /**
   * Id of Trash folder on Box.
   */
  public static final String      BOX_TRASH_ID             = "1";

  /**
   * Box item_status for active items.
   */
  public static final String      BOX_ITEM_STATE_ACTIVE    = "active";

  /**
   * Box item_status for trashed items.
   */
  public static final String      BOX_ITEM_STATE_TRASHED   = "trashed";

  /**
   * Not official part of the path used in file services with Box API.
   */
  protected static final String   BOX_FILES_PATH           = "files/0/f/";

  /**
   * URL prefix for Box files' UI.
   */
  public static final String      BOX_FILE_URL             = "https://app.box.com/" + BOX_FILES_PATH;

  /**
   * Custom mimetype for Box's webdoc files.
   */
  public static final String      BOX_WEBDOCUMENT_MIMETYPE = "application/x-exo.box.webdoc";

  /**
   * Extension for Box's webdoc files.
   */
  public static final String      BOX_WEBDOCUMENT_EXT      = "webdoc";

  /**
   * URL patter for Embedded UI of Box file. Based on:<br>
   * http://stackoverflow.com/questions/12816239/box-com-embedded-file-folder-viewer-code-via-api
   * http://developers.box.com/box-embed/
   */
  public static final String      BOX_EMBED_URL            = "https://app.box.com/embed_widget/000000000000/%s?"
                                                               + "view=list&sort=date&theme=gray&show_parent_path=no&show_item_feed_actions=no&session_expired=true";

  public static final Set<String> BOX_EVENTS               = new HashSet<String>();

  static {
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_CREATE);
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_UPLOAD);
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_MOVE);
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_COPY);
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_TRASH);
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_UNDELETE_VIA_TRASH);
    BOX_EVENTS.add(BoxEvent.EVENT_TYPE_ITEM_RENAME);
  }

  private BoxToken                token;

  private BoxClient               client;

  private ChangesLink             changesLink;

  /**
   * Create Box API from OAuth2 authentication code.
   * 
   * @param key {@link String} Box API key the same also as OAuth2 client_id
   * @param clientSecret {@link String}
   * @param authCode {@link String}
   * @throws BoxException if authentication failed for any reason.
   * @throws CloudDriveException if credentials store exception happen
   */
  BoxAPI(String key, String clientSecret, String authCode, String redirectUri) throws BoxException,
      CloudDriveException {

    this.client = new BoxClient(key, clientSecret);
    this.token = new BoxToken();
    this.client.addOAuthRefreshListener(token);

    try {
      BoxOAuthRequestObject obj = BoxOAuthRequestObject.createOAuthRequestObject(authCode,
                                                                                 key,
                                                                                 clientSecret,
                                                                                 redirectUri);
      BoxOAuthToken bt = client.getOAuthManager().createOAuth(obj);
      this.token.store(bt);
      this.client.authenticate(bt);
    } catch (BoxRestException e) {
      throw new BoxException("Error submiting authentication code: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      throw new BoxException("Error authenticating user code: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      throw new BoxException("Authentication code error: " + e.getMessage(), e);
    }

    // finally init changes link
    updateChangesLink();
  }

  /**
   * Create Box API from existing user credentials.
   * 
   * @param key {@link String} Box API key the same also as OAuth2 client_id
   * @param clientSecret {@link String}
   * @param accessToken {@link String}
   * @param refreshToken {@link String}
   * @param expirationTime long, token expiration time on milliseconds
   * @throws CloudDriveException if credentials store exception happen
   */
  BoxAPI(String key, String clientSecret, String accessToken, String refreshToken, long expirationTime) throws CloudDriveException {
    this.client = new BoxClient(key, clientSecret);
    this.token = new BoxToken();
    this.token.load(accessToken, refreshToken, expirationTime);
    this.client.addOAuthRefreshListener(token);
    this.client.authenticateFromSecureStorage(token);

    // XXX changes link will be loaded on demand
    // updateChangesLink();
  }

  /**
   * Update OAuth2 tokens to a new one.
   * 
   * @param newToken {@link BoxToken}
   * @throws CloudDriveException
   */
  void updateToken(UserToken newToken) throws CloudDriveException {
    this.token.merge(newToken);
  }

  /**
   * Current OAuth2 token associated with this API instance.
   * 
   * @return {@link BoxToken}
   */
  UserToken getToken() {
    return token;
  }

  /**
   * Currently connected Box user.
   * 
   * @return
   * @throws BoxException
   */
  com.box.boxjavalibv2.dao.BoxUser currentUser() throws BoxException {
    BoxDefaultRequestObject obj = new BoxDefaultRequestObject();
    try {
      return client.getUsersManager().getCurrentUser(obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error requesting current user: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      throw new BoxException("Error getting current user: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      throw new BoxException("Authentication error for current user: " + e.getMessage(), e);
    }
  }

  /**
   * The Box root folder.
   * 
   * @return {@link BoxFolder}
   * @throws BoxException
   */
  BoxFolder getRootFolder() throws BoxException {
    try {
      BoxFolder root = client.getFoldersManager().getFolder(BOX_ROOT_ID, null);
      return root;
    } catch (BoxRestException e) {
      throw new BoxException("Error getting root folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      throw new BoxException("Error reading root folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      throw new BoxException("Authentication error for root folder: " + e.getMessage(), e);
    }
  }

  ItemsIterator getFolderItems(String folderId) throws CloudDriveException {
    return new ItemsIterator(folderId);
  }

  Calendar parseDate(String dateString) throws ParseException {
    Calendar calendar = Calendar.getInstance();
    Date d = ISO8601DateParser.parse(dateString);
    calendar.setTime(d);
    return calendar;
  }

  String formatDate(Calendar date) {
    return ISO8601DateParser.toString(date.getTime());
  }

  /**
   * Link (URl) to the Box file for opening on Box site (UI).
   * 
   * @param item {@link BoxItem}
   * @return String with the file URL.
   */
  String getLink(BoxItem item) {
    BoxSharedLink shared = item.getSharedLink();
    if (shared != null) {
      String link = shared.getUrl();
      if (link != null) {
        return link;
      }
    }

    // XXX This link build not on official documentation, but from observed URLs from Box app site.
    StringBuilder link = new StringBuilder();
    link.append(BOX_FILE_URL);
    String id = item.getId();
    if (BOX_ROOT_ID.equals(id)) {
      link.append(id);
    } else if (item instanceof BoxFile) {
      String parentId = item.getParent().getId();
      link.append(parentId);
      link.append("/1/f_");
      link.append(id);
    } else if (item instanceof BoxFolder) {
      link.append(id);
      link.append('/');
      link.append(item.getName());
    } else {
      // for unknown open root folder
      link.append(BOX_FILE_URL);
    }

    link.append("/");
    // TODO this doesn't take in account custom domain for Enterprise on Box
    return link.toString();
  }

  /**
   * Link (URL) to embed a file onto external app (in PLF).
   * 
   * @param item {@link BoxItem}
   * @return String with the file embed URL.
   */
  String getEmbedLink(BoxItem item) {
    StringBuilder linkValue = new StringBuilder();
    BoxSharedLink shared = item.getSharedLink();
    if (shared != null) {
      String link = shared.getUrl();
      String[] lparts = link.split("/");
      if (lparts.length > 3 && lparts[lparts.length - 2].equals("s")) {
        // XXX unofficial way of linkValue extracting from shared link
        linkValue.append("s/");
        linkValue.append(lparts[lparts.length - 1]);
      }
    }

    if (linkValue.length() == 0) {
      linkValue.append(BOX_FILES_PATH);
      // XXX This link build not on official documentation, but from observed URLs from Box app site.
      String id = item.getId();
      if (BOX_ROOT_ID.equals(id)) {
        linkValue.append(id);
      } else if (item instanceof BoxFile) {
        String parentId = item.getParent().getId();
        linkValue.append(parentId);
        linkValue.append("/1/f_");
        linkValue.append(id);
      } else if (item instanceof BoxFolder) {
        linkValue.append(id);
      } else {
        // for unknown open root folder
        linkValue.append(BOX_FILE_URL);
      }
    }

    // TODO this doesn't take in account custom domain for Enterprise on Box
    return String.format(BOX_EMBED_URL, linkValue.toString());
  }

  String getChangesLink() throws BoxException {
    if (changesLink != null) {
      return changesLink.getUrl();
    } else {
      return updateChangesLink();
    }
  }

  String updateChangesLink() throws BoxException {
    BoxDefaultRequestObject obj = new BoxDefaultRequestObject();
    try {
      BoxCollection changesPoll = client.getEventsManager().getEventOptions(obj);
      ArrayList<BoxTypedObject> ce = changesPoll.getEntries();
      if (ce.size() > 0) {
        BoxTypedObject c = ce.get(0);

        Object urlObj = c.getValue("url");
        String url = urlObj != null ? urlObj.toString() : null;

        Object typeObj = c.getValue("type");
        String type = typeObj != null ? typeObj.toString() : null;

        Object ttlObj = c.getValue("ttl");
        if (ttlObj == null) {
          ttlObj = c.getExtraData("ttl");
        }
        long ttl;
        try {
          ttl = ttlObj != null ? Long.parseLong(ttlObj.toString()) : 0;
        } catch (NumberFormatException e) {
          LOG.warn("Error parsing ttl value in Events response [" + ttlObj + "]: " + e);
          ttl = 0;
        }

        Object maxRetriesObj = c.getValue("max_retries");
        if (maxRetriesObj == null) {
          maxRetriesObj = c.getExtraData("max_retries");
        }
        long maxRetries;
        try {
          maxRetries = maxRetriesObj != null ? Long.parseLong(maxRetriesObj.toString()) : 0;
        } catch (NumberFormatException e) {
          LOG.warn("Error parsing max_retries value in Events response [" + maxRetriesObj + "]: " + e);
          maxRetries = 0;
        }

        Object retryTimeoutObj = c.getValue("retry_timeout");
        if (retryTimeoutObj == null) {
          retryTimeoutObj = c.getExtraData("retry_timeout");
        }
        long retryTimeout;
        try {
          retryTimeout = retryTimeoutObj != null ? Long.parseLong(retryTimeoutObj.toString()) : 0;
        } catch (NumberFormatException e) {
          LOG.warn("Error parsing retry_timeout value in Events response [" + retryTimeoutObj + "]: " + e);
          retryTimeout = 0;
        }

        changesLink = new ChangesLink(type, url, ttl, maxRetries, retryTimeout);
        return url;
      } else {
        throw new BoxException("Empty entries from Events service.");
      }
    } catch (BoxRestException e) {
      throw new BoxException("Error requesting changes long poll URL: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      throw new BoxException("Error reading changes long poll URL: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      throw new BoxException("Authentication error for changes long poll URL: " + e.getMessage(), e);
    }
  }

  EventsIterator getEvents(long streamPosition) throws BoxException, AuthTokenException {
    return new EventsIterator(streamPosition);
  }

  BoxFile createFile(String parentId, String name, Calendar created, InputStream data) throws BoxException,
                                                                                      AuthTokenException,
                                                                                      NotFoundException {
    try {
      // TODO You can optionally specify a Content-MD5 header with the SHA1 hash of the file to ensure that
      // the file is not corrupted in transit.
      BoxFileUploadRequestObject obj = BoxFileUploadRequestObject.uploadFileRequestObject(parentId,
                                                                                          name,
                                                                                          data);
      obj.setLocalFileCreatedAt(created.getTime());
      obj.put("created_at", formatDate(created));
      return client.getFilesManager().uploadFile(obj);
    } catch (UnsupportedEncodingException e) {
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (BoxRestException e) {
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then parent not found
        throw new NotFoundException("Parent not found " + parentId, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn�t have access to upload a file " + name, e);
      } else if (status == 409) {
        // conflict - the same name file exists
        throw new NotFoundException("File with the same name as creating already exists " + name, e);
      }
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when uploading file: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BoxException("File " + name + " uploading interrupted.", e);
    }
  }

  BoxFolder createFolder(String parentId, String name, Calendar created) throws BoxException,
                                                                        AuthTokenException,
                                                                        NotFoundException {
    try {
      BoxFolderRequestObject obj = BoxFolderRequestObject.createFolderRequestObject(name, parentId);
      obj.put("created_at", formatDate(created));
      return client.getFoldersManager().createFolder(obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error creating folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then parent not found
        throw new NotFoundException("Parent not found " + parentId, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn�t have access to create a folder " + name, e);
      } else if (status == 409) {
        // conflict - the same name file exists
        throw new NotFoundException("File with the same name as creating already exists " + name, e);
      }
      throw new BoxException("Error creating folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when creating folder: " + e.getMessage(), e);
    }
  }

  /**
   * Delete a cloud file by given fileId. Depending on Box enterprise settings for this user, the file will
   * either be actually deleted from Box or moved to the Trash.
   * 
   * @param id {@link String}
   * @throws BoxException
   * @throws AuthTokenException
   * @throws NotFoundException
   */
  void deleteFile(String id) throws BoxException, AuthTokenException, NotFoundException {
    try {
      BoxFileRequestObject obj = BoxFileRequestObject.deleteFileRequestObject();
      // obj.setIfMatch(etag); // // TODO use it!
      client.getFilesManager().deleteFile(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error deleting file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn�t have access to the file " + id, e);
      }
      throw new BoxException("Error deleting file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when deleting file: " + e.getMessage(), e);
    }
  }

  /**
   * Delete a cloud folder by given folderId. Depending on Box enterprise settings for this user, the folder
   * will
   * either be actually deleted from Box or moved to the Trash.
   * 
   * @param id {@link String}
   * @throws BoxException
   * @throws AuthTokenException
   * @throws NotFoundException
   */
  void deleteFolder(String id) throws BoxException, AuthTokenException, NotFoundException {
    try {
      BoxFolderRequestObject obj = BoxFolderRequestObject.deleteFolderRequestObject(true);
      // obj.setIfMatch(etag); // TODO use it!
      client.getFoldersManager().deleteFolder(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error deleting folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn�t have access to the folder " + id, e);
      }
      throw new BoxException("Error deleting folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when deleting folder: " + e.getMessage(), e);
    }
  }

  /**
   * Trash a cloud file by given fileId. Depending on Box enterprise settings for this user, the file will
   * either be actually deleted from Box or moved to the Trash. If the file was actually deleted on Box, this
   * method will throw {@link FileTrashRemovedException}, and the caller code should delete the file locally
   * also.
   * 
   * @param id {@link String}
   * @return {@link BoxFile} of the file successfully moved to Box Trash
   * @throws BoxException
   * @throws AuthTokenException
   * @throws FileTrashRemovedException if file was permanently removed.
   * @throws NotFoundException
   */
  BoxFile trashFile(String id) throws BoxException,
                              AuthTokenException,
                              FileTrashRemovedException,
                              NotFoundException {
    try {
      BoxFileRequestObject deleteObj = BoxFileRequestObject.deleteFileRequestObject();
      // deleteObj.setIfMatch(etag); // TODO use it!
      client.getFilesManager().deleteFile(id, deleteObj);

      // check if file actually removed or in the trash
      try {
        BoxDefaultRequestObject trashObj = new BoxDefaultRequestObject();
        return client.getFilesManager().getTrashFile(id, trashObj);
      } catch (BoxRestException e) {
        throw new BoxException("Error reading trashed file: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        int status = getErrorStatus(e);
        if (status == 404 || status == 412) {
          // not_found or precondition_failed - then file not found in the Trash
          // XXX throwing an exception not a best solution, but returning a boolean also can have double
          // meaning: not trashed at all or deleted instead of trashed
          throw new FileTrashRemovedException("Trashed file deleted permanently " + id);
        }
        throw new BoxException("Error reading trashed file: " + e.getMessage(), e);
      }
    } catch (BoxRestException e) {
      throw new BoxException("Error trashing file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn�t have access to the file " + id, e);
      }
      throw new BoxException("Error trashing file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when trashing file: " + e.getMessage(), e);
    }
  }

  /**
   * Trash a cloud folder by given folderId. Depending on Box enterprise settings for this user, the folder
   * will either be actually deleted from Box or moved to the Trash. If the folder was actually deleted in
   * Box, this method will return {@link FileTrashRemovedException}, and the caller code should delete the
   * folder locally also.
   * 
   * @param id {@link String}
   * @return {@link BoxFolder} of the folder successfully moved to Box Trash
   * @throws BoxException
   * @throws AuthTokenException
   * @throws FileTrashRemovedException if folder was permanently removed.
   * @throws NotFoundException
   */
  BoxFolder trashFolder(String id) throws BoxException,
                                  AuthTokenException,
                                  FileTrashRemovedException,
                                  NotFoundException {
    try {
      BoxFolderRequestObject deleteObj = BoxFolderRequestObject.deleteFolderRequestObject(true);
      // deleteObj.setIfMatch(etag); // TODO use it!
      client.getFoldersManager().deleteFolder(id, deleteObj);

      // check if file actually removed or in the trash
      try {
        BoxDefaultRequestObject trashObj = new BoxDefaultRequestObject();
        return client.getFoldersManager().getTrashFolder(id, trashObj);
      } catch (BoxRestException e) {
        throw new BoxException("Error reading trashed foler: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        int status = getErrorStatus(e);
        if (status == 404 || status == 412) {
          // not_found or precondition_failed - then foler not found in the Trash
          // XXX throwing an exception not a best solution, but returning a boolean also can have double
          // meaning: not trashed at all or deleted instead of trashed
          throw new FileTrashRemovedException("Trashed folder deleted permanently " + id);
        }
        throw new BoxException("Error reading trashed foler: " + e.getMessage(), e);
      }
    } catch (BoxRestException e) {
      throw new BoxException("Error trashing foler: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn�t have access to the folder " + id, e);
      }
      throw new BoxException("Error trashing foler: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when trashing foler: " + e.getMessage(), e);
    }
  }

  BoxFile untrashFile(String id) throws BoxException, AuthTokenException, NotFoundException {
    try {
      BoxItemRestoreRequestObject obj = BoxItemRestoreRequestObject.restoreItemRequestObject();
      return client.getFilesManager().restoreTrashFile(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error untrashing file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("Trashed file not found " + id, e);
      } else if (status == 405) {
        // method_not_allowed
        throw new NotFoundException("File not in the trash " + id, e);
      } else if (status == 409) {
        // conflict
        throw new NotFoundException("File with the same name as untrashed already exists " + id, e);
      }
      throw new BoxException("Error untrashing file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when untrashing file: " + e.getMessage(), e);
    }
  }

  BoxFolder untrashFolder(String id) throws BoxException, AuthTokenException, NotFoundException {
    try {
      BoxItemRestoreRequestObject obj = BoxItemRestoreRequestObject.restoreItemRequestObject();
      return client.getFoldersManager().restoreTrashFolder(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error untrashing folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("Trashed folder not found " + id, e);
      } else if (status == 405) {
        // method_not_allowed
        throw new NotFoundException("Folder not in the trash " + id, e);
      } else if (status == 409) {
        // conflict
        throw new NotFoundException("Folder with the same name as untrashed already exists " + id, e);
      }
      throw new BoxException("Error untrashing folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when untrashing folder: " + e.getMessage(), e);
    }
  }

  /**
   * Update file name or/and parent and set given modified date. If file was actually updated (name or/and
   * parent changed) this method return updated file object or <code>null</code> if file already exists
   * with such name and parent.
   * 
   * 
   * @param parentId {@link String}
   * @param id {@link String}
   * @param name {@link String}
   * @param modified {@link Calendar}
   * @return {@link BoxFile} of actually changed file or <code>null</code> if file already exists with
   *         such name and parent.
   * @throws BoxException
   * @throws AuthTokenException
   * @throws NotFoundException
   */
  BoxFile updateFile(String parentId, String id, String name, Calendar modified) throws BoxException,
                                                                                AuthTokenException,
                                                                                NotFoundException {

    BoxFile existing = readFile(id);
    int attemts = 0;
    boolean nameChanged = !existing.getName().equals(name);
    boolean parentChanged = !existing.getParent().getId().equals(parentId);
    while ((nameChanged || parentChanged) && attemts < 3) {
      attemts++;
      try {
        // if name of parent changed - we do actual update, we ignore modified date changes
        // otherwise, if name the same, Box service will respond with error 409 (conflict)
        BoxFileRequestObject obj = BoxFileRequestObject.updateFileRequestObject();
        // obj.setIfMatch(etag); // TODO use it
        if (nameChanged) {
          obj.setName(name);
        }
        if (parentChanged) {
          obj.setParent(parentId);
        }
        obj.put("modified_at", formatDate(modified));
        return client.getFilesManager().updateFileInfo(id, obj);
      } catch (BoxRestException e) {
        throw new BoxException("Error updating file: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        int status = getErrorStatus(e);
        if (status == 404 || status == 412) {
          // not_found or precondition_failed - then item not found
          throw new NotFoundException("File not found " + id, e);
        } else if (status == 409) {
          // conflict, try again
          if (attemts < 3) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("File with the same name as updated already exists " + id + ". Trying again.");
            }
            existing = readFile(id);
            nameChanged = !existing.getName().equals(name);
            parentChanged = !existing.getParent().getId().equals(parentId);
          } else {
            throw new NotFoundException("File with the same name as updated already exists " + id);
          }
        }
        throw new BoxException("Error updating file: " + e.getMessage(), e);
      } catch (UnsupportedEncodingException e) {
        throw new BoxException("Error updating file: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        if (e.isCallerResponsibleForFix()) {
          // we need new access token (refresh token already expired here)
          throw new AuthTokenException("Authentication failure. Reauthenticate.");
        }
        throw new BoxException("Authentication error when updating file: " + e.getMessage(), e);
      }
    }
    // else we return null, it means existing file wasn't changed
    return null;
  }

  BoxFile updateFileContent(String parentId, String id, String name, Calendar modified, InputStream data) throws BoxException,
                                                                                                         AuthTokenException,
                                                                                                         NotFoundException {
    try {
      BoxFileUploadRequestObject obj = BoxFileUploadRequestObject.uploadFileRequestObject(parentId,
                                                                                          name,
                                                                                          data);
      obj.setLocalFileLastModifiedAt(modified.getTime());
      obj.put("modified_at", formatDate(modified));
      return client.getFilesManager().uploadNewVersion(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error uploading new version of file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      }
      throw new BoxException("Error uploading new version of file: " + e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      throw new BoxException("Error uploading new version of file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when uploading new version of file: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BoxException("File " + name + ", new version uploading interrupted.", e);
    }
  }

  /**
   * Update folder name or/and parent and set given modified date. If folder was actually updated (name or/and
   * parent changed) this method return updated folder object or <code>null</code> if folder already exists
   * with such name and parent.
   * 
   * @param parentId {@link String}
   * @param id {@link String}
   * @param name {@link String}
   * @param modified {@link Calendar}
   * @return {@link BoxFolder} of actually changed folder or <code>null</code> if folder already exists with
   *         such name and parent.
   * @throws BoxException
   * @throws AuthTokenException
   * @throws NotFoundException
   */
  BoxFolder updateFolder(String parentId, String id, String name, Calendar modified) throws BoxException,
                                                                                    AuthTokenException,
                                                                                    NotFoundException {
    BoxFolder existing = readFolder(id);
    int attemts = 0;
    boolean nameChanged = !existing.getName().equals(name);
    boolean parentChanged = !existing.getParent().getId().equals(parentId);
    while ((nameChanged || parentChanged) && attemts < 3) {
      attemts++;
      // if name of parent changed - we do actual update, we ignore modified date changes
      // otherwise, if name the same, Box service will respond with error 409 (conflict)
      try {
        BoxFolderRequestObject obj = BoxFolderRequestObject.createFolderRequestObject(name, parentId);
        obj.put("modified_at", formatDate(modified));
        return client.getFoldersManager().updateFolderInfo(id, obj);
      } catch (BoxRestException e) {
        throw new BoxException("Error updating folder: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        int status = getErrorStatus(e);
        if (status == 404 || status == 412) {
          // not_found or precondition_failed - then item not found
          throw new NotFoundException("Folder not found " + id, e);
        } else if (status == 409) {
          // conflict, try again
          if (attemts < 3) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Folder with the same name as updated already exists " + id + ". Trying again.");
            }
            existing = readFolder(id);
            nameChanged = !existing.getName().equals(name);
            parentChanged = !existing.getParent().getId().equals(parentId);
          } else {
            throw new NotFoundException("Folder with the same name as updated already exists " + id);
          }
        }
        throw new BoxException("Error updating folder: " + e.getMessage(), e);
      } catch (UnsupportedEncodingException e) {
        throw new BoxException("Error updating folder: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        if (e.isCallerResponsibleForFix()) {
          // we need new access token (refresh token already expired here)
          throw new AuthTokenException("Authentication failure. Reauthenticate.");
        }
        throw new BoxException("Authentication error when updating folder: " + e.getMessage(), e);
      }
    }
    // else we return null, it means existing folder wasn't changed
    return null;
  }

  BoxFile readFile(String id) throws BoxException, AuthTokenException, NotFoundException {
    try {
      return client.getFilesManager().getFile(id, new BoxDefaultRequestObject());
    } catch (BoxRestException e) {
      throw new BoxException("Error reading file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      }
      throw new BoxException("Error reading file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when reading file: " + e.getMessage(), e);
    }
  }

  BoxFolder readFolder(String id) throws BoxException, AuthTokenException, NotFoundException {
    try {
      return client.getFoldersManager().getFolder(id, new BoxDefaultRequestObject());
    } catch (BoxRestException e) {
      throw new BoxException("Error reading folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("Folder not found " + id, e);
      }
      throw new BoxException("Error reading folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      if (e.isCallerResponsibleForFix()) {
        // we need new access token (refresh token already expired here)
        throw new AuthTokenException("Authentication failure. Reauthenticate.");
      }
      throw new BoxException("Authentication error when reading folder: " + e.getMessage(), e);
    }
  }

  // ********* internal *********

  /**
   * Find server error status.
   * 
   * @param e {@link BoxServerException}
   * @return int
   */
  int getErrorStatus(BoxServerException e) {
    BoxServerError se = e.getError();
    if (se != null) {
      int status = se.getStatus();
      if (status != 0) {
        return status;
      }
    }
    return e.getStatusCode();
  }
}
