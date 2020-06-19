package org.exoplatform.wcm.connector.collaboration.editors;

import java.util.List;

import org.exoplatform.services.cms.documents.impl.EditorProvidersHelper.ProviderInfo;

/**
 * The Class PreviewInfo is used to initialize preview of the document.
 */
public class PreviewInfo {
  
  /** The file id. */
  private final String fileId;
  
  /** The providers info. */
  private final List<ProviderInfo> providersInfo;

  /**
   * Instantiates a new preview info.
   *
   * @param fileId the file id
   * @param providersInfo the providers info
   */
  public PreviewInfo(String fileId, List<ProviderInfo> providersInfo) {
    super();
    this.fileId = fileId;
    this.providersInfo = providersInfo;
  }

  /**
   * Gets the file id.
   *
   * @return the file id
   */
  public String getFileId() {
    return fileId;
  }

  /**
   * Gets the providers info.
   *
   * @return the providers info
   */
  public List<ProviderInfo> getProvidersInfo() {
    return providersInfo;
  }

}
