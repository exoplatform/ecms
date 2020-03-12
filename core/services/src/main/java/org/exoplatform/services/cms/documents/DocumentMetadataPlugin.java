package org.exoplatform.services.cms.documents;

import java.io.InputStream;
import java.util.Date;
import java.util.List;


/**
 * The Interface DocumentMetadataPlugin is used to add metadata to the documents.
 */
public interface DocumentMetadataPlugin {

  /**
   * Updates metadata of the document (creator, content type, created).
   * 
   * @param source the source of template file
   * @param extension the extension of the file
   * @param created the created date of the file
   * @param creator the name of creator 
   * @return the result stream of the file with correct metadata
   * @throws Exception the exception
   */
  InputStream updateMetadata(InputStream source, String extension, Date created, String creator) throws Exception;
  
  
  /**
   * Gets the supported extensions.
   *
   * @return the supported extensions
   */
  List<String> getSupportedExtensions();

}
