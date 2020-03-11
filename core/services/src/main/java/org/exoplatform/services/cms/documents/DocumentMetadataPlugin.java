package org.exoplatform.services.cms.documents;

import java.io.InputStream;


/**
 * The Interface DocumentMetadataPlugin is used to add metadata to the documents.
 */
public interface DocumentMetadataPlugin {

  /**
   * Adds the metadata to the document.
   *
   * @param source the source
   * @param template the template
   * @return the byte array input stream
   * @throws Exception the exception
   */
  InputStream addMetadata(InputStream source, DocumentTemplate template) throws Exception;

}
