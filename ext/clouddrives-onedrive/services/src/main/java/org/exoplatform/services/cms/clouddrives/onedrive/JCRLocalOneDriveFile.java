package org.exoplatform.services.cms.clouddrives.onedrive;

import org.exoplatform.services.cms.clouddrives.jcr.JCRLocalCloudFile;

import javax.jcr.Node;
import java.util.Calendar;

public class JCRLocalOneDriveFile extends JCRLocalCloudFile {

  /**
   * The constant BUSINESS.
   */
  protected static final String BUSINESS     = "business";

  /**
   * The constant EMPTY_STRING.
   */
  protected static final String EMPTY_STRING = "";

  /**
   * The one drive api.
   */
  protected final OneDriveAPI   api;

  /**
   * The Account type.
   */
  protected final String        accountType;

  // FYI transient fields will not appear in serialized forms like JSON object
  // (we don't need to convert it to JSON)
  // on client side
  /** The created date. */
  private transient Calendar    createdDate;

  /** The modified date. */
  private transient Calendar    modifiedDate;

  /** The local modified date. */
  private transient Calendar    localModifiedDate;

  /** The node. */
  private transient Node        node;

  /** The changed. */
  private transient boolean     changed;

  /**
   * Local cloud file or folder (full internal constructor).
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param editLink {@link String}
   * @param previewLink {@link String}
   * @param thumbnailLink {@link String}
   * @param type {@link String}
   * @param typeMode {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param folder {@link Boolean}
   * @param size the size
   * @param node {@link Node}
   * @param changed {@link Boolean}
   * @param api - the one drive api
   * @param accountType
   */
  protected JCRLocalOneDriveFile(String path,
                                 String id,
                                 String title,
                                 String link,
                                 String editLink,
                                 String previewLink,
                                 String thumbnailLink,
                                 String type,
                                 String typeMode,
                                 String lastUser,
                                 String author,
                                 Calendar createdDate,
                                 Calendar modifiedDate,
                                 boolean folder,
                                 long size,
                                 Node node,
                                 boolean changed,
                                 OneDriveAPI api,
                                 String accountType) {
    super(path,
          id,
          title,
          link,
          editLink,
          previewLink,
          thumbnailLink,
          type,
          typeMode,
          lastUser,
          author,
          createdDate,
          modifiedDate,
          folder,
          size,
          node,
          changed);
    this.api = api;
    this.accountType = accountType;
  }

  /**
   * Local cloud file with edit link.
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param editLink {@link String}
   * @param previewLink {@link String}
   * @param thumbnailLink {@link String}
   * @param type {@link String}
   * @param typeMode {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param size the size
   * @param node {@link Node}
   * @param changed {@link Boolean}
   * @param api - the one drive api
   * @param accountType
   */
  public JCRLocalOneDriveFile(String path,
                              String id,
                              String title,
                              String link,
                              String editLink,
                              String previewLink,
                              String thumbnailLink,
                              String type,
                              String typeMode,
                              String lastUser,
                              String author,
                              Calendar createdDate,
                              Calendar modifiedDate,
                              long size,
                              Node node,
                              boolean changed,
                              OneDriveAPI api,
                              String accountType) {
    super(path,
          id,
          title,
          link,
          editLink,
          previewLink,
          thumbnailLink,
          type,
          typeMode,
          lastUser,
          author,
          createdDate,
          modifiedDate,
          size,
          node,
          changed);
    this.api = api;
    this.accountType = accountType;
  }

  /**
   * Local cloud file without edit link.
   *
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param previewLink {@link String}
   * @param thumbnailLink {@link String}
   * @param type {@link String}
   * @param typeMode {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param size the size
   * @param node {@link Node}
   * @param changed {@link Boolean}
   * @param api - the one drive api
   */
  public JCRLocalOneDriveFile(String path,
                              String id,
                              String title,
                              String link,
                              String previewLink,
                              String thumbnailLink,
                              String type,
                              String typeMode,
                              String lastUser,
                              String author,
                              Calendar createdDate,
                              Calendar modifiedDate,
                              long size,
                              Node node,
                              boolean changed,
                              OneDriveAPI api,
                              String accountType) {
    super(path,
          id,
          title,
          link,
          previewLink,
          thumbnailLink,
          type,
          typeMode,
          lastUser,
          author,
          createdDate,
          modifiedDate,
          size,
          node,
          changed);
    this.api = api;
    this.accountType = accountType;
  }

  /**
   * Local cloud folder (without edit, preview, thumbnail links, type mode and
   * size).
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param title {@link String}
   * @param link {@link String}
   * @param type {@link String}
   * @param lastUser {@link String}
   * @param author {@link String}
   * @param createdDate {@link Calendar}
   * @param modifiedDate {@link Calendar}
   * @param node {@link Node}
   * @param changed {@link Boolean}
   * @param api - the one drive api
   * @param accountType
   */
  public JCRLocalOneDriveFile(String path,
                              String id,
                              String title,
                              String link,
                              String type,
                              String lastUser,
                              String author,
                              Calendar createdDate,
                              Calendar modifiedDate,
                              Node node,
                              boolean changed,
                              OneDriveAPI api,
                              String accountType) {
    super(path, id, title, link, type, lastUser, author, createdDate, modifiedDate, node, changed);
    this.api = api;
    this.accountType = accountType;
  }

  /**
   * Gets the preview link.
   *
   * @return the previewLink
   */
  @Override
  public String getPreviewLink() {
    String previewLink;

    if (BUSINESS.equals(accountType)) {
      previewLink = api.getBusinessPreviewLink(this.getId());
    } else {
      // we try to get the personal onedrive file preview link
      previewLink = super.getPreviewLink();

      // if it's empty we get the business onedrive file preview link
      if (previewLink == null || EMPTY_STRING.equals(previewLink)) {
        previewLink = api.getBusinessPreviewLink(this.getId());
      }
    }

    return previewLink;
  }
}
