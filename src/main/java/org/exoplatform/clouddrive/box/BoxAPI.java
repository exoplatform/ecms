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

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.BoxConfigBuilder;
import com.box.boxjavalibv2.BoxRESTClient;
import com.box.boxjavalibv2.authorization.IAuthSecureStorage;
import com.box.boxjavalibv2.authorization.OAuthDataController.OAuthTokenState;
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
import com.box.boxjavalibv2.dao.IAuthData;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.jsonparsing.BoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.BoxResourceHub;
import com.box.boxjavalibv2.requests.requestobjects.BoxEventRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderDeleteRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxItemCopyRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxItemRestoreRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxRequestExtras;
import com.box.boxjavalibv2.utils.ISO8601DateParser;
import com.box.boxjavalibv2.utils.Utils;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.requestsbase.BoxDefaultRequestObject;
import com.box.restclientv2.requestsbase.BoxFileUploadRequestObject;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.ConflictException;
import org.exoplatform.clouddrive.FileTrashRemovedException;
import org.exoplatform.clouddrive.NotFoundException;
import org.exoplatform.clouddrive.RefreshAccessException;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.clouddrive.utils.ChunkIterator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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

  protected static final Log      LOG                      = ExoLogger.getLogger(BoxAPI.class);

  /**
   * Pagination size used within Box API.
   */
  public static final int         BOX_PAGE_SIZE            = 100;

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

  class StoredToken extends UserToken implements OAuthRefreshListener, IAuthSecureStorage {

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
      Map<String, Object> data = new HashMap<String, Object>();
      data.put(BoxOAuthToken.FIELD_ACCESS_TOKEN, getAccessToken());
      data.put(BoxOAuthToken.FIELD_REFRESH_TOKEN, getRefreshToken());
      data.put(BoxOAuthToken.FIELD_EXPIRES_IN, getExpirationTime());
      data.put(BoxOAuthToken.FIELD_TOKEN_TYPE, "bearer");
      return new BoxOAuthToken(data);
    }
  }

  /**
   * Iterator over whole set of items from Box service. This iterator hides next-chunk logic on
   * request to the service. <br>
   * Iterator methods can throw {@link BoxException} in case of remote or communication errors.
   */
  class ItemsIterator extends ChunkIterator<BoxItem> {
    final String folderId;

    int          offset = 0, total = 0;

    /**
     * Parent folder.
     */
    BoxFolder    parent;

    ItemsIterator(String folderId) throws CloudDriveException {
      this.folderId = folderId;

      // fetch first
      this.iter = nextChunk();
    }

    protected Iterator<BoxItem> nextChunk() throws CloudDriveException {
      BoxDefaultRequestObject obj = new BoxDefaultRequestObject();
      obj.setPage(BOX_PAGE_SIZE, offset);

      BoxRequestExtras ext = obj.getRequestExtras();
      ext.addField(BoxItem.FIELD_ID);
      ext.addField(BoxItem.FIELD_PARENT);
      ext.addField(BoxItem.FIELD_NAME);
      ext.addField(BoxItem.FIELD_TYPE);
      ext.addField(BoxItem.FIELD_ETAG);
      ext.addField(BoxItem.FIELD_SEQUENCE_ID);
      ext.addField(BoxItem.FIELD_CREATED_AT);
      ext.addField(BoxItem.FIELD_MODIFIED_AT);
      ext.addField(BoxItem.FIELD_DESCRIPTION);
      ext.addField(BoxItem.FIELD_SIZE);
      ext.addField(BoxItem.FIELD_CREATED_BY);
      ext.addField(BoxItem.FIELD_MODIFIED_BY);
      ext.addField(BoxItem.FIELD_OWNED_BY);
      ext.addField(BoxItem.FIELD_SHARED_LINK);
      ext.addField(BoxItem.FIELD_ITEM_STATUS);
      ext.addField(BoxItem.FIELD_PATH_COLLECTION);
      ext.addField(BoxFolder.FIELD_ITEM_COLLECTION);

      try {
        parent = client.getFoldersManager().getFolder(folderId, obj);

        BoxCollection items = parent.getItemCollection();
        // total number of files in the folder
        total = items.getTotalCount();
        if (offset == 0) {
          available(total);
        }

        offset += items.getEntries().size();

        ArrayList<BoxItem> oitems = new ArrayList<BoxItem>();
        // put folders first, then files
        oitems.addAll(Utils.getTypedObjects(items, BoxFolder.class));
        oitems.addAll(Utils.getTypedObjects(items, BoxFile.class));
        return oitems.iterator();
      } catch (BoxRestException e) {
        throw new BoxException("Error getting folder items: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        int status = getErrorStatus(e);
        if (status == 404 || status == 412) {
          // not_found or precondition_failed - then folder not found
          throw new NotFoundException("Folder not found " + folderId, e);
        }
        throw new BoxException("Error reading folder items: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        checkTokenState();
        throw new BoxException("Authentication error on folder items: " + e.getMessage(), e);
      }
    }

    protected boolean hasNextChunk() {
      return total > offset;
    }
  }

  /**
   * Iterator over set of events from Box service. This iterator hides next-chunk logic on
   * request to the service. <br>
   * Iterator methods can throw {@link BoxException} in case of remote or communication errors.
   */
  class EventsIterator extends ChunkIterator<BoxEvent> {
    /**
     * Set of already fetched event Ids. Used to ignore duplicates from different requests.
     */
    final Set<String> eventIds = new HashSet<String>();

    Long              streamPosition;

    Integer           offset   = 0, chunkSize = 0;

    List<BoxEvent>    nextChunk;

    EventsIterator(long streamPosition) throws BoxException, RefreshAccessException {
      this.streamPosition = streamPosition <= -1 ? BoxEventRequestObject.STREAM_POSITION_NOW : streamPosition;

      // fetch first
      this.iter = nextChunk();
    }

    protected Iterator<BoxEvent> nextChunk() throws BoxException, RefreshAccessException {
      try {
        BoxEventRequestObject request = BoxEventRequestObject.getEventsRequestObject(streamPosition);

        // interest to tree changes only
        request.setStreamType(BoxEventRequestObject.STREAM_TYPE_CHANGES);
        request.setLimit(BOX_PAGE_SIZE);

        BoxEventCollection ec = client.getEventsManager().getEvents(request);

        // for next chunk and next iterators
        streamPosition = ec.getNextStreamPosition();

        ArrayList<BoxEvent> events = new ArrayList<BoxEvent>();
        for (BoxTypedObject eobj : ec.getEntries()) {
          BoxEvent event = (BoxEvent) eobj;
          if (BOX_EVENTS.contains(event.getEventType())) {
            String id = event.getId();
            if (!eventIds.contains(id)) {
              eventIds.add(id);
              events.add(event);
            }
          }
        }

        this.chunkSize = events.size();
        return events.iterator();
      } catch (BoxRestException e) {
        throw new BoxException("Error requesting Events service: " + e.getMessage(), e);
      } catch (BoxServerException e) {
        throw new BoxException("Error reading Events service: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        checkTokenState();
        throw new BoxException("Authentication error for Events service: " + e.getMessage(), e);
      }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean hasNextChunk() {
      // if something was read in previous chunk, then we may have a next chunk
      return chunkSize > 0;
    }

    long getNextStreamPosition() {
      return streamPosition;
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

  /**
   * Box REST client adopted to Apache HTTP 4.1 (from Platform 4.0) and using allow-all hostname validator.
   * Purpose of this client is to workaround <a href=
   * "http://stackoverflow.com/questions/23529852/multiple-files-upload-to-box-fails-with-http-client-error-connection-still-allo"
   * >multiple files upload problem</a>.
   */
  class RESTClient extends BoxRESTClient {

    final HttpClient httpClient;

    RESTClient() {
      super();

      SchemeRegistry schemeReg = new SchemeRegistry();
      schemeReg.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
      SSLSocketFactory socketFactory;
      try {
        SSLContext sslContext = SSLContext.getInstance(SSLSocketFactory.TLS);
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(null, null);
        KeyManager[] keymanagers = kmfactory.getKeyManagers();
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init((KeyStore) null);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        sslContext.init(keymanagers, trustmanagers, null);
        socketFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      } catch (Exception ex) {
        throw new IllegalStateException("Failure initializing default SSL context for Box REST client", ex);
      }
      schemeReg.register(new Scheme("https", 443, socketFactory));

      ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager(schemeReg);
      // XXX 2 recommended by RFC 2616 sec 8.1.4, we make it bigger for quicker // upload
      connectionManager.setDefaultMaxPerRoute(4);
      // 20 by default, we twice it also
      connectionManager.setMaxTotal(40);

      this.httpClient = new DefaultHttpClient(connectionManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpClient getRawHttpClient() {
      return httpClient;
    }
  }

  private StoredToken token;

  private BoxClient   client;

  private ChangesLink changesLink;

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

    BoxResourceHub hub = new BoxResourceHub();
    BoxJSONParser parser = new BoxJSONParser(hub);
    this.client = new BoxClient(key,
                                clientSecret,
                                hub,
                                parser,
                                new RESTClient(),
                                new BoxConfigBuilder().build());

    // this.client = new BoxClient(key, clientSecret);
    this.token = new StoredToken();
    this.client.addOAuthRefreshListener(token);

    try {
      // BoxOAuthRequestObject obj = BoxOAuthRequestObject.createOAuthRequestObject(authCode,
      // key,
      // clientSecret,
      // redirectUri);
      BoxOAuthToken bt = this.client.getOAuthManager().createOAuth(authCode, key, clientSecret, redirectUri);

      // BoxOAuthToken bt = client.getOAuthManager().createOAuth(obj);
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
    // this.client = new BoxClient(key, clientSecret);
    BoxResourceHub hub = new BoxResourceHub();
    BoxJSONParser parser = new BoxJSONParser(hub);
    this.client = new BoxClient(key,
                                clientSecret,
                                hub,
                                parser,
                                new RESTClient(),
                                new BoxConfigBuilder().build());

    this.token = new StoredToken();
    this.token.load(accessToken, refreshToken, expirationTime);
    this.client.addOAuthRefreshListener(token);
    this.client.authenticateFromSecureStorage(token);
  }

  /**
   * Update OAuth2 token to a new one.
   * 
   * @param newToken {@link StoredToken}
   * @throws CloudDriveException
   */
  void updateToken(UserToken newToken) throws CloudDriveException {
    this.token.merge(newToken);
  }

  /**
   * Current OAuth2 token associated with this API instance.
   * 
   * @return {@link StoredToken}
   */
  StoredToken getToken() {
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

  String getChangesLink() throws BoxException, RefreshAccessException {
    if (changesLink != null) {
      return changesLink.getUrl();
    } else {
      return updateChangesLink();
    }
  }

  String updateChangesLink() throws BoxException, RefreshAccessException {
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
      checkTokenState();
      throw new BoxException("Authentication error for changes long poll URL: " + e.getMessage(), e);
    }
  }

  EventsIterator getEvents(long streamPosition) throws BoxException, RefreshAccessException {
    return new EventsIterator(streamPosition);
  }

  BoxFile createFile(String parentId, String name, Calendar created, InputStream data) throws BoxException,
                                                                                      NotFoundException,
                                                                                      RefreshAccessException,
                                                                                      ConflictException {
    try {
      // To speedup the process we check if parent exists first.
      // How this speedups: if parent not found we will not wait for the content upload to the Box side.
      try {
        readFolder(parentId);
      } catch (NotFoundException e) {
        // parent not found
        throw new NotFoundException("Parent not found " + parentId + ". Cannot start file uploading " + name,
                                    e);
      }

      // TODO You can optionally specify a Content-MD5 header with the SHA1 hash of the file to ensure that
      // the file is not corrupted in transit.
      BoxFileUploadRequestObject obj = BoxFileUploadRequestObject.uploadFileRequestObject(parentId,
                                                                                          name,
                                                                                          data);
      obj.setLocalFileCreatedAt(created.getTime());
      obj.put("created_at", formatDate(created));
      return client.getFilesManager().uploadFile(obj);
    } catch (BoxJSONException e) {
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (BoxRestException e) {
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then parent not found
        throw new NotFoundException("Parent not found " + parentId + ". File uploading canceled for " + name,
                                    e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn’t have access to upload a file " + name, e);
      } else if (status == 409) {
        // conflict - the same name file exists
        throw new ConflictException("File with the same name as creating already exists " + name, e);
      }
      throw new BoxException("Error uploading file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when uploading file: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BoxException("File " + name + " uploading interrupted.", e);
    }
  }

  BoxFolder createFolder(String parentId, String name, Calendar created) throws BoxException,
                                                                        NotFoundException,
                                                                        RefreshAccessException,
                                                                        ConflictException {
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
        throw new NotFoundException("The user doesn’t have access to create a folder " + name, e);
      } else if (status == 409) {
        // conflict - the same name file exists
        throw new ConflictException("File with the same name as creating already exists " + name, e);
      }
      throw new BoxException("Error creating folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when creating folder: " + e.getMessage(), e);
    }
  }

  BoxFolder createSharedFolder(String parentId, String name, Calendar created) throws BoxException,
                                                                        NotFoundException,
                                                                        RefreshAccessException,
                                                                        ConflictException {
    try {
      BoxFolderRequestObject obj = BoxFolderRequestObject.createSharedLinkRequestObject(null); // TODO
      obj.put("created_at", formatDate(created));
      return client.getSharedFoldersManager("sharedLink", "password").createFolder(obj); // TODO
    } catch (BoxRestException e) {
      throw new BoxException("Error creating folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then parent not found
        throw new NotFoundException("Parent not found " + parentId, e);
      } else if (status == 403) {
        throw new NotFoundException("The user doesn’t have access to create a folder " + name, e);
      } else if (status == 409) {
        // conflict - the same name file exists
        throw new ConflictException("File with the same name as creating already exists " + name, e);
      }
      throw new BoxException("Error creating folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when creating folder: " + e.getMessage(), e);
    }
  }

  /**
   * Delete a cloud file by given fileId. Depending on Box enterprise settings for this user, the file will
   * either be actually deleted from Box or moved to the Trash.
   * 
   * @param id {@link String}
   * @throws BoxException
   * @throws NotFoundException
   * @throws RefreshAccessException
   */
  void deleteFile(String id) throws BoxException, NotFoundException, RefreshAccessException {
    try {
      BoxDefaultRequestObject obj = new BoxDefaultRequestObject();
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
        throw new NotFoundException("The user doesn’t have access to the file " + id, e);
      }
      throw new BoxException("Error deleting file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
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
   * @throws NotFoundException
   * @throws RefreshAccessException
   */
  void deleteFolder(String id) throws BoxException, NotFoundException, RefreshAccessException {
    try {
      BoxFolderDeleteRequestObject obj = BoxFolderDeleteRequestObject.deleteFolderRequestObject(true);
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
        throw new NotFoundException("The user doesn’t have access to the folder " + id, e);
      }
      throw new BoxException("Error deleting folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
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
   * @throws FileTrashRemovedException if file was permanently removed.
   * @throws NotFoundException
   * @throws RefreshAccessException
   */
  BoxFile trashFile(String id) throws BoxException,
                              FileTrashRemovedException,
                              NotFoundException,
                              RefreshAccessException {
    try {
      BoxDefaultRequestObject deleteObj = new BoxDefaultRequestObject();
      // deleteObj.setIfMatch(etag); // TODO use it!
      client.getFilesManager().deleteFile(id, deleteObj);

      // check if file actually removed or in the trash
      try {
        BoxDefaultRequestObject trashObj = new BoxDefaultRequestObject();
        return client.getTrashManager().getTrashFile(id, trashObj);
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
        throw new NotFoundException("The user doesn’t have access to the file " + id, e);
      }
      throw new BoxException("Error trashing file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
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
   * @throws FileTrashRemovedException if folder was permanently removed.
   * @throws NotFoundException
   * @throws RefreshAccessException
   */
  BoxFolder trashFolder(String id) throws BoxException,
                                  FileTrashRemovedException,
                                  NotFoundException,
                                  RefreshAccessException {
    try {
      BoxFolderDeleteRequestObject deleteObj = BoxFolderDeleteRequestObject.deleteFolderRequestObject(true);
      // deleteObj.setIfMatch(etag); // TODO use it!
      client.getFoldersManager().deleteFolder(id, deleteObj);

      // check if file actually removed or in the trash
      try {
        BoxDefaultRequestObject trashObj = new BoxDefaultRequestObject();
        return client.getTrashManager().getTrashFolder(id, trashObj);
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
        throw new NotFoundException("The user doesn’t have access to the folder " + id, e);
      }
      throw new BoxException("Error trashing foler: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when trashing foler: " + e.getMessage(), e);
    }
  }

  BoxFile untrashFile(String id, String name) throws BoxException,
                                             NotFoundException,
                                             RefreshAccessException,
                                             ConflictException {
    try {
      BoxItemRestoreRequestObject obj = BoxItemRestoreRequestObject.restoreItemRequestObject();
      if (name != null) {
        obj.setNewName(name);
      }
      return client.getTrashManager().restoreTrashFile(id, obj);
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
        throw new ConflictException("File with the same name as untrashed already exists " + id, e);
      }
      throw new BoxException("Error untrashing file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when untrashing file: " + e.getMessage(), e);
    }
  }

  BoxFolder untrashFolder(String id, String name) throws BoxException,
                                                 NotFoundException,
                                                 RefreshAccessException,
                                                 ConflictException {
    try {
      BoxItemRestoreRequestObject obj = BoxItemRestoreRequestObject.restoreItemRequestObject();
      if (name != null) {
        obj.setNewName(name);
      }
      return client.getTrashManager().restoreTrashFolder(id, obj);
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
        throw new ConflictException("Folder with the same name as untrashed already exists " + id, e);
      }
      throw new BoxException("Error untrashing folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
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
   * @throws NotFoundException
   * @throws RefreshAccessException
   * @throws ConflictException
   */
  BoxFile updateFile(String parentId, String id, String name, Calendar modified) throws BoxException,
                                                                                NotFoundException,
                                                                                RefreshAccessException,
                                                                                ConflictException {

    BoxFile existing = readFile(id);
    int attemts = 0;
    boolean nameChanged = !existing.getName().equals(name);
    boolean parentChanged = !existing.getParent().getId().equals(parentId);
    while ((nameChanged || parentChanged) && attemts < 3) {
      attemts++;
      try {
        // if name or parent changed - we do actual update, we ignore modified date changes
        // otherwise, if name the same, Box service will respond with error 409 (conflict)
        BoxFileRequestObject obj = BoxFileRequestObject.getRequestObject();
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
            nameChanged = !existing.getName().equalsIgnoreCase(name);
            parentChanged = !existing.getParent().getId().equals(parentId);
          } else {
            throw new ConflictException("File with the same name as updated already exists " + id);
          }
        } else {
          throw new BoxException("Error updating file: " + e.getMessage(), e);
        }
      } catch (UnsupportedEncodingException e) {
        throw new BoxException("Error updating file: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        checkTokenState();
        throw new BoxException("Authentication error when updating file: " + e.getMessage(), e);
      }
    }
    // else we return null, it means existing file wasn't changed
    return null;
  }

  BoxFile updateFileContent(String parentId, String id, String name, Calendar modified, InputStream data) throws BoxException,
                                                                                                         NotFoundException,
                                                                                                         RefreshAccessException {
    try {
      BoxFileUploadRequestObject obj = BoxFileUploadRequestObject.uploadFileRequestObject(parentId,
                                                                                          name,
                                                                                          data);
      obj.setLocalFileLastModifiedAt(modified.getTime());
      obj.put("modified_at", formatDate(modified));
      return client.getFilesManager().uploadNewVersion(id, obj);
    } catch (BoxJSONException e) {
      throw new BoxException("Error uploading new version of file: " + e.getMessage(), e);
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
      checkTokenState();
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
   * @throws NotFoundException
   * @throws RefreshAccessException
   * @throws ConflictException
   */
  BoxFolder updateFolder(String parentId, String id, String name, Calendar modified) throws BoxException,
                                                                                    NotFoundException,
                                                                                    RefreshAccessException,
                                                                                    ConflictException {
    BoxFolder existing = readFolder(id);
    int attemts = 0;
    boolean nameChanged = !existing.getName().equals(name);
    boolean parentChanged = !existing.getParent().getId().equals(parentId);
    while ((nameChanged || parentChanged) && attemts < 3) {
      attemts++;
      // if name or parent changed - we do actual update, we ignore modified date changes
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
            nameChanged = !existing.getName().equalsIgnoreCase(name);
            parentChanged = !existing.getParent().getId().equals(parentId);
          } else {
            throw new ConflictException("Folder with the same name as updated already exists " + id);
          }
        } else {
          throw new BoxException("Error updating folder: " + e.getMessage(), e);
        }
      } catch (UnsupportedEncodingException e) {
        throw new BoxException("Error updating folder: " + e.getMessage(), e);
      } catch (AuthFatalFailureException e) {
        checkTokenState();
        throw new BoxException("Authentication error when updating folder: " + e.getMessage(), e);
      }
    }
    // else we return null, it means existing folder wasn't changed
    return null;
  }

  /**
   * Copy file to a new one. If file was successfully copied this method return new file object.
   * 
   * 
   * @param id {@link String}
   * @param parentId {@link String}
   * @param name {@link String}
   * @param modified {@link Calendar}
   * @return {@link BoxFile} of actually copied file.
   * @throws BoxException
   * @throws NotFoundException
   * @throws RefreshAccessException
   * @throws ConflictException
   */
  BoxFile copyFile(String id, String parentId, String name) throws BoxException,
                                                           NotFoundException,
                                                           RefreshAccessException,
                                                           ConflictException {
    try {
      BoxItemCopyRequestObject obj = BoxItemCopyRequestObject.copyItemRequestObject(parentId);
      // obj.setIfMatch(etag); // TODO use it
      obj.setName(name);
      return client.getFilesManager().copyFile(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error copying file: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("File not found " + id, e);
      } else if (status == 409) {
        // conflict, try again
        if (LOG.isDebugEnabled()) {
          LOG.debug("File with the same name as copying already exists " + id + ". Trying again.");
        }
        throw new ConflictException("File with the same name as copying already exists " + id);
      }
      throw new BoxException("Error copying file: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when copying file: " + e.getMessage(), e);
    }
  }

  /**
   * Copy folder to a new one. If folder was successfully copied this method return new folder object.
   * 
   * @param id {@link String}
   * @param parentId {@link String}
   * @param name {@link String}
   * @return {@link BoxFile} of actually copied folder.
   * @throws BoxException
   * @throws NotFoundException
   * @throws RefreshAccessException
   * @throws ConflictException
   */
  BoxFolder copyFolder(String id, String parentId, String name) throws BoxException,
                                                               NotFoundException,
                                                               RefreshAccessException,
                                                               ConflictException {
    try {
      BoxItemCopyRequestObject obj = BoxItemCopyRequestObject.copyItemRequestObject(parentId);
      // obj.setIfMatch(etag); // TODO use it
      obj.setName(name);
      return client.getFoldersManager().copyFolder(id, obj);
    } catch (BoxRestException e) {
      throw new BoxException("Error copying folder: " + e.getMessage(), e);
    } catch (BoxServerException e) {
      int status = getErrorStatus(e);
      if (status == 404 || status == 412) {
        // not_found or precondition_failed - then item not found
        throw new NotFoundException("Folder not found " + id, e);
      } else if (status == 409) {
        // conflict, try again
        if (LOG.isDebugEnabled()) {
          LOG.debug("Folder with the same name as copying already exists " + id + ". Trying again.");
        }
        throw new ConflictException("Folder with the same name as copying already exists " + id);
      }
      throw new BoxException("Error copying folder: " + e.getMessage(), e);
    } catch (AuthFatalFailureException e) {
      checkTokenState();
      throw new BoxException("Authentication error when copying folder: " + e.getMessage(), e);
    }
  }

  BoxFile readFile(String id) throws BoxException, NotFoundException, RefreshAccessException {
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
      checkTokenState();
      throw new BoxException("Authentication error when reading file: " + e.getMessage(), e);
    }
  }

  BoxFolder readFolder(String id) throws BoxException, NotFoundException, RefreshAccessException {
    try {// client.getSharedFoldersManager("", "").createFolder(requestObject)
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
      checkTokenState();
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
  private int getErrorStatus(BoxServerException e) {
    BoxServerError se = e.getError();
    if (se != null) {
      int status = se.getStatus();
      if (status != 0) {
        return status;
      }
    }
    return e.getStatusCode();
  }

  /**
   * Check if need new access token from user (refresh token already expired).
   * 
   * @throws RefreshAccessException if client failed to refresh the access token and need new new token
   */
  private void checkTokenState() throws RefreshAccessException {
    if (OAuthTokenState.FAIL.equals(client.getAuthState())) {
      // we need new access token (refresh token already expired here)
      throw new RefreshAccessException("Authentication failure. Reauthenticate.");
    }
  }
}
