package org.exoplatform.services.rest.transferRules;

/**
 * Created by The eXo Platform
 */
public class TransferRulesStatusModel {

  private String sharedDocumentStatus;

  private String downloadDocumentStatus;

  public TransferRulesStatusModel(String sharedDocumentStatus, String downloadDocumentStatus) {
    this.sharedDocumentStatus = sharedDocumentStatus;
    this.downloadDocumentStatus = downloadDocumentStatus;
  }

  public TransferRulesStatusModel(String sharedDocumentStatus) {
    this.sharedDocumentStatus = sharedDocumentStatus;
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
