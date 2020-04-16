package org.exoplatform.services.entity;


public class Document {
  
  private String name;
  
  private String title;
  
  private String url;
  
  private String parentTitle;
  
  private String mimeType;
  
  private String date;
  
  /**
   * @param name
   * @param title
   * @param url
   * @param parentTitle
   * @param mimeType
   * @param date
   */
  public Document(String name, String title, String url, String parentTitle, String mimeType, String date) {
    this.name = name;
    this.title = title;
    this.url = url;
    this.parentTitle = parentTitle;
    this.mimeType = mimeType;
    this.date = date;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
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
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the parentTitle
   */
  public String getParentTitle() {
    return parentTitle;
  }

  /**
   * @param parentTitle the parentTitle to set
   */
  public void setParentTitle(String parentTitle) {
    this.parentTitle = parentTitle;
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
