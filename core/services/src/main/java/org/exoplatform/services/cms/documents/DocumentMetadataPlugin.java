package org.exoplatform.services.cms.documents;

import java.io.InputStream;


/**
 * The Interface DocumentMetadataPlugin is used to add metadata to the documents.
 */
public interface DocumentMetadataPlugin {

  /**
   * Adds metadata to the newly created office document (creator, content type, created).
   * 
   * @param source the source of template file
   * @param extension the extension of the file
   * @param mimeType the mimeType of the file
   * @param creator the name of creator 
   * @return the result stream of the file with correct metadata
   * @throws Exception the exception
   */
  InputStream addMetadata(InputStream source, String extension, String mimeType, String creator) throws Exception;

}
