package org.exoplatform.services.rest.transferRules;

/**
 * Created by The eXo Platform
 */
public class TransferRulesStatusModel {

  private String sharedDocumentStatus;

  private String uploadDocumentStatus;

  private String downloadDocumentStatus;

  public String getUploadDocumentStatus() {
    return uploadDocumentStatus;
  }

  public void setUploadDocumentStatus(String uploadDocumentStatus) {
    this.uploadDocumentStatus = uploadDocumentStatus;
  }

  public String getDownloadDocumentStatus() {
    return downloadDocumentStatus;
  }

  public void setDownloadDocumentStatus(String downloadDocumentStatus) {
    this.downloadDocumentStatus = downloadDocumentStatus;
  }

  public String getSharedDocumentStatus() {
    return sharedDocumentStatus;
  }

  public void setSharedDocumentStatus(String sharedDocumentStatus) {
    this.sharedDocumentStatus = sharedDocumentStatus;
  }
}
