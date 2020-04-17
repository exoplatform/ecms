package org.exoplatform.services.entity;


public class Document {
  
  private String id;
  
  private String title;
  
  private String path;
  
  private String drive;
  
  private String mimeType;
  
  private String date;
  
  /**
   * @param id
   * @param title
   * @param path
   * @param drive
   * @param mimeType
   * @param date
   */
  public Document(String id, String title, String path, String drive, String mimeType, String date) {
    super();
    this.id = id;
    this.title = title;
    this.path = path;
    this.drive = drive;
    this.mimeType = mimeType;
    this.date = date;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @param path the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * @return the drive
   */
  public String getDrive() {
    return drive;
  }

  /**
   * @param drive the drive to set
   */
  public void setDrive(String drive) {
    this.drive = drive;
  }

  /**
   * @return the mimeType
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * @param mimeType the mimeType to set
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * @return the date
   */
  public String getDate() {
    return date;
  }

  /**
   * @param date the date to set
   */
  public void setDate(String date) {
    this.date = date;
  }
}
