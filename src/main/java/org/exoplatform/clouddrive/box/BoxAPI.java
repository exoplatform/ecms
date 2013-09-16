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
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.dao.BoxSharedLink;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.interfaces.IAuthData;
import com.box.boxjavalibv2.requests.requestobjects.BoxDefaultRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxOAuthRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.box.boxjavalibv2.resourcemanagers.BoxFoldersManager;
import com.box.boxjavalibv2.utils.ISO8601DateParser;
import com.box.restclientv2.exceptions.BoxRestException;

import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.oauth2.UserToken;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

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

  class BoxToken extends UserToken implements OAuthRefreshListener {

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
  }

  /**
   * Iterator over whole set of items from Box service. This iterator hides next-chunk logic on
   * request to the service. <br>
   * Iterator methods can throw {@link BoxException} in case of remote or communication errors.
   */
  class ItemsIterator {
    final String      folderId;

    Iterator<BoxItem> iter;

    BoxItem           next;

    /**
     * Forecast of available items in the iterator. Calculated on each {@link #nextPage()}. Used for progress
     * indicator.
     */
    volatile int      available;

    /**
     * Totally fetched items. Changes on each {@link #next()}. Used for progress indicator.
     */
    volatile int      fetched;

    int               limit, offset;

    /**
     * Parent folder.
     */
    BoxFolder         parent;

    ItemsIterator(String folderId) throws BoxException {
      this.folderId = folderId;
      this.limit = 1000;
      this.offset = 0;

      // fetch first
      this.iter = nextChunk();
    }

    Iterator<BoxItem> nextChunk() throws BoxException {
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
        // BoxCollection items = client.getFoldersManager().getFolderItems(folderId, obj);
        // offset = items.getNextStreamPosition();

        parent = client.getFoldersManager().getFolder(folderId, obj);

        BoxCollection items = parent.getItemCollection();
        available(items.getTotalCount());

        // ArrayList<BoxTypedObject> entries = items.getEntries();
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
        throw new BoxException("Authentication error on folder items: " + e.getMessage(), e);
      }
    }

    boolean hasNextChunk() {
      return false; // XXX we assume all fully loaded by once
    }

    boolean hasNext() throws BoxException {
      if (next == null) {
        if (iter.hasNext()) {
          next = iter.next();
        } else {
          // try to fetch next portion of changes
          while (hasNextChunk()) {
            iter = nextChunk();
            if (iter.hasNext()) {
              next = iter.next();
              break;
            }
          }
        }
        return next != null;
      } else {
        return true;
      }
    }

    BoxItem next() throws NoSuchElementException {
      if (next == null) {
        throw new NoSuchElementException("No more data in the Box folder.");
      } else {
        BoxItem i = next;
        next = null;
        fetched++;
        return i;
      }
    }

    /**
     * Calculate a forecast of items available to fetch. Call it on each {@link #nextChunk()}.
     * 
     * @param newValue int
     */
    void available(int newValue) {
      // TODO use explicit values from Box API
      if (available == 0) {
        // magic here as we're in indeterminate progress during the fetching
        // logic based on page bundles we're getting from the drive
        // first page it's 100%, assume the second is filled on 25%
        available = hasNextChunk() ? Math.round(newValue * 1.25f) : newValue;
      } else {
        // All previously set newValue was fetched.
        // Assuming the next page is filled on 25%.
        int newFetched = available;
        available += hasNextChunk() ? Math.round(newValue * 1.25f) : newValue;
        fetched = newFetched;
      }
    }
  }

  protected static final Log    LOG            = ExoLogger.getLogger(BoxAPI.class);

  public static final String    NO_STATE       = "__no_state_set__";

  /**
   * Id of root folder on Box.
   */
  public static final String    BOX_ROOT_ID    = "0";

  /**
   * Not official part of the path used in file services with Box API.
   */
  protected static final String BOX_FILES_PATH = "files/0/f/";

  /**
   * URL prefix for Box files' UI.
   */
  public static final String    BOX_FILE_URL   = "https://app.box.com/" + BOX_FILES_PATH;

  /**
   * URL patter for Embedded UI of Box file. Based on:<br>
   * http://stackoverflow.com/questions/12816239/box-com-embedded-file-folder-viewer-code-via-api
   * http://developers.box.com/box-embed/
   */
  public static final String    BOX_EMBED_URL  = "https://app.box.com/embed_widget/000000000000/%s?show_parent_path=no";

  private BoxToken              token;

  private BoxClient             client;

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

  ItemsIterator getFolderItems(String folderId) throws BoxException {
    return new ItemsIterator(folderId);
  }

  Calendar parseDate(String dateString) {
    Calendar calendar = Calendar.getInstance();
    try {
      Date d = ISO8601DateParser.parse(dateString);
      calendar.setTime(d);
    } catch (ParseException e) {
      LOG.error("Error persing date: " + dateString, e);
    }
    return calendar;
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
}
